import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:travel_app/router.dart';
import 'package:travel_app/services/auth_session.dart';
import 'package:travel_app/theme/app_theme.dart';

void main() {
  runApp(const ProviderScope(child: TravelApp()));
}

class TravelApp extends ConsumerWidget {
  const TravelApp({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final status = ref.watch(authSessionProvider);
    if (status == AuthStatus.unknown) {
      return MaterialApp(
        title: 'Travel',
        theme: AppTheme.light,
        home: const Scaffold(body: SizedBox.expand()),
      );
    }

    return MaterialApp.router(
      title: 'Travel',
      theme: AppTheme.light,
      routerConfig: ref.watch(goRouterProvider),
    );
  }
}
