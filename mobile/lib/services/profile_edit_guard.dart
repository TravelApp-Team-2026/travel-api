import 'package:flutter/foundation.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:travel_app/services/auth_session.dart';

/// Index of the profile branch in the bottom navigation.
const profileBranchIndex = 3;

class ProfileEditGuard {
  const ProfileEditGuard({required this.dirty, required this.discard});

  /// Name or bio differs from the last loaded or saved profile.
  final bool dirty;
  final VoidCallback discard;
}

class ProfileEditGuardNotifier extends Notifier<ProfileEditGuard?> {
  @override
  ProfileEditGuard? build() {
    // Logging out drops the draft. The profile screen is already gone by then.
    ref.watch(authSessionProvider);
    return null;
  }

  void update({required bool dirty, required VoidCallback discard}) {
    final current = state;
    if (current != null && current.dirty == dirty) {
      return;
    }
    state = ProfileEditGuard(dirty: dirty, discard: discard);
  }
}

final profileEditGuardProvider =
    NotifierProvider<ProfileEditGuardNotifier, ProfileEditGuard?>(
      ProfileEditGuardNotifier.new,
    );
