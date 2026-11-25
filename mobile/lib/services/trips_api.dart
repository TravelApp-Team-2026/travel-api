import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:travel_app/services/api_client.dart';
import 'package:travel_app/services/api_exception.dart';

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

class TripsApi {
  TripsApi(this._client);

  final ApiClient _client;

  Future<List<TripSummary>> listTrips() async {
    final response = await _client.get<dynamic>('/api/v1/trips');
    final data = response.data;
    if (data is! List) {
      throw const ApiException('Serwer zwrócił nieprawidłową listę podróży.');
    }
    return [for (final item in data) TripSummary.fromJson(item)];
  }
}

final tripsApiProvider = Provider<TripsApi>(
  (ref) => TripsApi(ref.watch(apiClientProvider)),
);

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

String formatStopCount(int count) {
  return '$count ${_plural(count, 'przystanek', 'przystanki', 'przystanków')}';
}

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
