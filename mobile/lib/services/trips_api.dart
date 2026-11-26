import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:travel_app/services/api_client.dart';
import 'package:travel_app/services/api_exception.dart';
import 'package:travel_app/services/place_search_api.dart';
import 'package:travel_app/theme/transport_mode.dart';

const _months = [
  'sty',
  'lut',
  'mar',
  'kwi',
  'maj',
  'cze',
  'lip',
  'sie',
  'wrz',
  'paź',
  'lis',
  'gru',
];

class TripSummary {
  const TripSummary({
    required this.id,
    required this.title,
    required this.startDate,
    required this.endDate,
    required this.stopCount,
    required this.totalDistanceKm,
  });

  final int id;
  final String title;
  final DateTime startDate;
  final DateTime? endDate;
  final int stopCount;
  final double totalDistanceKm;

  factory TripSummary.fromJson(Object? data) {
    if (data is! Map) {
      throw const ApiException('Serwer zwrócił nieprawidłową podróż.');
    }
    final id = data['id'];
    final title = data['title'];
    final startDate = data['startDate'];
    final endDate = data['endDate'];
    final stopCount = data['stopCount'];
    final totalDistanceKm = data['totalDistanceKm'];
    if (id is! num ||
        title is! String ||
        startDate is! String ||
        stopCount is! num ||
        totalDistanceKm is! num ||
        (endDate != null && endDate is! String)) {
      throw const ApiException('Serwer zwrócił nieprawidłową podróż.');
    }
    return TripSummary(
      id: id.toInt(),
      title: title,
      startDate: _parseDate(startDate),
      endDate: endDate == null ? null : _parseDate(endDate as String),
      stopCount: stopCount.toInt(),
      totalDistanceKm: totalDistanceKm.toDouble(),
    );
  }
}

DateTime _parseDate(String value) {
  final parts = value.split('-');
  if (parts.length != 3) {
    throw const ApiException('Serwer zwrócił nieprawidłową podróż.');
  }
  final year = int.tryParse(parts[0]);
  final month = int.tryParse(parts[1]);
  final day = int.tryParse(parts[2]);
  if (year == null || month == null || day == null) {
    throw const ApiException('Serwer zwrócił nieprawidłową podróż.');
  }
  final date = DateTime(year, month, day);
  if (date.year != year || date.month != month || date.day != day) {
    throw const ApiException('Serwer zwrócił nieprawidłową podróż.');
  }
  return date;
}

class TripLeg {
  const TripLeg({required this.mode, this.distanceKm = 0});

  final TransportMode mode;
  final double distanceKm;
}

class TripDetails {
  const TripDetails({
    required this.id,
    required this.title,
    this.description,
    required this.startDate,
    this.endDate,
    required this.totalDistanceKm,
    required this.waypoints,
    required this.legs,
  });

  final int id;
  final String title;
  final String? description;
  final DateTime startDate;
  final DateTime? endDate;
  final double totalDistanceKm;
  final List<Place> waypoints;
  final List<TripLeg> legs;

  List<TransportMode> get modes => [for (final leg in legs) leg.mode];

  factory TripDetails.fromJson(Object? data) {
    if (data is! Map) {
      throw const ApiException('Serwer zwrócił nieprawidłową podróż.');
    }
    final id = data['id'];
    final title = data['title'];
    final description = data['description'];
    final startDate = data['startDate'];
    final endDate = data['endDate'];
    final totalDistanceKm = data['totalDistanceKm'];
    final waypoints = data['waypoints'];
    final segments = data['segments'];
    if (id is! num ||
        title is! String ||
        startDate is! String ||
        totalDistanceKm is! num ||
        waypoints is! List ||
        segments is! List ||
        (description != null && description is! String) ||
        (endDate != null && endDate is! String)) {
      throw const ApiException('Serwer zwrócił nieprawidłową podróż.');
    }
    final places = [for (final item in waypoints) Place.fromJson(item)];
    final legs = [for (final item in segments) _legFromSegment(item)];
    if (places.length < 2 || legs.length != places.length - 1) {
      throw const ApiException('Serwer zwrócił nieprawidłową podróż.');
    }
    return TripDetails(
      id: id.toInt(),
      title: title,
      description: description as String?,
      startDate: _parseDate(startDate),
      endDate: endDate == null ? null : _parseDate(endDate as String),
      totalDistanceKm: totalDistanceKm.toDouble(),
      waypoints: places,
      legs: legs,
    );
  }
}

TripLeg _legFromSegment(Object? data) {
  if (data is! Map) {
    throw const ApiException('Serwer zwrócił nieprawidłowy odcinek.');
  }
  final value = data['transportMode'];
  final km = data['distanceKm'];
  if (value is! String || km is! num) {
    throw const ApiException('Serwer zwrócił nieprawidłowy odcinek.');
  }
  final mode = TransportMode.tryParse(value);
  if (mode == null) {
    throw const ApiException('Serwer zwrócił nieprawidłowy odcinek.');
  }
  return TripLeg(mode: mode, distanceKm: km.toDouble());
}

