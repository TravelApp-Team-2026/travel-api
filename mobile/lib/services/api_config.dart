import 'package:flutter_riverpod/flutter_riverpod.dart';

/// Backend base URL.
///
/// On the Android emulator `localhost` is the emulator itself, not the
/// development machine. `10.0.2.2` is the emulator alias of the host loopback.
///
/// Override when running or building:
/// `flutter run --dart-define=API_BASE_URL=http://10.0.2.2:8080`
class ApiConfig {
  const ApiConfig._();

  static const String baseUrl = String.fromEnvironment(
    'API_BASE_URL',
    defaultValue: 'http://10.0.2.2:8080',
  );
}

final apiBaseUrlProvider = Provider<String>((ref) => ApiConfig.baseUrl);
