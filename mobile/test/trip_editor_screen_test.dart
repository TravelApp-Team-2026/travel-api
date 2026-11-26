import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:go_router/go_router.dart';
import 'package:travel_app/screens/place_search_screen.dart';
import 'package:travel_app/screens/trip_editor_screen.dart';
import 'package:travel_app/services/place_search_api.dart';
import 'package:travel_app/services/trips_api.dart';
import 'package:travel_app/theme/app_theme.dart';
import 'package:travel_app/theme/transport_mode.dart';

import 'fake_place_search_api.dart';
import 'fake_trips_api.dart';

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

const _krakow = Place(
  placeName: 'Kraków',
  latitude: 50.06,
  longitude: 19.94,
  countryName: 'Polska',
  countryCode: 'PL',
);

final _mallorca = TripDetails(
  id: 1,
  title: 'Majorka — lato 2026',
  startDate: DateTime(2026, 8, 12),
  endDate: DateTime(2026, 8, 19),
  totalDistanceKm: 4183,
  waypoints: const [_legnica, _berlin, _palma, _valldemossa, _berlin, _legnica],
  legs: const [
    TripLeg(mode: TransportMode.car),
    TripLeg(mode: TransportMode.plane),
    TripLeg(mode: TransportMode.car),
    TripLeg(mode: TransportMode.plane),
    TripLeg(mode: TransportMode.car),
  ],
);

void main() {
  testWidgets('new trip has title, dates and optional description', (
    tester,
  ) async {
    await _pumpEditor(tester);

    expect(find.text('Nowa podróż'), findsOneWidget);
    expect(find.text('Nazwa podróży'), findsOneWidget);
    expect(find.text('Od'), findsOneWidget);
    expect(find.text('Do (opcjonalnie)'), findsOneWidget);
    expect(find.text('Dodaj opis (opcjonalnie)'), findsOneWidget);
    expect(find.text('Trasa'), findsOneWidget);
    expect(find.text('0 przystanków'), findsOneWidget);
    expect(find.text('+ Dodaj przystanek'), findsOneWidget);
    expect(find.text('Zapisz podróż'), findsOneWidget);

    await tester.tap(find.text('Dodaj opis (opcjonalnie)'));
    await tester.pump();
    expect(find.text('Opis (opcjonalnie)'), findsOneWidget);
  });

  testWidgets('stops from place search are numbered and removable', (
    tester,
  ) async {
    await _pumpEditor(
      tester,
      places: FakePlaceSearchApi(results: [_legnica, _berlin]),
    );

    await _addStop(tester, 'Legnica');
    await _addStop(tester, 'Berlin');

    expect(find.text('1'), findsOneWidget);
    expect(find.text('2'), findsOneWidget);
    expect(find.text('Legnica'), findsOneWidget);
    expect(find.text('Berlin'), findsOneWidget);
    expect(find.text('2 przystanki'), findsOneWidget);
    expect(find.text('Samochód'), findsOneWidget);

    await tester.tap(find.byKey(const Key('remove-stop-0')));
    await tester.pump();

    expect(find.text('Legnica'), findsNothing);
    expect(find.text('Berlin'), findsOneWidget);
    expect(find.text('1'), findsOneWidget);
    expect(find.text('Samochód'), findsNothing);
  });

  testWidgets('transport chip between stops opens the mode sheet', (
    tester,
  ) async {
    await _pumpEditor(
      tester,
      places: FakePlaceSearchApi(results: [_berlin, _palma]),
    );
    await _addStop(tester, 'Berlin');
    await _addStop(tester, 'Palma de Mallorca');

    await tester.tap(find.text('Samochód'));
    await tester.pumpAndSettle();

    expect(find.text('Środek transportu'), findsOneWidget);
    expect(find.text('Odcinek: Berlin → Palma de Mallorca'), findsOneWidget);
    expect(find.text('Pociąg'), findsOneWidget);
    expect(find.text('Inne'), findsOneWidget);

    await tester.tap(find.text('Samolot').last);
    await tester.pumpAndSettle();

    expect(find.text('Środek transportu'), findsNothing);
    expect(find.text('Samolot'), findsOneWidget);
    expect(find.text('Samochód'), findsNothing);
  });

  testWidgets('same screen loads an existing reference trip', (tester) async {
    await _pumpEditor(
      tester,
      trips: FakeTripsApi()..details = _mallorca,
      tripId: 1,
    );

    expect(find.text('Edytuj podróż'), findsOneWidget);
    expect(find.text('Majorka — lato 2026'), findsWidgets);
    expect(find.text('12 sie 2026'), findsOneWidget);
    expect(find.text('19 sie 2026'), findsOneWidget);
    expect(find.text('6 przystanków'), findsOneWidget);
    expect(find.text('Palma de Mallorca'), findsOneWidget);
    expect(find.text('Valldemossa'), findsOneWidget);
    expect(find.text('Samochód'), findsWidgets);
    expect(find.text('Samolot'), findsWidgets);
    await tester.drag(find.byType(CustomScrollView), const Offset(0, -500));
    await tester.pumpAndSettle();
    expect(find.text('Legnica'), findsWidgets);
    expect(find.text('Berlin'), findsWidgets);
  });

  testWidgets('stops can be reordered by dragging the handle', (tester) async {
    await _pumpEditor(
      tester,
      trips: FakeTripsApi()
        ..details = TripDetails(
          id: 2,
          title: 'Test',
          startDate: DateTime(2026, 5, 1),
          totalDistanceKm: 0,
          waypoints: const [_krakow, _berlin, _palma],
          legs: const [
            TripLeg(mode: TransportMode.car),
            TripLeg(mode: TransportMode.plane),
          ],
        ),
      tripId: 2,
    );

    expect(_placeNames(tester), ['Kraków', 'Berlin', 'Palma de Mallorca']);

    await tester.timedDrag(
      find.byKey(const Key('drag-stop-0')),
      const Offset(0, 180),
      const Duration(milliseconds: 400),
    );
    await tester.pumpAndSettle();

    expect(
      _placeNames(tester),
      isNot(['Kraków', 'Berlin', 'Palma de Mallorca']),
    );
  });

  testWidgets('calendar icon opens the date picker', (tester) async {
    await _pumpEditor(tester);

    await tester.tap(find.byKey(const Key('trip-pick-start-date')));
    await tester.pumpAndSettle();

    expect(find.byType(DatePickerDialog), findsOneWidget);
  });

  testWidgets('close is blocked while a save is in flight', (tester) async {
    final trips = FakeTripsApi()..saveDelay = const Duration(milliseconds: 400);
    await _pumpEditor(
      tester,
      trips: trips,
      places: FakePlaceSearchApi(results: [_legnica, _berlin]),
    );

    await tester.enterText(find.byType(TextField).first, 'Śląsk');
    await _addStop(tester, 'Legnica');
    await _addStop(tester, 'Berlin');
    await tester.tap(find.text('Zapisz podróż'));
    await tester.pump();

    final close = find.descendant(
      of: find.byType(AppBar),
      matching: find.byType(IconButton),
    );
    expect(tester.widget<IconButton>(close).onPressed, isNull);

    await tester.tap(close, warnIfMissed: false);
    await tester.pump();
    expect(find.text('Nowa podróż'), findsOneWidget);
    expect(find.text('Zapisuję…'), findsOneWidget);

    await tester.pumpAndSettle();
  });

  testWidgets('save sends the ordered stops and modes', (tester) async {
    final trips = FakeTripsApi();
    await _pumpEditor(
      tester,
      trips: trips,
      places: FakePlaceSearchApi(results: [_legnica, _berlin]),
    );

    await tester.enterText(find.byType(TextField).first, 'Śląsk');
    await _addStop(tester, 'Legnica');
    await _addStop(tester, 'Berlin');
    await tester.tap(find.text('Zapisz podróż'));
    await tester.pumpAndSettle();

    final saved = trips.lastSaved;
    expect(saved, isNotNull);
    expect(saved!.title, 'Śląsk');
    expect(saved.waypoints, [_legnica, _berlin]);
    expect(saved.modes, [TransportMode.car]);
    expect(trips.lastSavedId, isNull);
  });
}

