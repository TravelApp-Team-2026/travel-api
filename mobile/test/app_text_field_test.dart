import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:travel_app/widgets/app_text_field.dart';

void main() {
  testWidgets('backend error does not block the next submit', (tester) async {
    final formKey = GlobalKey<FormState>();
    String? serverError = 'Nieprawidłowy adres e-mail lub hasło.';
    bool? valid;

    await tester.pumpWidget(
      MaterialApp(
        home: StatefulBuilder(
          builder: (context, setState) {
            return Scaffold(
              body: Form(
                key: formKey,
                child: Column(
                  children: [
                    AppTextField(
                      label: 'Hasło',
                      initialValue: 'secret-password',
                      errorText: serverError,
                      validator: (value) =>
                          (value == null || value.isEmpty) ? 'Hasło jest wymagane.' : null,
                    ),
                    TextButton(
                      onPressed: () {
                        setState(() => serverError = null);
                        valid = formKey.currentState!.validate();
                      },
                      child: const Text('Zaloguj się'),
                    ),
                  ],
                ),
              ),
            );
          },
        ),
      ),
    );

    expect(find.text(serverError!), findsOneWidget);

    await tester.tap(find.text('Zaloguj się'));
    await tester.pump();

    expect(valid, isTrue);
    expect(find.text('Nieprawidłowy adres e-mail lub hasło.'), findsNothing);
  });
}
