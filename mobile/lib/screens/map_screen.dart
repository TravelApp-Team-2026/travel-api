import 'package:flutter/material.dart';
import 'package:travel_app/widgets/app_map.dart';

/// Map tab: the shared OSM widget. Routes and countries are later issues.
class MapScreen extends StatelessWidget {
  const MapScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return const AppMap();
  }
}
