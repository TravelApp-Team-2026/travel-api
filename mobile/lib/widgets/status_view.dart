import 'package:flutter/material.dart';

import 'package:travel_app/widgets/icon_badge.dart';

/// Shared layout for [EmptyStateView] and [ErrorView].
class StatusView extends StatelessWidget {
  const StatusView({
    super.key,
    required this.icon,
    required this.iconColor,
    required this.haloColor,
    required this.title,
    required this.message,
    this.haloRadius = 44,
    this.innerRadius = 32,
    this.action,
  });

  final IconData icon;
  final Color iconColor;
  final Color haloColor;
  final String title;
  final String message;
  final double haloRadius;
  final double innerRadius;
  final Widget? action;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final colors = theme.colorScheme;

    return Center(
      child: Padding(
        padding: const EdgeInsets.symmetric(horizontal: 32),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            IconBadge(
              icon: icon,
              color: iconColor,
              haloColor: haloColor,
              haloRadius: haloRadius,
              innerRadius: innerRadius,
            ),
            const SizedBox(height: 24),
            Text(
              title,
              textAlign: TextAlign.center,
              style: theme.textTheme.titleLarge?.copyWith(
                color: colors.onSurface,
              ),
            ),
            const SizedBox(height: 8),
            Text(
              message,
              textAlign: TextAlign.center,
              style: theme.textTheme.bodyMedium?.copyWith(
                color: colors.onSurfaceVariant,
              ),
            ),
            if (action != null) ...[const SizedBox(height: 24), action!],
          ],
        ),
      ),
    );
  }
}
