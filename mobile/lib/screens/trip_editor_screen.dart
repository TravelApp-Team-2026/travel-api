import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';

/// Opened from the trips list +. The editor form is a later issue.
class TripEditorScreen extends StatelessWidget {
  const TripEditorScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        leading: IconButton(
          icon: const Icon(Icons.close),
          onPressed: () => context.pop(),
        ),
        title: const Text('Nowa podróż'),
      ),
    );
  }
}
