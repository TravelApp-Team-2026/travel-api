import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:travel_app/services/api_exception.dart';
import 'package:travel_app/services/place_search_api.dart';
import 'package:travel_app/theme/app_palette.dart';
import 'package:travel_app/widgets/empty_state_view.dart';
import 'package:travel_app/widgets/error_view.dart';

/// Place search from the mockups. Submits to Nominatim via `GET /api/v1/geo/search`.
class PlaceSearchScreen extends ConsumerStatefulWidget {
  const PlaceSearchScreen({super.key, this.onSelected});

  final ValueChanged<Place>? onSelected;

  @override
  ConsumerState<PlaceSearchScreen> createState() => _PlaceSearchScreenState();
}

enum _Status { idle, loading, results, empty, error }

class _PlaceSearchScreenState extends ConsumerState<PlaceSearchScreen> {
  final _controller = TextEditingController();
  final _focus = FocusNode();
  var _status = _Status.idle;
  var _request = 0;
  var _places = const <Place>[];
  var _submittedQuery = '';
  String? _loadError;
  String? _fieldError;

  @override
  void initState() {
    super.initState();
    _controller.addListener(_onText);
  }

  @override
  void dispose() {
    _controller.removeListener(_onText);
    _controller.dispose();
    _focus.dispose();
    super.dispose();
  }

  void _onText() {
    if (_fieldError != null) {
      _fieldError = null;
    }
    setState(() {});
  }

  Future<void> _search(String raw) async {
    final query = raw.trim().replaceAll(RegExp(r'\s+'), ' ');
    if (query.isEmpty) {
      _request++;
      setState(() {
        _fieldError = 'Wpisz nazwę miejsca.';
        _status = _Status.idle;
        _places = const [];
        _submittedQuery = '';
        _loadError = null;
      });
      return;
    }
    if (_status == _Status.loading && query == _submittedQuery) {
      return;
    }

    final request = ++_request;
    setState(() {
      _status = _Status.loading;
      _submittedQuery = query;
      _fieldError = null;
      _loadError = null;
    });
    FocusScope.of(context).unfocus();

    try {
      final places = await ref.read(placeSearchApiProvider).search(query);
      if (!mounted || request != _request) {
        return;
      }
      setState(() {
        _places = places;
        _status = places.isEmpty ? _Status.empty : _Status.results;
      });
    } on ApiException catch (error) {
      if (!mounted || request != _request) {
        return;
      }
      final field = error.fieldErrors['q'];
      setState(() {
        _fieldError = field;
        _loadError = error.message;
        _status = field != null ? _Status.idle : _Status.error;
        _places = const [];
      });
    }
  }

  void _clear() {
    _request++;
    _controller.clear();
    setState(() {
      _status = _Status.idle;
      _places = const [];
      _submittedQuery = '';
      _fieldError = null;
      _loadError = null;
    });
    _focus.requestFocus();
  }

  void _select(Place place) {
    widget.onSelected?.call(place);
    _pop(place);
  }

  void _pop([Place? result]) {
    final router = GoRouter.maybeOf(context);
    if (router != null) {
      if (router.canPop()) {
        router.pop<Place>(result);
      }
      return;
    }
    if (Navigator.of(context).canPop()) {
      Navigator.of(context).pop(result);
    }
  }

  @override
  Widget build(BuildContext context) {
    final colors = Theme.of(context).colorScheme;

    return Scaffold(
      body: SafeArea(
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            Padding(
              padding: const EdgeInsets.fromLTRB(8, 8, 16, 0),
              child: Row(
                children: [
                  IconButton(
                    icon: const Icon(Icons.arrow_back),
                    onPressed: _pop,
                  ),
                  Expanded(
                    child: _SearchField(
                      controller: _controller,
                      focusNode: _focus,
                      loading: _status == _Status.loading,
                      errorText: _fieldError,
                      onSubmitted: _search,
                      onClear: _controller.text.isEmpty ? null : _clear,
                    ),
                  ),
                ],
              ),
            ),
            const SizedBox(height: 16),
            Expanded(child: _body(colors)),
            Padding(
              padding: const EdgeInsets.fromLTRB(16, 8, 16, 12),
              child: Text(
                'Dane: © współtwórcy OpenStreetMap',
                textAlign: TextAlign.center,
                style: Theme.of(context).textTheme.labelSmall?.copyWith(
                  color: colors.onSurfaceVariant,
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _body(ColorScheme colors) {
    switch (_status) {
      case _Status.idle:
        return const SizedBox.expand();
      case _Status.loading:
        return const Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            _SectionLabel(text: 'SZUKAM…'),
            Expanded(child: _LoadingSkeleton()),
          ],
        );
      case _Status.empty:
        return EmptyStateView(
          icon: Icons.search,
          title: 'Brak wyników dla „$_submittedQuery”',
          message:
              'Sprawdź pisownię albo wpisz pełną nazwę miejsca, np. miasto i kraj.',
        );
      case _Status.error:
        return ErrorView(
          title: 'Nie udało się wyszukać miejsca',
          message: _loadError ?? 'Spróbuj ponownie.',
          onRetry: () => unawaited(_search(_controller.text)),
        );
      case _Status.results:
        return Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            _SectionLabel(text: 'WYNIKI DLA „${_submittedQuery.toUpperCase()}”'),
            Expanded(
              child: ListView.separated(
                keyboardDismissBehavior:
                    ScrollViewKeyboardDismissBehavior.onDrag,
                padding: const EdgeInsets.fromLTRB(16, 4, 16, 8),
                itemCount: _places.length,
                separatorBuilder: (context, index) => Divider(
                  height: 1,
                  indent: 60,
                  color: colors.outline.withValues(alpha: 0.8),
                ),
                itemBuilder: (context, index) {
                  final place = _places[index];
                  return _PlaceTile(place: place, onTap: () => _select(place));
                },
              ),
            ),
          ],
        );
    }
  }
}

class _SectionLabel extends StatelessWidget {
  const _SectionLabel({required this.text});

  final String text;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.fromLTRB(16, 0, 16, 8),
      child: Text(
        text,
        style: context.textRoles.listHeader.copyWith(
          color: Theme.of(context).colorScheme.onSurfaceVariant,
        ),
      ),
    );
  }
}

