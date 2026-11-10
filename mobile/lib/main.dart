import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:travel_app/screens/home_screen.dart';
import 'package:travel_app/theme/app_theme.dart';

void main() {
  runApp(const ProviderScope(child: TravelApp()));
}

final GoRouter appRouter = GoRouter(
  routes: [
    GoRoute(
      path: '/',
      builder: (context, state) => const HomeScreen(),
    ),
  ],
);

class TravelApp extends StatelessWidget {
  const TravelApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp.router(
      title: 'Travel',
      theme: AppTheme.light,
      routerConfig: appRouter,
    );
  }
}
