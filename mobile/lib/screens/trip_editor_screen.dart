import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:travel_app/services/api_exception.dart';
import 'package:travel_app/services/place_search_api.dart';
import 'package:travel_app/services/trips_api.dart';
import 'package:travel_app/theme/app_colors.dart';
import 'package:travel_app/theme/app_palette.dart';
import 'package:travel_app/theme/transport_mode.dart';
import 'package:travel_app/widgets/app_button.dart';
import 'package:travel_app/widgets/app_text_field.dart';
import 'package:travel_app/widgets/error_view.dart';
import 'package:travel_app/widgets/loading_view.dart';
import 'package:travel_app/widgets/transport_sheet.dart';

class TripEditorScreen extends ConsumerStatefulWidget {
  const TripEditorScreen({super.key, this.tripId});

  final int? tripId;

  @override
  ConsumerState<TripEditorScreen> createState() => _TripEditorScreenState();
}

enum _Status { ready, loading, error }

class _EditorStop {
  _EditorStop(this.place, {this.outgoing});

  final Place place;
  TransportMode? outgoing;
  final key = UniqueKey();
}

class _TripEditorScreenState extends ConsumerState<TripEditorScreen> {
  final _title = TextEditingController();
  final _description = TextEditingController();
  final _stops = <_EditorStop>[];

  var _status = _Status.ready;
  var _saving = false;
  var _showDescription = false;
  var _request = 0;
  DateTime? _start;
  DateTime? _end;
  String? _loadError;
  String? _titleError;
  String? _startError;
  String? _endError;
  String? _descriptionError;
  String? _routeError;
  String? _formError;

  bool get _editing => widget.tripId != null;

  @override
  void initState() {
    super.initState();
    if (_editing) {
      unawaited(_load());
    } else {
      final now = DateTime.now();
      _start = DateTime(now.year, now.month, now.day);
    }
  }

  @override
  void dispose() {
    _title.dispose();
    _description.dispose();
    super.dispose();
  }

  Future<void> _load() async {
    final id = widget.tripId;
    if (id == null) {
      return;
    }
    final request = ++_request;
    setState(() {
      _status = _Status.loading;
      _loadError = null;
    });
    try {
      final trip = await ref.read(tripsApiProvider).getTrip(id);
      if (!mounted || request != _request) {
        return;
      }
      _applyTrip(trip);
      setState(() => _status = _Status.ready);
    } on ApiException catch (error) {
      if (!mounted || request != _request) {
        return;
      }
      setState(() {
        _status = _Status.error;
        _loadError = error.message;
      });
    }
  }

  void _applyTrip(TripDetails trip) {
    _title.text = trip.title;
    _description.text = trip.description ?? '';
    _showDescription = trip.description != null && trip.description!.isNotEmpty;
    _start = trip.startDate;
    _end = trip.endDate;
    _stops
      ..clear()
      ..addAll([
        for (var i = 0; i < trip.waypoints.length; i++)
          _EditorStop(
            trip.waypoints[i],
            outgoing: i < trip.modes.length ? trip.modes[i] : null,
          ),
      ]);
    _normalizeModes();
  }

  void _normalizeModes() {
    for (var i = 0; i < _stops.length; i++) {
      if (i == _stops.length - 1) {
        _stops[i].outgoing = null;
      } else {
        _stops[i].outgoing ??= TransportMode.car;
      }
    }
  }

  Future<void> _addStop() async {
    if (_stops.length >= 50) {
      return;
    }
    final place = await context.push<Place>('/place-search');
    if (!mounted || place == null) {
      return;
    }
    setState(() {
      _stops.add(_EditorStop(place));
      _normalizeModes();
      _routeError = null;
    });
  }

  void _removeStop(int index) {
    setState(() {
      _stops.removeAt(index);
      _normalizeModes();
    });
  }

  void _reorder(int oldIndex, int newIndex) {
    setState(() {
      if (newIndex > oldIndex) {
        newIndex -= 1;
      }
      final stop = _stops.removeAt(oldIndex);
      _stops.insert(newIndex, stop);
      _normalizeModes();
    });
  }

  Future<void> _pickMode(int index) async {
    if (index < 0 || index >= _stops.length - 1) {
      return;
    }
    final from = _stops[index];
    final to = _stops[index + 1];
    final fromKey = from.key;
    final selected = from.outgoing ?? TransportMode.car;
    final next = await showTransportSheet(
      context: context,
      selected: selected,
      fromName: from.place.placeName,
      toName: to.place.placeName,
    );
    if (!mounted || next == null) {
      return;
    }
    final current = _stops.indexWhere((stop) => stop.key == fromKey);
    if (current < 0 || current >= _stops.length - 1) {
      return;
    }
    setState(() => _stops[current].outgoing = next);
  }

