import 'package:flutter/material.dart';
import 'package:flutter_map/flutter_map.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:latlong2/latlong.dart';
import 'package:travel_app/theme/app_theme.dart';
import 'package:travel_app/widgets/app_map.dart';
import 'package:url_launcher/link.dart';
import 'package:url_launcher_platform_interface/url_launcher_platform_interface.dart';

import 'recording_url_launcher.dart';
import 'silent_map_tile_provider.dart';

void main() {
  testWidgets('OSM tiles, pan/zoom, app id, cache and attribution', (
    tester,
  ) async {
    tester.view.physicalSize = const Size(360, 800);
    tester.view.devicePixelRatio = 1;
    addTearDown(tester.view.resetPhysicalSize);
    addTearDown(tester.view.resetDevicePixelRatio);

    await tester.pumpWidget(
      ProviderScope(
        overrides: [silentMapTilesOverride],
        child: MaterialApp(
          theme: AppTheme.light,
          home: const Scaffold(body: AppMap()),
        ),
      ),
    );
    await tester.pump(appMapTileThrottle);

    expect(find.byType(FlutterMap), findsOneWidget);
    expect(find.text(osmAttribution), findsOneWidget);
    expect(tester.takeException(), isNull);

    final map = tester.widget<FlutterMap>(find.byType(FlutterMap));
    expect(
      InteractiveFlag.hasDrag(map.options.interactionOptions.flags),
      isTrue,
    );
    expect(
      InteractiveFlag.hasPinchZoom(map.options.interactionOptions.flags),
      isTrue,
    );
    expect(
      InteractiveFlag.hasScrollWheelZoom(map.options.interactionOptions.flags),
      isTrue,
    );
    expect(map.options.maxZoom, appMapMaxZoom);
    expect(
      InteractiveFlag.hasFlingAnimation(map.options.interactionOptions.flags),
      isFalse,
    );

    final layer = tester.widget<TileLayer>(find.byType(TileLayer));
    expect(layer.maxNativeZoom, appMapMaxNativeZoom);
    expect(layer.panBuffer, 1);
    expect(layer.keepBuffer, 2);
    expect(layer.urlTemplate, osmTileUrlTemplate);
    expect(layer.tileProvider, isA<SilentMapTileProvider>());
    expect(
      layer.tileProvider.headers['User-Agent'],
      'flutter_map ($osmUserAgentPackageName)',
    );
    expect(osmUserAgentPackageName, 'com.example.travel_app');
    expect(
      createOsmTileProvider().cachingProvider,
      isA<BuiltInMapCachingProvider>(),
    );

    final link = tester.widget<Link>(find.byType(Link));
    expect(link.uri, osmAttributionUri);
    expect(link.target, LinkTarget.blank);
  });

  testWidgets('attribution opens the OSM copyright page', (tester) async {
    final launcher = RecordingUrlLauncher();
    final previous = UrlLauncherPlatform.instance;
    UrlLauncherPlatform.instance = launcher;
    addTearDown(() => UrlLauncherPlatform.instance = previous);

    await tester.pumpWidget(
      ProviderScope(
        overrides: [silentMapTilesOverride],
        child: MaterialApp(
          theme: AppTheme.light,
          home: const Scaffold(body: AppMap()),
        ),
      ),
    );
    await tester.pump(appMapTileThrottle);

    await tester.tap(find.byKey(const Key('osm-attribution')));
    await tester.pump();

    expect(launcher.launched, [osmAttributionUri.toString()]);
  });

  testWidgets('each AppMap owns its own tile provider', (tester) async {
    await tester.pumpWidget(
      ProviderScope(
        overrides: [silentMapTilesOverride],
        child: const MaterialApp(
          home: Scaffold(
            body: Column(
              children: [
                Expanded(child: AppMap()),
                Expanded(child: AppMap()),
              ],
            ),
          ),
        ),
      ),
    );
    await tester.pump(appMapTileThrottle);

    final layers = tester
        .widgetList<TileLayer>(find.byType(TileLayer))
        .toList();
    expect(layers, hasLength(2));
    expect(identical(layers[0].tileProvider, layers[1].tileProvider), isFalse);
  });

  test('fitMapToBounds covers every point', () {
    const legnica = LatLng(51.21, 16.16);
    const berlin = LatLng(52.52, 13.40);

    final fit = fitMapToBounds([legnica, berlin]);
    expect(fit, isA<FitCoordinates>());
    final coordinates = (fit as FitCoordinates).coordinates;
    expect(coordinates, containsAll([legnica, berlin]));

    final single = fitMapToBounds([legnica]);
    expect(single, isA<FitCoordinates>());
    expect((single as FitCoordinates).coordinates, [legnica]);

    final empty = fitMapToBounds(const []);
    expect(empty, isA<FitCoordinates>());
    expect((empty as FitCoordinates).coordinates, [appMapDefaultCenter]);
  });
}
