import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:travel_app/main.dart';
import 'package:travel_app/services/api_exception.dart';
import 'package:travel_app/services/token_storage.dart';
import 'package:travel_app/services/place_search_api.dart';
import 'package:travel_app/services/trips_api.dart';
import 'package:travel_app/theme/transport_mode.dart';

import 'fake_trips_api.dart';
import 'memory_token_storage.dart';

final _mallorca = TripSummary(
  id: 1,
  title: 'Majorka — lato 2026',
  startDate: DateTime(2026, 8, 12),
  endDate: DateTime(2026, 8, 19),
  stopCount: 6,
  totalDistanceKm: 4183,
);

final _prague = TripSummary(
  id: 2,
  title: 'Weekend w Pradze',
  startDate: DateTime(2026, 7, 4),
  endDate: DateTime(2026, 7, 6),
  stopCount: 3,
  totalDistanceKm: 640,
);

void main() {
  testWidgets('cards show title, date range, stops and distance', (
    tester,
  ) async {
    await _pumpTrips(tester, FakeTripsApi([_mallorca, _prague]));

    expect(find.text('Moje podróże'), findsOneWidget);
    expect(find.text('2 podróże · 4 823 km łącznie'), findsOneWidget);

    expect(find.text('Majorka — lato 2026'), findsOneWidget);
    expect(find.text('12–19 sie 2026'), findsOneWidget);
    expect(find.text('6 przystanków'), findsOneWidget);
    expect(find.text('4 183 km'), findsOneWidget);

    expect(find.text('Weekend w Pradze'), findsOneWidget);
    expect(find.text('4–6 lip 2026'), findsOneWidget);
    expect(find.text('3 przystanki'), findsOneWidget);
    expect(find.text('640 km'), findsOneWidget);
  });

  testWidgets('failed load shows the error state', (tester) async {
    final api = FakeTripsApi()
      ..listError = const ApiException(
        'Brak połączenia z serwerem. Sprawdź połączenie internetowe.',
      );
    await _pumpTrips(tester, api);

    expect(find.text('Nie udało się pobrać danych'), findsOneWidget);
    expect(find.text('Brak połączenia z serwerem'), findsOneWidget);
    expect(
      find.text('Brak połączenia z serwerem. Sprawdź połączenie internetowe.'),
      findsOneWidget,
    );
    expect(find.text('Spróbuj ponownie'), findsOneWidget);
  });

  testWidgets('empty state offers adding the first trip', (tester) async {
    await _pumpTrips(tester, FakeTripsApi());

    expect(find.text('Brak zapisanych podróży'), findsOneWidget);
    expect(find.text('Nie masz jeszcze podróży'), findsOneWidget);
    expect(
      find.text(
        'Dodaj pierwszą podróż, aby zobaczyć ją na mapie '
        'i zacząć budować swoje statystyki.',
      ),
      findsOneWidget,
    );
    expect(find.text('Dodaj pierwszą podróż'), findsOneWidget);
    expect(find.byKey(const Key('add-trip')), findsNothing);

    await tester.tap(find.text('Dodaj pierwszą podróż'));
    await tester.pumpAndSettle();
    expect(find.text('Nowa podróż'), findsOneWidget);
  });

  testWidgets('pull-to-refresh reloads the list', (tester) async {
    final api = FakeTripsApi([_prague]);
    await _pumpTrips(tester, api);

    expect(find.text('Weekend w Pradze'), findsOneWidget);
    expect(api.lists, 1);

    api.trips = [_mallorca];
    await tester.fling(
      find.byType(RefreshIndicator),
      const Offset(0, 400),
      1000,
    );
    await tester.pumpAndSettle();

    expect(api.lists, 2);
    expect(find.text('Majorka — lato 2026'), findsOneWidget);
    expect(find.text('Weekend w Pradze'), findsNothing);
  });

  testWidgets('+ opens the trip editor', (tester) async {
    await _pumpTrips(tester, FakeTripsApi([_prague]));

    await tester.tap(find.byKey(const Key('add-trip')));
    await tester.pumpAndSettle();

    expect(find.text('Nowa podróż'), findsOneWidget);
    expect(find.text('Weekend w Pradze'), findsNothing);
  });

  testWidgets('tapping a card opens trip details', (tester) async {
    await _pumpTrips(
      tester,
      FakeTripsApi([_mallorca, _prague])
        ..details = TripDetails(
          id: 1,
          title: 'Majorka — lato 2026',
          startDate: DateTime(2026, 8, 12),
          endDate: DateTime(2026, 8, 19),
          totalDistanceKm: 4183,
          waypoints: const [
            Place(
              placeName: 'Legnica',
              latitude: 51.21,
              longitude: 16.16,
              countryName: 'Polska',
              countryCode: 'PL',
            ),
            Place(
              placeName: 'Berlin',
              latitude: 52.52,
              longitude: 13.40,
              countryName: 'Niemcy',
              countryCode: 'DE',
            ),
          ],
          legs: const [TripLeg(mode: TransportMode.car, distanceKm: 340)],
        ),
    );

    await tester.tap(find.byKey(const Key('trip-card-1')));
    await tester.pumpAndSettle();

    expect(find.text('Majorka — lato 2026'), findsWidgets);
    expect(find.text('Weekend w Pradze'), findsNothing);
    expect(find.byIcon(Icons.arrow_back), findsOneWidget);
    expect(find.text('Odcinki'), findsOneWidget);
  });

  testWidgets('editing from details reloads the trips list', (tester) async {
    const legnica = Place(
      placeName: 'Legnica',
      latitude: 51.21,
      longitude: 16.16,
      countryName: 'Polska',
      countryCode: 'PL',
    );
    const berlin = Place(
      placeName: 'Berlin',
      latitude: 52.52,
      longitude: 13.40,
      countryName: 'Niemcy',
      countryCode: 'DE',
    );
    final api = FakeTripsApi([_mallorca])
      ..details = TripDetails(
        id: 1,
        title: 'Majorka — lato 2026',
        startDate: DateTime(2026, 8, 12),
        endDate: DateTime(2026, 8, 19),
        totalDistanceKm: 4183,
        waypoints: const [legnica, berlin],
        legs: const [TripLeg(mode: TransportMode.car)],
      );

    await _pumpTrips(tester, api);
    await tester.tap(find.byKey(const Key('trip-card-1')));
    await tester.pumpAndSettle();
    await tester.tap(find.byKey(const Key('edit-trip')));
    await tester.pumpAndSettle();

    await tester.enterText(find.byType(TextField).first, 'Majorka — poprawka');
    await tester.tap(find.text('Zapisz podróż'));
    await tester.pumpAndSettle();
    await tester.tap(find.byIcon(Icons.arrow_back));
    await tester.pumpAndSettle();

    expect(find.text('Majorka — poprawka'), findsOneWidget);
    expect(find.text('Majorka — lato 2026'), findsNothing);
  });
}

void _useView(WidgetTester tester) {
  tester.view.physicalSize = const Size(360, 800);
  tester.view.devicePixelRatio = 1;
  addTearDown(tester.view.resetPhysicalSize);
  addTearDown(tester.view.resetDevicePixelRatio);
}

Future<void> _pumpTrips(WidgetTester tester, FakeTripsApi api) {
  _useView(tester);
  return tester
      .pumpWidget(
        ProviderScope(
          overrides: [
            tokenStorageProvider.overrideWithValue(MemoryTokenStorage('jwt')),
            tripsApiProvider.overrideWithValue(api),
          ],
          child: const TravelApp(),
        ),
      )
      .then((_) => tester.pumpAndSettle());
}
