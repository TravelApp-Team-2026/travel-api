String? validateEmail(String? value) {
  final text = value?.trim() ?? '';
  if (text.isEmpty) {
    return 'E-mail jest wymagany.';
  }
  if (!RegExp(r'^[^@\s]+@[^@\s]+\.[^@\s]+$').hasMatch(text)) {
    return 'Podaj poprawny adres e-mail.';
  }
  return null;
}

String? validateRequiredPassword(String? value) {
  if (value == null || value.isEmpty) {
    return 'Hasło jest wymagane.';
  }
  return null;
}