List<String> _placeNames(WidgetTester tester) {
  const wanted = {'Kraków', 'Berlin', 'Palma de Mallorca'};
  return [
    for (final text in tester.widgetList<Text>(find.byType(Text)))
      if (wanted.contains(text.data)) text.data!,
  ];
}

void _useView(WidgetTester tester) {
  tester.view.physicalSize = const Size(360, 1200);
  tester.view.devicePixelRatio = 1;
  addTearDown(tester.view.resetPhysicalSize);
  addTearDown(tester.view.resetDevicePixelRatio);
}

Future<void> _pumpEditor(
  WidgetTester tester, {
  FakeTripsApi? trips,
  FakePlaceSearchApi? places,
  int? tripId,
}) async {
  _useView(tester);
  final router = GoRouter(
    initialLocation: tripId == null ? '/new' : '/$tripId/edit',
    routes: [
      GoRoute(
        path: '/new',
        builder: (context, state) => const TripEditorScreen(),
      ),
      GoRoute(
        path: '/:id/edit',
        builder: (context, state) =>
            TripEditorScreen(tripId: int.parse(state.pathParameters['id']!)),
      ),
      GoRoute(
        path: '/place-search',
        builder: (context, state) => const PlaceSearchScreen(),
      ),
    ],
  );
  addTearDown(router.dispose);
  await tester.pumpWidget(
    ProviderScope(
      overrides: [
        tripsApiProvider.overrideWithValue(trips ?? FakeTripsApi()),
        placeSearchApiProvider.overrideWithValue(
          places ?? FakePlaceSearchApi(),
        ),
      ],
      child: MaterialApp.router(theme: AppTheme.light, routerConfig: router),
    ),
  );
  await tester.pumpAndSettle();
}

Future<void> _addStop(WidgetTester tester, String name) async {
  await tester.tap(find.byKey(const Key('add-stop')));
  await tester.pumpAndSettle();
  await tester.enterText(find.byType(TextField), name);
  await tester.testTextInput.receiveAction(TextInputAction.search);
  await tester.pumpAndSettle();
  await tester.tap(find.text(name).last);
  await tester.pumpAndSettle();
}
