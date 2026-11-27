import 'package:flutter/material.dart';
import 'package:flutter_map/flutter_map.dart';
import 'package:latlong2/latlong.dart';
import 'package:travel_app/theme/app_colors.dart';
import 'package:travel_app/theme/transport_mode.dart';
import 'package:travel_app/widgets/map_geometry.dart';

const mapTrainTicksKey = Key('map-train-ticks');

/// Polylines and train ticks as separate [FlutterMap] children — not a nested
/// stack, so each layer participates in the map the way flutter_map expects.
List<Widget> mapRouteChildren(
  List<MapLeg> legs, {
  int? selectedIndex,
  LayerHitNotifier<int>? hitNotifier,
}) {
  final selected =
      selectedIndex != null && selectedIndex >= 0 && selectedIndex < legs.length
      ? selectedIndex
      : null;
  Widget layer = PolylineLayer<int>(
    simplificationTolerance: 0,
    cullingMargin: null,
    drawInSingleWorld: true,
    hitNotifier: hitNotifier,
    polylines: [
      if (selected != null)
        Polyline(
          points: legs[selected].points,
          color: AppColors.primary.withValues(alpha: 0.28),
          strokeWidth: 16,
          strokeCap: StrokeCap.round,
          strokeJoin: StrokeJoin.round,
          hitValue: selected,
        ),
      for (var i = 0; i < legs.length; i++)
        polylineFor(
          legs[i],
          hitValue: i,
          selected: selected == i,
          dimmed: selected != null && selected != i,
        ),
    ],
  );
  if (hitNotifier == null) {
    layer = IgnorePointer(child: layer);
  }
  return [layer, MapTrainTickLayer(legs: legs, selectedIndex: selected)];
}

Polyline<int> polylineFor(
  MapLeg leg, {
  int? hitValue,
  bool selected = false,
  bool dimmed = false,
}) {
  final stroke = leg.mode.stroke;
  var color = leg.mode.color;
  var border = AppColors.routeHalo;
  if (dimmed) {
    color = color.withValues(alpha: 0.45);
    border = border.withValues(alpha: 0.45);
  }
  return Polyline(
    points: leg.points,
    color: color,
    strokeWidth: selected ? 5.5 : stroke.width,
    borderStrokeWidth: selected ? 2.75 : 2,
    borderColor: border,
    pattern: stroke.style == TransportLineStyle.dashed
        ? StrokePattern.dashed(segments: stroke.dashPattern)
        : const StrokePattern.solid(),
    strokeCap: StrokeCap.round,
    strokeJoin: StrokeJoin.round,
    hitValue: hitValue,
  );
}

/// Screen-space ticks on train legs. Ignores pointers so pan/zoom and later
/// route taps reach the map.
class MapTrainTickLayer extends StatelessWidget {
  const MapTrainTickLayer({super.key, required this.legs, this.selectedIndex});

  final List<MapLeg> legs;
  final int? selectedIndex;

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
            for (var i = 0; i < legs.length; i++)
              if (legs[i].mode == TransportMode.train)
                (
                  leg: legs[i],
                  dimmed: selectedIndex != null && selectedIndex != i,
                ),
          ],
        ),
      ),
    );
  }
}

class _TrainTickPainter extends CustomPainter {
  _TrainTickPainter({required this.camera, required this.legs});

  final MapCamera camera;
  final List<({MapLeg leg, bool dimmed})> legs;

  @override
  void paint(Canvas canvas, Size size) {
    final stroke = TransportMode.train.stroke;
    for (final item in legs) {
      final paint = Paint()
        ..color = item.dimmed
            ? TransportMode.train.color.withValues(alpha: 0.45)
            : TransportMode.train.color
        ..strokeWidth = stroke.tickWidth
        ..strokeCap = StrokeCap.round;
      _paintTicks(canvas, _screenPoints(item.leg.points), stroke, paint);
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
