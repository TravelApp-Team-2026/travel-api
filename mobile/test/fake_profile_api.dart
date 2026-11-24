import 'package:dio/dio.dart';
import 'package:travel_app/services/api_client.dart';
import 'package:travel_app/services/api_exception.dart';
import 'package:travel_app/services/profile_api.dart';

class FakeProfileApi extends ProfileApi {
  FakeProfileApi(this.profile) : super(ApiClient(Dio()));

  UserProfile profile;
  int updates = 0;
  String? displayName;
  String? bio;
  bool? isPublic;
  ApiException? updateError;

  @override
  Future<UserProfile> getProfile() async => profile;

  @override
  Future<UserProfile> updateProfile({
    required String displayName,
    required String bio,
    required bool isPublic,
  }) async {
    if (updateError != null) {
      throw updateError!;
    }
    updates++;
    this.displayName = displayName;
    this.bio = bio;
    this.isPublic = isPublic;
    final trimmedBio = bio.trim();
    profile = UserProfile(
      id: profile.id,
      email: profile.email,
      displayName: displayName.trim(),
      bio: trimmedBio.isEmpty ? null : trimmedBio,
      isPublic: isPublic,
    );
    return profile;
  }
}
