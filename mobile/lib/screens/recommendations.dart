import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../models.dart';
import '../state.dart';
import '../widgets.dart';

class RecommendationsScreen extends ConsumerWidget {
  const RecommendationsScreen({super.key});
  @override Widget build(BuildContext context, WidgetRef ref) {
    final user = ref.watch(profileProvider)!;
    return AsyncBody(value: ref.watch(recommendationsProvider(user.id)), retry: () => ref.invalidate(recommendationsProvider(user.id)), data: (items) => ListView.builder(padding: const EdgeInsets.all(22), itemCount: items.length + 1, itemBuilder: (_, i) {
      if (i == 0) { return Column(children: [PageTitle(eyebrow: 'Seu gosto, novas histórias', title: 'Para você, ${user.name}.', subtitle: 'Somente títulos que você ainda não avaliou.'), if (items.isEmpty) const EmptyState('Precisamos de novas histórias', 'Busque e importe conteúdos. Com o TMDB configurado, também buscamos candidatos populares.')]); }
      return Padding(padding: const EdgeInsets.only(bottom: 15), child: RecommendationCard(items[i-1]));
    }));
  }
}
class JointScreen extends ConsumerStatefulWidget {
  const JointScreen({super.key});
  @override ConsumerState<JointScreen> createState() => _JointScreenState();
}
class _JointScreenState extends ConsumerState<JointScreen> {
  final selected = <int>{};
  bool busy = false;
  Object? error;
  List<Recommendation>? results;
  @override void initState() { super.initState(); selected.add(ref.read(profileProvider)!.id); }
  Future<void> find() async {
    setState(() { busy = true; error = null; results = null; });
    try {
      final response = await ref.read(dioProvider).post('/recommendations/joint', data: {'userIds': selected.toList(), 'limit': 20});
      if (mounted) { setState(() => results = objects(response.data).map(Recommendation.fromJson).toList()); }
    } catch (e) { if (mounted) { setState(() => error = e); } }
    finally { if (mounted) { setState(() => busy = false); } }
  }
  @override Widget build(BuildContext context) => AsyncBody(value: ref.watch(profilesProvider), retry: () => ref.invalidate(profilesProvider), data: (profiles) => ListView(padding: const EdgeInsets.all(22), children: [const PageTitle(eyebrow: 'O melhor dos dois mundos', title: 'Quem vai assistir?', subtitle: 'Uma boa escolha para todo mundo. Os modelos continuam individuais.'), Wrap(spacing: 12, runSpacing: 12, children: profiles.map((p) => FilterChip(label: Padding(padding: const EdgeInsets.all(9), child: Text(p.name)), selected: selected.contains(p.id), onSelected: busy ? null : (value) => setState(() { if (value) { selected.add(p.id); } else { selected.remove(p.id); } results = null; }))).toList()), const SizedBox(height: 24), FilledButton.icon(onPressed: busy || selected.length < 2 ? null : find, icon: const Icon(Icons.people_outline, size: 20), label: Text(busy ? 'Buscando a sintonia…' : 'Encontrar algo para nós')), if (selected.length < 2) const Padding(padding: EdgeInsets.only(top: 12), child: Text('Selecione pelo menos dois perfis.', style: TextStyle(color: secondaryText))), if (busy) ...List.generate(3, (_) => Container(height: 160, margin: const EdgeInsets.only(top: 16), decoration: BoxDecoration(color: alternate, borderRadius: BorderRadius.circular(12)))), if (error != null) ErrorNotice(error!, retry: find), if (results != null && results!.isEmpty) const EmptyState('Precisamos de mais histórias', 'Importem títulos que nenhum participante avaliou.'), for (final item in results ?? <Recommendation>[]) Padding(padding: const EdgeInsets.only(top: 20), child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [RecommendationCard(item), const SizedBox(height: 8), Text(item.individualScores.entries.map((e) => '${profiles.where((p) => p.id.toString() == e.key).firstOrNull?.name ?? 'Perfil'}: ${(e.value*100).round()}%').join(' · '), style: const TextStyle(color: secondaryText, fontSize: 11))]))]));
}
