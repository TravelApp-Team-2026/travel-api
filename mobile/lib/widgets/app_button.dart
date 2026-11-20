import 'package:flutter/material.dart';

import 'package:travel_app/theme/app_palette.dart';
import 'package:travel_app/theme/app_theme.dart';

class AppButton extends StatelessWidget {
  const AppButton({
    super.key,
    required this.label,
    required this.onPressed,
    this.isLoading = false,
    this.loadingLabel,
    this.expand = true,
  });

  final String label;
  final VoidCallback? onPressed;
  final bool isLoading;
  final String? loadingLabel;
  final bool expand;

  bool get _enabled => onPressed != null && !isLoading;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final button = FilledButton(
      onPressed: _enabled ? onPressed : null,
      style: appButtonStyle(
        colors: theme.colorScheme,
        palette: context.palette,
        textStyle: context.textRoles.button,
        isLoading: isLoading,
      ),
      child: isLoading ? _loadingChild(theme.colorScheme.onPrimary) : Text(label),
    );

    return LayoutBuilder(
      builder: (context, constraints) {
        if (expand && constraints.maxWidth.isFinite) {
          return SizedBox(width: constraints.maxWidth, child: button);
        }
        return button;
      },
    );
  }

  Widget _loadingChild(Color color) {
    return Row(
      mainAxisAlignment: MainAxisAlignment.center,
      mainAxisSize: MainAxisSize.min,
      children: [
        SizedBox(
          width: 16,
          height: 16,
          child: CircularProgressIndicator(strokeWidth: 2.4, color: color),
        ),
        const SizedBox(width: 8),
        Text(loadingLabel ?? label),
      ],
    );
  }
}
