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

class LoginScreen extends ConsumerStatefulWidget {
  const LoginScreen({super.key});

  @override
  ConsumerState<LoginScreen> createState() => _LoginScreenState();
}

class _LoginScreenState extends ConsumerState<LoginScreen> {
  final _formKey = GlobalKey<FormState>();
  final _email = TextEditingController();
  final _password = TextEditingController();

  var _loading = false;
  var _obscurePassword = true;
  String? _emailError;
  String? _passwordError;

  @override
  void dispose() {
    _email.dispose();
    _password.dispose();
    super.dispose();
  }

  Future<void> _submit() async {
    setState(() {
      _emailError = null;
      _passwordError = null;
    });
    if (!_formKey.currentState!.validate()) {
      return;
    }

    setState(() => _loading = true);
    try {
      final token = await ref.read(authApiProvider).login(
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
        _emailError = error.fieldErrors['email'];
        _passwordError =
            error.fieldErrors['password'] ??
            (error.fieldErrors.isEmpty ? error.message : null);
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
          padding: const EdgeInsets.fromLTRB(24, 48, 24, 24),
          children: [
            Row(
              children: [
                Container(
                  width: 48,
                  height: 48,
                  decoration: BoxDecoration(
                    color: colors.primary,
                    borderRadius: BorderRadius.circular(14),
                  ),
                  child: Icon(Icons.location_on, color: colors.onPrimary),
                ),
                const SizedBox(width: 12),
                Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text('Travel Api', style: theme.textTheme.headlineSmall),
                    Text(
                      'Twój osobisty dziennik podróży',
                      style: theme.textTheme.bodySmall?.copyWith(
                        color: colors.onSurfaceVariant,
                      ),
                    ),
                  ],
                ),
              ],
            ),
            const SizedBox(height: 36),
            Text('Zaloguj się', style: theme.textTheme.titleLarge),
            const SizedBox(height: 20),
            Form(
              key: _formKey,
              child: Column(
                children: [
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
                    hintText: 'Wpisz hasło',
                    errorText: _passwordError,
                    enabled: !_loading,
                    obscureText: _obscurePassword,
                    textInputAction: TextInputAction.done,
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
                    validator: validateRequiredPassword,
                  ),
                  const SizedBox(height: 24),
                  AppButton(
                    label: 'Zaloguj się',
                    isLoading: _loading,
                    onPressed: _submit,
                  ),
                ],
              ),
            ),
            const SizedBox(height: 28),
            Text(
              'Nie masz konta?',
              textAlign: TextAlign.center,
              style: theme.textTheme.bodyMedium?.copyWith(
                color: colors.onSurfaceVariant,
              ),
            ),
            const SizedBox(height: 4),
            TextButton(
              onPressed: _loading ? null : () => context.go('/register'),
              child: Text(
                'Zarejestruj się',
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
