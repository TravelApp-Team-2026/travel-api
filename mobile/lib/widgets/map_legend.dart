import 'package:flutter/material.dart';
import 'package:travel_app/theme/app_colors.dart';
import 'package:travel_app/theme/transport_line.dart';
import 'package:travel_app/theme/transport_mode.dart';

/// Bottom-left legend from `docs/mockups/map_global.svg`.
class MapLegend extends StatelessWidget {
  const MapLegend({super.key});

  @override
  Widget build(BuildContext context) {
    return Material(
      color: AppColors.surface.withValues(alpha: 0.96),
      elevation: 0,
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(12),
        side: const BorderSide(color: AppColors.border),
      ),
      child: Padding(
        padding: const EdgeInsets.fromLTRB(10, 10, 12, 10),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            for (final mode in TransportMode.values)
              Padding(
                padding: const EdgeInsets.symmetric(vertical: 3),
                child: Row(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    CustomPaint(
                      size: const Size(24, 14),
                      painter: TransportLinePainter(mode),
                    ),
                    const SizedBox(width: 10),
                    Text(
                      mode.label,
                      style: Theme.of(context).textTheme.labelMedium?.copyWith(
                        color: AppColors.text,
                        fontWeight: FontWeight.w600,
                      ),
                    ),
                  ],
                ),
              ),
          ],
        ),
      ),
    );
  }
}
