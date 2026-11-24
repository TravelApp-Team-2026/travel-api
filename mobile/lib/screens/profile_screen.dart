import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:travel_app/services/api_exception.dart';
import 'package:travel_app/services/auth_session.dart';
import 'package:travel_app/services/profile_api.dart';
import 'package:travel_app/services/profile_edit_guard.dart';
import 'package:travel_app/theme/app_colors.dart';
import 'package:travel_app/theme/app_palette.dart';
import 'package:travel_app/widgets/app_button.dart';
import 'package:travel_app/widgets/app_text_field.dart';
import 'package:travel_app/widgets/error_view.dart';
import 'package:travel_app/widgets/loading_view.dart';

/// Profile from the mockup: read-only e-mail, editable name and bio,
/// public visibility, save and logout.
class ProfileScreen extends ConsumerStatefulWidget {
  const ProfileScreen({super.key});

  @override
  ConsumerState<ProfileScreen> createState() => _ProfileScreenState();
}

enum _Status { loading, ready, error }

class _ProfileScreenState extends ConsumerState<ProfileScreen> {
  final _formKey = GlobalKey<FormState>();
  final _name = TextEditingController();
  final _bio = TextEditingController();
  final _email = TextEditingController();

  var _status = _Status.loading;
  var _saving = false;
  var _public = false;
  var _savedPublic = false;
  var _request = 0;
  String _savedName = '';
  String _savedBio = '';
  String? _loadError;
  String? _nameError;
  String? _bioError;
  String? _formError;

  bool get _textDirty =>
      _status == _Status.ready &&
      (_name.text.trim() != _savedName || _bio.text.trim() != _savedBio);

  bool get _fieldsDirty => _textDirty || (_status == _Status.ready && _public != _savedPublic);

  @override
  void initState() {
    super.initState();
    _name.addListener(_onDraftChanged);
    _bio.addListener(_onDraftChanged);
    unawaited(_load());
  }

  @override
  void dispose() {
    _name.removeListener(_onDraftChanged);
    _bio.removeListener(_onDraftChanged);
    _name.dispose();
    _bio.dispose();
    _email.dispose();
    super.dispose();
  }

  void _onDraftChanged() {
    _syncGuard();
    if (mounted) {
      setState(() {});
    }
  }

  void _syncGuard() {
    ref.read(profileEditGuardProvider.notifier).update(
      dirty: _textDirty,
      discard: _discardEdits,
    );
  }

  void _discardEdits() {
    if (!mounted) {
      return;
    }
    _name.text = _savedName;
    _bio.text = _savedBio;
    setState(() => _public = _savedPublic);
    _syncGuard();
  }

  Future<void> _load() async {
    final request = ++_request;
    setState(() {
      _status = _Status.loading;
      _loadError = null;
    });
    try {
      final profile = await ref.read(profileApiProvider).getProfile();
      if (!mounted || request != _request) {
        return;
      }
      _apply(profile);
      setState(() => _status = _Status.ready);
      _syncGuard();
    } on ApiException catch (error) {
      if (!mounted || request != _request) {
        return;
      }
      setState(() {
        _status = _Status.error;
        _loadError = error.message;
      });
    }
  }

  void _apply(UserProfile profile) {
    _savedName = profile.displayName.trim();
    _savedBio = (profile.bio ?? '').trim();
    _savedPublic = profile.isPublic;
    _name.text = _savedName;
    _bio.text = _savedBio;
    _email.text = profile.email;
    _public = _savedPublic;
  }