  Future<void> _pickStart() async {
    final picked = await _pickDate(_start ?? DateTime.now());
    if (picked == null) {
      return;
    }
    setState(() {
      _start = picked;
      _startError = null;
      if (_end != null && _end!.isBefore(picked)) {
        _end = picked;
        _endError = null;
      }
    });
  }

  Future<void> _pickEnd() async {
    final picked = await _pickDate(_end ?? _start ?? DateTime.now());
    if (picked == null) {
      return;
    }
    setState(() {
      _end = picked;
      _endError = null;
    });
  }

  Future<DateTime?> _pickDate(DateTime initial) {
    return showDatePicker(
      context: context,
      initialDate: initial,
      firstDate: DateTime(1900),
      lastDate: DateTime(2100),
    );
  }

  bool _validate() {
    var ok = true;
    String? titleError;
    String? startError;
    String? endError;
    String? descriptionError;
    String? routeError;
    final title = _title.text.trim();
    if (title.isEmpty) {
      titleError = 'Podaj nazwę podróży.';
      ok = false;
    } else if (title.length > 100) {
      titleError = 'Nazwa podróży może mieć maksymalnie 100 znaków.';
      ok = false;
    }
    if (_start == null) {
      startError = 'Podaj datę rozpoczęcia podróży.';
      ok = false;
    }
    if (_start != null && _end != null && _end!.isBefore(_start!)) {
      endError =
          'Data zakończenia nie może być wcześniejsza niż data rozpoczęcia.';
      ok = false;
    }
    if (_description.text.length > 1000) {
      descriptionError = 'Opis podróży może mieć maksymalnie 1000 znaków.';
      ok = false;
    }
    if (_stops.length < 2) {
      routeError =
          'Podróż musi mieć co najmniej 2 przystanki i maksymalnie 50.';
      ok = false;
    }
    setState(() {
      _titleError = titleError;
      _startError = startError;
      _endError = endError;
      _descriptionError = descriptionError;
      _routeError = routeError;
      _formError = null;
    });
    return ok;
  }

  SaveTrip _draft() {
    final description = _description.text.trim();
    return SaveTrip(
      title: _title.text.trim(),
      description: description.isEmpty ? null : description,
      startDate: _start!,
      endDate: _end,
      waypoints: [for (final stop in _stops) stop.place],
      modes: [
        for (var i = 0; i < _stops.length - 1; i++)
          _stops[i].outgoing ?? TransportMode.car,
      ],
    );
  }

