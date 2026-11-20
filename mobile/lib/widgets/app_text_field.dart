import 'package:flutter/material.dart';

class AppTextField extends FormField<String> {
  AppTextField({
    super.key,
    required this.label,
    this.controller,
    this.hintText,
    String? errorText,
    this.obscureText = false,
    this.keyboardType,
    this.textInputAction,
    this.onChanged,
    super.validator,
    super.onSaved,
    super.autovalidateMode,
    String? initialValue,
    super.enabled,
  }) : super(
         initialValue: controller?.text ?? initialValue ?? '',
         forceErrorText: (errorText == null || errorText.isEmpty)
             ? null
             : errorText,
         builder: (field) => (field as _AppTextFieldState).buildField(),
       );

  final String label;
  final TextEditingController? controller;
  final String? hintText;
  final bool obscureText;
  final TextInputType? keyboardType;
  final TextInputAction? textInputAction;
  final ValueChanged<String>? onChanged;

  @override
  FormFieldState<String> createState() => _AppTextFieldState();
}

class _AppTextFieldState extends FormFieldState<String> {
  TextEditingController? _localController;

  @override
  AppTextField get widget => super.widget as AppTextField;

  TextEditingController get _controller =>
      widget.controller ?? _localController!;

  @override
  void initState() {
    super.initState();
    if (widget.controller == null) {
      _localController = TextEditingController(text: widget.initialValue);
    } else {
      widget.controller!.addListener(_syncFromController);
    }
  }

  @override
  void didUpdateWidget(AppTextField oldWidget) {
    super.didUpdateWidget(oldWidget);
    if (oldWidget.controller == widget.controller) {
      return;
    }

    oldWidget.controller?.removeListener(_syncFromController);

    if (widget.controller == null) {
      _localController ??= TextEditingController(
        text: value ?? widget.initialValue ?? '',
      );
      return;
    }

    _localController?.dispose();
    _localController = null;
    widget.controller!.addListener(_syncFromController);
    if (widget.controller!.text != (value ?? '')) {
      didChange(widget.controller!.text);
    }
  }

  @override
  void dispose() {
    widget.controller?.removeListener(_syncFromController);
    _localController?.dispose();
    super.dispose();
  }

  @override
  void reset() {
    super.reset();
    _controller.text = widget.initialValue ?? '';
  }

  void _syncFromController() {
    if (_controller.text != value) {
      didChange(_controller.text);
    }
  }

  Widget buildField() {
    final theme = Theme.of(context);
    final decoration = theme.inputDecorationTheme;
    final colors = theme.colorScheme;
    final error = errorText;
    final hasError = error != null && error.isNotEmpty;
    final border = hasError ? decoration.errorBorder : decoration.enabledBorder;
    final focusedBorder = hasError
        ? decoration.focusedErrorBorder
        : decoration.focusedBorder;

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(
          widget.label,
          style: theme.textTheme.labelLarge?.copyWith(
            color: colors.onSurfaceVariant,
          ),
        ),
        const SizedBox(height: 8),
        TextField(
          controller: _controller,
          enabled: widget.enabled,
          obscureText: widget.obscureText,
          keyboardType: widget.keyboardType,
          textInputAction: widget.textInputAction,
          cursorColor: theme.textSelectionTheme.cursorColor,
          style: theme.textTheme.bodyLarge?.copyWith(color: colors.onSurface),
          onChanged: (text) {
            didChange(text);
            widget.onChanged?.call(text);
          },
          decoration: InputDecoration(
            hintText: widget.hintText,
            filled: decoration.filled,
            fillColor: decoration.fillColor,
            hintStyle: decoration.hintStyle,
            isDense: decoration.isDense,
            contentPadding: decoration.contentPadding,
            enabledBorder: border,
            focusedBorder: focusedBorder,
            disabledBorder: decoration.disabledBorder,
          ),
        ),
        if (hasError) ...[
          const SizedBox(height: 8),
          Semantics(
            liveRegion: true,
            container: true,
            child: Row(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                const _ErrorMark(),
                const SizedBox(width: 8),
                Expanded(
                  child: Text(
                    error,
                    style: theme.textTheme.labelLarge?.copyWith(
                      color: colors.error,
                    ),
                  ),
                ),
              ],
            ),
          ),
        ],
      ],
    );
  }
}

class _ErrorMark extends StatelessWidget {
  const _ErrorMark();

  @override
  Widget build(BuildContext context) {
    final colors = Theme.of(context).colorScheme;
    return Container(
      width: 16,
      height: 16,
      alignment: Alignment.center,
      decoration: BoxDecoration(color: colors.error, shape: BoxShape.circle),
      child: Text(
        '!',
        style: Theme.of(context).textTheme.labelSmall?.copyWith(
          color: colors.onError,
          fontWeight: FontWeight.w700,
          height: 1,
        ),
      ),
    );
  }
}
