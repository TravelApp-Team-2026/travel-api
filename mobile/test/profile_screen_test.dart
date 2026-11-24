import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:travel_app/main.dart';
import 'package:travel_app/screens/profile_screen.dart';
import 'package:travel_app/services/api_exception.dart';
import 'package:travel_app/services/profile_api.dart';
import 'package:travel_app/services/token_storage.dart';
import 'package:travel_app/theme/app_theme.dart';

import 'fake_profile_api.dart';
import 'memory_token_storage.dart';

void main() {
  const profile = UserProfile(
    id: 1,
    email: 'jan.kowalski@example.com',
    displayName: 'Jan Kowalski',
    bio: 'Lubię góry.',
    isPublic: false,
  );

  testWidgets('email is read-only and name, bio and visibility are editable', (
    tester,
  ) async {
    final api = FakeProfileApi(profile);
    await _pumpProfile(tester, api);

    expect(find.text('Profil'), findsOneWidget);
    expect(find.text('Konto i prywatność'), findsOneWidget);
    expect(find.text('JK'), findsOneWidget);
    expect(find.text('Jan Kowalski'), findsWidgets);
    expect(
      find.text(
        'Inni użytkownicy mogą oglądać Twoje podróże i statystyki. '
        'Adres e-mail nigdy nie jest widoczny.',
      ),
      findsOneWidget,
    );
    expect(find.text('11 / 300'), findsOneWidget);

    final email = tester.widget<TextField>(
      find.widgetWithText(TextField, 'jan.kowalski@example.com'),
    );
    expect(email.readOnly, isTrue);
    expect(email.canRequestFocus, isFalse);

    final name = tester.widget<TextField>(
      find.widgetWithText(TextField, 'Jan Kowalski'),
    );
    final bio = tester.widget<TextField>(
      find.widgetWithText(TextField, 'Lubię góry.'),
    );
    expect(name.readOnly, isFalse);
    expect(bio.readOnly, isFalse);
  });

  testWidgets('save sends the edited profile', (tester) async {
    final api = FakeProfileApi(profile);
    await _pumpProfile(tester, api);

    await tester.enterText(
      find.widgetWithText(TextField, 'Jan Kowalski'),
      'Anna Nowak',
    );
    await tester.enterText(
      find.widgetWithText(TextField, 'Lubię góry.'),
      'Nowy opis',
    );
    await tester.tap(find.byKey(const Key('public-profile-switch')));
    await tester.pump();
    await tester.ensureVisible(find.text('Zapisz zmiany'));
    await tester.tap(find.text('Zapisz zmiany'));
    await tester.pumpAndSettle();

    expect(api.updates, 1);
    expect(api.displayName, 'Anna Nowak');
    expect(api.bio, 'Nowy opis');
    expect(api.isPublic, isTrue);
    expect(find.text('AN'), findsOneWidget);
    expect(find.text('Zmiany zapisane!'), findsOneWidget);
    await tester.tap(find.text('Ok'));
    await tester.pumpAndSettle();
    expect(find.text('Zmiany zapisane!'), findsNothing);
  });

  testWidgets('save does nothing when the profile is unchanged', (tester) async {
    final api = FakeProfileApi(profile);
    await _pumpProfile(tester, api);

    await tester.ensureVisible(find.text('Zapisz zmiany'));
    await tester.tap(find.text('Zapisz zmiany'));
    await tester.pump();

    expect(api.updates, 0);
    expect(find.text('Zmiany zapisane!'), findsNothing);
  });

  testWidgets('blank display name is not sent', (tester) async {
    final api = FakeProfileApi(profile);
    await _pumpProfile(tester, api);

    await tester.enterText(find.widgetWithText(TextField, 'Jan Kowalski'), ' ');
    await tester.ensureVisible(find.text('Zapisz zmiany'));
    await tester.tap(find.text('Zapisz zmiany'));
    await tester.pump();

    expect(api.updates, 0);
    expect(find.text('Nazwa wyświetlana nie może być pusta.'), findsOneWidget);
  });

  testWidgets('a field error from the server stays on the name', (tester) async {
    final api = FakeProfileApi(profile)
      ..updateError = const ApiException(
        'Przesłane dane są nieprawidłowe.',
        fieldErrors: {
          'displayName': 'Nazwa wyświetlana może mieć maksymalnie 50 znaków.',
        },
      );
    await _pumpProfile(tester, api);

    await tester.enterText(
      find.widgetWithText(TextField, 'Jan Kowalski'),
      'Anna',
    );
    await tester.ensureVisible(find.text('Zapisz zmiany'));
    await tester.tap(find.text('Zapisz zmiany'));
    await tester.pumpAndSettle();

    expect(
      find.text('Nazwa wyświetlana może mieć maksymalnie 50 znaków.'),
      findsOneWidget,
    );
    expect(find.text('jan.kowalski@example.com'), findsOneWidget);
  });

  testWidgets('logout asks, then clears the token and returns to login', (
    tester,
  ) async {
    final storage = MemoryTokenStorage('jwt');
    final api = FakeProfileApi(profile);
    _useView(tester, const Size(800, 1600));
    await tester.pumpWidget(
      ProviderScope(
        overrides: [
          tokenStorageProvider.overrideWithValue(storage),
          profileApiProvider.overrideWithValue(api),
        ],
        child: const TravelApp(),
      ),
    );
    await tester.pumpAndSettle();

    await tester.tap(find.text('Profil'));
    await tester.pumpAndSettle();
    await tester.ensureVisible(find.text('Wyloguj się'));
    await tester.tap(find.text('Wyloguj się'));
    await tester.pumpAndSettle();

    expect(find.text('Wylogować się?'), findsOneWidget);
    expect(
      find.text(
        'Zostaniesz wylogowany z tego urządzenia.\n'
        'Twoje podróże pozostaną na koncie.',
      ),
      findsOneWidget,
    );
    await tester.tap(find.text('Anuluj'));
    await tester.pumpAndSettle();
    expect(storage.token, 'jwt');
    expect(find.text('Wylogować się?'), findsNothing);

    await tester.ensureVisible(find.text('Wyloguj się'));
    await tester.tap(find.text('Wyloguj się'));
    await tester.pumpAndSettle();
    await tester.tap(find.text('Wyloguj'));
    await tester.pumpAndSettle();

    expect(storage.token, isNull);
    expect(find.text('Zaloguj się'), findsWidgets);
    expect(find.text('Konto i prywatność'), findsNothing);
  });

  testWidgets('leaving with an edited name asks before discarding it', (
    tester,
  ) async {
    final api = FakeProfileApi(profile);
    _useView(tester, const Size(800, 1600));
    await tester.pumpWidget(
      ProviderScope(
        overrides: [
          tokenStorageProvider.overrideWithValue(MemoryTokenStorage('jwt')),
          profileApiProvider.overrideWithValue(api),
        ],
        child: const TravelApp(),
      ),
    );
    await tester.pumpAndSettle();

    await tester.tap(find.text('Profil'));
    await tester.pumpAndSettle();
    await tester.enterText(
      find.widgetWithText(TextField, 'Jan Kowalski'),
      'Anna Nowak',
    );
    await tester.tap(find.text('Mapa'));
    await tester.pumpAndSettle();

    expect(find.text('Masz niezapisane zmiany'), findsOneWidget);
    expect(
      find.text('Czy jesteś pewien że chcesz je anulować?'),
      findsOneWidget,
    );
    await tester.tap(find.text('Nie'));
    await tester.pumpAndSettle();

    expect(find.text('Masz niezapisane zmiany'), findsNothing);
    expect(find.text('Konto i prywatność'), findsOneWidget);
    expect(
      find.widgetWithText(TextField, 'Anna Nowak'),
      findsOneWidget,
    );

    await tester.tap(find.text('Mapa'));
    await tester.pumpAndSettle();
    await tester.tap(find.text('Tak'));
    await tester.pumpAndSettle();

    expect(find.text('Mapa'), findsNWidgets(2));
    expect(find.text('Konto i prywatność'), findsNothing);

    await tester.tap(find.text('Profil').first);
    await tester.pumpAndSettle();
    expect(
      find.widgetWithText(TextField, 'Jan Kowalski'),
      findsOneWidget,
    );
    expect(api.updates, 0);
  });
}

void _useView(WidgetTester tester, Size size) {
  tester.view.physicalSize = size;
  tester.view.devicePixelRatio = 1;
  addTearDown(tester.view.resetPhysicalSize);
  addTearDown(tester.view.resetDevicePixelRatio);
}

Future<void> _pumpProfile(WidgetTester tester, FakeProfileApi api) {
  _useView(tester, const Size(360, 1200));
  return tester.pumpWidget(
    ProviderScope(
      overrides: [profileApiProvider.overrideWithValue(api)],
      child: MaterialApp(theme: AppTheme.light, home: const ProfileScreen()),
    ),
  ).then((_) => tester.pumpAndSettle());
}
