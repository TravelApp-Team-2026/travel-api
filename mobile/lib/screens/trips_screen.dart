import 'dart:async';

import 'package:flutter/gestures.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:travel_app/services/api_exception.dart';
import 'package:travel_app/services/trips_api.dart';
import 'package:travel_app/theme/app_palette.dart';
import 'package:travel_app/widgets/empty_state_view.dart';
import 'package:travel_app/widgets/error_view.dart';
import 'package:travel_app/widgets/loading_view.dart';

/// Trips list from the mockup: cards, pull-to-refresh and empty state.
class TripsScreen extends ConsumerStatefulWidget {
  const TripsScreen({super.key});

  @override
  ConsumerState<TripsScreen> createState() => _TripsScreenState();
}

enum _Status { loading, ready, error }

class _TripsScreenState extends ConsumerState<TripsScreen> {
  var _status = _Status.loading;
  var _request = 0;
  var _trips = const <TripSummary>[];
  String? _loadError;

  @override
  void initState() {
    super.initState();
    unawaited(_load());
  }

  Future<void> _load({bool fromRefresh = false}) async {
    final request = ++_request;
    if (!fromRefresh) {
      setState(() {
        _status = _Status.loading;
        _loadError = null;
      });
    }
    try {
      final trips = await ref.read(tripsApiProvider).listTrips();
      if (!mounted || request != _request) {
        return;
      }
      setState(() {
        _trips = trips;
        _status = _Status.ready;
        _loadError = null;
      });
    } on ApiException catch (error) {
      if (!mounted || request != _request) {
        return;
      }
      if (fromRefresh && _status == _Status.ready) {
        return;
      }
      setState(() {
        _status = _Status.error;
        _loadError = error.message;
      });
    }
  }

  Future<void> _openEditor() async {
    await context.push('/trips/new');
    if (mounted) {
      await _load(fromRefresh: true);
    }
  }

  void _openDetails(TripSummary trip) {
    context.push('/trips/${trip.id}', extra: trip);
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final colors = theme.colorScheme;
    final showFab = _status == _Status.ready && _trips.isNotEmpty;

    return Scaffold(
      floatingActionButton: showFab
          ? FloatingActionButton(
              key: const Key('add-trip'),
              onPressed: _openEditor,
              backgroundColor: colors.primary,
              foregroundColor: colors.onPrimary,
              elevation: 0,
              shape: const CircleBorder(),
              child: const Icon(Icons.add),
            )
          : null,
      body: SafeArea(
        bottom: false,
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            Padding(
              padding: const EdgeInsets.fromLTRB(24, 16, 24, 0),
              child: _Header(status: _status, trips: _trips),
            ),
            const SizedBox(height: 12),
            Expanded(child: _body()),
          ],
        ),
      ),
    );
  }

  Widget _body() {
    switch (_status) {
      case _Status.loading:
        return const LoadingView();
      case _Status.error:
        return _Refreshable(
          onRefresh: () => _load(fromRefresh: true),
          child: ErrorView(
            title: 'Brak połączenia z serwerem',
            message:
                _loadError ??
                'Sprawdź połączenie z internetem i spróbuj ponownie za chwilę.',
            onRetry: () => unawaited(_load()),
          ),
        );
      case _Status.ready:
        if (_trips.isEmpty) {
          return _Refreshable(
            onRefresh: () => _load(fromRefresh: true),
            child: EmptyStateView(
              icon: Icons.luggage_outlined,
              title: 'Nie masz jeszcze podróży',
              message:
                  'Dodaj pierwszą podróż, aby zobaczyć ją na mapie '
                  'i zacząć budować swoje statystyki.',
              actionLabel: 'Dodaj pierwszą podróż',
              onAction: _openEditor,
            ),
          );
        }
        return _PullToRefresh(
          onRefresh: () => _load(fromRefresh: true),
          child: ListView.separated(
            physics: _refreshPhysics,
            padding: const EdgeInsets.fromLTRB(16, 12, 16, 88),
            itemCount: _trips.length,
            separatorBuilder: (context, index) => const SizedBox(height: 12),
            itemBuilder: (context, index) {
              final trip = _trips[index];
              return _TripCard(trip: trip, onTap: () => _openDetails(trip));
            },
          ),
        );
    }
  }
}

const _refreshPhysics = AlwaysScrollableScrollPhysics(
  parent: BouncingScrollPhysics(),
);

