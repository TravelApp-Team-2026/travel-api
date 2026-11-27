import 'dart:async';
import 'dart:math' as math;

import 'package:flutter/material.dart';
import 'package:flutter_map/flutter_map.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:latlong2/latlong.dart';
import 'package:travel_app/services/api_exception.dart';
import 'package:travel_app/services/trips_api.dart';
import 'package:travel_app/theme/app_colors.dart';
import 'package:travel_app/theme/app_palette.dart';
import 'package:travel_app/theme/transport_mode.dart';
import 'package:travel_app/widgets/app_map.dart';
import 'package:travel_app/widgets/error_view.dart';
import 'package:travel_app/widgets/loading_view.dart';
import 'package:travel_app/widgets/map_geometry.dart';
import 'package:travel_app/widgets/map_legend.dart';
import 'package:travel_app/widgets/map_route_layer.dart';
import 'package:travel_app/widgets/trip_stop_marker.dart';

const _tripSheetExpanded = 0.55;
const _tripSheetHandleBar = 40.0;

class TripDetailsScreen extends ConsumerStatefulWidget {
  const TripDetailsScreen({super.key, required this.tripId});

  final int? tripId;

  @override
  ConsumerState<TripDetailsScreen> createState() => _TripDetailsScreenState();
}

enum _Status { loading, ready, error }

