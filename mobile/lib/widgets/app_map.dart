import 'package:flutter/material.dart';
import 'package:flutter_map/flutter_map.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:latlong2/latlong.dart';
import 'package:travel_app/theme/app_colors.dart';
import 'package:url_launcher/link.dart';

/// Android `applicationId`. OSM blocks generic agents such as `com.example.app`.
const osmUserAgentPackageName = 'com.example.travel_app';

const osmTileUrlTemplate = 'https://tile.openstreetmap.org/{z}/{x}/{y}.png';

const osmAttribution = '© OpenStreetMap contributors';

final osmAttributionUri = Uri.https('www.openstreetmap.org', '/copyright');

/// Geometric centre of Poland — a sensible default before a trip is fitted.
const appMapDefaultCenter = LatLng(52.069167, 19.480556);

const appMapDefaultZoom = 6.0;

/// Hard zoom cap. Street-level 19 floods OSM during a fast pinch.
const appMapMaxZoom = 17.0;

const appMapTileThrottle = Duration(milliseconds: 150);

/// One HTTP client per map. [TileLayer] disposes it; do not share across maps.
NetworkTileProvider createOsmTileProvider() {
  return NetworkTileProvider(
    cachingProvider: BuiltInMapCachingProvider.getOrCreateInstance(),
  );
}

typedef MapTileProviderFactory = TileProvider Function();

/// Builds the tile provider for each [AppMap]. Tests override this so widget
/// tests never hit the network.
final mapTileProviderFactoryProvider = Provider<MapTileProviderFactory>(
  (ref) => createOsmTileProvider,
);

/// Shared OSM map: pan, zoom, cached tiles, visible attribution.
///
/// Lives under a [ProviderScope]. Pass [tileProvider] only when the caller
/// already owns a provider; otherwise the map reads
/// [mapTileProviderFactoryProvider].
class AppMap extends ConsumerStatefulWidget {
  const AppMap({
    super.key,
    this.mapController,
    this.children = const [],
    this.initialCenter = appMapDefaultCenter,
    this.initialZoom = appMapDefaultZoom,
    this.initialCameraFit,
    this.tileProvider,
    this.onMapReady,
  });

  final MapController? mapController;
  final List<Widget> children;
  final LatLng initialCenter;
  final double initialZoom;
  final CameraFit? initialCameraFit;
  final TileProvider? tileProvider;
  final VoidCallback? onMapReady;

  @override
  ConsumerState<AppMap> createState() => _AppMapState();
}

class _AppMapState extends ConsumerState<AppMap> {
  late final TileProvider _tileProvider;
  late final TileUpdateTransformer _tileUpdates;

  @override
  void initState() {
    super.initState();
    _tileProvider =
        widget.tileProvider ?? ref.read(mapTileProviderFactoryProvider)();
    _tileUpdates = TileUpdateTransformers.throttle(appMapTileThrottle);
  }

  @override
  Widget build(BuildContext context) {
    return FlutterMap(
      mapController: widget.mapController,
      options: MapOptions(
        initialCenter: widget.initialCenter,
        initialZoom: widget.initialZoom,
        initialCameraFit: widget.initialCameraFit,
        minZoom: 3,
        maxZoom: appMapMaxZoom,
        backgroundColor: AppColors.surfaceVariant,
        interactionOptions: const InteractionOptions(
          flags:
              InteractiveFlag.drag |
              InteractiveFlag.flingAnimation |
              InteractiveFlag.pinchMove |
              InteractiveFlag.pinchZoom |
              InteractiveFlag.doubleTapZoom |
              InteractiveFlag.scrollWheelZoom,
        ),
        onMapReady: widget.onMapReady,
      ),
      children: [
        TileLayer(
          urlTemplate: osmTileUrlTemplate,
          userAgentPackageName: osmUserAgentPackageName,
          maxNativeZoom: appMapMaxZoom.toInt(),
          panBuffer: 0,
          tileUpdateTransformer: _tileUpdates,
          tileDisplay: const TileDisplay.instantaneous(),
          tileProvider: _tileProvider,
        ),
        ...widget.children,
        const _OsmAttribution(),
      ],
    );
  }
}

class _OsmAttribution extends StatelessWidget {
  const _OsmAttribution();

  @override
  Widget build(BuildContext context) {
    return Align(
      alignment: Alignment.bottomRight,
      child: SafeArea(
        child: Padding(
          padding: const EdgeInsets.all(8),
          child: Link(
            uri: osmAttributionUri,
            target: LinkTarget.blank,
            builder: (context, followLink) {
              return MouseRegion(
                cursor: SystemMouseCursors.click,
                child: GestureDetector(
                  key: const Key('osm-attribution'),
                  onTap: followLink,
                  child: Semantics(
                    link: true,
                    button: true,
                    label: osmAttribution,
                    child: DecoratedBox(
                      decoration: BoxDecoration(
                        color: AppColors.surface.withValues(alpha: 0.96),
                        borderRadius: const BorderRadius.all(
                          Radius.circular(4),
                        ),
                      ),
                      child: const Padding(
                        padding: EdgeInsets.symmetric(
                          horizontal: 8,
                          vertical: 4,
                        ),
                        child: Text(
                          osmAttribution,
                          style: TextStyle(
                            fontSize: 11,
                            fontWeight: FontWeight.w400,
                            color: AppColors.text,
                            decoration: TextDecoration.underline,
                            decorationColor: AppColors.text,
                          ),
                        ),
                      ),
                    ),
                  ),
                ),
              );
            },
          ),
        ),
      ),
    );
  }
}

/// Fits [points] in the viewport. One point zooms in; none keeps the default
/// Poland view.
CameraFit fitMapToBounds(
  Iterable<LatLng> points, {
  EdgeInsets padding = const EdgeInsets.all(48),
  double maxZoom = 16,
}) {
  final list = points.toList();
  if (list.isEmpty) {
    return const CameraFit.coordinates(
      coordinates: [appMapDefaultCenter],
      minZoom: appMapDefaultZoom,
      maxZoom: appMapDefaultZoom,
    );
  }
  if (list.length == 1) {
    return CameraFit.coordinates(coordinates: list, minZoom: 12, maxZoom: 12);
  }
  return CameraFit.bounds(
    bounds: LatLngBounds.fromPoints(list),
    padding: padding,
    maxZoom: maxZoom,
  );
}
