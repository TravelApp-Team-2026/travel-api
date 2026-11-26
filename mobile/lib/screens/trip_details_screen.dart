import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:travel_app/services/api_exception.dart';
import 'package:travel_app/services/trips_api.dart';
import 'package:travel_app/theme/app_colors.dart';
import 'package:travel_app/theme/app_palette.dart';
import 'package:travel_app/theme/transport_mode.dart';
import 'package:travel_app/widgets/error_view.dart';
import 'package:travel_app/widgets/loading_view.dart';

class TripDetailsScreen extends ConsumerStatefulWidget {
  const TripDetailsScreen({super.key, required this.tripId});

  final int? tripId;

  @override
  ConsumerState<TripDetailsScreen> createState() => _TripDetailsScreenState();
}

enum _Status { loading, ready, error }

class _TripDetailsScreenState extends ConsumerState<TripDetailsScreen> {
  var _status = _Status.loading;
  var _request = 0;
  var _deleting = false;
  TripDetails? _trip;
  String? _loadError;
  String? _deleteError;

  @override
  void initState() {
    super.initState();
    unawaited(_load());
  }

  Future<void> _load({bool fromRefresh = false}) async {
    final id = widget.tripId;
    if (id == null) {
      setState(() {
        _status = _Status.error;
        _loadError = 'Nie znaleziono podróży.';
      });
      return;
    }
    final request = ++_request;
    final showSpinner = !fromRefresh || _trip == null;
    setState(() {
      if (showSpinner) {
        _status = _Status.loading;
      }
      _loadError = null;
      _deleteError = null;
    });
    try {
      final trip = await ref.read(tripsApiProvider).getTrip(id);
      if (!mounted || request != _request) {
        return;
      }
      setState(() {
        _trip = trip;
        _status = _Status.ready;
      });
    } on ApiException catch (error) {
      if (!mounted || request != _request) {
        return;
      }
      setState(() {
        _loadError = error.message;
        if (showSpinner) {
          _status = _Status.error;
        }
      });
    }
  }

  Future<void> _edit() async {
    final id = widget.tripId;
    if (id == null || _deleting) {
      return;
    }
    final saved = await context.push<bool>('/trips/$id/edit');
    if (saved == true && mounted) {
      await _load(fromRefresh: true);
    }
  }

  Future<void> _delete() async {
    final trip = _trip;
    if (trip == null || _deleting) {
      return;
    }
    final confirmed = await showDialog<bool>(
      context: context,
      barrierColor: const Color(0x8C3C3836),
      builder: (context) => _DeleteTripDialog(title: trip.title),
    );
    if (confirmed != true || !mounted) {
      return;
    }
    setState(() {
      _deleting = true;
      _deleteError = null;
    });
    try {
      await ref.read(tripsApiProvider).deleteTrip(trip.id);
      if (!mounted) {
        return;
      }
      if (context.canPop()) {
        context.pop();
      } else {
        setState(() => _deleting = false);
        context.go('/trips');
      }
    } on ApiException catch (error) {
      if (!mounted) {
        return;
      }
      setState(() {
        _deleting = false;
        _deleteError = error.message;
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    return PopScope(
      canPop: !_deleting,
      child: Scaffold(
        appBar: AppBar(
          leading: IconButton(
            icon: const Icon(Icons.arrow_back),
            onPressed: _deleting ? null : () => context.pop(),
          ),
          actions: [
            if (_status == _Status.ready) ...[
              IconButton(
                key: const Key('edit-trip'),
                icon: const Icon(Icons.edit_outlined),
                onPressed: _deleting ? null : () => unawaited(_edit()),
              ),
              IconButton(
                key: const Key('delete-trip'),
                icon: Icon(Icons.delete_outline, color: context.palette.danger),
                onPressed: _deleting ? null : () => unawaited(_delete()),
              ),
            ],
          ],
        ),
        body: switch (_status) {
          _Status.loading => const LoadingView(),
          _Status.error => ErrorView(
            title: 'Nie udało się wczytać podróży',
            message: _loadError ?? 'Spróbuj ponownie.',
            onRetry: () => unawaited(_load()),
          ),
          _Status.ready => _body(),
        },
      ),
    );
  }

  Widget _body() {
    final trip = _trip!;
    final theme = Theme.of(context);
    final colors = theme.colorScheme;
    final description = trip.description?.trim();

    return ListView(
      padding: const EdgeInsets.fromLTRB(16, 8, 16, 32),
      children: [
        Text(trip.title, style: theme.textTheme.headlineMedium),
        const SizedBox(height: 8),
        Row(
          children: [
            Icon(
              Icons.calendar_today_outlined,
              size: 16,
              color: colors.onSurfaceVariant,
            ),
            const SizedBox(width: 8),
            Expanded(
              child: Text(
                formatTripDateRange(trip.startDate, trip.endDate),
                style: theme.textTheme.bodySmall?.copyWith(
                  color: colors.onSurfaceVariant,
                ),
              ),
            ),
          ],
        ),
        if (description != null && description.isNotEmpty) ...[
          const SizedBox(height: 12),
          Text(description, style: theme.textTheme.bodyMedium),
        ],
        const SizedBox(height: 16),
        Row(
          children: [
            Expanded(
              child: _StatCard(
                value: formatDistanceKm(trip.totalDistanceKm),
                label: 'łączny dystans',
              ),
            ),
            const SizedBox(width: 8),
            Expanded(
              child: _StatCard(
                value: '${trip.waypoints.length}',
                label: formatStopNoun(trip.waypoints.length),
              ),
            ),
            const SizedBox(width: 8),
            Expanded(
              child: _StatCard(
                value: '${trip.legs.length}',
                label: formatSegmentNoun(trip.legs.length),
              ),
            ),
          ],
        ),
        if (_deleteError != null || _loadError != null) ...[
          const SizedBox(height: 16),
          Text(
            _deleteError ?? _loadError!,
            style: theme.textTheme.labelLarge?.copyWith(color: colors.error),
          ),
        ],
        const SizedBox(height: 24),
        Text('Odcinki', style: theme.textTheme.titleMedium),
        const SizedBox(height: 8),
        for (var i = 0; i < trip.legs.length; i++) ...[
          if (i > 0) const Divider(height: 1),
          _LegTile(
            from: trip.waypoints[i].placeName,
            to: trip.waypoints[i + 1].placeName,
            mode: trip.legs[i].mode,
            distanceKm: trip.legs[i].distanceKm,
          ),
        ],
      ],
    );
  }
}

class _StatCard extends StatelessWidget {
  const _StatCard({required this.value, required this.label});

  final String value;
  final String label;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final colors = theme.colorScheme;

    return Container(
      padding: const EdgeInsets.fromLTRB(14, 12, 14, 12),
      decoration: BoxDecoration(
        color: colors.surface,
        borderRadius: BorderRadius.circular(14),
        border: Border.all(color: colors.outline, width: 1.5),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            value,
            maxLines: 1,
            overflow: TextOverflow.ellipsis,
            style: theme.textTheme.titleSmall?.copyWith(
              color: context.palette.primaryPressed,
            ),
          ),
          const SizedBox(height: 4),
          Text(
            label,
            maxLines: 1,
            overflow: TextOverflow.ellipsis,
            style: theme.textTheme.labelSmall?.copyWith(
              color: colors.onSurfaceVariant,
            ),
          ),
        ],
      ),
    );
  }
}

