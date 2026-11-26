import 'package:flutter/material.dart';

class AppTextField extends FormField<String> {
  AppTextField({
    super.key,
    required this.label,
    this.controller,
    this.hintText,
    this.errorText,
    this.obscureText = false,
    this.keyboardType,
    this.textInputAction,
    this.onChanged,
    this.onTap,
    this.prefixIcon,
    this.suffixIcon,
    this.helperText,
    this.maxLines = 1,
    this.maxLength,
    this.counterInside = false,
    this.readOnly = false,
    this.fillColor,
    this.textColor,
    super.validator,
    super.onSaved,
    super.autovalidateMode,
    String? initialValue,
    super.enabled,
  }) : super(
         initialValue: controller?.text ?? initialValue ?? '',
         builder: (field) => (field as _AppTextFieldState).buildField(),
       );

  final String label;
  final TextEditingController? controller;
  final String? hintText;

  /// Message from outside the field, such as a backend error.
  /// Shown like a validation error, but it does not take part in [Form.validate].
  final String? errorText;
  final bool obscureText;
  final TextInputType? keyboardType;
  final TextInputAction? textInputAction;
  final ValueChanged<String>? onChanged;
  final VoidCallback? onTap;
  final Widget? prefixIcon;
  final Widget? suffixIcon;
  final String? helperText;
  final int maxLines;
  final int? maxLength;

  /// Draws `length / maxLength` inside the field instead of under it.
  final bool counterInside;
  final bool readOnly;
  final Color? fillColor;
  final Color? textColor;

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

  /// Backend message wins while it is set. Validation errors show once it is cleared.
  String? get _displayedError {
    final external = widget.errorText;
    if (external != null && external.isNotEmpty) {
      return external;
    }
    final validation = errorText;
    if (validation != null && validation.isNotEmpty) {
      return validation;
    }
    return null;
  }

  Widget buildField() {
    final theme = Theme.of(context);
    final decoration = theme.inputDecorationTheme;
    final colors = theme.colorScheme;
    final error = _displayedError;
    final hasError = error != null;
    final border = hasError ? decoration.errorBorder : decoration.enabledBorder;
    final focusedBorder = hasError
        ? decoration.focusedErrorBorder
        : decoration.focusedBorder;

    final field = TextField(
      controller: _controller,
      enabled: widget.enabled,
      readOnly: widget.readOnly,
      canRequestFocus:
          widget.enabled && (!widget.readOnly || widget.onTap != null),
      onTap: widget.onTap,
      obscureText: widget.obscureText,
      keyboardType: widget.keyboardType,
      textInputAction: widget.textInputAction,
      textAlignVertical: widget.maxLines > 1 ? TextAlignVertical.top : null,
      maxLines: widget.maxLines,
      minLines: widget.maxLines > 1 ? widget.maxLines : null,
      maxLength: widget.maxLength,
      buildCounter: widget.maxLength == null
          ? null
          : (
              context, {
              required currentLength,
              required isFocused,
              maxLength,
            }) => null,
      cursorColor: theme.textSelectionTheme.cursorColor,
      style: theme.textTheme.bodyLarge?.copyWith(
        color: widget.textColor ?? colors.onSurface,
      ),
      onChanged: (text) {
        didChange(text);
        widget.onChanged?.call(text);
      },
      decoration: InputDecoration(
        hintText: widget.hintText,
        filled: decoration.filled,
        fillColor: widget.fillColor ?? decoration.fillColor,
        hintStyle: decoration.hintStyle,
        isDense: decoration.isDense,
        contentPadding: widget.counterInside
            ? const EdgeInsets.fromLTRB(16, 12, 16, 28)
            : decoration.contentPadding,
        prefixIcon: widget.prefixIcon,
        suffixIcon: widget.suffixIcon,
        enabledBorder: border,
        focusedBorder: focusedBorder,
        disabledBorder: decoration.disabledBorder,
      ),
    );

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
        if (!widget.counterInside || widget.maxLength == null)
          field
        else
          Stack(
            children: [
              field,
              Positioned(
                right: 12,
                bottom: 8,
                child: Text(
                  '${_controller.text.characters.length} / ${widget.maxLength}',
                  style: theme.textTheme.labelSmall?.copyWith(
                    color: colors.onSurfaceVariant,
                  ),
                ),
              ),
            ],
          ),
        if (!hasError &&
            widget.helperText != null &&
            widget.helperText!.isNotEmpty) ...[
          const SizedBox(height: 8),
          Text(
            widget.helperText!,
            style: theme.textTheme.labelSmall?.copyWith(
              color: colors.onSurfaceVariant,
            ),
          ),
        ],
        if (error != null) ...[
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
