import 'dart:async';

import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:travel_app/services/token_storage.dart';

enum AuthStatus { unknown, signedIn, signedOut }

class AuthSession extends Notifier<AuthStatus> {
  @override
  AuthStatus build() {
    unawaited(_restore());
    return AuthStatus.unknown;
  }

  Future<void> _restore() async {
    final signedIn = await _hasToken();
    if (!ref.mounted) {
      return;
    }
    state = signedIn ? AuthStatus.signedIn : AuthStatus.signedOut;
  }

  Future<bool> _hasToken() async {
    try {
      final token = await ref.read(tokenStorageProvider).getToken();
      return token != null && token.isNotEmpty;
    } catch (_) {
      return false;
    }
  }

  void markSignedIn() {
    state = AuthStatus.signedIn;
  }

  /// Drops the stored token. A 401 from any request ends here.
  Future<void> signOut() async {
    try {
      await ref.read(tokenStorageProvider).deleteToken();
    } catch (_) {}
    if (!ref.mounted) {
      return;
    }
    state = AuthStatus.signedOut;
  }
}

final authSessionProvider = NotifierProvider<AuthSession, AuthStatus>(
  AuthSession.new,
);