class _LegTile extends StatelessWidget {
  const _LegTile({
    required this.from,
    required this.to,
    required this.mode,
    required this.distanceKm,
  });

  final String from;
  final String to;
  final TransportMode mode;
  final double distanceKm;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final colors = theme.colorScheme;

    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 12),
      child: Row(
        children: [
          CircleAvatar(
            radius: 16,
            backgroundColor: mode.color.withValues(alpha: 0.2),
            child: Icon(mode.icon, size: 18, color: mode.onColor),
          ),
          const SizedBox(width: 12),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  '$from → $to',
                  maxLines: 1,
                  overflow: TextOverflow.ellipsis,
                  style: theme.textTheme.titleSmall,
                ),
                const SizedBox(height: 2),
                Text(
                  mode.label,
                  style: theme.textTheme.labelSmall?.copyWith(
                    color: colors.onSurfaceVariant,
                  ),
                ),
              ],
            ),
          ),
          const SizedBox(width: 8),
          Text(formatDistanceKm(distanceKm), style: theme.textTheme.titleSmall),
        ],
      ),
    );
  }
}

class _DeleteTripDialog extends StatelessWidget {
  const _DeleteTripDialog({required this.title});

  final String title;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final colors = theme.colorScheme;

    return Dialog(
      backgroundColor: colors.surface,
      insetPadding: const EdgeInsets.symmetric(horizontal: 32),
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(24)),
      child: Padding(
        padding: const EdgeInsets.fromLTRB(16, 24, 16, 16),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            CircleAvatar(
              radius: 28,
              backgroundColor: const Color(0xFFF8E0D4),
              child: Icon(
                Icons.delete_outline,
                color: context.palette.danger,
                size: 28,
              ),
            ),
            const SizedBox(height: 16),
            Text(
              'Usunąć podróż?',
              textAlign: TextAlign.center,
              style: theme.textTheme.titleMedium,
            ),
            const SizedBox(height: 8),
            Text(
              'Podróż „$title” wraz z przystankami i odcinkami zostanie '
              'trwale usunięta.',
              textAlign: TextAlign.center,
              style: theme.textTheme.bodySmall?.copyWith(
                color: colors.onSurfaceVariant,
              ),
            ),
            const SizedBox(height: 20),
            Row(
              children: [
                Expanded(
                  child: OutlinedButton(
                    onPressed: () => Navigator.pop(context, false),
                    style: OutlinedButton.styleFrom(
                      foregroundColor: AppColors.text,
                      side: const BorderSide(
                        color: AppColors.border,
                        width: 1.8,
                      ),
                      minimumSize: const Size.fromHeight(48),
                      shape: const RoundedRectangleBorder(
                        borderRadius: BorderRadius.all(Radius.circular(14)),
                      ),
                      textStyle: context.textRoles.button,
                    ),
                    child: const Text('Anuluj'),
                  ),
                ),
                const SizedBox(width: 8),
                Expanded(
                  child: OutlinedButton(
                    onPressed: () => Navigator.pop(context, true),
                    style: OutlinedButton.styleFrom(
                      foregroundColor: AppColors.onPrimary,
                      backgroundColor: AppColors.danger,
                      side: const BorderSide(
                        color: AppColors.danger,
                        width: 1.8,
                      ),
                      minimumSize: const Size.fromHeight(48),
                      shape: const RoundedRectangleBorder(
                        borderRadius: BorderRadius.all(Radius.circular(14)),
                      ),
                      textStyle: context.textRoles.button,
                    ),
                    child: const Text('Usuń'),
                  ),
                ),
              ],
            ),
          ],
        ),
      ),
    );
  }
}
