import 'package:flutter/material.dart';

import 'package:travel_app/theme/app_colors.dart';
import 'package:travel_app/theme/app_palette.dart';
import 'package:travel_app/theme/transport_mode.dart';
import 'package:travel_app/widgets/app_button.dart';
import 'package:travel_app/widgets/app_text_field.dart';
import 'package:travel_app/widgets/empty_state_view.dart';
import 'package:travel_app/widgets/error_view.dart';
import 'package:travel_app/widgets/loading_view.dart';

class T014TestScreen extends StatefulWidget {
  const T014TestScreen({super.key});

  @override
  State<T014TestScreen> createState() => _T014TestScreenState();
}

class _T014TestScreenState extends State<T014TestScreen> {
  final _formKey = GlobalKey<FormState>();
  bool _loading = false;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final colors = theme.colorScheme;

    return Scaffold(
      appBar: AppBar(title: const Text('T-014_test')),
      body: ListView(
        padding: const EdgeInsets.fromLTRB(16, 8, 16, 32),
        children: [
          _Section(
            title: 'Kolory',
            child: Wrap(
              spacing: 8,
              runSpacing: 8,
              children: [
                for (final swatch in _swatches(context))
                  _Swatch(name: swatch.$1, color: swatch.$2),
              ],
            ),
          ),
          _Section(
            title: 'Typografia',
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text('Tytuł ekranu', style: theme.textTheme.headlineMedium),
                Text('Logo', style: theme.textTheme.headlineSmall),
                Text('Nagłówek sekcji', style: theme.textTheme.titleLarge),
                Text('Pasek ekranu', style: theme.textTheme.titleMedium),
                Text('Karta', style: theme.textTheme.titleSmall),
                Text('Pole', style: theme.textTheme.bodyLarge),
                Text('Treść', style: theme.textTheme.bodyMedium),
                Text(
                  'Podtytuł',
                  style: theme.textTheme.bodySmall?.copyWith(
                    color: colors.onSurfaceVariant,
                  ),
                ),
                Text(
                  'Etykieta pola',
                  style: theme.textTheme.labelLarge?.copyWith(
                    color: colors.onSurfaceVariant,
                  ),
                ),
                Text(
                  'PODPIS',
                  style: context.textRoles.listHeader.copyWith(
                    color: colors.onSurfaceVariant,
                  ),
                ),
              ],
            ),
          ),
          _Section(
            title: 'Transport',
            child: Column(
              children: [
                for (final mode in TransportMode.values) ...[
                  _TransportRow(mode: mode),
                  const SizedBox(height: 12),
                ],
              ],
            ),
          ),
          _Section(
            title: 'Przycisk',
            child: Column(
              children: [
                AppButton(label: 'Zapisz', onPressed: () {}),
                const SizedBox(height: 12),
                AppButton(
                  label: 'Zapisz',
                  loadingLabel: 'Zapisuję',
                  isLoading: _loading,
                  onPressed: () => setState(() => _loading = !_loading),
                ),
                const SizedBox(height: 12),
                const AppButton(label: 'Zapisz', onPressed: null),
                const SizedBox(height: 12),
                const Row(
                  children: [
                    Expanded(
                      child: AppButton(label: 'Anuluj', onPressed: _noop),
                    ),
                    SizedBox(width: 12),
                    Expanded(
                      child: AppButton(label: 'Zapisz', onPressed: _noop),
                    ),
                  ],
                ),
              ],
            ),
          ),
          _Section(
            title: 'Pole tekstowe',
            child: Form(
              key: _formKey,
              child: Column(
                children: [
                  AppTextField(
                    label: 'E-mail',
                    hintText: 'jan.kowalski@example.com',
                    keyboardType: TextInputType.emailAddress,
                  ),
                  const SizedBox(height: 16),
                  AppTextField(
                    label: 'Hasło',
                    initialValue: 'haslo',
                    obscureText: true,
                    errorText: 'Nieprawidłowy e-mail lub hasło',
                  ),
                  const SizedBox(height: 16),
                  AppTextField(
                    label: 'Walidacja formularza',
                    hintText: 'Wpisz cokolwiek',
                    validator: (value) {
                      if (value == null || value.trim().isEmpty) {
                        return 'To pole jest wymagane';
                      }
                      return null;
                    },
                  ),
                  const SizedBox(height: 12),
                  AppButton(
                    label: 'Sprawdź',
                    onPressed: () => _formKey.currentState?.validate(),
                  ),
                ],
              ),
            ),
          ),
          const _Section(
            title: 'Ładowanie',
            child: SizedBox(height: 120, child: LoadingView(message: 'SZUKAM…')),
          ),
          const _Section(
            title: 'Błąd',
            child: ErrorView(
              title: 'Brak połączenia z serwerem',
              message:
                  'Sprawdź połączenie z internetem i spróbuj ponownie za chwilę.',
              onRetry: _noop,
            ),
          ),
          const _Section(
            title: 'Pusty stan',
            child: EmptyStateView(
              icon: Icons.luggage_outlined,
              title: 'Nie masz jeszcze podróży',
              message:
                  'Dodaj pierwszą podróż, aby zobaczyć ją na mapie i zacząć budować swoje statystyki.',
              actionLabel: 'Dodaj pierwszą podróż',
              onAction: _noop,
            ),
          ),
        ],
      ),
    );
  }

  List<(String, Color)> _swatches(BuildContext context) {
    final palette = context.palette;
    final colors = Theme.of(context).colorScheme;
    return [
      ('Primary', colors.primary),
      ('Pressed', palette.primaryPressed),
      ('Tint', colors.primaryContainer),
      ('Background', Theme.of(context).scaffoldBackgroundColor),
      ('Surface', colors.surface),
      ('Variant', colors.surfaceContainerHighest),
      ('Border', colors.outline),
      ('Text', colors.onSurface),
      ('Secondary text', colors.onSurfaceVariant),
      ('Placeholder', palette.placeholder),
      ('Success', palette.success),
      ('Warning', palette.warning),
      ('Error', colors.error),
      ('Info', palette.info),
      ('Danger', palette.danger),
    ];
  }
}

