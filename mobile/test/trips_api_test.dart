import 'dart:typed_data';

import 'package:dio/dio.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:travel_app/services/api_client.dart';
import 'package:travel_app/services/api_exception.dart';
import 'package:travel_app/services/place_search_api.dart';
import 'package:travel_app/services/trips_api.dart';
import 'package:travel_app/theme/transport_mode.dart';

void main() {
  test('list reads GET /api/v1/trips', () async {
    final dio = Dio();
    RequestOptions? seen;
    dio.httpClientAdapter = _JsonAdapter('''
      [
        {
          "id": 1,
          "title": "Majorka — lato 2026",
          "startDate": "2026-08-12",
          "endDate": "2026-08-19",
          "stopCount": 6,
          "totalDistanceKm": 4183.00
        }
      ]
      ''', onRequest: (options) => seen = options);

    final trips = await TripsApi(ApiClient(dio)).listTrips();

    expect(seen?.method, 'GET');
    expect(seen?.path, '/api/v1/trips');
    expect(trips, hasLength(1));
    expect(trips.single.id, 1);
    expect(trips.single.title, 'Majorka — lato 2026');
    expect(trips.single.startDate, DateTime(2026, 8, 12));
    expect(trips.single.endDate, DateTime(2026, 8, 19));
    expect(trips.single.stopCount, 6);
    expect(trips.single.totalDistanceKm, 4183);
  });

  test('formats dates, stops and distance as on the mockup', () {
    expect(
      formatTripDateRange(DateTime(2026, 8, 12), DateTime(2026, 8, 19)),
      '12–19 sie 2026',
    );
    expect(
      formatTripDateRange(DateTime(2026, 7, 4), DateTime(2026, 7, 6)),
      '4–6 lip 2026',
    );
    expect(formatTripDateRange(DateTime(2026, 2, 14), null), '14 lut 2026');
    expect(formatStopNoun(1), 'przystanek');
    expect(formatStopNoun(3), 'przystanki');
    expect(formatStopNoun(6), 'przystanków');
    expect(formatStopCount(1), '1 przystanek');
    expect(formatStopCount(3), '3 przystanki');
    expect(formatStopCount(6), '6 przystanków');
    expect(formatSegmentNoun(1), 'odcinek');
    expect(formatSegmentNoun(2), 'odcinki');
    expect(formatSegmentNoun(5), 'odcinków');
    expect(formatSegmentCount(1), '1 odcinek');
    expect(formatSegmentCount(2), '2 odcinki');
    expect(formatSegmentCount(5), '5 odcinków');
    expect(formatDistanceKm(4183), '4 183 km');
    expect(formatDistanceKm(640), '640 km');
    expect(formatTripsSubtitle(4, 11628), '4 podróże · 11 628 km łącznie');
    expect(formatTripsSubtitle(0, 0), 'Brak zapisanych podróży');
    expect(formatTripDay(DateTime(2026, 8, 12)), '12 sie 2026');
    expect(formatIsoDate(DateTime(2026, 8, 12)), '2026-08-12');
  });

  test('getTrip reads GET /api/v1/trips/{id}', () async {
    final dio = Dio();
    RequestOptions? seen;
    dio.httpClientAdapter = _JsonAdapter(_tripBody, onRequest: (o) => seen = o);

    final trip = await TripsApi(ApiClient(dio)).getTrip(7);

    expect(seen?.method, 'GET');
    expect(seen?.path, '/api/v1/trips/7');
    expect(trip.title, 'Majorka — lato 2026');
    expect(trip.waypoints.map((p) => p.placeName), [
      'Legnica',
      'Berlin',
      'Palma de Mallorca',
      'Valldemossa',
      'Berlin',
      'Legnica',
    ]);
    expect(trip.modes, [
      TransportMode.car,
      TransportMode.plane,
      TransportMode.car,
      TransportMode.plane,
      TransportMode.car,
    ]);
    expect(
      [for (final leg in trip.legs) leg.distanceKm],
      [340, 1742, 21, 1741, 339],
    );
  });

  test('deleteTrip sends DELETE /api/v1/trips/{id}', () async {
    final dio = Dio();
    RequestOptions? seen;
    dio.httpClientAdapter = _JsonAdapter(
      '',
      statusCode: 204,
      onRequest: (o) => seen = o,
    );

    await TripsApi(ApiClient(dio)).deleteTrip(7);

    expect(seen?.method, 'DELETE');
    expect(seen?.path, '/api/v1/trips/7');
  });

  test('createTrip posts SaveTrip JSON', () async {
    final dio = Dio();
    RequestOptions? seen;
    dio.httpClientAdapter = _JsonAdapter(_tripBody, onRequest: (o) => seen = o);

    await TripsApi(ApiClient(dio)).createTrip(
      SaveTrip(
        title: 'Majorka — lato 2026',
        startDate: DateTime(2026, 8, 12),
        endDate: DateTime(2026, 8, 19),
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
        modes: const [TransportMode.car],
      ),
    );

    expect(seen?.method, 'POST');
    expect(seen?.path, '/api/v1/trips');
    expect(seen?.data, {
      'title': 'Majorka — lato 2026',
      'description': null,
      'startDate': '2026-08-12',
      'endDate': '2026-08-19',
      'waypoints': [
        {
          'placeName': 'Legnica',
          'latitude': 51.21,
          'longitude': 16.16,
          'countryCode': 'PL',
          'countryName': 'Polska',
        },
        {
          'placeName': 'Berlin',
          'latitude': 52.52,
          'longitude': 13.40,
          'countryCode': 'DE',
          'countryName': 'Niemcy',
        },
      ],
      'segments': [
        {'transportMode': 'CAR'},
      ],
    });
  });

  test('invalid date is an ApiException', () async {
    final dio = Dio();
    dio.httpClientAdapter = _JsonAdapter('''
      [
        {
          "id": 1,
          "title": "Zła data",
          "startDate": "not-a-date",
          "endDate": null,
          "stopCount": 2,
          "totalDistanceKm": 0
        }
      ]
      ''');

    expect(TripsApi(ApiClient(dio)).listTrips(), throwsA(isA<ApiException>()));
  });
}

