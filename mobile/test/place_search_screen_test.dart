import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:go_router/go_router.dart';
import 'package:travel_app/screens/place_search_screen.dart';
import 'package:travel_app/services/api_exception.dart';
import 'package:travel_app/services/place_search_api.dart';
import 'package:travel_app/theme/app_theme.dart';

import 'fake_place_search_api.dart';

const _berlin = Place(
  placeName: 'Berlin',
  latitude: 52.52,
  longitude: 13.40,
  countryName: 'Niemcy',
  countryCode: 'DE',
);

const _krakow = Place(
  placeName: 'Kraków',
  latitude: 50.06,
  longitude: 19.94,
  countryName: 'Polska',
  countryCode: 'PL',
);

void main() {
  testWidgets('search runs on submit, not on each character', (tester) async {
    final api = FakePlaceSearchApi(results: [_berlin]);
    await _pumpSearch(tester, api);

    await tester.enterText(find.byType(TextField), 'Ber');
    await tester.pump();
    await tester.enterText(find.byType(TextField), 'Berlin');
    await tester.pump();

    expect(api.queries, isEmpty);

    await tester.testTextInput.receiveAction(TextInputAction.search);
    await tester.pumpAndSettle();

    expect(api.queries, ['Berlin']);
  });

  testWidgets('results show place name and country', (tester) async {
    final api = FakePlaceSearchApi(results: [_berlin, _krakow]);
    await _pumpSearch(tester, api);
    await _submit(tester, 'Berlin');

    expect(find.text('WYNIKI DLA „BERLIN”'), findsOneWidget);
    expect(find.text('Berlin'), findsWidgets);
    expect(find.text('Niemcy'), findsOneWidget);
    expect(find.text('Kraków'), findsOneWidget);
    expect(find.text('Polska'), findsOneWidget);
  });

  testWidgets('selecting a result pops a typed Place through go_router', (
    tester,
  ) async {
    final api = FakePlaceSearchApi(results: [_berlin, _krakow]);
    late Future<Place?> selected;
    await _pumpSearchOnRouter(tester, api, onOpen: (context) {
      selected = context.push<Place>('/place-search');
    });

    await tester.tap(find.text('open'));
    await tester.pumpAndSettle();
    await _submit(tester, 'Berlin');
    await tester.tap(find.text('Kraków'));
    await tester.pumpAndSettle();

    expect(await selected, _krakow);
    expect((await selected)?.countryCode, 'PL');
    expect(find.text('open'), findsOneWidget);
  });

  testWidgets('selecting a result also notifies onSelected', (tester) async {
    final api = FakePlaceSearchApi(results: [_berlin, _krakow]);
    Place? selected;
    await _pumpSearch(tester, api, onSelected: (place) => selected = place);
    await _submit(tester, 'Berlin');

    await tester.tap(find.text('Kraków'));
    await tester.pumpAndSettle();

    expect(selected, _krakow);
    expect(selected?.latitude, 50.06);
    expect(selected?.longitude, 19.94);
    expect(selected?.countryCode, 'PL');
  });

  testWidgets('loading state is shown while the provider answers', (
    tester,
  ) async {
    final api = FakePlaceSearchApi(results: [_berlin])
      ..delay = const Duration(milliseconds: 200);
    await _pumpSearch(tester, api);

    await tester.enterText(find.byType(TextField), 'Berlin');
    await tester.testTextInput.receiveAction(TextInputAction.search);
    await tester.pump();

    expect(find.text('SZUKAM…'), findsOneWidget);
    expect(find.byType(CircularProgressIndicator), findsOneWidget);

    await tester.pumpAndSettle();
    expect(find.text('SZUKAM…'), findsNothing);
    expect(find.text('Niemcy'), findsOneWidget);
  });

  testWidgets('empty list shows the no-results state', (tester) async {
    final api = FakePlaceSearchApi();
    await _pumpSearch(tester, api);
    await _submit(tester, 'Berliinn');

    expect(find.text('Brak wyników dla „Berliinn”'), findsOneWidget);
    expect(
      find.text(
        'Sprawdź pisownię albo wpisz pełną nazwę miejsca, np. miasto i kraj.',
      ),
      findsOneWidget,
    );
    expect(find.text('Berlin'), findsNothing);
    expect(find.text('Dane: © współtwórcy OpenStreetMap'), findsOneWidget);
  });

  testWidgets('error state retries with the current field text', (tester) async {
    const unavailable =
        'Wyszukiwarka miejsc jest chwilowo niedostępna. Spróbuj ponownie później.';
    final api = FakePlaceSearchApi()..error = const ApiException(unavailable);
    await _pumpSearch(tester, api);
    await _submit(tester, 'Berlin');

    expect(find.text('Nie udało się wyszukać miejsca'), findsOneWidget);
    expect(find.text(unavailable), findsOneWidget);
    expect(api.queries, ['Berlin']);

    api
      ..error = null
      ..results = [_krakow];
    await tester.enterText(find.byType(TextField), 'Kraków');
    await tester.pump();
    await tester.tap(find.text('Spróbuj ponownie'));
    await tester.pumpAndSettle();

    expect(api.queries, ['Berlin', 'Kraków']);
    expect(find.text('Nie udało się wyszukać miejsca'), findsNothing);
    expect(find.text('Polska'), findsOneWidget);
  });
}

void _useView(WidgetTester tester) {
  tester.view.physicalSize = const Size(360, 800);
  tester.view.devicePixelRatio = 1;
  addTearDown(tester.view.resetPhysicalSize);
  addTearDown(tester.view.resetDevicePixelRatio);
}

Future<void> _pumpSearch(
  WidgetTester tester,
  FakePlaceSearchApi api, {
  ValueChanged<Place>? onSelected,
}) {
  _useView(tester);
  return tester.pumpWidget(
    ProviderScope(
      overrides: [placeSearchApiProvider.overrideWithValue(api)],
      child: MaterialApp(
        theme: AppTheme.light,
        home: PlaceSearchScreen(onSelected: onSelected),
      ),
    ),
  );
}

Future<void> _pumpSearchOnRouter(
  WidgetTester tester,
  FakePlaceSearchApi api, {
  required void Function(BuildContext context) onOpen,
}) {
  _useView(tester);
  final router = GoRouter(
    initialLocation: '/start',
    routes: [
      GoRoute(
        path: '/start',
        builder: (context, state) => Scaffold(
          body: TextButton(
            onPressed: () => onOpen(context),
            child: const Text('open'),
          ),
        ),
      ),
      GoRoute(
        path: '/place-search',
        builder: (context, state) => const PlaceSearchScreen(),
      ),
    ],
  );
  addTearDown(router.dispose);
  return tester.pumpWidget(
    ProviderScope(
      overrides: [placeSearchApiProvider.overrideWithValue(api)],
      child: MaterialApp.router(theme: AppTheme.light, routerConfig: router),
    ),
  );
}

Future<void> _submit(WidgetTester tester, String query) async {
  await tester.enterText(find.byType(TextField), query);
  await tester.testTextInput.receiveAction(TextInputAction.search);
  await tester.pumpAndSettle();
}
