import 'package:flutter/material.dart';

import 'package:travel_app/theme/app_palette.dart';
import 'package:travel_app/widgets/app_button.dart';
import 'package:travel_app/widgets/status_view.dart';

class ErrorView extends StatelessWidget {
  const ErrorView({
    super.key,
    required this.title,
    required this.message,
    this.icon = Icons.cloud_off_outlined,
    this.onRetry,
    this.retryLabel = 'Spróbuj ponownie',
  });

  final String title;
  final String message;
  final IconData icon;
  final VoidCallback? onRetry;
  final String retryLabel;

  @override
  Widget build(BuildContext context) {
    final warning = context.palette.warning;

    return StatusView(
      icon: icon,
      iconColor: warning,
      haloColor: warning.withValues(alpha: 0.14),
      haloRadius: 76,
      innerRadius: 56,
      title: title,
      message: message,
      action: onRetry == null
          ? null
          : AppButton(label: retryLabel, onPressed: onRetry),
    );
  }
}
