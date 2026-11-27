import 'package:flutter/material.dart';
import 'package:flutter_map/flutter_map.dart';
import 'package:latlong2/latlong.dart';
import 'package:travel_app/theme/app_colors.dart';
import 'package:travel_app/theme/transport_mode.dart';
import 'package:travel_app/widgets/map_geometry.dart';

const mapTrainTicksKey = Key('map-train-ticks');

/// Polylines and train ticks as separate [FlutterMap] children — not a nested
/// stack, so each layer participates in the map the way flutter_map expects.
List<Widget> mapRouteChildren(List<MapLeg> legs) {
  return [
    IgnorePointer(
      child: PolylineLayer(
        simplificationTolerance: 1,
        drawInSingleWorld: true,
        polylines: [for (final leg in legs) polylineFor(leg)],
      ),
    ),
    MapTrainTickLayer(legs: legs),
  ];
}

Polyline polylineFor(MapLeg leg) {
  final stroke = leg.mode.stroke;
  return Polyline(
    points: leg.points,
    color: leg.mode.color,
    strokeWidth: stroke.width,
    borderStrokeWidth: 2,
    borderColor: AppColors.routeHalo,
    pattern: stroke.style == TransportLineStyle.dashed
        ? StrokePattern.dashed(segments: stroke.dashPattern)
        : const StrokePattern.solid(),
    strokeCap: StrokeCap.round,
    strokeJoin: StrokeJoin.round,
  );
}

/// Screen-space ticks on train legs. Ignores pointers so pan/zoom and later
/// route taps reach the map.
class MapTrainTickLayer extends StatelessWidget {
  const MapTrainTickLayer({super.key, required this.legs});

  final List<MapLeg> legs;

  @override
  Widget build(BuildContext context) {
    final camera = MapCamera.of(context);
    return IgnorePointer(
      key: mapTrainTicksKey,
      child: CustomPaint(
        size: camera.nonRotatedSize,
        painter: _TrainTickPainter(
          camera: camera,
          legs: [
            for (final leg in legs)
              if (leg.mode == TransportMode.train) leg,
          ],
        ),
      ),
    );
  }
}

class _TrainTickPainter extends CustomPainter {
  _TrainTickPainter({required this.camera, required this.legs});

  final MapCamera camera;
  final List<MapLeg> legs;

  @override
  void paint(Canvas canvas, Size size) {
    final stroke = TransportMode.train.stroke;
    final paint = Paint()
      ..color = TransportMode.train.color
      ..strokeWidth = stroke.tickWidth
      ..strokeCap = StrokeCap.round;

    for (final leg in legs) {
      _paintTicks(canvas, _screenPoints(leg.points), stroke, paint);
    }
  }

  List<Offset> _screenPoints(List<LatLng> points) {
    return [for (final point in points) camera.latLngToScreenOffset(point)];
  }

  void _paintTicks(
    Canvas canvas,
    List<Offset> points,
    TransportStroke stroke,
    Paint paint,
  ) {
    if (points.length < 2) {
      return;
    }
    var remaining = stroke.tickSpacing / 2;
    for (var i = 0; i < points.length - 1; i++) {
      remaining = _ticksOnSegment(
        canvas,
        points[i],
        points[i + 1],
        remaining,
        stroke,
        paint,
      );
    }
  }

  double _ticksOnSegment(
    Canvas canvas,
    Offset start,
    Offset end,
    double remaining,
    TransportStroke stroke,
    Paint paint,
  ) {
    final delta = end - start;
    final length = delta.distance;
    if (length == 0) {
      return remaining;
    }
    final direction = delta / length;
    final normal = Offset(-direction.dy, direction.dx);
    final half = stroke.tickLength / 2;
    var distance = remaining;
    while (distance <= length) {
      final center = start + direction * distance;
      canvas.drawLine(center + normal * half, center - normal * half, paint);
      distance += stroke.tickSpacing;
    }
    return distance - length;
  }

  @override
  bool? hitTest(Offset position) => false;

  @override
  bool shouldRepaint(covariant _TrainTickPainter oldDelegate) {
    return oldDelegate.camera != camera || oldDelegate.legs != legs;
  }
}