  Future<void> _save() async {
    setState(() {
      _nameError = null;
      _bioError = null;
      _formError = null;
    });
    if (!_formKey.currentState!.validate()) {
      return;
    }
    if (!_fieldsDirty) {
      return;
    }

    setState(() => _saving = true);
    var saved = false;
    try {
      final profile = await ref.read(profileApiProvider).updateProfile(
        displayName: _name.text,
        bio: _bio.text,
        isPublic: _public,
      );
      if (!mounted) {
        return;
      }
      _apply(profile);
      _syncGuard();
      saved = true;
    } on ApiException catch (error) {
      if (!mounted) {
        return;
      }
      setState(() {
        _nameError = error.fieldErrors['displayName'];
        _bioError = error.fieldErrors['bio'];
        _formError = error.fieldErrors.isEmpty ? error.message : null;
      });
    } finally {
      if (mounted) {
        setState(() => _saving = false);
      }
    }
    if (saved && mounted) {
      await showDialog<void>(
        context: context,
        barrierColor: const Color(0x8C3C3836),
        builder: (context) => const _SavedChangesDialog(),
      );
    }
  }

  Future<void> _logout() async {
    final confirmed = await showDialog<bool>(
      context: context,
      barrierColor: const Color(0x8C3C3836),
      builder: (context) => const _LogoutDialog(),
    );
    if (confirmed != true || !mounted) {
      return;
    }
    await ref.read(authSessionProvider.notifier).signOut();
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final colors = theme.colorScheme;

    return Scaffold(
      body: SafeArea(
        bottom: false,
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            Padding(
              padding: const EdgeInsets.fromLTRB(24, 16, 24, 0),
              child: _TitleBlock(theme: theme, colors: colors),
            ),
            const SizedBox(height: 20),
            Expanded(child: _body(theme, colors)),
          ],
        ),
      ),
    );
  }

  Widget _body(ThemeData theme, ColorScheme colors) {
    switch (_status) {
      case _Status.loading:
        return const LoadingView();
      case _Status.error:
        return Column(
          children: [
            Expanded(
              child: ErrorView(
                title: 'Nie udało się wczytać profilu',
                message: _loadError ?? 'Spróbuj ponownie.',
                onRetry: _load,
              ),
            ),
            Padding(
              padding: const EdgeInsets.fromLTRB(16, 0, 16, 16),
              child: _LogoutButton(onPressed: _logout),
            ),
          ],
        );
      case _Status.ready:
        return _form(theme, colors);
    }
  }

  Widget _form(ThemeData theme, ColorScheme colors) {
    final iconColor = AppColors.placeholder;

    return Form(
      key: _formKey,
      child: ListView(
        padding: const EdgeInsets.fromLTRB(16, 0, 16, 24),
        children: [
          _IdentityHeader(name: _name.text),
          const SizedBox(height: 20),
          AppTextField(
            label: 'Nazwa wyświetlana',
            controller: _name,
            errorText: _nameError,
            enabled: !_saving,
            maxLength: 50,
            textInputAction: TextInputAction.next,
            prefixIcon: Icon(Icons.person_outline, color: iconColor, size: 20),
            onChanged: (_) {
              if (_nameError != null) {
                setState(() => _nameError = null);
              }
            },
            validator: _validateName,
          ),
          const SizedBox(height: 16),
          AppTextField(
            label: 'O mnie',
            controller: _bio,
            errorText: _bioError,
            enabled: !_saving,
            maxLines: 3,
            maxLength: 300,
            counterInside: true,
            keyboardType: TextInputType.multiline,
            textInputAction: TextInputAction.newline,
            onChanged: (_) {
              if (_bioError != null) {
                setState(() => _bioError = null);
              }
            },
          ),
          const SizedBox(height: 16),
          AppTextField(
            label: 'E-mail',
            controller: _email,
            readOnly: true,
            fillColor: colors.surfaceContainerHigh,
            textColor: colors.onSurfaceVariant,
            prefixIcon: Icon(Icons.mail_outline, color: iconColor, size: 20),
            suffixIcon: Icon(Icons.lock_outline, color: iconColor, size: 18),
          ),
          const SizedBox(height: 16),
          _PublicProfileCard(
            value: _public,
            onChanged: _saving
                ? null
                : (value) {
                    setState(() => _public = value);
                    _syncGuard();
                  },
          ),
          if (_formError != null) ...[
            const SizedBox(height: 12),
            Text(
              _formError!,
              style: theme.textTheme.labelLarge?.copyWith(color: colors.error),
            ),
          ],
          const SizedBox(height: 20),
          AppButton(
            label: 'Zapisz zmiany',
            isLoading: _saving,
            onPressed: _save,
          ),
          const SizedBox(height: 14),
          _LogoutButton(onPressed: _saving ? null : _logout),
        ],
      ),
    );
  }

  String? _validateName(String? value) {
    final name = value?.trim() ?? '';
    if (name.isEmpty) {
      return 'Nazwa wyświetlana nie może być pusta.';
    }
    if (name.length > 50) {
      return 'Nazwa wyświetlana może mieć maksymalnie 50 znaków.';
    }
    return null;
  }
}

