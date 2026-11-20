import 'package:flutter/material.dart';

import 'package:travel_app/theme/app_colors.dart';
import 'package:travel_app/theme/app_palette.dart';
import 'package:travel_app/theme/app_typography.dart';

abstract final class AppTheme {
  static ThemeData get light {
    const colorScheme = ColorScheme(
      brightness: Brightness.light,
      primary: AppColors.primary,
      onPrimary: AppColors.onPrimary,
      primaryContainer: AppColors.primaryTint,
      onPrimaryContainer: AppColors.primaryPressed,
      secondary: AppColors.primary,
      onSecondary: AppColors.onPrimary,
      secondaryContainer: AppColors.primaryTint,
      onSecondaryContainer: AppColors.text,
      error: AppColors.error,
      onError: AppColors.onPrimary,
      surface: AppColors.surface,
      onSurface: AppColors.text,
      onSurfaceVariant: AppColors.textSecondary,
      outline: AppColors.border,
      outlineVariant: AppColors.border,
      // Material multiplies this color by its own opacity. The 8% card shadow
      // used by our own boxes is [AppPalette.shadow].
      shadow: AppColors.text,
      surfaceTint: AppColors.primary,
      surfaceContainerLowest: AppColors.surface,
      surfaceContainerLow: AppColors.background,
      surfaceContainer: AppColors.surface,
      surfaceContainerHigh: AppColors.surfaceVariant,
      surfaceContainerHighest: AppColors.surfaceVariant,
    );

    final borderRadius = BorderRadius.circular(12);
    OutlineInputBorder border(Color color, double width) {
      return OutlineInputBorder(
        borderRadius: borderRadius,
        borderSide: BorderSide(color: color, width: width),
      );
    }

    return ThemeData(
      colorScheme: colorScheme,
      scaffoldBackgroundColor: AppColors.background,
      fontFamily: AppTypography.fontFamily,
      fontFamilyFallback: AppTypography.fontFamilyFallback,
      textTheme: AppTypography.textTheme.apply(
        bodyColor: AppColors.text,
        displayColor: AppColors.text,
      ),
      extensions: const [AppPalette.light, AppTextRoles.light],
      appBarTheme: AppBarTheme(
        backgroundColor: AppColors.background,
        foregroundColor: AppColors.text,
        elevation: 0,
        scrolledUnderElevation: 0,
        centerTitle: false,
        surfaceTintColor: Colors.transparent,
        titleTextStyle: AppTypography.appBar.copyWith(color: AppColors.text),
      ),
      dividerTheme: const DividerThemeData(
        color: AppColors.border,
        thickness: 1,
        space: 1,
      ),
      progressIndicatorTheme: const ProgressIndicatorThemeData(
        color: AppColors.primary,
        circularTrackColor: AppColors.border,
      ),
      textSelectionTheme: const TextSelectionThemeData(
        cursorColor: AppColors.primary,
        selectionColor: AppColors.primaryTint,
      ),
      inputDecorationTheme: InputDecorationTheme(
        filled: true,
        fillColor: AppColors.surface,
        isDense: true,
        contentPadding: const EdgeInsets.symmetric(
          horizontal: 16,
          vertical: 14,
        ),
        hintStyle: AppTypography.field.copyWith(color: AppColors.placeholder),
        enabledBorder: border(AppColors.border, 1.5),
        focusedBorder: border(AppColors.primary, 1.5),
        disabledBorder: border(AppColors.border, 1.5),
        errorBorder: border(AppColors.error, 2),
        focusedErrorBorder: border(AppColors.error, 2),
      ),
      filledButtonTheme: FilledButtonThemeData(
        style: appButtonStyle(
          colors: colorScheme,
          palette: AppPalette.light,
          textStyle: AppTextRoles.light.button,
          isLoading: false,
        ),
      ),
    );
  }
}

ButtonStyle appButtonStyle({
  required ColorScheme colors,
  required AppPalette palette,
  required TextStyle textStyle,
  required bool isLoading,
}) {
  return ButtonStyle(
    backgroundColor: WidgetStateProperty.resolveWith((states) {
      if (isLoading) {
        return colors.primary;
      }
      if (states.contains(WidgetState.disabled)) {
        return palette.disabled;
      }
      if (states.contains(WidgetState.pressed)) {
        return palette.primaryPressed;
      }
      return colors.primary;
    }),
    foregroundColor: WidgetStateProperty.resolveWith((states) {
      if (isLoading) {
        return colors.onPrimary;
      }
      if (states.contains(WidgetState.disabled)) {
        return palette.placeholder;
      }
      return colors.onPrimary;
    }),
    overlayColor: const WidgetStatePropertyAll(Colors.transparent),
    elevation: const WidgetStatePropertyAll(0),
    minimumSize: const WidgetStatePropertyAll(Size(64, 52)),
    padding: const WidgetStatePropertyAll(
      EdgeInsets.symmetric(horizontal: 20),
    ),
    shape: const WidgetStatePropertyAll(
      RoundedRectangleBorder(
        borderRadius: BorderRadius.all(Radius.circular(14)),
      ),
    ),
    textStyle: WidgetStatePropertyAll(textStyle),
  );
}