class _TripDetailsScreenState extends ConsumerState<TripDetailsScreen> {
  final _mapController = MapController();
  final _sheetController = DraggableScrollableController();
  final _sheetExtent = ValueNotifier(_tripSheetExpanded);
  final _hitNotifier = LayerHitNotifier<int>(null);
  var _status = _Status.loading;
  var _request = 0;
  var _deleting = false;
  var _mapReady = false;
  var _didFit = false;
  TripDetails? _trip;
  var _legs = const <MapLeg>[];
  var _stops = const <NumberedStop>[];
  String? _loadError;
  String? _deleteError;
  int? _selectedLeg;
  NumberedStop? _selectedStop;
  LatLng? _tooltipAt;
  Offset? _pointerDown;

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
        _mapReady = false;
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
        _legs = mapLegsFromTrip(trip);
        _stops = numberedStopsFromTrip(trip);
        _status = _Status.ready;
        _selectedLeg = null;
        _selectedStop = null;
        _tooltipAt = null;
        _didFit = false;
      });
      _fitIfReady();
    } on ApiException catch (error) {
      if (!mounted || request != _request) {
        return;
      }
      setState(() {
        _loadError = error.message;
        if (showSpinner) {
          _status = _Status.error;
          _mapReady = false;
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
  void dispose() {
    _mapController.dispose();
    _sheetController.dispose();
    _sheetExtent.dispose();
    _hitNotifier.dispose();
    super.dispose();
  }

  void _fitIfReady() {
    if (_didFit || !_mapReady) {
      return;
    }
    final points = [
      for (final stop in _stops) stop.point,
      for (final leg in _legs) ...leg.points,
    ];
    if (points.isEmpty) {
      return;
    }
    final sheet = MediaQuery.sizeOf(context).height * _tripSheetExpanded;
    _mapController.fitCamera(
      fitMapToBounds(
        points,
        padding: EdgeInsets.fromLTRB(48, 80, 48, sheet - 24),
      ),
    );
    _didFit = true;
  }

  void _selectStop(NumberedStop stop) {
    setState(() {
      if (_selectedStop?.label == stop.label && _selectedLeg == null) {
        _selectedStop = null;
        _tooltipAt = null;
        return;
      }
      _selectedStop = stop;
      _selectedLeg = null;
      _tooltipAt = stop.point;
    });
  }

  void _selectLeg(int index, {LatLng? at}) {
    if (index < 0 || index >= _legs.length) {
      return;
    }
    final points = _legs[index].points;
    setState(() {
      _selectedLeg = index;
      _selectedStop = null;
      _tooltipAt = at ?? points[points.length ~/ 2];
    });
  }

  void _onPointerDown(PointerDownEvent event, LatLng _) {
    _pointerDown = event.position;
  }

  void _onPointerUp(PointerUpEvent event, LatLng point) {
    final start = _pointerDown;
    _pointerDown = null;
    if (_deleting || start == null) {
      return;
    }
    if ((event.position - start).distance > 8) {
      return;
    }
    _onMapTap(point);
  }

  void _onMapTap(LatLng point) {
    if (_deleting) {
      return;
    }
    final camera = _mapController.camera;
    final tap = camera.latLngToScreenOffset(point);
    NumberedStop? stopHit;
    var stopDist = double.infinity;
    for (final stop in _stops) {
      final radius = stop.label.contains('·')
          ? tripStopPillWidth / 2
          : tripStopCircleSize / 2;
      final distance = (camera.latLngToScreenOffset(stop.point) - tap).distance;
      if (distance <= radius && distance < stopDist) {
        stopDist = distance;
        stopHit = stop;
      }
    }
    final hit = _hitNotifier.value;
    final legs = _uniqueLegHits(hit?.hitValues);
    if (stopHit != null &&
        (legs.isEmpty || stopDist <= tripStopCircleSize / 2)) {
      _selectStop(stopHit);
      return;
    }
    if (legs.isNotEmpty) {
      _selectLeg(legs.first, at: hit?.coordinate ?? point);
      return;
    }
    if (stopHit != null) {
      _selectStop(stopHit);
      return;
    }
    _clearMapSelection();
  }

  List<int> _uniqueLegHits(List<int>? hits) {
    if (hits == null || hits.isEmpty) {
      return const [];
    }
    final seen = <int>{};
    return [
      for (final index in hits)
        if (index >= 0 && index < _legs.length && seen.add(index)) index,
    ];
  }

  void _clearMapSelection() {
    if (_selectedLeg == null && _selectedStop == null) {
      return;
    }
    setState(() {
      _selectedLeg = null;
      _selectedStop = null;
      _tooltipAt = null;
    });
  }

  @override
  Widget build(BuildContext context) {
    return PopScope(
      canPop: !_deleting,
      child: Scaffold(
        appBar: _status == _Status.ready
            ? null
            : AppBar(
                leading: IconButton(
                  icon: const Icon(Icons.arrow_back),
                  onPressed: _deleting ? null : () => context.pop(),
                ),
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

  double _sheetMinSize(BuildContext context) {
    final height = MediaQuery.sizeOf(context).height;
    final collapsed =
        _tripSheetHandleBar + MediaQuery.paddingOf(context).bottom;
    return (collapsed / height).clamp(0.05, 0.2);
  }

  Future<void> _toggleSheet() async {
    if (!_sheetController.isAttached) {
      return;
    }
    final minSize = _sheetMinSize(context);
    final target = _sheetController.size <= (minSize + _tripSheetExpanded) / 2
        ? _tripSheetExpanded
        : minSize;
    await _sheetController.animateTo(
      target,
      duration: const Duration(milliseconds: 250),
      curve: Curves.easeOutCubic,
    );
  }

  Widget _body() {
    final trip = _trip!;
    final minSize = _sheetMinSize(context);
    return Stack(
      children: [
        Positioned.fill(child: _map(trip)),
        ValueListenableBuilder<double>(
          valueListenable: _sheetExtent,
          builder: (context, extent, child) {
            return Positioned(
              right: 16,
              bottom: MediaQuery.sizeOf(context).height * extent + 16,
              child: child!,
            );
          },
          child: const MapLegend(),
        ),
        NotificationListener<DraggableScrollableNotification>(
          onNotification: (notification) {
            _sheetExtent.value = notification.extent;
            return false;
          },
          child: DraggableScrollableSheet(
            controller: _sheetController,
            initialChildSize: _tripSheetExpanded,
            minChildSize: minSize,
            maxChildSize: _tripSheetExpanded,
            snap: true,
            snapSizes: [minSize, _tripSheetExpanded],
            builder: (context, scrollController) {
              return _sheet(trip, scrollController);
            },
          ),
        ),
      ],
    );
  }

  Widget _map(TripDetails trip) {
    final top = MediaQuery.paddingOf(context).top;
    return Stack(
      children: [
        AppMap(
          key: const Key('trip-details-map'),
          mapController: _mapController,
          onMapReady: () {
            _mapReady = true;
            _fitIfReady();
          },
          onPointerDown: _onPointerDown,
          onPointerUp: _onPointerUp,
          children: [
            ...mapRouteChildren(
              _legs,
              selectedIndex: _selectedLeg,
              hitNotifier: _hitNotifier,
            ),
            IgnorePointer(
              child: MarkerLayer(
                markers: [
                  for (final stop in _stops)
                    Marker(
                      point: stop.point,
                      width: stop.label.contains('·')
                          ? tripStopPillWidth
                          : tripStopCircleSize,
                      height: tripStopCircleSize,
                      child: TripStopMarker(
                        key: Key('trip-stop-${stop.label}'),
                        label: stop.label,
                      ),
                    ),
                ],
              ),
            ),
            if (_tooltipAt != null)
              _MapTooltipLayer(
                point: _tooltipAt!,
                child: _selectedStop != null
                    ? _PlaceTooltip(name: _selectedStop!.placeName)
                    : _selectedLeg != null
                    ? _LegTooltip(
                        mode: trip.legs[_selectedLeg!].mode,
                        distanceKm: trip.legs[_selectedLeg!].distanceKm,
                        from: trip.waypoints[_selectedLeg!].placeName,
                        to: trip.waypoints[_selectedLeg! + 1].placeName,
                      )
                    : const SizedBox.shrink(),
              ),
          ],
        ),
        Positioned(
          top: top + 8,
          left: 16,
          child: _MapRoundButton(
            icon: Icons.arrow_back,
            onPressed: _deleting ? null : () => context.pop(),
          ),
        ),
        Positioned(
          top: top + 8,
          right: 68,
          child: _MapRoundButton(
            key: const Key('edit-trip'),
            icon: Icons.edit_outlined,
            onPressed: _deleting ? null : () => unawaited(_edit()),
          ),
        ),
        Positioned(
          top: top + 8,
          right: 16,
          child: _MapRoundButton(
            key: const Key('delete-trip'),
            icon: Icons.delete_outline,
            color: context.palette.danger,
            onPressed: _deleting ? null : () => unawaited(_delete()),
          ),
        ),
      ],
    );
  }

  Widget _sheet(TripDetails trip, ScrollController scrollController) {
    final theme = Theme.of(context);
    final colors = theme.colorScheme;
    final description = trip.description?.trim();

    return Material(
      key: const Key('trip-details-sheet'),
      color: AppColors.background,
      elevation: 0,
      shape: const RoundedRectangleBorder(
        borderRadius: BorderRadius.vertical(top: Radius.circular(24)),
      ),
      clipBehavior: Clip.antiAlias,
      child: ListView(
        controller: scrollController,
        padding: const EdgeInsets.fromLTRB(16, 0, 16, 32),
        children: [
          GestureDetector(
            key: const Key('trip-sheet-handle'),
            behavior: HitTestBehavior.opaque,
            onTap: () => unawaited(_toggleSheet()),
            child: const SizedBox(
              height: _tripSheetHandleBar,
              child: Center(
                child: DecoratedBox(
                  decoration: BoxDecoration(
                    color: AppColors.border,
                    borderRadius: BorderRadius.all(Radius.circular(2)),
                  ),
                  child: SizedBox(width: 36, height: 4),
                ),
              ),
            ),
          ),
          const SizedBox(height: 16),
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
            if (i > 0 && _selectedLeg != i && _selectedLeg != i - 1)
              const Divider(height: 1),
            _LegTile(
              from: trip.waypoints[i].placeName,
              to: trip.waypoints[i + 1].placeName,
              mode: trip.legs[i].mode,
              distanceKm: trip.legs[i].distanceKm,
              selected: _selectedLeg == i,
              onTap: _deleting ? null : () => _selectLeg(i),
            ),
          ],
        ],
      ),
    );
  }
}

class _MapRoundButton extends StatelessWidget {
  const _MapRoundButton({
    super.key,
    required this.icon,
    required this.onPressed,
    this.color,
  });

  final IconData icon;
  final VoidCallback? onPressed;
  final Color? color;

  @override
  Widget build(BuildContext context) {
    return Material(
      color: AppColors.surface,
      elevation: 2,
      shadowColor: AppColors.shadow,
      shape: const CircleBorder(),
      child: IconButton(
        icon: Icon(icon, color: color ?? AppColors.text),
        onPressed: onPressed,
      ),
    );
  }
}

class _MapTooltipLayer extends StatelessWidget {
  const _MapTooltipLayer({required this.point, required this.child});

  final LatLng point;
  final Widget child;

  @override
  Widget build(BuildContext context) {
    final camera = MapCamera.of(context);
    final offset = camera.latLngToScreenOffset(point);
    final size = camera.nonRotatedSize;
    const tooltipWidth = 196.0;
    const tooltipHeight = 56.0;
    const pad = 8.0;
    final showAbove = offset.dy > 80;
    final minX = tooltipWidth / 2 + pad;
    final dx = offset.dx
        .clamp(minX, math.max(minX, size.width - tooltipWidth / 2 - pad))
        .toDouble();
    final dy =
        (showAbove
                ? offset.dy.clamp(tooltipHeight + pad, size.height)
                : offset.dy.clamp(
                    0.0,
                    math.max(0.0, size.height - tooltipHeight - pad),
                  ))
            .toDouble();
    return Stack(
      children: [
        Positioned(
          left: dx,
          top: dy,
          child: IgnorePointer(
            child: FractionalTranslation(
              translation: Offset(-0.5, showAbove ? -1.15 : 0.15),
              child: child,
            ),
          ),
        ),
      ],
    );
  }
}

class _PlaceTooltip extends StatelessWidget {
  const _PlaceTooltip({required this.name});

  final String name;

  @override
  Widget build(BuildContext context) {
    return Material(
      key: const Key('trip-map-tooltip'),
      color: AppColors.surface,
      elevation: 0,
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(12),
        side: const BorderSide(color: AppColors.primary, width: 1.8),
      ),
      child: Padding(
        padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 10),
        child: Text(
          name,
          style: Theme.of(
            context,
          ).textTheme.titleSmall?.copyWith(fontWeight: FontWeight.w700),
        ),
      ),
    );
  }
}

class _LegTooltip extends StatelessWidget {
  const _LegTooltip({
    required this.mode,
    required this.distanceKm,
    required this.from,
    required this.to,
  });

  final TransportMode mode;
  final double distanceKm;
  final String from;
  final String to;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return Material(
      key: const Key('trip-map-tooltip'),
      color: AppColors.surface,
      elevation: 0,
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(12),
        side: const BorderSide(color: AppColors.primary, width: 1.8),
      ),
      child: Padding(
        padding: const EdgeInsets.fromLTRB(12, 8, 14, 8),
        child: Row(
          mainAxisSize: MainAxisSize.min,
          children: [
            Icon(mode.icon, size: 22, color: mode.onColor),
            const SizedBox(width: 8),
            Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              mainAxisSize: MainAxisSize.min,
              children: [
                Text(
                  '${mode.label} · ${formatDistanceKm(distanceKm)}',
                  style: theme.textTheme.titleSmall?.copyWith(
                    fontWeight: FontWeight.w700,
                  ),
                ),
                Text(
                  '$from → $to',
                  style: theme.textTheme.labelSmall?.copyWith(
                    color: theme.colorScheme.onSurfaceVariant,
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
    required this.selected,
    this.onTap,
  });

  final String from;
  final String to;
  final TransportMode mode;
  final double distanceKm;
  final bool selected;
  final VoidCallback? onTap;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final colors = theme.colorScheme;

    return Material(
      color: selected ? AppColors.primaryTint : Colors.transparent,
      borderRadius: BorderRadius.circular(12),
      child: InkWell(
        onTap: onTap,
        borderRadius: BorderRadius.circular(12),
        child: Padding(
          padding: const EdgeInsets.symmetric(vertical: 12, horizontal: 8),
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
              Text(
                formatDistanceKm(distanceKm),
                style: theme.textTheme.titleSmall,
              ),
            ],
          ),
        ),
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
