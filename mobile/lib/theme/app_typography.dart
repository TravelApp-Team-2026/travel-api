import 'package:flutter/material.dart';

/// Type scale from `docs/mockups/komponenty.svg`.
///
/// Styles carry size and weight only. Color comes from [ColorScheme] or
/// [AppPalette] at the place that paints the text.
abstract final class AppTypography {
  static const String fontFamily = 'Inter';
  static const List<String> fontFamilyFallback = ['InterExt'];

  static TextStyle style({
    required double size,
    required FontWeight weight,
    double? letterSpacing,
  }) {
    return TextStyle(
      fontFamily: fontFamily,
      fontFamilyFallback: fontFamilyFallback,
      fontSize: size,
      fontWeight: weight,
      letterSpacing: letterSpacing,
    );
  }

  static TextStyle get screenTitle =>
      style(size: 24, weight: FontWeight.w700);

  static TextStyle get logo => style(size: 22, weight: FontWeight.w700);

  static TextStyle get sectionHeader =>
      style(size: 20, weight: FontWeight.w700);

  static TextStyle get appBar => style(size: 18, weight: FontWeight.w700);

  static TextStyle get cardTitle => style(size: 16, weight: FontWeight.w700);

  static TextStyle get button => style(size: 15, weight: FontWeight.w700);

  static TextStyle get field => style(size: 15, weight: FontWeight.w400);

  static TextStyle get body => style(size: 14, weight: FontWeight.w400);

  static TextStyle get subtitle => style(size: 13, weight: FontWeight.w400);

  static TextStyle get label => style(size: 12, weight: FontWeight.w600);

  static TextStyle get caption => style(size: 11, weight: FontWeight.w400);

  static TextStyle get navigation => style(size: 11, weight: FontWeight.w500);

  static TextStyle get listHeader => style(
    size: 11,
    weight: FontWeight.w700,
    letterSpacing: 1,
  );

  static TextTheme get textTheme {
    return TextTheme(
      headlineMedium: screenTitle,
      headlineSmall: logo,
      titleLarge: sectionHeader,
      titleMedium: appBar,
      titleSmall: cardTitle,
      bodyLarge: field,
      bodyMedium: body,
      bodySmall: subtitle,
      labelLarge: label,
      labelMedium: navigation,
      labelSmall: caption,
    );
  }
}

/// Text roles that have no slot on [TextTheme].
@immutable
class AppTextRoles extends ThemeExtension<AppTextRoles> {
  const AppTextRoles({required this.button, required this.listHeader});

  final TextStyle button;
  final TextStyle listHeader;

  static const light = AppTextRoles(
    button: TextStyle(
      fontFamily: AppTypography.fontFamily,
      fontFamilyFallback: AppTypography.fontFamilyFallback,
      fontSize: 15,
      fontWeight: FontWeight.w700,
    ),
    listHeader: TextStyle(
      fontFamily: AppTypography.fontFamily,
      fontFamilyFallback: AppTypography.fontFamilyFallback,
      fontSize: 11,
      fontWeight: FontWeight.w700,
      letterSpacing: 1,
    ),
  );

  @override
  AppTextRoles copyWith({TextStyle? button, TextStyle? listHeader}) {
    return AppTextRoles(
      button: button ?? this.button,
      listHeader: listHeader ?? this.listHeader,
    );
  }

  @override
  AppTextRoles lerp(ThemeExtension<AppTextRoles>? other, double t) {
    if (other is! AppTextRoles) {
      return this;
    }
    return AppTextRoles(
      button: TextStyle.lerp(button, other.button, t)!,
      listHeader: TextStyle.lerp(listHeader, other.listHeader, t)!,
    );
  }
}