class _SearchField extends StatelessWidget {
  const _SearchField({
    required this.controller,
    required this.focusNode,
    required this.loading,
    required this.onSubmitted,
    this.errorText,
    this.onClear,
  });

  final TextEditingController controller;
  final FocusNode focusNode;
  final bool loading;
  final String? errorText;
  final ValueChanged<String> onSubmitted;
  final VoidCallback? onClear;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final colors = theme.colorScheme;
    final hasError = errorText != null && errorText!.isNotEmpty;

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        TextField(
          controller: controller,
          focusNode: focusNode,
          autofocus: true,
          textInputAction: TextInputAction.search,
          onSubmitted: onSubmitted,
          maxLength: 200,
          buildCounter:
              (
                context, {
                required currentLength,
                required isFocused,
                maxLength,
              }) => null,
          style: theme.textTheme.bodyLarge,
          cursorColor: theme.textSelectionTheme.cursorColor,
          decoration: InputDecoration(
            hintText: 'Szukaj miejsca',
            prefixIcon: Icon(
              Icons.search,
              color: context.palette.primaryPressed,
              size: 22,
            ),
            suffixIcon: loading
                ? const Padding(
                    padding: EdgeInsets.all(12),
                    child: SizedBox(
                      width: 16,
                      height: 16,
                      child: CircularProgressIndicator(strokeWidth: 2.6),
                    ),
                  )
                : onClear == null
                ? null
                : IconButton(
                    key: const Key('place-search-clear'),
                    icon: Icon(
                      Icons.close,
                      size: 18,
                      color: colors.onSurfaceVariant,
                    ),
                    onPressed: onClear,
                  ),
          ),
        ),
        if (hasError) ...[
          const SizedBox(height: 8),
          Text(
            errorText!,
            style: theme.textTheme.labelLarge?.copyWith(color: colors.error),
          ),
        ],
      ],
    );
  }
}

class _PlaceTile extends StatelessWidget {
  const _PlaceTile({required this.place, required this.onTap});

  final Place place;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final colors = theme.colorScheme;

    return InkWell(
      onTap: onTap,
      child: Padding(
        padding: const EdgeInsets.symmetric(vertical: 12),
        child: Row(
          children: [
            CircleAvatar(
              radius: 20,
              backgroundColor: colors.primaryContainer,
              child: Icon(
                Icons.place_outlined,
                size: 20,
                color: context.palette.primaryPressed,
              ),
            ),
            const SizedBox(width: 16),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    place.placeName,
                    maxLines: 1,
                    overflow: TextOverflow.ellipsis,
                    style: theme.textTheme.bodyLarge?.copyWith(
                      fontWeight: FontWeight.w700,
                    ),
                  ),
                  const SizedBox(height: 2),
                  Text(
                    place.countryName,
                    maxLines: 1,
                    overflow: TextOverflow.ellipsis,
                    style: theme.textTheme.labelLarge?.copyWith(
                      fontWeight: FontWeight.w400,
                      color: colors.onSurfaceVariant,
                    ),
                  ),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class _LoadingSkeleton extends StatelessWidget {
  const _LoadingSkeleton();

  static const _widths = [
    (150.0, 90.0),
    (190.0, 110.0),
    (170.0, 80.0),
    (140.0, 100.0),
  ];

  @override
  Widget build(BuildContext context) {
    final color = Theme.of(context).colorScheme.surfaceContainerHigh;
    return ListView.builder(
      padding: const EdgeInsets.fromLTRB(16, 4, 16, 8),
      itemCount: _widths.length,
      itemBuilder: (context, index) {
        final width = _widths[index];
        return Padding(
          padding: const EdgeInsets.symmetric(vertical: 14),
          child: Row(
            children: [
              CircleAvatar(radius: 20, backgroundColor: color),
              const SizedBox(width: 16),
              Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  _Bar(width: width.$1, height: 14, color: color),
                  const SizedBox(height: 8),
                  _Bar(width: width.$2, height: 10, color: color),
                ],
              ),
            ],
          ),
        );
      },
    );
  }
}

class _Bar extends StatelessWidget {
  const _Bar({required this.width, required this.height, required this.color});

  final double width;
  final double height;
  final Color color;

  @override
  Widget build(BuildContext context) {
    return Container(
      width: width,
      height: height,
      decoration: BoxDecoration(
        color: color,
        borderRadius: BorderRadius.circular(height / 2),
      ),
    );
  }
}
