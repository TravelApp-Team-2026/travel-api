import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:travel_app/screens/home_screen.dart';
import 'package:travel_app/screens/login_screen.dart';
import 'package:travel_app/screens/register_screen.dart';
import 'package:travel_app/screens/t014_test_screen.dart';
import 'package:travel_app/theme/app_theme.dart';

void main() {
  runApp(const ProviderScope(child: TravelApp()));
}

final GoRouter appRouter = GoRouter(
  initialLocation: '/login',
  routes: [
    GoRoute(
      path: '/login',
      builder: (context, state) => const LoginScreen(),
    ),
    GoRoute(
      path: '/register',
      builder: (context, state) => const RegisterScreen(),
    ),
    GoRoute(
      path: '/',
      builder: (context, state) => const HomeScreen(),
    ),
    GoRoute(
      path: '/t014-test',
      builder: (context, state) => const T014TestScreen(),
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