void _noop() {}

class _Section extends StatelessWidget {
  const _Section({required this.title, required this.child});

  final String title;
  final Widget child;

  @override
  Widget build(BuildContext context) {
    final colors = Theme.of(context).colorScheme;
    return Padding(
      padding: const EdgeInsets.only(bottom: 28),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            title.toUpperCase(),
            style: context.textRoles.listHeader.copyWith(
              color: colors.onSurfaceVariant,
            ),
          ),
          const SizedBox(height: 12),
          child,
        ],
      ),
    );
  }
}

class _Swatch extends StatelessWidget {
  const _Swatch({required this.name, required this.color});

  final String name;
  final Color color;

  @override
  Widget build(BuildContext context) {
    final onColor = ThemeData.estimateBrightnessForColor(color) ==
            Brightness.dark
        ? AppColors.onPrimary
        : AppColors.text;
    return Container(
      width: 104,
      height: 64,
      padding: const EdgeInsets.all(8),
      decoration: BoxDecoration(
        color: color,
        borderRadius: BorderRadius.circular(12),
        border: Border.all(color: Theme.of(context).colorScheme.outline),
      ),
      child: Text(
        name,
        style: Theme.of(context).textTheme.labelSmall?.copyWith(color: onColor),
      ),
    );
  }
}

class _TransportRow extends StatelessWidget {
  const _TransportRow({required this.mode});

  final TransportMode mode;

  @override
  Widget build(BuildContext context) {
    return Row(
      children: [
        Icon(mode.icon, color: mode.color),
        const SizedBox(width: 12),
        Expanded(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(
                mode.label,
                style: Theme.of(context).textTheme.labelLarge?.copyWith(
                  color: mode.onColor,
                ),
              ),
              Text(
                mode.lineStyle.name,
                style: Theme.of(context).textTheme.labelSmall?.copyWith(
                  color: Theme.of(context).colorScheme.onSurfaceVariant,
                ),
              ),
            ],
          ),
        ),
        CustomPaint(
          size: const Size(120, 36),
          painter: _TransportLinePainter(mode),
        ),
      ],
    );
  }
}

class _TransportLinePainter extends CustomPainter {
  _TransportLinePainter(this.mode);

  final TransportMode mode;

  @override
  void paint(Canvas canvas, Size size) {
    final stroke = mode.stroke;
    final paint = Paint()
      ..color = mode.color
      ..style = PaintingStyle.stroke
      ..strokeWidth = stroke.width
      ..strokeCap = StrokeCap.round
      ..strokeJoin = StrokeJoin.round;

    final start = Offset(0, size.height * 0.7);
    final end = Offset(size.width, size.height * 0.7);

    switch (stroke.style) {
      case TransportLineStyle.solid:
        canvas.drawLine(start, end, paint);
      case TransportLineStyle.arc:
        final path = Path()
          ..moveTo(start.dx, start.dy)
          ..quadraticBezierTo(
            size.width / 2,
            size.height * 0.15,
            end.dx,
            end.dy,
          );
        canvas.drawPath(path, paint);
      case TransportLineStyle.dashed:
        _drawDashed(canvas, start, end, paint, stroke.dashPattern);
      case TransportLineStyle.ticked:
        canvas.drawLine(start, end, paint);
        final tickPaint = Paint()
          ..color = mode.color
          ..strokeWidth = stroke.tickWidth
          ..strokeCap = StrokeCap.round;
        final count = (size.width / stroke.tickSpacing).floor();
        for (var i = 0; i <= count; i++) {
          final x = i * stroke.tickSpacing;
          if (x > size.width) {
            break;
          }
          final y = start.dy;
          canvas.drawLine(
            Offset(x, y - stroke.tickLength / 2),
            Offset(x, y + stroke.tickLength / 2),
            tickPaint,
          );
        }
    }
  }

  void _drawDashed(
    Canvas canvas,
    Offset start,
    Offset end,
    Paint paint,
    List<double> pattern,
  ) {
    if (pattern.isEmpty) {
      canvas.drawLine(start, end, paint);
      return;
    }
    final delta = end - start;
    final total = delta.distance;
    if (total == 0) {
      return;
    }
    final direction = delta / total;
    var distance = 0.0;
    var draw = true;
    var index = 0;
    while (distance < total) {
      final length = pattern[index % pattern.length];
      final next = distance + length > total ? total : distance + length;
      if (draw) {
        canvas.drawLine(
          start + direction * distance,
          start + direction * next,
          paint,
        );
      }
      draw = !draw;
      distance = next;
      index++;
    }
  }

  @override
  bool shouldRepaint(covariant _TransportLinePainter oldDelegate) {
    return oldDelegate.mode != mode;
  }
}
