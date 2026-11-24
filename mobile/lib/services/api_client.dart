import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:travel_app/services/api_config.dart';
import 'package:travel_app/services/api_exception.dart';
import 'package:travel_app/services/auth_session.dart';
import 'package:travel_app/services/token_storage.dart';

class ApiClient {
  ApiClient(this._dio);

  final Dio _dio;

  Future<Response<T>> get<T>(
    String path, {
    Map<String, dynamic>? queryParameters,
  }) async {
    try {
      return await _dio.get<T>(path, queryParameters: queryParameters);
    } on DioException catch (e) {
      throw ApiException.fromDioException(e);
    }
  }

  Future<Response<T>> post<T>(String path, {Object? data}) async {
    try {
      return await _dio.post<T>(path, data: data);
    } on DioException catch (e) {
      throw ApiException.fromDioException(e);
    }
  }

  Future<Response<T>> put<T>(String path, {Object? data}) async {
    try {
      return await _dio.put<T>(path, data: data);
    } on DioException catch (e) {
      throw ApiException.fromDioException(e);
    }
  }

  Future<Response<T>> patch<T>(String path, {Object? data}) async {
    try {
      return await _dio.patch<T>(path, data: data);
    } on DioException catch (e) {
      throw ApiException.fromDioException(e);
    }
  }

  Future<Response<T>> delete<T>(String path) async {
    try {
      return await _dio.delete<T>(path);
    } on DioException catch (e) {
      throw ApiException.fromDioException(e);
    }
  }
}

/// Attaches the bearer token and, on HTTP 401, runs [onUnauthorized].
void addAuthInterceptor(
  Dio dio, {
  required Future<String?> Function() readToken,
  required Future<void> Function() onUnauthorized,
}) {
  dio.interceptors.add(
    InterceptorsWrapper(
      onRequest: (options, handler) async {
        final token = await readToken();
        if (token != null && token.isNotEmpty) {
          options.headers['Authorization'] = 'Bearer $token';
        } else {
          options.headers.remove('Authorization');
        }
        handler.next(options);
      },
      onError: (error, handler) async {
        try {
          if (error.response?.statusCode == 401) {
            await onUnauthorized();
          }
        } finally {
          handler.next(error);
        }
      },
    ),
  );
}

final apiClientProvider = Provider<ApiClient>((ref) {
  final tokenStorage = ref.watch(tokenStorageProvider);

  final dio = Dio(
    BaseOptions(
      baseUrl: ref.watch(apiBaseUrlProvider),
      connectTimeout: const Duration(seconds: 10),
      receiveTimeout: const Duration(seconds: 15),
      sendTimeout: const Duration(seconds: 15),
      headers: {'Accept': 'application/json'},
    ),
  );

  addAuthInterceptor(
    dio,
    readToken: tokenStorage.getToken,
    onUnauthorized: () async {
      if (!ref.mounted) {
        return;
      }
      await ref.read(authSessionProvider.notifier).signOut();
    },
  );

  ref.onDispose(() {
    dio.close();
  });

  return ApiClient(dio);
});
