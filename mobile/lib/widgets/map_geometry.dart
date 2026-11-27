import 'dart:math' as math;

import 'package:latlong2/latlong.dart';
import 'package:travel_app/services/place_search_api.dart';
import 'package:travel_app/services/trips_api.dart';
import 'package:travel_app/theme/transport_mode.dart';

/// Enough points that a short hop still looks like a curve when zoomed out.
const planeArcSegments = 48;

/// Control-point offset as a fraction of the chord. Quadratic sagitta is half
/// of this, so ~0.2 of the straight distance — the same visual angle on a
/// short hop and a transatlantic flight.
const planeArcBulge = 0.4;

/// ~30 m. OSRM can emit a point every few metres; the map cannot paint that.
const _lineSimplifyEpsilonDeg = 0.0003;
const _maxLinePoints = 192;

/// One drawable leg on the global map.
class MapLeg {
  const MapLeg({required this.mode, required this.points});

  final TransportMode mode;
  final List<LatLng> points;
}

/// A stop on the trip-details map. Repeat visits to the same coordinates share
/// one marker, labelled with every order (e.g. `1·6`).
class NumberedStop {
  const NumberedStop({
    required this.point,
    required this.placeName,
    required this.label,
    required this.orders,
  });

  final LatLng point;
  final String placeName;
  final String label;
  final List<int> orders;
}

/// Groups waypoints that sit on the same coordinates. Orders are 1-based.
List<NumberedStop> numberedStopsFromTrip(TripDetails trip) {
  final groups = <String, _StopGroup>{};
  final order = <String>[];
  for (var i = 0; i < trip.waypoints.length; i++) {
    final place = trip.waypoints[i];
    final key = '${place.latitude},${place.longitude}';
    final group = groups.putIfAbsent(key, () {
      order.add(key);
      return _StopGroup(place);
    });
    group.orders.add(i + 1);
  }
  return [
    for (final key in order)
      NumberedStop(
        point: LatLng(
          groups[key]!.place.latitude,
          groups[key]!.place.longitude,
        ),
        placeName: groups[key]!.place.placeName,
        label: groups[key]!.orders.join('·'),
        orders: List.unmodifiable(groups[key]!.orders),
      ),
  ];
}

class _StopGroup {
  _StopGroup(this.place);

  final Place place;
  final orders = <int>[];
}

/// Turns saved trip segments into map lines. API geometry is the source of
/// truth for roads. Plane always gets a display arc from the endpoints so the
/// bulge stays visible. Train and other fall back to a straight pair.
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

/// Quadratic display arc in map-shaped (lon·cos φ, lat) space so the curve is
/// symmetric on Mercator. The peak always sits north of the chord — a bulge
/// toward the top of the map, never a dip.
List<LatLng> planeArc(LatLng from, LatLng to) {
  final lon2 = _unwrap(to.longitude, from.longitude);
  final midLat = (from.latitude + to.latitude) / 2;
  final cosLat = math.cos(_rad(midLat)).clamp(0.2, 1.0);
  final x1 = from.longitude * cosLat;
  final y1 = from.latitude;
  final x2 = lon2 * cosLat;
  final y2 = to.latitude;
  final dx = x2 - x1;
  final dy = y2 - y1;
  final chord = math.sqrt(dx * dx + dy * dy);
  if (chord < 1e-9) {
    return [from, to];
  }
  var nx = -dy / chord;
  var ny = dx / chord;
  if (ny < 0) {
    nx = -nx;
    ny = -ny;
  }
  final cx = (x1 + x2) / 2 + nx * planeArcBulge * chord;
  final cy = ((y1 + y2) / 2 + ny * planeArcBulge * chord).clamp(-85.0, 85.0);
  final points = <LatLng>[from];
  var previousLon = from.longitude;
  for (var i = 1; i < planeArcSegments; i++) {
    final t = i / planeArcSegments;
    final u = 1 - t;
    final x = u * u * x1 + 2 * u * t * cx + t * t * x2;
    final y = u * u * y1 + 2 * u * t * cy + t * t * y2;
    final longitude = _unwrap(x / cosLat, previousLon);
    points.add(LatLng(y, longitude));
    previousLon = longitude;
  }
  points.add(LatLng(to.latitude, _unwrap(lon2, previousLon)));
  return points;
}

/// Keeps a longitude in (-180, 180]. Pacific arcs stay unwrapped when drawn
/// so the line takes the short ocean path; wrap only for bounds / equality.
double wrapLongitude(double longitude) {
  var wrapped = longitude % 360;
  if (wrapped > 180) {
    wrapped -= 360;
  } else if (wrapped <= -180) {
    wrapped += 360;
  }
  return wrapped;
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
