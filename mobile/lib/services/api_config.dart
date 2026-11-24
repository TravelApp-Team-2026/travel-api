import 'package:flutter_riverpod/flutter_riverpod.dart';

/// Backend base URL.
///
/// Default is this computer, so the Windows desktop app reaches the local
/// backend. The Android emulator is not this computer: there `localhost` is
/// the emulator itself, and the host is `10.0.2.2`.
///
/// Override when running or building:
/// `flutter run --dart-define=API_BASE_URL=http://10.0.2.2:8080`
class ApiConfig {
  const ApiConfig._();

  static const String baseUrl = String.fromEnvironment(
    'API_BASE_URL',
    defaultValue: 'http://127.0.0.1:8080',
  );
}

final apiBaseUrlProvider = Provider<String>((ref) => ApiConfig.baseUrl);
