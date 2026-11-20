import 'package:flutter/material.dart';

/// How a leg is drawn. Numbers come from `docs/mockups/paleta_kolorow.svg`.
enum TransportLineStyle { solid, arc, ticked, dashed }

/// Stroke shared by the map and the charts so neither screen invents its own.
@immutable
class TransportStroke {
  const TransportStroke({
    required this.style,
    this.width = 4,
    this.dashPattern = const [],
    this.tickSpacing = 16,
    this.tickLength = 10,
    this.tickWidth = 2,
  });

  final TransportLineStyle style;
  final double width;

  /// Empty unless [style] is [TransportLineStyle.dashed]. Values are on, off.
  final List<double> dashPattern;

  final double tickSpacing;
  final double tickLength;
  final double tickWidth;
}

/// One definition of color, text color, icon and line style per transport mode.
enum TransportMode {
  car(
    label: 'Samochód',
    color: Color(0xFF458588),
    onColor: Color(0xFF2E6A6D),
    icon: Icons.directions_car_outlined,
    stroke: TransportStroke(style: TransportLineStyle.solid),
  ),
  plane(
    label: 'Samolot',
    color: Color(0xFF98971A),
    onColor: Color(0xFF6B6A0F),
    icon: Icons.flight,
    stroke: TransportStroke(style: TransportLineStyle.arc),
  ),
  train(
    label: 'Pociąg',
    color: Color(0xFFD79921),
    onColor: Color(0xFF8A5A00),
    icon: Icons.train_outlined,
    stroke: TransportStroke(style: TransportLineStyle.ticked),
  ),
  other(
    label: 'Inne',
    color: Color(0xFFB16286),
    onColor: Color(0xFF8F3F68),
    icon: Icons.more_horiz,
    stroke: TransportStroke(
      style: TransportLineStyle.dashed,
      dashPattern: [3, 9],
    ),
  );

  const TransportMode({
    required this.label,
    required this.color,
    required this.onColor,
    required this.icon,
    required this.stroke,
  });

  final String label;
  final Color color;
  final Color onColor;
  final IconData icon;
  final TransportStroke stroke;

  TransportLineStyle get lineStyle => stroke.style;
}
