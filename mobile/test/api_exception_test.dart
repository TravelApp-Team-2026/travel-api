import 'package:dio/dio.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:travel_app/services/api_exception.dart';

void main() {
  group('ApiException', () {
    test('returns detail from ProblemDetail', () {
      final exception = DioException(
        requestOptions: RequestOptions(path: '/test'),
        response: Response(
          requestOptions: RequestOptions(path: '/test'),
          data: {
            'type': 'about:blank',
            'title': 'Bad Request',
            'status': 400,
            'detail': 'Nieprawidłowe dane.',
          },
          statusCode: 400,
        ),
        type: DioExceptionType.badResponse,
      );

      final result = ApiException.fromDioException(exception);

      expect(result.message, 'Nieprawidłowe dane.');
    });

    test('keeps field errors next to the message', () {
      final exception = DioException(
        requestOptions: RequestOptions(path: '/test'),
        response: Response(
          requestOptions: RequestOptions(path: '/test'),
          data: {
            'title': 'Nieprawidłowe żądanie',
            'detail': 'Przesłane dane są nieprawidłowe.',
            'fieldErrors': {'email': 'Podaj poprawny adres e-mail.'},
          },
          statusCode: 400,
        ),
        type: DioExceptionType.badResponse,
      );

      final result = ApiException.fromDioException(exception);

      expect(result.message, 'Przesłane dane są nieprawidłowe.');
      expect(result.fieldErrors['email'], 'Podaj poprawny adres e-mail.');
    });

    test('returns title when detail is missing', () {
      final exception = DioException(
        requestOptions: RequestOptions(path: '/test'),
        response: Response(
          requestOptions: RequestOptions(path: '/test'),
          data: {
            'title': 'Nieprawidłowe żądanie',
          },
          statusCode: 400,
        ),
        type: DioExceptionType.badResponse,
      );

      final result = ApiException.fromDioException(exception);

      expect(result.message, 'Nieprawidłowe żądanie');
    });

    test('returns timeout message', () {
      final exception = DioException(
        requestOptions: RequestOptions(path: '/test'),
        type: DioExceptionType.connectionTimeout,
      );

      final result = ApiException.fromDioException(exception);

      expect(
        result.message,
        'Serwer nie odpowiedział na czas. Spróbuj ponownie.',
      );
    });

    test('returns connection error message', () {
      final exception = DioException(
        requestOptions: RequestOptions(path: '/test'),
        type: DioExceptionType.connectionError,
      );

      final result = ApiException.fromDioException(exception);

      expect(
        result.message,
        'Brak połączenia z serwerem. Sprawdź połączenie internetowe.',
      );
    });

    test('returns server error message', () {
      final exception = DioException(
        requestOptions: RequestOptions(path: '/test'),
        response: Response(
          requestOptions: RequestOptions(path: '/test'),
          statusCode: 500,
        ),
        type: DioExceptionType.badResponse,
      );

      final result = ApiException.fromDioException(exception);

      expect(
        result.message,
        'Serwer zwrócił błąd. Spróbuj ponownie później.',
      );
    });

    test('returns default message for other errors', () {
      final exception = DioException(
        requestOptions: RequestOptions(path: '/test'),
        type: DioExceptionType.cancel,
      );

      final result = ApiException.fromDioException(exception);

      expect(
        result.message,
        'Coś poszło nie tak. Spróbuj ponownie.',
      );
    });
  });
}
