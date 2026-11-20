import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:travel_app/main.dart';

void main() {
  testWidgets('app starts on T-014_test', (WidgetTester tester) async {
    await tester.pumpWidget(const ProviderScope(child: TravelApp()));

    expect(find.text('T-014_test'), findsOneWidget);
  });
}
