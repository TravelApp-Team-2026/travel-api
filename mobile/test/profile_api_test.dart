import 'dart:typed_data';

import 'package:dio/dio.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:travel_app/services/api_client.dart';
import 'package:travel_app/services/profile_api.dart';

void main() {
  test('update sends PATCH /api/v1/users/me without the e-mail', () async {
    final dio = Dio();
    RequestOptions? seen;
    dio.httpClientAdapter = _JsonAdapter(
      '''
      {
        "id": 1,
        "email": "jan.kowalski@example.com",
        "displayName": "Anna Nowak",
        "bio": null,
        "isPublic": true
      }
      ''',
      onRequest: (options) => seen = options,
    );

    final profile = await ProfileApi(ApiClient(dio)).updateProfile(
      displayName: '  Anna Nowak  ',
      bio: '   ',
      isPublic: true,
    );

    expect(seen?.method, 'PATCH');
    expect(seen?.path, '/api/v1/users/me');
    expect(seen?.data, {
      'displayName': 'Anna Nowak',
      'bio': '',
      'isPublic': true,
    });
    expect(seen?.data, isNot(containsPair('email', anything)));
    expect(profile.displayName, 'Anna Nowak');
    expect(profile.bio, isNull);
    expect(profile.isPublic, isTrue);
    expect(profile.email, 'jan.kowalski@example.com');
  });

  test('get reads the profile payload', () async {
    final dio = Dio();
    dio.httpClientAdapter = _JsonAdapter('''
      {
        "id": 1,
        "email": "jan.kowalski@example.com",
        "displayName": "Jan Kowalski",
        "bio": "Lubię góry.",
        "isPublic": false
      }
      ''');

    final profile = await ProfileApi(ApiClient(dio)).getProfile();

    expect(profile.id, 1);
    expect(profile.email, 'jan.kowalski@example.com');
    expect(profile.displayName, 'Jan Kowalski');
    expect(profile.bio, 'Lubię góry.');
    expect(profile.isPublic, isFalse);
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