class _TitleBlock extends StatelessWidget {
  const _TitleBlock({required this.theme, required this.colors});

  final ThemeData theme;
  final ColorScheme colors;

  @override
  Widget build(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text('Profil', style: theme.textTheme.headlineMedium),
        const SizedBox(height: 4),
        Text(
          'Konto i prywatność',
          style: theme.textTheme.bodySmall?.copyWith(
            color: colors.onSurfaceVariant,
          ),
        ),
      ],
    );
  }
}

class _IdentityHeader extends StatelessWidget {
  const _IdentityHeader({required this.name});

  final String name;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final colors = theme.colorScheme;
    final shown = name.trim().isEmpty ? ' ' : name.trim();

    return Row(
      children: [
        CircleAvatar(
          radius: 32,
          backgroundColor: colors.primary,
          child: Text(
            _initials(shown),
            style: theme.textTheme.headlineMedium?.copyWith(
              color: colors.onPrimary,
              fontSize: 24,
            ),
          ),
        ),
        const SizedBox(width: 16),
        Expanded(
          child: Text(shown, style: theme.textTheme.titleMedium),
        ),
      ],
    );
  }
}

String _initials(String name) {
  final parts = name
      .trim()
      .split(RegExp(r'\s+'))
      .where((part) => part.isNotEmpty)
      .toList();
  if (parts.isEmpty) {
    return '?';
  }
  if (parts.length == 1) {
    final word = parts.first;
    final count = word.characters.length >= 2 ? 2 : 1;
    return word.characters.take(count).toString().toUpperCase();
  }
  return '${parts.first.characters.first}${parts.last.characters.first}'
      .toUpperCase();
}

class _PublicProfileCard extends StatelessWidget {
  const _PublicProfileCard({required this.value, required this.onChanged});

  final bool value;
  final ValueChanged<bool>? onChanged;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final colors = theme.colorScheme;

    return DecoratedBox(
      decoration: BoxDecoration(
        color: colors.surface,
        borderRadius: BorderRadius.circular(16),
        border: Border.all(color: colors.outline, width: 1.5),
      ),
      child: Padding(
        padding: const EdgeInsets.fromLTRB(16, 14, 12, 14),
        child: Row(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    'Profil publiczny',
                    style: theme.textTheme.bodyLarge?.copyWith(
                      fontWeight: FontWeight.w700,
                    ),
                  ),
                  const SizedBox(height: 4),
                  Text(
                    'Inni użytkownicy mogą oglądać Twoje podróże i statystyki. '
                    'Adres e-mail nigdy nie jest widoczny.',
                    style: theme.textTheme.labelLarge?.copyWith(
                      color: colors.onSurfaceVariant,
                      fontWeight: FontWeight.w400,
                    ),
                  ),
                ],
              ),
            ),
            const SizedBox(width: 12),
            _PublicSwitch(value: value, onChanged: onChanged),
          ],
        ),
      ),
    );
  }
}