  Future<void> _save() async {
    if (_saving || !_validate()) {
      return;
    }
    setState(() => _saving = true);
    try {
      final api = ref.read(tripsApiProvider);
      final id = widget.tripId;
      if (id == null) {
        await api.createTrip(_draft());
      } else {
        await api.updateTrip(id, _draft());
      }
      if (!mounted) {
        return;
      }
      if (context.canPop()) {
        context.pop(true);
      } else {
        setState(() => _saving = false);
      }
    } on ApiException catch (error) {
      if (!mounted) {
        return;
      }
      setState(() {
        _saving = false;
        _titleError = error.fieldErrors['title'] ?? _titleError;
        _startError = error.fieldErrors['startDate'] ?? _startError;
        _endError = error.fieldErrors['endDate'] ?? _endError;
        _descriptionError =
            error.fieldErrors['description'] ?? _descriptionError;
        _routeError =
            error.fieldErrors['waypoints'] ??
            error.fieldErrors['segments'] ??
            _routeError;
        _formError = error.message;
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    return PopScope(
      canPop: !_saving,
      child: Scaffold(
        appBar: AppBar(
          leading: IconButton(
            icon: const Icon(Icons.close),
            onPressed: _saving ? null : () => context.pop(),
          ),
          title: Text(_editing ? 'Edytuj podróż' : 'Nowa podróż'),
        ),
        body: switch (_status) {
          _Status.loading => const LoadingView(),
          _Status.error => ErrorView(
            title: 'Nie udało się wczytać podróży',
            message: _loadError ?? 'Spróbuj ponownie.',
            onRetry: () => unawaited(_load()),
          ),
          _Status.ready => _form(),
        },
      ),
    );
  }

  Widget _form() {
    final colors = Theme.of(context).colorScheme;
    return Column(
      children: [
        Expanded(
          child: CustomScrollView(
            keyboardDismissBehavior: ScrollViewKeyboardDismissBehavior.onDrag,
            slivers: [
              SliverPadding(
                padding: const EdgeInsets.fromLTRB(16, 8, 16, 0),
                sliver: SliverToBoxAdapter(child: _details(colors)),
              ),
              SliverPadding(
                padding: const EdgeInsets.fromLTRB(16, 24, 16, 8),
                sliver: SliverToBoxAdapter(child: _routeHeader(colors)),
              ),
              if (_stops.isEmpty)
                const SliverToBoxAdapter(child: SizedBox.shrink())
              else
                SliverPadding(
                  padding: const EdgeInsets.fromLTRB(16, 0, 16, 16),
                  sliver: SliverReorderableList(
                    itemCount: _stops.length,
                    onReorder: _reorder,
                    itemBuilder: (context, index) {
                      final stop = _stops[index];
                      return Padding(
                        key: stop.key,
                        padding: const EdgeInsets.only(bottom: 4),
                        child: Column(
                          children: [
                            _StopTile(
                              index: index,
                              stop: stop,
                              onRemove: () => _removeStop(index),
                            ),
                            if (index < _stops.length - 1)
                              _LegChip(
                                mode: stop.outgoing ?? TransportMode.car,
                                onTap: () => unawaited(_pickMode(index)),
                              ),
                          ],
                        ),
                      );
                    },
                  ),
                ),
              if (_routeError != null)
                SliverPadding(
                  padding: const EdgeInsets.fromLTRB(16, 0, 16, 16),
                  sliver: SliverToBoxAdapter(
                    child: Text(
                      _routeError!,
                      style: Theme.of(
                        context,
                      ).textTheme.labelLarge?.copyWith(color: colors.error),
                    ),
                  ),
                ),
              if (_formError != null)
                SliverPadding(
                  padding: const EdgeInsets.fromLTRB(16, 0, 16, 16),
                  sliver: SliverToBoxAdapter(
                    child: Text(
                      _formError!,
                      style: Theme.of(
                        context,
                      ).textTheme.labelLarge?.copyWith(color: colors.error),
                    ),
                  ),
                ),
            ],
          ),
        ),
        _Footer(
          canAdd: _stops.length < 50,
          saving: _saving,
          onAdd: () => unawaited(_addStop()),
          onSave: () => unawaited(_save()),
        ),
      ],
    );
  }

  Widget _details(ColorScheme colors) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        AppTextField(
          label: 'Nazwa podróży',
          controller: _title,
          errorText: _titleError,
          maxLength: 100,
          textInputAction: TextInputAction.next,
          onChanged: (_) {
            if (_titleError != null) {
              setState(() => _titleError = null);
            }
          },
        ),
        const SizedBox(height: 16),
        Row(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Expanded(
              child: AppTextField(
                key: ValueKey('trip-start-date-$_start'),
                label: 'Od',
                readOnly: true,
                errorText: _startError,
                prefixIcon: IconButton(
                  key: const Key('trip-pick-start-date'),
                  tooltip: 'Od',
                  visualDensity: VisualDensity.compact,
                  icon: const Icon(Icons.calendar_today_outlined, size: 20),
                  onPressed: () => unawaited(_pickStart()),
                ),
                initialValue: _start == null ? '' : formatTripDay(_start!),
                onTap: () => unawaited(_pickStart()),
              ),
            ),
            const SizedBox(width: 12),
            Expanded(
              child: AppTextField(
                key: ValueKey('trip-end-date-$_end'),
                label: 'Do (opcjonalnie)',
                readOnly: true,
                errorText: _endError,
                prefixIcon: IconButton(
                  key: const Key('trip-pick-end-date'),
                  tooltip: 'Do',
                  visualDensity: VisualDensity.compact,
                  icon: const Icon(Icons.calendar_today_outlined, size: 20),
                  onPressed: () => unawaited(_pickEnd()),
                ),
                suffixIcon: _end == null
                    ? null
                    : IconButton(
                        key: const Key('trip-clear-end-date'),
                        icon: const Icon(Icons.close, size: 18),
                        onPressed: () => setState(() {
                          _end = null;
                          _endError = null;
                        }),
                      ),
                initialValue: _end == null ? '' : formatTripDay(_end!),
                onTap: () => unawaited(_pickEnd()),
              ),
            ),
          ],
        ),
        const SizedBox(height: 16),
        if (_showDescription)
          AppTextField(
            label: 'Opis (opcjonalnie)',
            controller: _description,
            errorText: _descriptionError,
            maxLines: 4,
            maxLength: 1000,
            counterInside: true,
            onChanged: (_) {
              if (_descriptionError != null) {
                setState(() => _descriptionError = null);
              }
            },
          )
        else
          Align(
            alignment: Alignment.centerLeft,
            child: TextButton.icon(
              onPressed: () => setState(() => _showDescription = true),
              icon: Icon(Icons.add, color: context.palette.primaryPressed),
              label: Text(
                'Dodaj opis (opcjonalnie)',
                style: Theme.of(context).textTheme.labelLarge?.copyWith(
                  color: context.palette.primaryPressed,
                  fontWeight: FontWeight.w700,
                ),
              ),
            ),
          ),
      ],
    );
  }

  Widget _routeHeader(ColorScheme colors) {
    return Row(
      children: [
        Text('Trasa', style: Theme.of(context).textTheme.titleSmall),
        const Spacer(),
        Text(
          formatStopCount(_stops.length),
          style: Theme.of(
            context,
          ).textTheme.labelLarge?.copyWith(color: colors.onSurfaceVariant),
        ),
      ],
    );
  }
}

