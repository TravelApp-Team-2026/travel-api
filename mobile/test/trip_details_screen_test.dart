import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:travel_app/main.dart';
import 'package:travel_app/services/api_exception.dart';
import 'package:travel_app/services/place_search_api.dart';
import 'package:travel_app/services/token_storage.dart';
import 'package:travel_app/services/trips_api.dart';
import 'package:travel_app/theme/transport_mode.dart';

import 'fake_trips_api.dart';
import 'memory_token_storage.dart';

const _legnica = Place(
  placeName: 'Legnica',
  latitude: 51.21,
  longitude: 16.16,
  countryName: 'Polska',
  countryCode: 'PL',
);

const _berlin = Place(
  placeName: 'Berlin',
  latitude: 52.52,
  longitude: 13.40,
  countryName: 'Niemcy',
  countryCode: 'DE',
);

const _palma = Place(
  placeName: 'Palma de Mallorca',
  latitude: 39.57,
  longitude: 2.65,
  countryName: 'Hiszpania',
  countryCode: 'ES',
);

const _valldemossa = Place(
  placeName: 'Valldemossa',
  latitude: 39.71,
  longitude: 2.62,
  countryName: 'Hiszpania',
  countryCode: 'ES',
);

final _mallorcaSummary = TripSummary(
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

final _mallorca = TripDetails(
  id: 1,
  title: 'Majorka — lato 2026',
  description: 'Tydzień na Majorce: Palma i Sierra de Tramuntana.',
  startDate: DateTime(2026, 8, 12),
  endDate: DateTime(2026, 8, 19),
  totalDistanceKm: 4183,
  waypoints: const [_legnica, _berlin, _palma, _valldemossa, _berlin, _legnica],
  legs: const [
    TripLeg(mode: TransportMode.car, distanceKm: 340),
    TripLeg(mode: TransportMode.plane, distanceKm: 1742),
    TripLeg(mode: TransportMode.car, distanceKm: 21),
    TripLeg(mode: TransportMode.plane, distanceKm: 1741),
    TripLeg(mode: TransportMode.car, distanceKm: 339),
  ],
);

void main() {
  testWidgets('shows title, dates, description and total distance', (
    tester,
  ) async {
    await _openDetails(tester);

    expect(find.text('Majorka — lato 2026'), findsWidgets);
    expect(find.text('12–19 sie 2026'), findsOneWidget);
    expect(
      find.text('Tydzień na Majorce: Palma i Sierra de Tramuntana.'),
      findsOneWidget,
    );
    expect(find.text('4 183 km'), findsWidgets);
    expect(find.text('łączny dystans'), findsOneWidget);
  });

  testWidgets('lists ordered legs with mode and distance', (tester) async {
    await _openDetails(tester);

    expect(find.text('Odcinki'), findsOneWidget);
    expect(find.text('Legnica → Berlin'), findsOneWidget);
    expect(find.text('Berlin → Palma de Mallorca'), findsOneWidget);
    expect(find.text('Palma de Mallorca → Valldemossa'), findsOneWidget);
    expect(find.text('Valldemossa → Berlin'), findsOneWidget);
    expect(find.text('Berlin → Legnica'), findsOneWidget);
    expect(find.text('Samochód'), findsNWidgets(3));
    expect(find.text('Samolot'), findsNWidgets(2));
    expect(find.text('340 km'), findsOneWidget);
    expect(find.text('1 742 km'), findsOneWidget);
    expect(find.text('21 km'), findsOneWidget);
    expect(find.text('1 741 km'), findsOneWidget);
    expect(find.text('339 km'), findsOneWidget);
  });

  testWidgets('edit opens the trip editor', (tester) async {
    await _openDetails(tester);

    await tester.tap(find.byKey(const Key('edit-trip')));
    await tester.pumpAndSettle();

    expect(find.text('Edytuj podróż'), findsOneWidget);
    expect(find.text('Nazwa podróży'), findsOneWidget);

    await tester.tap(
      find.descendant(
        of: find.byType(AppBar),
        matching: find.byIcon(Icons.close),
      ),
    );
    await tester.pumpAndSettle();

    expect(find.text('Odcinki'), findsOneWidget);
    expect(find.text('Edytuj podróż'), findsNothing);
  });

  testWidgets('delete asks for confirmation and returns to the list', (
    tester,
  ) async {
    final api = await _openDetails(tester);

    await tester.tap(find.byKey(const Key('delete-trip')));
    await tester.pumpAndSettle();

    expect(find.text('Usunąć podróż?'), findsOneWidget);
    expect(
      find.text(
        'Podróż „Majorka — lato 2026” wraz z przystankami i odcinkami zostanie '
        'trwale usunięta.',
      ),
      findsOneWidget,
    );

    await tester.tap(find.text('Anuluj'));
    await tester.pumpAndSettle();
    expect(find.text('Odcinki'), findsOneWidget);
    expect(api.lastDeletedId, isNull);

    await tester.tap(find.byKey(const Key('delete-trip')));
    await tester.pumpAndSettle();
    await tester.tap(find.text('Usuń'));
    await tester.pumpAndSettle();

    expect(api.lastDeletedId, 1);
    expect(find.text('Moje podróże'), findsOneWidget);
    expect(find.text('Majorka — lato 2026'), findsNothing);
    expect(find.text('Weekend w Pradze'), findsOneWidget);
  });

  testWidgets('failed load shows retry', (tester) async {
    final api = FakeTripsApi([_mallorcaSummary])
      ..detailsError = const ApiException(
        'Brak połączenia z serwerem. Sprawdź połączenie internetowe.',
      );
    await _pumpTrips(tester, api);
    await tester.tap(find.byKey(const Key('trip-card-1')));
    await tester.pumpAndSettle();

    expect(find.text('Nie udało się wczytać podróży'), findsOneWidget);
    expect(find.text('Spróbuj ponownie'), findsOneWidget);
    expect(find.byKey(const Key('edit-trip')), findsNothing);
    expect(find.byKey(const Key('delete-trip')), findsNothing);

    api.detailsError = null;
    api.details = _mallorca;
    await tester.tap(find.text('Spróbuj ponownie'));
    await tester.pumpAndSettle();

    expect(find.text('Odcinki'), findsOneWidget);
  });
}

Future<FakeTripsApi> _openDetails(WidgetTester tester) async {
  final api = FakeTripsApi([_mallorcaSummary, _prague])..details = _mallorca;
  await _pumpTrips(tester, api);
  await tester.tap(find.byKey(const Key('trip-card-1')));
  await tester.pumpAndSettle();
  return api;
}

void _useView(WidgetTester tester) {
  tester.view.physicalSize = const Size(360, 1200);
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