class _PullToRefresh extends StatelessWidget {
  const _PullToRefresh({required this.onRefresh, required this.child});

  final Future<void> Function() onRefresh;
  final Widget child;

  @override
  Widget build(BuildContext context) {
    final colors = Theme.of(context).colorScheme;
    return ScrollConfiguration(
      behavior: ScrollConfiguration.of(context).copyWith(
        dragDevices: {
          PointerDeviceKind.touch,
          PointerDeviceKind.mouse,
          PointerDeviceKind.trackpad,
          PointerDeviceKind.stylus,
        },
      ),
      child: RefreshIndicator(
        color: colors.primary,
        backgroundColor: colors.surface,
        onRefresh: onRefresh,
        child: child,
      ),
    );
  }
}

class _Refreshable extends StatelessWidget {
  const _Refreshable({required this.onRefresh, required this.child});

  final Future<void> Function() onRefresh;
  final Widget child;

  @override
  Widget build(BuildContext context) {
    return LayoutBuilder(
      builder: (context, constraints) {
        return _PullToRefresh(
          onRefresh: onRefresh,
          child: SingleChildScrollView(
            physics: _refreshPhysics,
            child: ConstrainedBox(
              constraints: BoxConstraints(minHeight: constraints.maxHeight),
              child: child,
            ),
          ),
        );
      },
    );
  }
}

class _Header extends StatelessWidget {
  const _Header({required this.status, required this.trips});

  final _Status status;
  final List<TripSummary> trips;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final colors = theme.colorScheme;
    final subtitle = switch (status) {
      _Status.loading => '',
      _Status.error => 'Nie udało się pobrać danych',
      _Status.ready => formatTripsSubtitle(
        trips.length,
        trips.fold<double>(0, (sum, trip) => sum + trip.totalDistanceKm),
      ),
    };

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text('Moje podróże', style: theme.textTheme.headlineMedium),
        if (subtitle.isNotEmpty) ...[
          const SizedBox(height: 4),
          Text(
            subtitle,
            style: theme.textTheme.bodySmall?.copyWith(
              color: colors.onSurfaceVariant,
            ),
          ),
        ],
      ],
    );
  }
}

class _Meta extends StatelessWidget {
  const _Meta({
    required this.icon,
    required this.label,
    required this.color,
  });

  final IconData icon;
  final String label;
  final Color color;

  @override
  Widget build(BuildContext context) {
    return Row(
      children: [
        Icon(icon, size: 16, color: color),
        const SizedBox(width: 4),
        Expanded(
          child: Text(
            label,
            maxLines: 1,
            overflow: TextOverflow.ellipsis,
            style: Theme.of(context).textTheme.labelLarge?.copyWith(
              color: Theme.of(context).colorScheme.onSurface,
            ),
          ),
        ),
      ],
    );
  }
}

class _TripCard extends StatelessWidget {
  const _TripCard({required this.trip, required this.onTap});

  final TripSummary trip;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final colors = theme.colorScheme;
    final metaColor = context.palette.primaryPressed;

    return Material(
      color: colors.surface,
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(18),
        side: BorderSide(color: colors.outline, width: 1.5),
      ),
      child: InkWell(
        key: Key('trip-card-${trip.id}'),
        onTap: onTap,
        borderRadius: BorderRadius.circular(18),
        child: Padding(
          padding: const EdgeInsets.fromLTRB(16, 16, 16, 18),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(
                trip.title,
                maxLines: 1,
                overflow: TextOverflow.ellipsis,
                style: theme.textTheme.titleSmall,
              ),
              const SizedBox(height: 8),
              Row(
                children: [
                  Icon(
                    Icons.calendar_today_outlined,
                    size: 16,
                    color: colors.onSurfaceVariant,
                  ),
                  const SizedBox(width: 6),
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
              const SizedBox(height: 10),
              Row(
                children: [
                  Expanded(
                    child: _Meta(
                      icon: Icons.place_outlined,
                      label: formatStopCount(trip.stopCount),
                      color: metaColor,
                    ),
                  ),
                  const SizedBox(width: 12),
                  Expanded(
                    child: _Meta(
                      icon: Icons.route_outlined,
                      label: formatDistanceKm(trip.totalDistanceKm),
                      color: metaColor,
                    ),
                  ),
                ],
              ),
            ],
          ),
        ),
      ),
    );
  }
}
