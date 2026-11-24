import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import 'package:travel_app/services/token_storage.dart';

class MemoryTokenStorage extends TokenStorage {
  MemoryTokenStorage([this.token]) : super(const FlutterSecureStorage());

  String? token;

  @override
  Future<void> saveToken(String value) async {
    token = value;
  }

  @override
  Future<String?> getToken() async => token;

  @override
  Future<void> deleteToken() async {
    token = null;
  }
}
