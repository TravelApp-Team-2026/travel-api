import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:travel_app/screens/auth_validators.dart';
import 'package:travel_app/services/api_exception.dart';
import 'package:travel_app/services/auth_api.dart';
import 'package:travel_app/services/token_storage.dart';
import 'package:travel_app/theme/app_palette.dart';
import 'package:travel_app/widgets/app_button.dart';
import 'package:travel_app/widgets/app_text_field.dart';

class RegisterScreen extends ConsumerStatefulWidget {
  const RegisterScreen({super.key});

  @override
  ConsumerState<RegisterScreen> createState() => _RegisterScreenState();
}

class _RegisterScreenState extends ConsumerState<RegisterScreen> {
  final _formKey = GlobalKey<FormState>();
  final _name = TextEditingController();
  final _email = TextEditingController();
  final _password = TextEditingController();
  final _confirm = TextEditingController();

  var _loading = false;
  var _obscurePassword = true;
  var _obscureConfirm = true;
  String? _nameError;
  String? _emailError;
  String? _passwordError;
  String? _formError;

  @override
  void dispose() {
    _name.dispose();
    _email.dispose();
    _password.dispose();
    _confirm.dispose();
    super.dispose();
  }

  Future<void> _submit() async {
    setState(() {
      _nameError = null;
      _emailError = null;
      _passwordError = null;
      _formError = null;
    });
    if (!_formKey.currentState!.validate()) {
      return;
    }

    setState(() => _loading = true);
    try {
      final api = ref.read(authApiProvider);
      await api.register(
        email: _email.text,
        password: _password.text,
        displayName: _name.text,
      );
      final token = await api.login(
        email: _email.text,
        password: _password.text,
      );
      await ref.read(tokenStorageProvider).saveToken(token);
      if (mounted) {
        context.go('/');
      }
    } on ApiException catch (error) {
      if (!mounted) {
        return;
      }
      setState(() {
        _nameError = error.fieldErrors['displayName'];
        _emailError = error.fieldErrors['email'];
        _passwordError = error.fieldErrors['password'];
        if (error.fieldErrors.isEmpty) {
          _formError = error.message;
        }
      });
    } finally {
      if (mounted) {
        setState(() => _loading = false);
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final colors = theme.colorScheme;

    return Scaffold(
      body: SafeArea(
        child: ListView(
          padding: const EdgeInsets.fromLTRB(24, 12, 24, 24),
          children: [
            Align(
              alignment: Alignment.centerLeft,
              child: IconButton(
                onPressed: _loading ? null : () => context.go('/login'),
                icon: const Icon(Icons.arrow_back),
              ),
            ),
            Text(
              'Utwórz konto',
              style: theme.textTheme.headlineMedium?.copyWith(fontSize: 26),
            ),
            const SizedBox(height: 4),
            Text(
              'Zacznij zapisywać swoje podróże.',
              style: theme.textTheme.bodyMedium?.copyWith(
                color: colors.onSurfaceVariant,
              ),
            ),
            const SizedBox(height: 24),
            Form(
              key: _formKey,
              child: Column(
                children: [
                  AppTextField(
                    label: 'Nazwa wyświetlana',
                    controller: _name,
                    hintText: 'Jan',
                    errorText: _nameError,
                    enabled: !_loading,
                    textInputAction: TextInputAction.next,
                    prefixIcon: const Icon(Icons.person_outline),
                    onChanged: (_) {
                      if (_nameError != null) {
                        setState(() => _nameError = null);
                      }
                    },
                    validator: (value) {
                      if (value == null || value.trim().isEmpty) {
                        return 'Nazwa wyświetlana jest wymagana.';
                      }
                      return null;
                    },
                  ),
                  const SizedBox(height: 16),
                  AppTextField(
                    label: 'E-mail',
                    controller: _email,
                    hintText: 'jan@example.com',
                    errorText: _emailError,
                    enabled: !_loading,
                    keyboardType: TextInputType.emailAddress,
                    textInputAction: TextInputAction.next,
                    prefixIcon: const Icon(Icons.mail_outline),
                    onChanged: (_) {
                      if (_emailError != null) {
                        setState(() => _emailError = null);
                      }
                    },
                    validator: validateEmail,
                  ),
                  const SizedBox(height: 16),
                  AppTextField(
                    label: 'Hasło',
                    controller: _password,
                    errorText: _passwordError,
                    enabled: !_loading,
                    obscureText: _obscurePassword,
                    textInputAction: TextInputAction.next,
                    helperText: 'Minimum 8 znaków',
                    prefixIcon: const Icon(Icons.lock_outline),
                    suffixIcon: IconButton(
                      onPressed: _loading
                          ? null
                          : () => setState(
                              () => _obscurePassword = !_obscurePassword,
                            ),
                      icon: Icon(
                        _obscurePassword
                            ? Icons.visibility_outlined
                            : Icons.visibility_off_outlined,
                      ),
                    ),
                    onChanged: (_) {
                      if (_passwordError != null) {
                        setState(() => _passwordError = null);
                      }
                    },
                    validator: (value) {
                      if (value == null || value.isEmpty) {
                        return 'Hasło jest wymagane.';
                      }
                      if (value.length < 8) {
                        return 'Hasło musi mieć co najmniej 8 znaków.';
                      }
                      return null;
                    },
                  ),
                  const SizedBox(height: 16),
                  AppTextField(
                    label: 'Powtórz hasło',
                    controller: _confirm,
                    enabled: !_loading,
                    obscureText: _obscureConfirm,
                    textInputAction: TextInputAction.done,
                    prefixIcon: const Icon(Icons.lock_outline),
                    suffixIcon: IconButton(
                      onPressed: _loading
                          ? null
                          : () => setState(
                              () => _obscureConfirm = !_obscureConfirm,
                            ),
                      icon: Icon(
                        _obscureConfirm
                            ? Icons.visibility_outlined
                            : Icons.visibility_off_outlined,
                      ),
                    ),
                    validator: (value) {
                      if (value == null || value.isEmpty) {
                        return 'Powtórz hasło.';
                      }
                      if (value != _password.text) {
                        return 'Hasła nie są takie same';
                      }
                      return null;
                    },
                  ),
                  if (_formError != null) ...[
                    const SizedBox(height: 12),
                    Align(
                      alignment: Alignment.centerLeft,
                      child: Text(
                        _formError!,
                        style: theme.textTheme.labelLarge?.copyWith(
                          color: colors.error,
                        ),
                      ),
                    ),
                  ],
                  const SizedBox(height: 24),
                  AppButton(
                    label: 'Utwórz konto',
                    isLoading: _loading,
                    onPressed: _submit,
                  ),
                ],
              ),
            ),
            const SizedBox(height: 28),
            Text(
              'Masz już konto?',
              textAlign: TextAlign.center,
              style: theme.textTheme.bodyMedium?.copyWith(
                color: colors.onSurfaceVariant,
              ),
            ),
            const SizedBox(height: 4),
            TextButton(
              onPressed: _loading ? null : () => context.go('/login'),
              child: Text(
                'Zaloguj się',
                style: theme.textTheme.bodyLarge?.copyWith(
                  color: context.palette.primaryPressed,
                  fontWeight: FontWeight.w700,
                  decoration: TextDecoration.underline,
                  decorationColor: context.palette.primaryPressed,
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }
}
