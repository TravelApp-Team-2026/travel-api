import 'dart:math' as math;

import 'package:flutter_map/flutter_map.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:latlong2/latlong.dart';
import 'package:travel_app/services/place_search_api.dart';
import 'package:travel_app/services/trips_api.dart';
import 'package:travel_app/theme/transport_mode.dart';
import 'package:travel_app/widgets/map_geometry.dart';
import 'package:travel_app/widgets/map_route_layer.dart';

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

const _prague = Place(
  placeName: 'Praga',
  latitude: 50.08,
  longitude: 14.44,
  countryName: 'Czechy',
  countryCode: 'CZ',
);

void main() {
  test('simplifyLine keeps the shape and drops colinear points', () {
    const a = LatLng(51.0, 16.0);
    const b = LatLng(52.0, 17.0);
    expect(simplifyLine([a, b]), [a, b]);

    final colinear = [
      for (var i = 0; i <= 200; i++) LatLng(51.0 + i * 0.001, 16.0 + i * 0.001),
    ];
    final simplified = simplifyLine(colinear);
    expect(simplified.length, lessThan(8));
    expect(simplified.first, colinear.first);
    expect(simplified.last, colinear.last);
  });

  test('planeArc has endpoints and extra points', () {
    const from = LatLng(52.52, 13.40);
    const to = LatLng(39.57, 2.65);
    final arc = planeArc(from, to);
    expect(arc.length, greaterThan(2));
    expect(arc.first, from);
    expect(arc.last.latitude, closeTo(to.latitude, 0.0001));
    expect(wrapLongitude(arc.last.longitude), closeTo(to.longitude, 0.0001));
    expect(arc[1].latitude, isNot(from.latitude));
  });

  test('planeArc bulges north and is symmetric both ways', () {
    const lodz = LatLng(51.76, 19.46);
    const wroclaw = LatLng(51.11, 17.04);
    const berlin = LatLng(52.52, 13.40);
    const palma = LatLng(39.57, 2.65);
    for (final pair in [(lodz, wroclaw), (wroclaw, lodz), (berlin, palma)]) {
      final arc = planeArc(pair.$1, pair.$2);
      expect(arc.length, planeArcSegments + 1);
      final mid = arc[arc.length ~/ 2];
      final chordMidLat = (pair.$1.latitude + pair.$2.latitude) / 2;
      expect(mid.latitude, greaterThan(chordMidLat));
    }
    final out = planeArc(lodz, wroclaw);
    final back = planeArc(wroclaw, lodz);
    expect(
      out[out.length ~/ 2].latitude,
      closeTo(back[back.length ~/ 2].latitude, 0.05),
    );
  });

  test('planeArc stays continuous across the 180th meridian', () {
    const tokyo = LatLng(35.68, 139.76);
    const la = LatLng(34.05, -118.24);
    final arc = planeArc(tokyo, la);
    expect(arc.length, greaterThan(2));
    for (var i = 1; i < arc.length; i++) {
      expect(
        (arc[i].longitude - arc[i - 1].longitude).abs(),
        lessThan(90),
        reason: 'jump between ${arc[i - 1].longitude} and ${arc[i].longitude}',
      );
    }
  });

  test('planeArc Hawaii-Japan takes the Pacific, not the long way', () {
    const hawaii = LatLng(21.31, -157.86);
    const tokyo = LatLng(35.68, 139.76);
    final arc = planeArc(hawaii, tokyo);
    final lons = [for (final point in arc) point.longitude];
    expect(lons.reduce(math.max) - lons.reduce(math.min), lessThan(180));
    for (var i = 1; i < arc.length; i++) {
      expect((arc[i].longitude - arc[i - 1].longitude).abs(), lessThan(90));
    }
  });

  test('mapLegsFromTrip keeps stored roads and draws a bulged plane arc', () {
    const road = [
      LatLng(51.21, 16.16),
      LatLng(51.9, 15.2),
      LatLng(52.52, 13.40),
    ];
    const storedArc = [
      LatLng(52.52, 13.40),
      LatLng(46.0, 8.0),
      LatLng(39.57, 2.65),
    ];
    const shortFlight = [LatLng(39.57, 2.65), LatLng(39.71, 2.62)];
    final trip = TripDetails(
      id: 1,
      title: 'Test',
      startDate: DateTime(2026, 8, 12),
      totalDistanceKm: 1,
      waypoints: const [_legnica, _berlin, _palma, _prague],
      legs: const [
        TripLeg(mode: TransportMode.car, geometry: road),
        TripLeg(mode: TransportMode.plane, geometry: storedArc),
        TripLeg(mode: TransportMode.train),
      ],
    );

    final legs = mapLegsFromTrip(trip);
    expect(legs, hasLength(3));

    expect(legs[0].mode, TransportMode.car);
    expect(legs[0].points, road);

    expect(legs[1].mode, TransportMode.plane);
    expect(legs[1].points.length, greaterThan(2));
    expect(legs[1].points.first, storedArc.first);
    expect(legs[1].points.last.latitude, closeTo(_palma.latitude, 0.0001));
    expect(legs[1].points.last.longitude, closeTo(_palma.longitude, 0.0001));
    expect(legs[1].points, isNot(storedArc));

    expect(legs[2].mode, TransportMode.train);
    expect(legs[2].points, [
      LatLng(_palma.latitude, _palma.longitude),
      LatLng(_prague.latitude, _prague.longitude),
    ]);

    final emptyPlane = mapLegsFromTrip(
      TripDetails(
        id: 2,
        title: 'Lot',
        startDate: DateTime(2026, 8, 12),
        totalDistanceKm: 1,
        waypoints: const [_berlin, _palma],
        legs: const [TripLeg(mode: TransportMode.plane)],
      ),
    );
    expect(emptyPlane.single.points.length, greaterThan(2));

    final twoPointPlane = mapLegsFromTrip(
      TripDetails(
        id: 3,
        title: 'Krótki lot',
        startDate: DateTime(2026, 8, 12),
        totalDistanceKm: 1,
        waypoints: const [_palma, _prague],
        legs: const [TripLeg(mode: TransportMode.plane, geometry: shortFlight)],
      ),
    );
    expect(twoPointPlane.single.points.length, greaterThan(2));
    expect(twoPointPlane.single.points.first, shortFlight.first);
    expect(
      twoPointPlane.single.points.last.latitude,
      closeTo(_prague.latitude, 0.0001),
    );
  });

  test('numberedStopsFromTrip merges return visits into one marker', () {
    final trip = TripDetails(
      id: 1,
      title: 'Pętla',
      startDate: DateTime(2026, 8, 12),
      totalDistanceKm: 1,
      waypoints: const [_legnica, _berlin, _palma, _berlin, _legnica],
      legs: const [
        TripLeg(mode: TransportMode.car),
        TripLeg(mode: TransportMode.plane),
        TripLeg(mode: TransportMode.plane),
        TripLeg(mode: TransportMode.car),
      ],
    );
    final stops = numberedStopsFromTrip(trip);
    expect(stops, hasLength(3));
    expect(stops[0].label, '1·5');
    expect(stops[0].placeName, 'Legnica');
    expect(stops[1].label, '2·4');
    expect(stops[1].placeName, 'Berlin');
    expect(stops[2].label, '3');
    expect(stops[2].placeName, 'Palma de Mallorca');
  });

  test('polylineFor uses T-014 color and dashed other', () {
    const a = LatLng(51.21, 16.16);
    const b = LatLng(52.52, 13.40);
    final car = polylineFor(
      const MapLeg(mode: TransportMode.car, points: [a, b]),
    );
    expect(car.color, TransportMode.car.color);
    expect(car.strokeWidth, TransportMode.car.stroke.width);
    expect(car.pattern, const StrokePattern.solid());

    final other = polylineFor(
      const MapLeg(mode: TransportMode.other, points: [a, b]),
    );
    expect(other.color, TransportMode.other.color);
    expect(
      other.pattern,
      StrokePattern.dashed(segments: TransportMode.other.stroke.dashPattern),
    );
  });
}
