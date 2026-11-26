import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:travel_app/screens/login_screen.dart';
import 'package:travel_app/screens/map_screen.dart';
import 'package:travel_app/screens/main_shell.dart';
import 'package:travel_app/screens/place_search_screen.dart';
import 'package:travel_app/screens/profile_screen.dart';
import 'package:travel_app/screens/register_screen.dart';
import 'package:travel_app/screens/t014_test_screen.dart';
import 'package:travel_app/screens/trip_details_screen.dart';
import 'package:travel_app/screens/trip_editor_screen.dart';
import 'package:travel_app/screens/trips_screen.dart';
import 'package:travel_app/services/auth_session.dart';

final goRouterProvider = Provider<GoRouter>((ref) {
  final refresh = ValueNotifier<int>(0);
  final rootKey = GlobalKey<NavigatorState>();
  ref.listen(authSessionProvider, (_, _) {
    refresh.value++;
  });

  final router = GoRouter(
    navigatorKey: rootKey,
    refreshListenable: refresh,
    initialLocation: ref.read(authSessionProvider) == AuthStatus.signedIn
        ? '/trips'
        : '/login',
    redirect: (context, state) {
      final status = ref.read(authSessionProvider);
      if (status == AuthStatus.unknown) {
        return null;
      }

      final location = state.matchedLocation;
      final isPublic = location == '/login' || location == '/register';
      if (status == AuthStatus.signedOut) {
        return isPublic ? null : '/login';
      }
      if (isPublic) {
        return '/trips';
      }
      return null;
    },
    routes: [
      GoRoute(path: '/login', builder: (context, state) => const LoginScreen()),
      GoRoute(
        path: '/register',
        builder: (context, state) => const RegisterScreen(),
      ),
      GoRoute(
        path: '/t014-test',
        builder: (context, state) => const T014TestScreen(),
      ),
      GoRoute(
        path: '/place-search',
        parentNavigatorKey: rootKey,
        builder: (context, state) => const PlaceSearchScreen(),
      ),
      StatefulShellRoute.indexedStack(
        builder: (context, state, shell) => MainShell(shell: shell),
        branches: [
          StatefulShellBranch(
            routes: [
              GoRoute(
                path: '/trips',
                builder: (context, state) => const TripsScreen(),
                routes: [
                  GoRoute(
                    path: 'new',
                    parentNavigatorKey: rootKey,
                    builder: (context, state) => const TripEditorScreen(),
                  ),
                  GoRoute(
                    path: ':id/edit',
                    parentNavigatorKey: rootKey,
                    builder: (context, state) {
                      final id = int.tryParse(state.pathParameters['id'] ?? '');
                      return TripEditorScreen(tripId: id);
                    },
                  ),
                  GoRoute(
                    path: ':id',
                    parentNavigatorKey: rootKey,
                    builder: (context, state) {
                      final id = int.tryParse(state.pathParameters['id'] ?? '');
                      return TripDetailsScreen(tripId: id);
                    },
                  ),
                ],
              ),
            ],
          ),
          StatefulShellBranch(
            routes: [
              GoRoute(
                path: '/map',
                builder: (context, state) => const MapScreen(),
              ),
            ],
          ),
          StatefulShellBranch(
            routes: [
              GoRoute(
                path: '/statistics',
                builder: (context, state) =>
                    const SectionPlaceholder(title: 'Statystyki'),
              ),
            ],
          ),
          StatefulShellBranch(
            routes: [
              GoRoute(
                path: '/profile',
                builder: (context, state) => const ProfileScreen(),
              ),
            ],
          ),
        ],
      ),
    ],
  );

  ref.onDispose(() {
    router.dispose();
    refresh.dispose();
  });
  return router;
});