class _PublicSwitch extends StatelessWidget {
  const _PublicSwitch({required this.value, required this.onChanged});

  final bool value;
  final ValueChanged<bool>? onChanged;

  @override
  Widget build(BuildContext context) {
    return Semantics(
      button: true,
      toggled: value,
      label: 'Profil publiczny',
      child: GestureDetector(
        key: const Key('public-profile-switch'),
        behavior: HitTestBehavior.opaque,
        onTap: onChanged == null ? null : () => onChanged!(!value),
        child: Padding(
          padding: const EdgeInsets.only(top: 2),
          child: AnimatedContainer(
            duration: const Duration(milliseconds: 150),
            width: 44,
            height: 26,
            padding: const EdgeInsets.all(3),
            decoration: BoxDecoration(
              color: value ? AppColors.primary : AppColors.disabled,
              borderRadius: BorderRadius.circular(13),
            ),
            child: AnimatedAlign(
              duration: const Duration(milliseconds: 150),
              alignment: value ? Alignment.centerRight : Alignment.centerLeft,
              child: Container(
                width: 20,
                height: 20,
                decoration: const BoxDecoration(
                  color: AppColors.onPrimary,
                  shape: BoxShape.circle,
                ),
              ),
            ),
          ),
        ),
      ),
    );
  }
}

class _LogoutButton extends StatelessWidget {
  const _LogoutButton({required this.onPressed});

  final VoidCallback? onPressed;

  @override
  Widget build(BuildContext context) {
    return OutlinedButton(
      onPressed: onPressed,
      style: OutlinedButton.styleFrom(
        foregroundColor: AppColors.danger,
        disabledForegroundColor: AppColors.placeholder,
        side: BorderSide(
          color: onPressed == null ? AppColors.disabled : AppColors.danger,
          width: 1.8,
        ),
        minimumSize: const Size.fromHeight(48),
        shape: const RoundedRectangleBorder(
          borderRadius: BorderRadius.all(Radius.circular(14)),
        ),
        textStyle: context.textRoles.button,
      ),
      child: const Text('Wyloguj się'),
    );
  }
}

/// Asks whether to drop unsaved name or bio edits.
///
/// The layout follows the logout and delete dialogs. There is no separate
/// mockup for this question.
Future<bool> confirmDiscardProfileChanges(BuildContext context) async {
  final leave = await showDialog<bool>(
    context: context,
    barrierColor: const Color(0x8C3C3836),
    builder: (context) => const _UnsavedChangesDialog(),
  );
  return leave == true;
}

class _SavedChangesDialog extends StatelessWidget {
  const _SavedChangesDialog();

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final colors = theme.colorScheme;

    return Dialog(
      backgroundColor: colors.surface,
      insetPadding: const EdgeInsets.symmetric(horizontal: 32),
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(24)),
      child: Padding(
        padding: const EdgeInsets.fromLTRB(16, 24, 16, 16),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            CircleAvatar(
              radius: 28,
              backgroundColor: colors.primaryContainer,
              child: Icon(
                Icons.edit_outlined,
                color: context.palette.primaryPressed,
                size: 28,
              ),
            ),
            const SizedBox(height: 16),
            Text(
              'Zmiany zapisane!',
              textAlign: TextAlign.center,
              style: theme.textTheme.titleMedium,
            ),
            const SizedBox(height: 20),
            Row(
              children: [
                Expanded(
                  child: _DialogButton(
                    label: 'Ok',
                    filled: true,
                    fillColor: AppColors.primary,
                    onPressed: () => Navigator.pop(context),
                  ),
                ),
              ],
            ),
          ],
        ),
      ),
    );
  }
}

class _UnsavedChangesDialog extends StatelessWidget {
  const _UnsavedChangesDialog();

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final colors = theme.colorScheme;

