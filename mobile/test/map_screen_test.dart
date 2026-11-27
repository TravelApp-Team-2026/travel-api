import 'package:flutter/material.dart';
import 'package:flutter_map/flutter_map.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:latlong2/latlong.dart';
import 'package:travel_app/screens/map_screen.dart';
import 'package:travel_app/services/api_exception.dart';
import 'package:travel_app/services/place_search_api.dart';
import 'package:travel_app/services/trips_api.dart';
import 'package:travel_app/theme/app_theme.dart';
import 'package:travel_app/theme/transport_mode.dart';
import 'package:travel_app/widgets/app_map.dart';
import 'package:travel_app/widgets/error_view.dart';
import 'package:travel_app/widgets/map_legend.dart';
import 'package:travel_app/widgets/map_route_layer.dart';

import 'fake_trips_api.dart';
import 'silent_map_tile_provider.dart';

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
  placeName: 'Palma',
  latitude: 39.57,
  longitude: 2.65,
  countryName: 'Hiszpania',
  countryCode: 'ES',
);

const _prague = Place(
  placeName: 'Praga',
  latitude: 50.08,
  longitude: 14.44,
  countryName: 'Czechy',
  countryCode: 'CZ',
);

const _valldemossa = Place(
  placeName: 'Valldemossa',
  latitude: 39.71,
  longitude: 2.62,
  countryName: 'Hiszpania',
  countryCode: 'ES',
);

final _trip = TripDetails(
  id: 1,
  title: 'Europa',
  startDate: DateTime(2026, 8, 12),
  totalDistanceKm: 2000,
  waypoints: const [_legnica, _berlin, _palma, _prague, _valldemossa],
  legs: const [
    TripLeg(
      mode: TransportMode.car,
      geometry: [
        LatLng(51.21, 16.16),
        LatLng(51.9, 15.2),
        LatLng(52.52, 13.40),
      ],
    ),
    TripLeg(mode: TransportMode.plane),
    TripLeg(mode: TransportMode.train),
    TripLeg(mode: TransportMode.other),
  ],
);

final _summary = TripSummary(
  id: 1,
  title: 'Europa',
  startDate: DateTime(2026, 8, 12),
  endDate: null,
  stopCount: 5,
  totalDistanceKm: 2000,
);

Widget _mapApp(FakeTripsApi api, {bool visible = true}) {
  return ProviderScope(
    overrides: [
      silentMapTilesOverride,
      tripsApiProvider.overrideWithValue(api),
    ],
    child: TickerMode(
      enabled: visible,
      child: MaterialApp(theme: AppTheme.light, home: const MapScreen()),
    ),
  );
}

Future<void> _settleMap(WidgetTester tester) async {
  await tester.pump();
  await tester.pump(appMapTileThrottle);
  await tester.pump(appMapTileThrottle);
}

void main() {
  testWidgets('draws each transport style and the legend', (tester) async {
    tester.view.physicalSize = const Size(360, 800);
    tester.view.devicePixelRatio = 1;
    addTearDown(tester.view.resetPhysicalSize);
    addTearDown(tester.view.resetDevicePixelRatio);

    final api = FakeTripsApi([_summary])..detailsById = [_trip];

    await tester.pumpWidget(_mapApp(api));
    await _settleMap(tester);

    expect(find.byType(MapLegend), findsOneWidget);
    expect(find.text('Samochód'), findsOneWidget);
    expect(find.text('Samolot'), findsOneWidget);
    expect(find.text('Pociąg'), findsOneWidget);
    expect(find.text('Inne'), findsOneWidget);

    final layer = tester.widget<PolylineLayer<int>>(
      find.byType(PolylineLayer<int>),
    );
    expect(layer.polylines, hasLength(4));
    expect(layer.polylines[0].points, hasLength(3));
    expect(layer.polylines[0].color, TransportMode.car.color);
    expect(layer.polylines[1].points.length, greaterThan(2));
    expect(layer.polylines[1].color, TransportMode.plane.color);
    expect(layer.polylines[2].points, hasLength(2));
    expect(layer.polylines[2].color, TransportMode.train.color);
    expect(
      layer.polylines[3].pattern,
      StrokePattern.dashed(segments: TransportMode.other.stroke.dashPattern),
    );
    expect(find.byKey(mapTrainTicksKey), findsOneWidget);
    expect(
      tester.widget<IgnorePointer>(find.byKey(mapTrainTicksKey)).ignoring,
      isTrue,
    );
  });

  testWidgets('failed load shows retry', (tester) async {
    final api = FakeTripsApi()..listError = const ApiException('Brak sieci.');

    await tester.pumpWidget(_mapApp(api));
    await _settleMap(tester);

    expect(find.byType(ErrorView), findsOneWidget);
    expect(find.text('Nie udało się wczytać tras'), findsOneWidget);
    expect(find.text('Brak sieci.'), findsOneWidget);
  });

  testWidgets('one failed trip still draws the others', (tester) async {
    final api = FakeTripsApi([
      _summary,
      TripSummary(
        id: 2,
        title: 'Zła',
        startDate: DateTime(2026, 8, 1),
        endDate: null,
        stopCount: 2,
        totalDistanceKm: 1,
      ),
    ])..detailsById = [_trip];

    await tester.pumpWidget(_mapApp(api));
    await _settleMap(tester);

    expect(find.byType(ErrorView), findsNothing);
    final layer = tester.widget<PolylineLayer<int>>(
      find.byType(PolylineLayer<int>),
    );
    expect(layer.polylines, hasLength(4));
  });

  testWidgets('returning to the tab reloads trips', (tester) async {
    final api = FakeTripsApi();

    await tester.pumpWidget(_mapApp(api));
    await _settleMap(tester);
    expect(api.lists, 1);
    expect(
      tester
          .widget<PolylineLayer<int>>(find.byType(PolylineLayer<int>))
          .polylines,
      isEmpty,
    );

    await tester.pumpWidget(_mapApp(api, visible: false));
    await tester.pump();

    api.trips = [_summary];
    api.detailsById = [_trip];

    await tester.pumpWidget(_mapApp(api));
    await _settleMap(tester);

    expect(api.lists, 2);
    expect(
      tester
          .widget<PolylineLayer<int>>(find.byType(PolylineLayer<int>))
          .polylines,
      hasLength(4),
    );
  });

  testWidgets('reload error keeps already drawn routes', (tester) async {
    final api = FakeTripsApi([_summary])..detailsById = [_trip];

    await tester.pumpWidget(_mapApp(api));
    await _settleMap(tester);
    expect(
      tester
          .widget<PolylineLayer<int>>(find.byType(PolylineLayer<int>))
          .polylines,
      hasLength(4),
    );

    await tester.pumpWidget(_mapApp(api, visible: false));
    await tester.pump();
    api.listError = const ApiException('Brak sieci.');

    await tester.pumpWidget(_mapApp(api));
    await _settleMap(tester);

    expect(find.byType(ErrorView), findsNothing);
    expect(
      tester
          .widget<PolylineLayer<int>>(find.byType(PolylineLayer<int>))
          .polylines,
      hasLength(4),
    );
  });
}
