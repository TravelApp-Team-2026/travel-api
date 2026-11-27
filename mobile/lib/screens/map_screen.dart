import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter_map/flutter_map.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:latlong2/latlong.dart';
import 'package:travel_app/services/api_exception.dart';
import 'package:travel_app/services/trips_api.dart';
import 'package:travel_app/theme/app_colors.dart';
import 'package:travel_app/widgets/app_map.dart';
import 'package:travel_app/widgets/error_view.dart';
import 'package:travel_app/widgets/map_geometry.dart';
import 'package:travel_app/widgets/map_legend.dart';
import 'package:travel_app/widgets/map_route_layer.dart';

/// Map tab: OSM, trip legs in T-014 styles, legend from the mockup.
class MapScreen extends ConsumerStatefulWidget {
  const MapScreen({super.key});

  @override
  ConsumerState<MapScreen> createState() => _MapScreenState();
}

class _MapScreenState extends ConsumerState<MapScreen> {
  final _controller = MapController();
  var _legs = const <MapLeg>[];
  var _stops = const <LatLng>[];
  var _mapReady = false;
  var _visible = false;
  var _didFit = false;
  var _request = 0;
  String? _error;

  @override
  void didChangeDependencies() {
    super.didChangeDependencies();
    final visible = TickerMode.valuesOf(context).enabled;
    if (visible == _visible) {
      return;
    }
    _visible = visible;
    if (visible) {
      unawaited(_load());
    }
  }

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  Future<void> _load() async {
    final request = ++_request;
    if (_error != null) {
      setState(() => _error = null);
    }
    try {
      final api = ref.read(tripsApiProvider);
      final summaries = await api.listTrips();
      final trips = await _tripsFor(api, summaries);
      if (!mounted || request != _request) {
        return;
      }
      if (summaries.isNotEmpty && trips.isEmpty) {
        setState(() => _error = 'Nie udało się wczytać tras.');
        return;
      }
      final legs = [for (final trip in trips) ...mapLegsFromTrip(trip)];
      final stops = [
        for (final trip in trips)
          for (final place in trip.waypoints)
            LatLng(place.latitude, place.longitude),
      ];
      setState(() {
        _legs = legs;
        _stops = stops;
      });
      if (!_didFit) {
        _fitIfReady();
      }
    } on ApiException catch (error) {
      if (!mounted || request != _request) {
        return;
      }
      if (_legs.isNotEmpty) {
        return;
      }
      setState(() => _error = error.message);
    }
  }

  Future<List<TripDetails>> _tripsFor(
    TripsApi api,
    List<TripSummary> summaries,
  ) async {
    if (summaries.isEmpty) {
      return const [];
    }
    final results = await Future.wait([
      for (final trip in summaries) _tryGetTrip(api, trip.id),
    ]);
    return [for (final trip in results) ?trip];
  }

  Future<TripDetails?> _tryGetTrip(TripsApi api, int id) async {
    try {
      return await api.getTrip(id);
    } on ApiException {
      return null;
    }
  }

  void _fitIfReady() {
    if (_didFit || !_mapReady || _legs.isEmpty) {
      return;
    }
    final points = [for (final leg in _legs) ...leg.points];
    if (points.isEmpty) {
      return;
    }
    _controller.fitCamera(fitMapToBounds(points));
    _didFit = true;
  }

  @override
  Widget build(BuildContext context) {
    return Stack(
      children: [
        AppMap(
          mapController: _controller,
          onMapReady: () {
            _mapReady = true;
            _fitIfReady();
          },
          children: [
            ...mapRouteChildren(_legs),
            IgnorePointer(
              child: CircleLayer(
                circles: [
                  for (final stop in _stops)
                    CircleMarker(
                      point: stop,
                      radius: 4,
                      color: AppColors.marker,
                      borderStrokeWidth: 1.5,
                      borderColor: AppColors.background,
                    ),
                ],
              ),
            ),
          ],
        ),
        const Positioned(left: 16, bottom: 16, child: MapLegend()),
        if (_error != null)
          ColoredBox(
            color: AppColors.background.withValues(alpha: 0.94),
            child: ErrorView(
              title: 'Nie udało się wczytać tras',
              message: _error!,
              onRetry: () => unawaited(_load()),
            ),
          ),
      ],
    );
  }
}