const _tripBody = '''
{
  "id": 7,
  "title": "Majorka — lato 2026",
  "description": null,
  "startDate": "2026-08-12",
  "endDate": "2026-08-19",
  "totalDistanceKm": 4183.00,
  "waypoints": [
    {"order": 0, "placeName": "Legnica", "latitude": 51.21, "longitude": 16.16, "countryCode": "PL", "countryName": "Polska"},
    {"order": 1, "placeName": "Berlin", "latitude": 52.52, "longitude": 13.40, "countryCode": "DE", "countryName": "Niemcy"},
    {"order": 2, "placeName": "Palma de Mallorca", "latitude": 39.57, "longitude": 2.65, "countryCode": "ES", "countryName": "Hiszpania"},
    {"order": 3, "placeName": "Valldemossa", "latitude": 39.71, "longitude": 2.62, "countryCode": "ES", "countryName": "Hiszpania"},
    {"order": 4, "placeName": "Berlin", "latitude": 52.52, "longitude": 13.40, "countryCode": "DE", "countryName": "Niemcy"},
    {"order": 5, "placeName": "Legnica", "latitude": 51.21, "longitude": 16.16, "countryCode": "PL", "countryName": "Polska"}
  ],
  "segments": [
    {"order": 0, "transportMode": "CAR", "distanceKm": 340, "geometry": []},
    {"order": 1, "transportMode": "PLANE", "distanceKm": 1742, "geometry": []},
    {"order": 2, "transportMode": "CAR", "distanceKm": 21, "geometry": []},
    {"order": 3, "transportMode": "PLANE", "distanceKm": 1741, "geometry": []},
    {"order": 4, "transportMode": "CAR", "distanceKm": 339, "geometry": []}
  ]
}
''';

class _JsonAdapter implements HttpClientAdapter {
  _JsonAdapter(this.body, {this.onRequest, this.statusCode = 200});

  final String body;
  final int statusCode;
  final void Function(RequestOptions options)? onRequest;

  @override
  Future<ResponseBody> fetch(
    RequestOptions options,
    Stream<Uint8List>? requestStream,
    Future<void>? cancelFuture,
  ) async {
    onRequest?.call(options);
    return ResponseBody.fromString(
      body,
      statusCode,
      headers: {
        Headers.contentTypeHeader: ['application/json'],
      },
    );
  }

  @override
  void close({bool force = false}) {}
}
