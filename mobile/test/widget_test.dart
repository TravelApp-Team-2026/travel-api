import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:travel_app/main.dart';
import 'package:travel_app/services/token_storage.dart';

import 'memory_token_storage.dart';
import 'silent_map_tile_provider.dart';

void main() {
  testWidgets('app starts on the login screen', (tester) async {
    await tester.pumpWidget(
      ProviderScope(
        overrides: [
          silentMapTilesOverride,
          tokenStorageProvider.overrideWithValue(MemoryTokenStorage()),
        ],
        child: const TravelApp(),
      ),
    );
    await tester.pumpAndSettle();

    expect(find.text('Zaloguj się'), findsWidgets);
    expect(find.text('E-mail'), findsOneWidget);
    expect(find.text('Hasło'), findsOneWidget);
    expect(find.text('Podróże'), findsNothing);
  });
}
