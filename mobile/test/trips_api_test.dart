import 'dart:typed_data';

import 'package:dio/dio.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:travel_app/services/api_client.dart';
import 'package:travel_app/services/api_exception.dart';
import 'package:travel_app/services/trips_api.dart';

void main() {
  test('list reads GET /api/v1/trips', () async {
    final dio = Dio();
    RequestOptions? seen;
    dio.httpClientAdapter = _JsonAdapter(
      '''
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
      ''',
      onRequest: (options) => seen = options,
    );

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
    expect(formatStopCount(1), '1 przystanek');
    expect(formatStopCount(3), '3 przystanki');
    expect(formatStopCount(6), '6 przystanków');
    expect(formatDistanceKm(4183), '4 183 km');
    expect(formatDistanceKm(640), '640 km');
    expect(formatTripsSubtitle(4, 11628), '4 podróże · 11 628 km łącznie');
    expect(formatTripsSubtitle(0, 0), 'Brak zapisanych podróży');
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

    expect(
      TripsApi(ApiClient(dio)).listTrips(),
      throwsA(isA<ApiException>()),
    );
  });
}

class _JsonAdapter implements HttpClientAdapter {
  _JsonAdapter(this.body, {this.onRequest});

  final String body;
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
      200,
      headers: {
        Headers.contentTypeHeader: ['application/json'],
      },
    );
  }

  @override
  void close({bool force = false}) {}
}
