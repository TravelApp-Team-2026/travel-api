import 'package:flutter/material.dart';
import 'package:travel_app/theme/transport_mode.dart';

/// Draws the T-014 stroke for a mode. Used by the design-system screen and the
/// map legend so the preview matches the line on the map.
class TransportLinePainter extends CustomPainter {
  const TransportLinePainter(this.mode);

  final TransportMode mode;

  @override
  void paint(Canvas canvas, Size size) {
    final stroke = mode.stroke;
    final paint = Paint()
      ..color = mode.color
      ..style = PaintingStyle.stroke
      ..strokeWidth = stroke.width.clamp(1, size.height / 2)
      ..strokeCap = StrokeCap.round
      ..strokeJoin = StrokeJoin.round;

    final start = Offset(0, size.height * 0.7);
    final end = Offset(size.width, size.height * 0.7);

    switch (stroke.style) {
      case TransportLineStyle.solid:
        canvas.drawLine(start, end, paint);
      case TransportLineStyle.arc:
        final path = Path()
          ..moveTo(start.dx, start.dy)
          ..quadraticBezierTo(
            size.width / 2,
            size.height * 0.15,
            end.dx,
            end.dy,
          );
        canvas.drawPath(path, paint);
      case TransportLineStyle.dashed:
        _drawDashed(canvas, start, end, paint, stroke.dashPattern);
      case TransportLineStyle.ticked:
        canvas.drawLine(start, end, paint);
        final tickPaint = Paint()
          ..color = mode.color
          ..strokeWidth = stroke.tickWidth
          ..strokeCap = StrokeCap.round;
        var spacing = stroke.tickSpacing;
        if (spacing * 3 > size.width) {
          spacing = size.width / 4;
        }
        if (spacing <= 0) {
          break;
        }
        final half = stroke.tickLength / 2;
        var x = spacing / 2;
        while (x <= size.width) {
          canvas.drawLine(
            Offset(x, start.dy - half),
            Offset(x, start.dy + half),
            tickPaint,
          );
          x += spacing;
        }
    }
  }

  void _drawDashed(
    Canvas canvas,
    Offset start,
    Offset end,
    Paint paint,
    List<double> pattern,
  ) {
    if (pattern.isEmpty) {
      canvas.drawLine(start, end, paint);
      return;
    }
    final delta = end - start;
    final total = delta.distance;
    if (total == 0) {
      return;
    }
    final direction = delta / total;
    var distance = 0.0;
    var draw = true;
    var index = 0;
    while (distance < total) {
      final length = pattern[index % pattern.length];
      final next = distance + length > total ? total : distance + length;
      if (draw) {
        canvas.drawLine(
          start + direction * distance,
          start + direction * next,
          paint,
        );
      }
      draw = !draw;
      distance = next;
      index++;
    }
  }

  @override
  bool shouldRepaint(covariant TransportLinePainter oldDelegate) {
    return oldDelegate.mode != mode;
  }
}
