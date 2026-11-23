import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:travel_app/main.dart';

void main() {
  testWidgets('app starts on the login screen', (WidgetTester tester) async {
    await tester.pumpWidget(const ProviderScope(child: TravelApp()));

    expect(find.text('Zaloguj się'), findsWidgets);
    expect(find.text('E-mail'), findsOneWidget);
    expect(find.text('Hasło'), findsOneWidget);
  });
}
