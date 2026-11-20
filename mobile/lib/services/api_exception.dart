import 'package:dio/dio.dart';

class ApiException implements Exception {
  const ApiException(this.message);

  final String message;

  factory ApiException.fromDioException(DioException exception) {
    final data = exception.response?.data;

    if (data is Map) {
      final detail = data['detail'];
      final title = data['title'];

      if (detail is String && detail.isNotEmpty) {
        return ApiException(detail);
      }

      if (title is String && title.isNotEmpty) {
        return ApiException(title);
      }
    }

    switch (exception.type) {
      case DioExceptionType.connectionTimeout:
      case DioExceptionType.sendTimeout:
      case DioExceptionType.receiveTimeout:
        return const ApiException(
          'Serwer nie odpowiedział na czas. Spróbuj ponownie.',
        );

      case DioExceptionType.connectionError:
        return const ApiException(
          'Brak połączenia z serwerem. Sprawdź połączenie internetowe.',
        );

      case DioExceptionType.badResponse:
        return const ApiException(
          'Serwer zwrócił błąd. Spróbuj ponownie później.',
        );

      default:
        return const ApiException('Coś poszło nie tak. Spróbuj ponownie.');
    }
  }

  @override
  String toString() => message;
}
