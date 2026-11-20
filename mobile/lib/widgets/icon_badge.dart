import 'package:flutter/material.dart';

class IconBadge extends StatelessWidget {
  const IconBadge({
    super.key,
    required this.icon,
    required this.color,
    required this.haloColor,
    this.haloRadius = 44,
    this.innerRadius = 32,
  });

  final IconData icon;
  final Color color;
  final Color haloColor;
  final double haloRadius;
  final double innerRadius;

  @override
  Widget build(BuildContext context) {
    final surface = Theme.of(context).colorScheme.surface;
    final border = Theme.of(context).colorScheme.outline;

    return SizedBox(
      width: haloRadius * 2,
      height: haloRadius * 2,
      child: Stack(
        alignment: Alignment.center,
        children: [
          Container(
            width: haloRadius * 2,
            height: haloRadius * 2,
            decoration: BoxDecoration(color: haloColor, shape: BoxShape.circle),
          ),
          Container(
            width: innerRadius * 2,
            height: innerRadius * 2,
            decoration: BoxDecoration(
              color: surface,
              shape: BoxShape.circle,
              border: Border.all(color: border, width: 1.5),
            ),
            child: Icon(icon, color: color, size: innerRadius * 0.9),
          ),
        ],
      ),
    );
  }
}
