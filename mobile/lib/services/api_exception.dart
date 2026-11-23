import 'package:dio/dio.dart';

class ApiException implements Exception {
  const ApiException(this.message, {this.fieldErrors = const {}});

  final String message;

  /// Field name to message, from a ProblemDetail `fieldErrors` object.
  final Map<String, String> fieldErrors;

  factory ApiException.fromDioException(DioException exception) {
    final data = exception.response?.data;
    final fieldErrors = _fieldErrors(data);

    if (data is Map) {
      final detail = data['detail'];
      final title = data['title'];

      if (detail is String && detail.isNotEmpty) {
        return ApiException(detail, fieldErrors: fieldErrors);
      }

      if (title is String && title.isNotEmpty) {
        return ApiException(title, fieldErrors: fieldErrors);
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

  static Map<String, String> _fieldErrors(Object? data) {
    if (data is! Map) {
      return const {};
    }
    final raw = data['fieldErrors'];
    if (raw is! Map) {
      return const {};
    }
    return {
      for (final entry in raw.entries)
        if (entry.key is String &&
            entry.value is String &&
            (entry.value as String).isNotEmpty)
          entry.key as String: entry.value as String,
    };
  }
}
