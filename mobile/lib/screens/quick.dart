import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../models.dart';
import '../state.dart';
import '../widgets.dart';

class QuickScreen extends ConsumerStatefulWidget {
  const QuickScreen({super.key});
  @override ConsumerState<QuickScreen> createState() => _QuickScreenState();
}
class _QuickScreenState extends ConsumerState<QuickScreen> {
  final skipped = <String>{};
  int? choice;
  bool busy = false, saved = false;
  Object? error;
  @override void initState() { super.initState(); final user = ref.read(profileProvider)!; skipped.addAll(ref.read(preferencesProvider).getStringList('quick-skip:${user.id}') ?? []); }
  void skip(DiscoveryItem item) {
    setState(() { skipped.add(item.key); choice = null; error = null; });
    ref.read(preferencesProvider).setStringList('quick-skip:${ref.read(profileProvider)!.id}', skipped.toList());
  }
  Future<void> rate(DiscoveryItem item, int value) async {
    final userId = ref.read(profileProvider)!.id;
    setState(() { choice = value; busy = true; error = null; saved = false; });
    try {
      final dio = ref.read(dioProvider);
      final id = item.source == 'LOCAL' ? item.id! : integer((await dio.post('/discovery/import/${item.type}/${item.tmdbId}')).data['id']);
      await dio.put('/users/$userId/ratings', data: {'targetType': item.type, 'targetId': id, 'value': value});
      if (!mounted) { return; }
      skip(item); setState(() => saved = true);
      ref.invalidate(modelProvider(userId)); ref.invalidate(historyProvider(userId)); ref.invalidate(recommendationsProvider(userId));
    } catch (e) { if (mounted) { setState(() => error = e); } }
    finally { if (mounted) { setState(() => busy = false); } }
  }
  @override Widget build(BuildContext context) {
    final userId = ref.watch(profileProvider)!.id;
    final model = ref.watch(modelProvider(userId));
    return AsyncBody(value: ref.watch(quickProvider(userId)), retry: () => ref.invalidate(quickProvider(userId)), data: (response) {
      final available = response.results.where((item) => !skipped.contains(item.key));
      final item = available.isEmpty ? null : available.first;
      return ListView(padding: const EdgeInsets.all(22), children: [const PageTitle(eyebrow: 'Poucos toques, novas descobertas', title: 'Avaliação rápida', subtitle: 'Já assistiu? Deixe sua nota e descubra a próxima história.'), if (model.value != null) Panel(child: ModelProgress(model.value!)), const SizedBox(height: 22), if (item == null) const EmptyState('Você chegou ao fim desta seleção', 'Explore outros títulos na busca ou volte depois para mais descobertas.') else Panel(child: Column(children: [Poster(title: item.title, path: item.posterPath, width: 190), const SizedBox(height: 20), Text('${labels[item.type]} · ${item.year}', style: const TextStyle(color: lightPurple, fontSize: 11)), const SizedBox(height: 10), Text(item.title, textAlign: TextAlign.center, style: Theme.of(context).textTheme.headlineMedium), const SizedBox(height: 14), StarRating(value: choice, label: 'Avaliar ${item.title}', onChanged: busy ? null : (value) => rate(item, value)), Semantics(liveRegion: true, child: Text(busy ? 'Salvando…' : saved ? '✓ Salvo. Vamos para a próxima.' : '1 Não gosto · 3 Neutro · 5 Gosto muito', style: const TextStyle(color: secondaryText, fontSize: 11))), const SizedBox(height: 16), OutlinedButton.icon(onPressed: busy ? null : () => skip(item), icon: const Icon(Icons.skip_next, size: 18), label: const Text('Não assisti')), if (error != null) ErrorNotice('Não foi possível salvar. ${errorMessage(error!)}', retry: () => rate(item, choice!))])), if (response.warning != null) ErrorNotice(response.warning!, retry: () => ref.invalidate(quickProvider(userId))), const SizedBox(height: 20), const Text('Vale gostar muito, pouco ou não gostar. Sua opinião é o que importa.', style: TextStyle(color: secondaryText, fontSize: 12))]);
    });
  }
}
