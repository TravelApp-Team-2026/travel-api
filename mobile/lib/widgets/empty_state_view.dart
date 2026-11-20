import 'package:flutter/material.dart';

import 'package:travel_app/widgets/app_button.dart';
import 'package:travel_app/widgets/status_view.dart';

class EmptyStateView extends StatelessWidget {
  const EmptyStateView({
    super.key,
    required this.icon,
    required this.title,
    required this.message,
    this.actionLabel,
    this.onAction,
  });

  final IconData icon;
  final String title;
  final String message;
  final String? actionLabel;
  final VoidCallback? onAction;

  @override
  Widget build(BuildContext context) {
    final colors = Theme.of(context).colorScheme;
    final showAction = actionLabel != null && onAction != null;

    return StatusView(
      icon: icon,
      iconColor: colors.primary,
      haloColor: colors.primaryContainer,
      title: title,
      message: message,
      action: showAction
          ? AppButton(label: actionLabel!, onPressed: onAction)
          : null,
    );
  }
}
