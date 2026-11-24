import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:travel_app/services/api_client.dart';
import 'package:travel_app/services/api_exception.dart';

class UserProfile {
  const UserProfile({
    required this.id,
    required this.email,
    required this.displayName,
    required this.bio,
    required this.isPublic,
  });

  final int id;
  final String email;
  final String displayName;
  final String? bio;
  final bool isPublic;

  factory UserProfile.fromJson(Object? data) {
    if (data is! Map) {
      throw const ApiException('Serwer zwrócił nieprawidłowy profil.');
    }
    final id = data['id'];
    final email = data['email'];
    final displayName = data['displayName'];
    final bio = data['bio'];
    final isPublic = data['isPublic'];
    if (id is! num ||
        email is! String ||
        displayName is! String ||
        isPublic is! bool ||
        (bio != null && bio is! String)) {
      throw const ApiException('Serwer zwrócił nieprawidłowy profil.');
    }
    return UserProfile(
      id: id.toInt(),
      email: email,
      displayName: displayName,
      bio: bio as String?,
      isPublic: isPublic,
    );
  }
}

class ProfileApi {
  ProfileApi(this._client);

  final ApiClient _client;

  Future<UserProfile> getProfile() async {
    final response = await _client.get<dynamic>('/api/v1/users/me');
    return UserProfile.fromJson(response.data);
  }

  /// Partial update of the editable fields. The e-mail is never sent.
  Future<UserProfile> updateProfile({
    required String displayName,
    required String bio,
    required bool isPublic,
  }) async {
    final response = await _client.patch<dynamic>(
      '/api/v1/users/me',
      data: {
        'displayName': displayName.trim(),
        'bio': bio.trim(),
        'isPublic': isPublic,
      },
    );
    return UserProfile.fromJson(response.data);
  }
}

final profileApiProvider = Provider<ProfileApi>(
  (ref) => ProfileApi(ref.watch(apiClientProvider)),
);
