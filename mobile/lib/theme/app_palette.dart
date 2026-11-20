import 'package:flutter/material.dart';

import 'package:travel_app/theme/app_colors.dart';
import 'package:travel_app/theme/app_typography.dart';

/// Palette colors that [ColorScheme] does not model.
@immutable
class AppPalette extends ThemeExtension<AppPalette> {
  const AppPalette({
    required this.primaryPressed,
    required this.placeholder,
    required this.disabled,
    required this.success,
    required this.warning,
    required this.info,
    required this.danger,
    required this.shadow,
    required this.visitedCountry,
    required this.routeHalo,
    required this.marker,
  });

  final Color primaryPressed;
  final Color placeholder;
  final Color disabled;
  final Color success;
  final Color warning;
  final Color info;
  final Color danger;
  final Color shadow;
  final Color visitedCountry;
  final Color routeHalo;
  final Color marker;

  static const light = AppPalette(
    primaryPressed: AppColors.primaryPressed,
    placeholder: AppColors.placeholder,
    disabled: AppColors.disabled,
    success: AppColors.success,
    warning: AppColors.warning,
    info: AppColors.info,
    danger: AppColors.danger,
    shadow: AppColors.shadow,
    visitedCountry: AppColors.visitedCountry,
    routeHalo: AppColors.routeHalo,
    marker: AppColors.marker,
  );

  @override
  AppPalette copyWith({
    Color? primaryPressed,
    Color? placeholder,
    Color? disabled,
    Color? success,
    Color? warning,
    Color? info,
    Color? danger,
    Color? shadow,
    Color? visitedCountry,
    Color? routeHalo,
    Color? marker,
  }) {
    return AppPalette(
      primaryPressed: primaryPressed ?? this.primaryPressed,
      placeholder: placeholder ?? this.placeholder,
      disabled: disabled ?? this.disabled,
      success: success ?? this.success,
      warning: warning ?? this.warning,
      info: info ?? this.info,
      danger: danger ?? this.danger,
      shadow: shadow ?? this.shadow,
      visitedCountry: visitedCountry ?? this.visitedCountry,
      routeHalo: routeHalo ?? this.routeHalo,
      marker: marker ?? this.marker,
    );
  }

  @override
  AppPalette lerp(ThemeExtension<AppPalette>? other, double t) {
    if (other is! AppPalette) {
      return this;
    }
    return AppPalette(
      primaryPressed: Color.lerp(primaryPressed, other.primaryPressed, t)!,
      placeholder: Color.lerp(placeholder, other.placeholder, t)!,
      disabled: Color.lerp(disabled, other.disabled, t)!,
      success: Color.lerp(success, other.success, t)!,
      warning: Color.lerp(warning, other.warning, t)!,
      info: Color.lerp(info, other.info, t)!,
      danger: Color.lerp(danger, other.danger, t)!,
      shadow: Color.lerp(shadow, other.shadow, t)!,
      visitedCountry: Color.lerp(visitedCountry, other.visitedCountry, t)!,
      routeHalo: Color.lerp(routeHalo, other.routeHalo, t)!,
      marker: Color.lerp(marker, other.marker, t)!,
    );
  }
}

extension AppThemeContext on BuildContext {
  AppPalette get palette => Theme.of(this).extension<AppPalette>()!;

  AppTextRoles get textRoles => Theme.of(this).extension<AppTextRoles>()!;
}
