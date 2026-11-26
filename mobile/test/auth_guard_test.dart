import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:flutter_map/flutter_map.dart';
import 'package:travel_app/main.dart';
import 'package:travel_app/services/auth_session.dart';
import 'package:travel_app/services/profile_api.dart';
import 'package:travel_app/services/token_storage.dart';
import 'package:travel_app/services/trips_api.dart';

import 'fake_profile_api.dart';
import 'fake_trips_api.dart';
import 'memory_token_storage.dart';
import 'silent_map_tile_provider.dart';

void main() {
  testWidgets('stored token opens the main screen', (tester) async {
    await tester.pumpWidget(
      ProviderScope(
        overrides: [
          silentMapTilesOverride,
          tokenStorageProvider.overrideWithValue(MemoryTokenStorage('jwt')),
          tripsApiProvider.overrideWithValue(FakeTripsApi()),
        ],
        child: const TravelApp(),
      ),
    );
    await tester.pumpAndSettle();

    expect(find.text('Moje podróże'), findsOneWidget);
    expect(find.text('Podróże'), findsOneWidget);
    expect(find.text('Mapa'), findsOneWidget);
    expect(find.text('Statystyki'), findsOneWidget);
    expect(find.text('Profil'), findsOneWidget);
    expect(find.text('Zaloguj się'), findsNothing);
  });

  testWidgets('bottom navigation switches placeholder tabs', (tester) async {
    await tester.pumpWidget(
      ProviderScope(
        overrides: [
          silentMapTilesOverride,
          tokenStorageProvider.overrideWithValue(MemoryTokenStorage('jwt')),
          tripsApiProvider.overrideWithValue(FakeTripsApi()),
          profileApiProvider.overrideWithValue(
            FakeProfileApi(
              const UserProfile(
                id: 1,
                email: 'jan.kowalski@example.com',
                displayName: 'Jan Kowalski',
                bio: null,
                isPublic: false,
              ),
            ),
          ),
        ],
        child: const TravelApp(),
      ),
    );
    await tester.pumpAndSettle();

    await tester.tap(find.text('Mapa'));
    await tester.pumpAndSettle();
    expect(find.text('Mapa'), findsOneWidget);
    expect(find.byType(FlutterMap), findsOneWidget);
    expect(find.text('Moje podróże'), findsNothing);

    await tester.tap(find.text('Statystyki').first);
    await tester.pumpAndSettle();
    expect(find.text('Statystyki'), findsNWidgets(2));

    await tester.tap(find.text('Profil').first);
    await tester.pumpAndSettle();
    expect(find.text('Profil'), findsNWidgets(2));

    await tester.tap(find.text('Podróże'));
    await tester.pumpAndSettle();
    expect(find.text('Moje podróże'), findsOneWidget);
  });

  testWidgets('signing out clears the token and returns to login', (
    tester,
  ) async {
    final storage = MemoryTokenStorage('jwt');
    await tester.pumpWidget(
      ProviderScope(
        overrides: [
          silentMapTilesOverride,
          tokenStorageProvider.overrideWithValue(storage),
          tripsApiProvider.overrideWithValue(FakeTripsApi()),
        ],
        child: const TravelApp(),
      ),
    );
    await tester.pumpAndSettle();

    final context = tester.element(find.text('Moje podróże'));
    await ProviderScope.containerOf(
      context,
    ).read(authSessionProvider.notifier).signOut();
    await tester.pumpAndSettle();

    expect(storage.token, isNull);
    expect(find.text('Zaloguj się'), findsWidgets);
    expect(find.text('Moje podróże'), findsNothing);
  });
}
