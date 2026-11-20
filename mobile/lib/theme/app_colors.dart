import 'package:flutter/material.dart';

/// Colors from `docs/mockups/paleta_kolorow.svg`.
///
/// Brand, surface and error colors also go into [ColorScheme]. State colors
/// (success, warning, info, danger) and the pressed-button color stay here,
/// because Material has no role for them.
abstract final class AppColors {
  static const Color primary = Color(0xFF087F75);
  static const Color primaryPressed = Color(0xFF066A62);
  static const Color primaryTint = Color(0xFFD4E8E1);
  static const Color onPrimary = Color(0xFFFFFFFF);

  static const Color background = Color(0xFFFBF1C7);
  static const Color surface = Color(0xFFFFFBEA);
  static const Color surfaceVariant = Color(0xFFEBDBB2);
  static const Color border = Color(0xFFD5C4A1);

  static const Color text = Color(0xFF3C3836);
  static const Color textSecondary = Color(0xFF665C54);
  static const Color placeholder = Color(0xFF7C6F64);
  static const Color disabled = Color(0xFFD5C4A1);

  static const Color success = Color(0xFF6F6B0D);
  static const Color warning = Color(0xFF9A5F0A);
  static const Color error = Color(0xFF9D0006);
  static const Color info = Color(0xFF076678);
  static const Color danger = Color(0xFFCC241D);

  /// Card shadow from the palette: #3C3836 at 8%, blur 12, offset y 4.
  static const Color shadow = Color(0x143C3836);
  static const Color visitedCountry = Color(0x40087F75);
  static const Color routeHalo = Color(0xFFFFFBEA);
  static const Color marker = Color(0xFF3C3836);

  static const List<BoxShadow> cardShadow = [
    BoxShadow(color: shadow, blurRadius: 12, offset: Offset(0, 4)),
  ];
}
