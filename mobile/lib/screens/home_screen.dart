import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:travel_app/services/api_config.dart';

class HomeScreen extends ConsumerWidget {
  const HomeScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final baseUrl = ref.watch(apiBaseUrlProvider);
    return Scaffold(
      appBar: AppBar(
        title: const Text('Travel'),
      ),
      body: Center(
        child: Text(baseUrl),
      ),
    );
  }
}
