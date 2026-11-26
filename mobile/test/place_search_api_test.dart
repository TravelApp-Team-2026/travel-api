import 'dart:typed_data';

import 'package:dio/dio.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:travel_app/services/api_client.dart';
import 'package:travel_app/services/place_search_api.dart';

void main() {
  test('search calls GET /api/v1/geo/search', () async {
    final dio = Dio();
    RequestOptions? seen;
    dio.httpClientAdapter = _JsonAdapter('''
      [
        {
          "placeName": "Kraków",
          "latitude": 50.0619474,
          "longitude": 19.9368564,
          "countryName": "Polska",
          "countryCode": "PL"
        }
      ]
      ''', onRequest: (options) => seen = options);

    final places = await PlaceSearchApi(ApiClient(dio)).search('Kraków');

    expect(seen?.method, 'GET');
    expect(seen?.path, '/api/v1/geo/search');
    expect(seen?.queryParameters, {'q': 'Kraków'});
    expect(
      places.single,
      const Place(
        placeName: 'Kraków',
        latitude: 50.0619474,
        longitude: 19.9368564,
        countryName: 'Polska',
        countryCode: 'PL',
      ),
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
