import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:travel_app/screens/profile_screen.dart';
import 'package:travel_app/services/profile_edit_guard.dart';
import 'package:travel_app/theme/app_palette.dart';

/// Bottom navigation from the mockups: Podróże, Mapa, Statystyki, Profil.
/// Tab bodies are placeholders for later milestones.
class MainShell extends ConsumerWidget {
  const MainShell({super.key, required this.shell});

  final StatefulNavigationShell shell;

  Future<void> _openBranch(
    BuildContext context,
    WidgetRef ref,
    int index,
  ) async {
    if (shell.currentIndex == profileBranchIndex &&
        index != shell.currentIndex) {
      final guard = ref.read(profileEditGuardProvider);
      if (guard != null && guard.dirty) {
        final leave = await confirmDiscardProfileChanges(context);
        if (!leave || !context.mounted) {
          return;
        }
        guard.discard();
      }
    }
    shell.goBranch(index, initialLocation: index == shell.currentIndex);
  }

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    return Scaffold(
      body: shell,
      bottomNavigationBar: _NavBar(
        currentIndex: shell.currentIndex,
        onSelected: (index) {
          unawaited(_openBranch(context, ref, index));
        },
      ),
    );
  }
}

class SectionPlaceholder extends StatelessWidget {
  const SectionPlaceholder({super.key, required this.title});

  final String title;

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: SafeArea(
        bottom: false,
        child: Align(
          alignment: Alignment.topLeft,
          child: Padding(
            padding: const EdgeInsets.fromLTRB(24, 16, 24, 0),
            child: Text(title, style: Theme.of(context).textTheme.headlineMedium),
          ),
        ),
      ),
    );
  }
}

class _Tab {
  const _Tab(this.label, this.icon);

  final String label;
  final IconData icon;
}

const _tabs = [
  _Tab('Podróże', Icons.luggage_outlined),
  _Tab('Mapa', Icons.map_outlined),
  _Tab('Statystyki', Icons.bar_chart),
  _Tab('Profil', Icons.person_outline),
];

class _NavBar extends StatelessWidget {
  const _NavBar({required this.currentIndex, required this.onSelected});

  final int currentIndex;
  final ValueChanged<int> onSelected;

  @override
  Widget build(BuildContext context) {
    final colors = Theme.of(context).colorScheme;

    return Material(
      color: colors.surface,
      child: DecoratedBox(
        decoration: BoxDecoration(
          border: Border(top: BorderSide(color: colors.outline)),
        ),
        child: SafeArea(
          top: false,
          child: SizedBox(
            height: 72,
            child: Row(
              children: [
                for (var index = 0; index < _tabs.length; index++)
                  Expanded(
                    child: _NavButton(
                      tab: _tabs[index],
                      selected: index == currentIndex,
                      onTap: () => onSelected(index),
                    ),
                  ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}

class _NavButton extends StatelessWidget {
  const _NavButton({
    required this.tab,
    required this.selected,
    required this.onTap,
  });

  final _Tab tab;
  final bool selected;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    final colors = Theme.of(context).colorScheme;
    final color = selected ? context.palette.primaryPressed : colors.onSurfaceVariant;

    return Semantics(
      button: true,
      selected: selected,
      child: InkWell(
        onTap: onTap,
        splashColor: Colors.transparent,
        highlightColor: Colors.transparent,
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Container(
              width: 56,
              height: 30,
              alignment: Alignment.center,
              decoration: BoxDecoration(
                color: selected ? colors.primaryContainer : Colors.transparent,
                borderRadius: BorderRadius.circular(15),
              ),
              child: Icon(tab.icon, size: 24, color: color),
            ),
            const SizedBox(height: 4),
            Text(
              tab.label,
              style: Theme.of(context).textTheme.labelMedium?.copyWith(
                color: color,
                fontWeight: selected ? FontWeight.w700 : FontWeight.w500,
              ),
            ),
          ],
        ),
      ),
    );
  }
}
