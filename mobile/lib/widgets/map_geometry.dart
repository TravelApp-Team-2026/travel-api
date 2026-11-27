import 'dart:math' as math;

import 'package:latlong2/latlong.dart';
import 'package:travel_app/services/trips_api.dart';
import 'package:travel_app/theme/transport_mode.dart';

const earthRadiusKm = 6371.0088;
const planeArcKmPerSegment = 100.0;
const planeArcMaxSegments = 64;
const _minSinAngle = 1e-9;

/// ~30 m. OSRM can emit a point every few metres; the map cannot paint that.
const _lineSimplifyEpsilonDeg = 0.0003;
const _maxLinePoints = 192;

/// One drawable leg on the global map.
class MapLeg {
  const MapLeg({required this.mode, required this.points});

  final TransportMode mode;
  final List<LatLng> points;
}

/// Turns saved trip segments into map lines. API geometry is the source of
/// truth. Plane without a stored line gets a great-circle arc from the
/// endpoints. Train and other fall back to a straight pair.
List<MapLeg> mapLegsFromTrip(TripDetails trip) {
  final legs = <MapLeg>[];
  for (var i = 0; i < trip.legs.length; i++) {
    final leg = trip.legs[i];
    final from = LatLng(
      trip.waypoints[i].latitude,
      trip.waypoints[i].longitude,
    );
    final to = LatLng(
      trip.waypoints[i + 1].latitude,
      trip.waypoints[i + 1].longitude,
    );
    legs.add(MapLeg(mode: leg.mode, points: _pointsFor(leg, from, to)));
  }
  return legs;
}

List<LatLng> _pointsFor(TripLeg leg, LatLng from, LatLng to) {
  final geometry = leg.geometry;
  switch (leg.mode) {
    case TransportMode.car:
      return simplifyLine(geometry.length >= 2 ? geometry : [from, to]);
    case TransportMode.plane:
      if (geometry.length >= 2) {
        return geometry;
      }
      return planeArc(from, to);
    case TransportMode.train:
    case TransportMode.other:
      if (geometry.length >= 2) {
        return [geometry.first, geometry.last];
      }
      return [from, to];
  }
}

/// Drops intermediate road points that do not change the shape on the map.
List<LatLng> simplifyLine(List<LatLng> points) {
  if (points.length <= 2) {
    return points;
  }
  final simplified = _rdp(points, _lineSimplifyEpsilonDeg);
  if (simplified.length <= _maxLinePoints) {
    return simplified;
  }
  final step = (simplified.length - 1) / (_maxLinePoints - 1);
  return [
    for (var i = 0; i < _maxLinePoints; i++)
      simplified[(i * step).round().clamp(0, simplified.length - 1)],
  ];
}

List<LatLng> _rdp(List<LatLng> points, double epsilon) {
  final keep = List<bool>.filled(points.length, false);
  keep[0] = true;
  keep[points.length - 1] = true;
  final stack = <(int, int)>[(0, points.length - 1)];
  while (stack.isNotEmpty) {
    final (start, end) = stack.removeLast();
    var maxDistance = 0.0;
    var index = start;
    for (var i = start + 1; i < end; i++) {
      final distance = _perpDistance(points[i], points[start], points[end]);
      if (distance > maxDistance) {
        maxDistance = distance;
        index = i;
      }
    }
    if (maxDistance > epsilon) {
      keep[index] = true;
      stack.add((start, index));
      stack.add((index, end));
    }
  }
  return [
    for (var i = 0; i < points.length; i++)
      if (keep[i]) points[i],
  ];
}

double _perpDistance(LatLng point, LatLng start, LatLng end) {
  final dx = end.longitude - start.longitude;
  final dy = end.latitude - start.latitude;
  if (dx == 0 && dy == 0) {
    return math.sqrt(
      math.pow(point.longitude - start.longitude, 2) +
          math.pow(point.latitude - start.latitude, 2),
    );
  }
  final t =
      ((point.longitude - start.longitude) * dx +
          (point.latitude - start.latitude) * dy) /
      (dx * dx + dy * dy);
  final u = t.clamp(0.0, 1.0);
  return math.sqrt(
    math.pow(point.longitude - (start.longitude + u * dx), 2) +
        math.pow(point.latitude - (start.latitude + u * dy), 2),
  );
}

/// Great-circle arc matching the backend `FlightRouteStrategy`.
List<LatLng> planeArc(LatLng from, LatLng to) {
  final distanceKm = greatCircleDistanceKm(from, to);
  final segments = math.min(
    planeArcMaxSegments,
    math.max(1, (distanceKm / planeArcKmPerSegment).ceil()),
  );
  return _arc(from, to, segments);
}

double greatCircleDistanceKm(LatLng from, LatLng to) {
  return earthRadiusKm * _centralAngle(from, to);
}

List<LatLng> _arc(LatLng from, LatLng to, int segments) {
  final angle = _centralAngle(from, to);
  final sinAngle = math.sin(angle);
  if (sinAngle < _minSinAngle) {
    return [from, to];
  }

  final lat1 = _rad(from.latitude);
  final lon1 = _rad(from.longitude);
  final lat2 = _rad(to.latitude);
  final lon2 = _rad(to.longitude);
  final points = <LatLng>[from];
  var previousLon = from.longitude;
  for (var i = 1; i < segments; i++) {
    final fraction = i / segments;
    final a = math.sin((1 - fraction) * angle) / sinAngle;
    final b = math.sin(fraction * angle) / sinAngle;
    final x =
        a * math.cos(lat1) * math.cos(lon1) +
        b * math.cos(lat2) * math.cos(lon2);
    final y =
        a * math.cos(lat1) * math.sin(lon1) +
        b * math.cos(lat2) * math.sin(lon2);
    final z = a * math.sin(lat1) + b * math.sin(lat2);
    final latitude = _deg(math.atan2(z, math.sqrt(x * x + y * y)));
    final longitude = _unwrap(_deg(math.atan2(y, x)), previousLon);
    points.add(LatLng(latitude, longitude));
    previousLon = longitude;
  }
  points.add(LatLng(to.latitude, _unwrap(to.longitude, previousLon)));
  return points;
}

double _centralAngle(LatLng from, LatLng to) {
  final lat1 = _rad(from.latitude);
  final lat2 = _rad(to.latitude);
  final sinHalfLat = math.sin((lat2 - lat1) / 2);
  final sinHalfLon = math.sin(_rad(to.longitude - from.longitude) / 2);
  final h =
      sinHalfLat * sinHalfLat +
      math.cos(lat1) * math.cos(lat2) * sinHalfLon * sinHalfLon;
  return 2 * math.asin(math.min(1, math.sqrt(h)));
}

double _unwrap(double longitude, double previous) {
  return longitude + 360 * _rint((previous - longitude) / 360);
}

/// Java `Math.rint`: nearest integer, ties to even.
double _rint(double value) {
  if (value.isNaN || value.isInfinite) {
    return value;
  }
  final abs = value.abs();
  final truncated = abs.truncateToDouble();
  final fraction = abs - truncated;
  late final double magnitude;
  if (fraction > 0.5) {
    magnitude = truncated + 1;
  } else if (fraction < 0.5) {
    magnitude = truncated;
  } else if (truncated % 2 == 0) {
    magnitude = truncated;
  } else {
    magnitude = truncated + 1;
  }
  return value.isNegative ? -magnitude : magnitude;
}

double _rad(double degrees) => degrees * math.pi / 180;

double _deg(double radians) => radians * 180 / math.pi;