class _StopTile extends StatelessWidget {
  const _StopTile({
    required this.index,
    required this.stop,
    required this.onRemove,
  });

  final int index;
  final _EditorStop stop;
  final VoidCallback onRemove;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final colors = theme.colorScheme;
    return Material(
      color: AppColors.surface,
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(12),
        side: const BorderSide(color: AppColors.border, width: 1.5),
      ),
      child: Padding(
        padding: const EdgeInsets.fromLTRB(10, 8, 4, 8),
        child: Row(
          children: [
            CircleAvatar(
              radius: 11,
              backgroundColor: colors.onSurface,
              child: Text(
                '${index + 1}',
                style: theme.textTheme.labelLarge?.copyWith(
                  color: colors.surface,
                  fontWeight: FontWeight.w700,
                  fontSize: 12,
                ),
              ),
            ),
            const SizedBox(width: 10),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    stop.place.placeName,
                    maxLines: 1,
                    overflow: TextOverflow.ellipsis,
                    style: theme.textTheme.bodySmall?.copyWith(
                      fontWeight: FontWeight.w700,
                    ),
                  ),
                  Text(
                    stop.place.countryName,
                    maxLines: 1,
                    overflow: TextOverflow.ellipsis,
                    style: theme.textTheme.labelSmall?.copyWith(
                      color: colors.onSurfaceVariant,
                    ),
                  ),
                ],
              ),
            ),
            IconButton(
              key: Key('remove-stop-$index'),
              icon: Icon(Icons.close, size: 18, color: colors.onSurfaceVariant),
              onPressed: onRemove,
            ),
            ReorderableDragStartListener(
              index: index,
              child: SizedBox(
                key: Key('drag-stop-$index'),
                width: 36,
                height: 40,
                child: Icon(
                  Icons.drag_indicator,
                  color: colors.onSurfaceVariant,
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class _LegChip extends StatelessWidget {
  const _LegChip({required this.mode, required this.onTap});

  final TransportMode mode;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.fromLTRB(20, 4, 0, 8),
      child: Row(
        children: [
          Container(
            width: 3.5,
            height: 28,
            decoration: BoxDecoration(
              color: mode.color,
              borderRadius: BorderRadius.circular(2),
            ),
          ),
          const SizedBox(width: 16),
          Material(
            color: mode.color.withValues(alpha: 0.18),
            shape: StadiumBorder(
              side: BorderSide(color: mode.color, width: 1.2),
            ),
            child: InkWell(
              onTap: onTap,
              customBorder: const StadiumBorder(),
              child: Padding(
                padding: const EdgeInsets.fromLTRB(8, 4, 6, 4),
                child: Row(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    Icon(mode.icon, size: 16, color: mode.onColor),
                    const SizedBox(width: 6),
                    Text(
                      mode.label,
                      style: Theme.of(context).textTheme.labelLarge?.copyWith(
                        color: mode.onColor,
                        fontWeight: FontWeight.w700,
                      ),
                    ),
                    Icon(Icons.expand_more, size: 16, color: mode.onColor),
                  ],
                ),
              ),
            ),
          ),
        ],
      ),
    );
  }
}

class _Footer extends StatelessWidget {
  const _Footer({
    required this.canAdd,
    required this.saving,
    required this.onAdd,
    required this.onSave,
  });

  final bool canAdd;
  final bool saving;
  final VoidCallback onAdd;
  final VoidCallback onSave;

  @override
  Widget build(BuildContext context) {
    final palette = context.palette;
    return Material(
      color: AppColors.surface,
      child: SafeArea(
        top: false,
        child: Padding(
          padding: const EdgeInsets.fromLTRB(16, 12, 16, 12),
          child: Row(
            children: [
              Expanded(
                child: OutlinedButton(
                  key: const Key('add-stop'),
                  onPressed: saving || !canAdd ? null : onAdd,
                  style: OutlinedButton.styleFrom(
                    foregroundColor: palette.primaryPressed,
                    side: BorderSide(color: palette.primaryPressed, width: 1.8),
                    minimumSize: const Size(64, 48),
                    shape: RoundedRectangleBorder(
                      borderRadius: BorderRadius.circular(14),
                    ),
                  ),
                  child: const Text('+ Dodaj przystanek'),
                ),
              ),
              const SizedBox(width: 12),
              Expanded(
                child: AppButton(
                  key: const Key('save-trip'),
                  label: 'Zapisz podróż',
                  loadingLabel: 'Zapisuję…',
                  isLoading: saving,
                  onPressed: saving ? null : onSave,
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