    return Dialog(
      backgroundColor: colors.surface,
      insetPadding: const EdgeInsets.symmetric(horizontal: 32),
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(24)),
      child: Padding(
        padding: const EdgeInsets.fromLTRB(16, 24, 16, 16),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            CircleAvatar(
              radius: 28,
              backgroundColor: colors.primaryContainer,
              child: Icon(
                Icons.edit_outlined,
                color: context.palette.primaryPressed,
                size: 28,
              ),
            ),
            const SizedBox(height: 16),
            Text(
              'Masz niezapisane zmiany',
              textAlign: TextAlign.center,
              style: theme.textTheme.titleMedium,
            ),
            const SizedBox(height: 8),
            Text(
              'Czy jesteś pewien że chcesz je anulować?',
              textAlign: TextAlign.center,
              style: theme.textTheme.bodySmall?.copyWith(
                color: colors.onSurfaceVariant,
              ),
            ),
            const SizedBox(height: 20),
            Row(
              children: [
                Expanded(
                  child: _DialogButton(
                    label: 'Nie',
                    filled: false,
                    onPressed: () => Navigator.pop(context, false),
                  ),
                ),
                const SizedBox(width: 8),
                Expanded(
                  child: _DialogButton(
                    label: 'Tak',
                    filled: true,
                    fillColor: AppColors.primary,
                    onPressed: () => Navigator.pop(context, true),
                  ),
                ),
              ],
            ),
          ],
        ),
      ),
    );
  }
}

class _LogoutDialog extends StatelessWidget {
  const _LogoutDialog();

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final colors = theme.colorScheme;

    return Dialog(
      backgroundColor: colors.surface,
      insetPadding: const EdgeInsets.symmetric(horizontal: 32),
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(24)),
      child: Padding(
        padding: const EdgeInsets.fromLTRB(16, 24, 16, 16),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            CircleAvatar(
              radius: 28,
              backgroundColor: colors.primaryContainer,
              child: Icon(
                Icons.person_outline,
                color: context.palette.primaryPressed,
                size: 28,
              ),
            ),
            const SizedBox(height: 16),
            Text('Wylogować się?', style: theme.textTheme.titleMedium),
            const SizedBox(height: 8),
            Text(
              'Zostaniesz wylogowany z tego urządzenia.\n'
              'Twoje podróże pozostaną na koncie.',
              textAlign: TextAlign.center,
              style: theme.textTheme.bodySmall?.copyWith(
                color: colors.onSurfaceVariant,
              ),
            ),
            const SizedBox(height: 20),
            Row(
              children: [
                Expanded(
                  child: _DialogButton(
                    label: 'Anuluj',
                    filled: false,
                    onPressed: () => Navigator.pop(context, false),
                  ),
                ),
                const SizedBox(width: 8),
                Expanded(
                  child: _DialogButton(
                    label: 'Wyloguj',
                    filled: true,
                    onPressed: () => Navigator.pop(context, true),
                  ),
                ),
              ],
            ),
          ],
        ),
      ),
    );
  }
}

class _DialogButton extends StatelessWidget {
  const _DialogButton({
    required this.label,
    required this.filled,
    required this.onPressed,
    this.fillColor,
  });

  final String label;
  final bool filled;
  final VoidCallback onPressed;
  final Color? fillColor;

  @override
  Widget build(BuildContext context) {
    final color = fillColor ?? AppColors.danger;
    final style = OutlinedButton.styleFrom(
      foregroundColor: filled ? AppColors.onPrimary : AppColors.text,
      backgroundColor: filled ? color : Colors.transparent,
      side: BorderSide(
        color: filled ? color : AppColors.border,
        width: 1.8,
      ),
      minimumSize: const Size.fromHeight(48),
      shape: const RoundedRectangleBorder(
        borderRadius: BorderRadius.all(Radius.circular(14)),
      ),
      textStyle: context.textRoles.button,
    );

    return OutlinedButton(
      onPressed: onPressed,
      style: style,
      child: Text(label),
    );
  }
}