class SaveTrip {
  const SaveTrip({
    required this.title,
    this.description,
    required this.startDate,
    this.endDate,
    required this.waypoints,
    required this.modes,
  });

  final String title;
  final String? description;
  final DateTime startDate;
  final DateTime? endDate;
  final List<Place> waypoints;
  final List<TransportMode> modes;

  Map<String, dynamic> toJson() {
    return {
      'title': title,
      'description': description,
      'startDate': formatIsoDate(startDate),
      'endDate': endDate == null ? null : formatIsoDate(endDate!),
      'waypoints': [
        for (final place in waypoints)
          {
            'placeName': place.placeName,
            'latitude': place.latitude,
            'longitude': place.longitude,
            'countryCode': place.countryCode,
            'countryName': place.countryName,
          },
      ],
      'segments': [
        for (final mode in modes) {'transportMode': mode.apiName},
      ],
    };
  }
}

class TripsApi {
  TripsApi(this._client);

  final ApiClient _client;

  static const saveTimeout = Duration(minutes: 5);

  Future<List<TripSummary>> listTrips() async {
    final response = await _client.get<dynamic>('/api/v1/trips');
    final data = response.data;
    if (data is! List) {
      throw const ApiException('Serwer zwrócił nieprawidłową listę podróży.');
    }
    return [for (final item in data) TripSummary.fromJson(item)];
  }

  Future<TripDetails> getTrip(int id) async {
    final response = await _client.get<dynamic>('/api/v1/trips/$id');
    return TripDetails.fromJson(response.data);
  }

  Future<TripDetails> createTrip(SaveTrip trip) async {
    final response = await _client.post<dynamic>(
      '/api/v1/trips',
      data: trip.toJson(),
      receiveTimeout: saveTimeout,
    );
    return TripDetails.fromJson(response.data);
  }

  Future<TripDetails> updateTrip(int id, SaveTrip trip) async {
    final response = await _client.put<dynamic>(
      '/api/v1/trips/$id',
      data: trip.toJson(),
      receiveTimeout: saveTimeout,
    );
    return TripDetails.fromJson(response.data);
  }

  Future<void> deleteTrip(int id) async {
    await _client.delete('/api/v1/trips/$id');
  }
}

final tripsApiProvider = Provider<TripsApi>(
  (ref) => TripsApi(ref.watch(apiClientProvider)),
);

String formatTripDay(DateTime date) => _dayMonthYear(date);

String formatIsoDate(DateTime date) {
  final year = date.year.toString().padLeft(4, '0');
  final month = date.month.toString().padLeft(2, '0');
  final day = date.day.toString().padLeft(2, '0');
  return '$year-$month-$day';
}

String formatTripDateRange(DateTime start, DateTime? end) {
  if (end == null || _sameDay(start, end)) {
    return _dayMonthYear(start);
  }
  if (start.year == end.year && start.month == end.month) {
    return '${start.day}–${_dayMonthYear(end)}';
  }
  if (start.year == end.year) {
    return '${_dayMonth(start)}–${_dayMonthYear(end)}';
  }
  return '${_dayMonthYear(start)}–${_dayMonthYear(end)}';
}

String formatStopNoun(int count) {
  return _plural(count, 'przystanek', 'przystanki', 'przystanków');
}

String formatStopCount(int count) => '$count ${formatStopNoun(count)}';

String formatSegmentNoun(int count) {
  return _plural(count, 'odcinek', 'odcinki', 'odcinków');
}

String formatSegmentCount(int count) => '$count ${formatSegmentNoun(count)}';

String formatDistanceKm(num km) {
  return '${_grouped(km.round())} km';
}

String formatTripsSubtitle(int count, num totalKm) {
  if (count == 0) {
    return 'Brak zapisanych podróży';
  }
  final trips = '$count ${_plural(count, 'podróż', 'podróże', 'podróży')}';
  return '$trips · ${formatDistanceKm(totalKm)} łącznie';
}

bool _sameDay(DateTime a, DateTime b) {
  return a.year == b.year && a.month == b.month && a.day == b.day;
}

String _month(DateTime date) => _months[date.month - 1];

String _dayMonth(DateTime date) => '${date.day} ${_month(date)}';

String _dayMonthYear(DateTime date) => '${_dayMonth(date)} ${date.year}';

String _plural(int count, String one, String few, String many) {
  final mod10 = count % 10;
  final mod100 = count % 100;
  if (count == 1) {
    return one;
  }
  if (mod10 >= 2 && mod10 <= 4 && (mod100 < 12 || mod100 > 14)) {
    return few;
  }
  return many;
}

String _grouped(int value) {
  final digits = value.abs().toString();
  final buffer = StringBuffer();
  for (var i = 0; i < digits.length; i++) {
    if (i > 0 && (digits.length - i) % 3 == 0) {
      buffer.write(' ');
    }
    buffer.write(digits[i]);
  }
  return value < 0 ? '-$buffer' : buffer.toString();
}
