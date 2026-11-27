import 'package:flutter/material.dart';
import 'package:travel_app/theme/app_colors.dart';

const tripStopCircleSize = 22.0;
const tripStopPillWidth = 38.0;

/// Numbered stop from `docs/mockups/trip_details.svg`. One visit is a circle;
/// a return to the same coordinates is a pill (`1·6`).
class TripStopMarker extends StatelessWidget {
  const TripStopMarker({super.key, required this.label, this.onTap});

  final String label;
  final VoidCallback? onTap;

  bool get isPill => label.contains('·');

  double get width => isPill ? tripStopPillWidth : tripStopCircleSize;

  @override
  Widget build(BuildContext context) {
    final marker = DecoratedBox(
      decoration: BoxDecoration(
        color: AppColors.marker,
        borderRadius: BorderRadius.circular(11),
        border: Border.all(color: AppColors.background, width: 2.5),
      ),
      child: SizedBox(
        width: width,
        height: tripStopCircleSize,
        child: Center(
          child: Text(
            label,
            style: const TextStyle(
              color: AppColors.onPrimary,
              fontSize: 12,
              fontWeight: FontWeight.w700,
              height: 1,
            ),
          ),
        ),
      ),
    );
    if (onTap == null) {
      return marker;
    }
    return GestureDetector(onTap: onTap, child: marker);
  }
}
