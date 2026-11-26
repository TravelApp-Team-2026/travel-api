import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';

/// Opened from a trip card. The details body is a later issue.
class TripDetailsScreen extends StatelessWidget {
  const TripDetailsScreen({super.key, required this.title, this.tripId});

  final String title;
  final int? tripId;

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        leading: IconButton(
          icon: const Icon(Icons.arrow_back),
          onPressed: () => context.pop(),
        ),
        title: Text(title),
        actions: [
          if (tripId != null)
            IconButton(
              key: const Key('edit-trip'),
              icon: const Icon(Icons.edit_outlined),
              onPressed: () => context.push('/trips/$tripId/edit'),
            ),
        ],
      ),
    );
  }
}
