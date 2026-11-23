import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:travel_app/services/api_client.dart';
import 'package:travel_app/services/api_exception.dart';

class AuthApi {
  AuthApi(this._client);

  final ApiClient _client;

  Future<void> register({
    required String email,
    required String password,
    required String displayName,
  }) async {
    await _client.post<dynamic>(
      '/api/v1/auth/register',
      data: {
        'email': email.trim(),
        'password': password,
        'displayName': displayName.trim(),
      },
    );
  }

  /// Expects `{ "token": "<jwt>" }`. `accessToken` is accepted as a fallback.
  Future<String> login({
    required String email,
    required String password,
  }) async {
    final response = await _client.post<dynamic>(
      '/api/v1/auth/login',
      data: {
        'email': email.trim(),
        'password': password,
      },
    );
    return _readToken(response.data);
  }

  String _readToken(Object? data) {
    if (data is Map) {
      final token = data['token'] ?? data['accessToken'];
      if (token is String && token.isNotEmpty) {
        return token;
      }
    }
    throw const ApiException('Logowanie nie zwróciło tokenu.');
  }
}

final authApiProvider = Provider<AuthApi>(
  (ref) => AuthApi(ref.watch(apiClientProvider)),
);
