import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../state.dart';
import '../widgets.dart';

class HomeScreen extends ConsumerWidget {
  const HomeScreen({super.key});
  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final user = ref.watch(profileProvider)!;
    final model = ref.watch(modelProvider(user.id));
    final picks = ref.watch(recommendationsProvider(user.id));
    return RefreshIndicator(
      onRefresh: () async {
        ref.invalidate(modelProvider(user.id));
        ref.invalidate(recommendationsProvider(user.id));
      },
      color: lightPurple,
      child: ListView(
        padding: const EdgeInsets.all(22),
        children: [
          Panel(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                PageTitle(
                  eyebrow: 'Um universo de histórias',
                  title:
                      'Olá, ${user.name}.\nO próximo favorito começa com você.',
                  subtitle: 'Descubra o que combina com o seu momento.',
                ),
                FilledButton.icon(
                  onPressed: () => context.go('/search'),
                  icon: const Icon(Icons.search, size: 18),
                  label: const Text('Encontrar uma história'),
                ),
              ],
            ),
          ),
          const SizedBox(height: 22),
          _action(
            context,
            '/quick',
            Icons.bolt,
            'Avaliação rápida',
            'Já assistiu? Um toque e a próxima.',
          ),
          _action(
            context,
            '/genres',
            Icons.favorite_outline,
            'Seus gostos',
            'Os gêneros que te conquistam.',
          ),
          _action(
            context,
            '/together',
            Icons.people_outline,
            'Hoje é para nós',
            'Uma boa história para compartilhar.',
          ),
          const SizedBox(height: 18),
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Text(
                'Seu gosto em construção',
                style: Theme.of(context).textTheme.titleLarge,
              ),
              IconButton(
                tooltip: 'Meu modelo',
                onPressed: () => context.push('/model'),
                icon: const Icon(Icons.arrow_forward, size: 20),
              ),
            ],
          ),
          const SizedBox(height: 12),
          model.when(
            data: (status) => Panel(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  ModelProgress(status),
                  if (status.trainedAt != null) ...[
                    const SizedBox(height: 15),
                    Text(
                      '${status.ratingsUsedInLastTraining} avaliações no último treino · ${status.newRatingsSinceLastTraining} novas',
                      style: Theme.of(context).textTheme.bodySmall,
                    ),
                  ],
                  const SizedBox(height: 18),
                  OutlinedButton(
                    onPressed: () => context.go('/quick'),
                    child: const Text('Continuar avaliando'),
                  ),
                ],
              ),
            ),
            error: (e, _) => ErrorNotice(
              e,
              retry: () => ref.invalidate(modelProvider(user.id)),
            ),
            loading: () => Container(height: 180, color: alternate),
          ),
          const SizedBox(height: 28),
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Text(
                'Escolhas para você',
                style: Theme.of(context).textTheme.titleLarge,
              ),
              TextButton(
                onPressed: () => context.go('/for-you'),
                child: const Text('Ver todas'),
              ),
            ],
          ),
          const SizedBox(height: 12),
          ...picks.when(
            data: (items) => items.isEmpty
                ? [
                    const EmptyState(
                      'Novas histórias vêm aí',
                      'Busque e importe títulos para começar.',
                    ),
                  ]
                : items
                      .take(5)
                      .map(
                        (item) => Padding(
                          padding: const EdgeInsets.only(bottom: 14),
                          child: RecommendationCard(item),
                        ),
                      )
                      .toList(),
            error: (e, _) => [
              ErrorNotice(
                e,
                retry: () => ref.invalidate(recommendationsProvider(user.id)),
              ),
            ],
            loading: () => [
              Container(
                height: 220,
                decoration: BoxDecoration(
                  color: alternate,
                  borderRadius: BorderRadius.circular(14),
                ),
              ),
            ],
          ),
          const SizedBox(height: 20),
          const Text(
            'LUME · Seu próximo favorito\nDados e imagens: TMDB',
            textAlign: TextAlign.center,
            style: TextStyle(color: secondaryText, fontSize: 10),
          ),
        ],
      ),
    );
  }

  Widget _action(
    BuildContext context,
    String path,
    IconData icon,
    String title,
    String subtitle,
  ) => Padding(
    padding: const EdgeInsets.only(bottom: 12),
    child: InkWell(
      onTap: () => context.push(path),
      borderRadius: BorderRadius.circular(14),
      child: Panel(
        padding: const EdgeInsets.all(17),
        child: Row(
          children: [
            Icon(icon, color: lightPurple),
            const SizedBox(width: 17),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    title,
                    style: const TextStyle(fontWeight: FontWeight.w600),
                  ),
                  Text(
                    subtitle,
                    style: const TextStyle(fontSize: 11, color: secondaryText),
                  ),
                ],
              ),
            ),
            const Icon(Icons.arrow_forward, color: secondaryText, size: 18),
          ],
        ),
      ),
    ),
  );
}
