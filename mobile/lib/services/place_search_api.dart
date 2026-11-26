import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:travel_app/services/api_client.dart';
import 'package:travel_app/services/api_exception.dart';

class Place {
  const Place({
    required this.placeName,
    required this.latitude,
    required this.longitude,
    required this.countryName,
    required this.countryCode,
  });

  final String placeName;
  final double latitude;
  final double longitude;
  final String countryName;
  final String countryCode;

  factory Place.fromJson(Object? data) {
    if (data is! Map) {
      throw const ApiException('Serwer zwrócił nieprawidłowe miejsce.');
    }
    final placeName = data['placeName'];
    final latitude = data['latitude'];
    final longitude = data['longitude'];
    final countryName = data['countryName'];
    final countryCode = data['countryCode'];
    if (placeName is! String ||
        latitude is! num ||
        longitude is! num ||
        countryName is! String ||
        countryCode is! String) {
      throw const ApiException('Serwer zwrócił nieprawidłowe miejsce.');
    }
    return Place(
      placeName: placeName,
      latitude: latitude.toDouble(),
      longitude: longitude.toDouble(),
      countryName: countryName,
      countryCode: countryCode,
    );
  }

  @override
  bool operator ==(Object other) {
    return other is Place &&
        other.placeName == placeName &&
        other.latitude == latitude &&
        other.longitude == longitude &&
        other.countryName == countryName &&
        other.countryCode == countryCode;
  }

  @override
  int get hashCode =>
      Object.hash(placeName, latitude, longitude, countryName, countryCode);
}

class PlaceSearchApi {
  PlaceSearchApi(this._client);

  final ApiClient _client;

  Future<List<Place>> search(String query) async {
    final response = await _client.get<dynamic>(
      '/api/v1/geo/search',
      queryParameters: {'q': query},
    );
    final data = response.data;
    if (data is! List) {
      throw const ApiException('Serwer zwrócił nieprawidłową listę miejsc.');
    }
    return [for (final item in data) Place.fromJson(item)];
  }
}

final placeSearchApiProvider = Provider<PlaceSearchApi>(
  (ref) => PlaceSearchApi(ref.watch(apiClientProvider)),
);
