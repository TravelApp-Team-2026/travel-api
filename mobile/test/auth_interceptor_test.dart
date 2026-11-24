import 'dart:typed_data';

import 'package:dio/dio.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:travel_app/services/api_client.dart';

void main() {
  test('401 runs the unauthorized callback and still fails the request', () async {
    final dio = Dio();
    dio.httpClientAdapter = _StatusAdapter(401);
    var calls = 0;
    addAuthInterceptor(
      dio,
      readToken: () async => 'jwt',
      onUnauthorized: () async {
        calls++;
      },
    );

    await expectLater(dio.get<dynamic>('/trips'), throwsA(isA<DioException>()));
    expect(calls, 1);
  });

  test('other status codes leave the session alone', () async {
    final dio = Dio();
    dio.httpClientAdapter = _StatusAdapter(500);
    var calls = 0;
    addAuthInterceptor(
      dio,
      readToken: () async => 'jwt',
      onUnauthorized: () async {
        calls++;
      },
    );

    await expectLater(dio.get<dynamic>('/trips'), throwsA(isA<DioException>()));
    expect(calls, 0);
  });

  test('sends Authorization Bearer when a token is stored', () async {
    final dio = Dio();
    RequestOptions? seen;
    dio.httpClientAdapter = _StatusAdapter(200, onRequest: (options) => seen = options);
    addAuthInterceptor(
      dio,
      readToken: () async => 'jwt',
      onUnauthorized: () async {},
    );

    await dio.get<dynamic>('/trips');

    expect(seen?.headers['Authorization'], 'Bearer jwt');
  });
}

class _StatusAdapter implements HttpClientAdapter {
  _StatusAdapter(this.statusCode, {this.onRequest});

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
      '{}',
      statusCode,
      headers: {
        Headers.contentTypeHeader: ['application/json'],
      },
    );
  }

  @override
  void close({bool force = false}) {}
}
