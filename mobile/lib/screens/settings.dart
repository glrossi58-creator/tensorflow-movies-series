import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import '../models.dart';
import '../state.dart';
import '../widgets.dart';

class SettingsScreen extends ConsumerStatefulWidget {
  const SettingsScreen({super.key});
  @override ConsumerState<SettingsScreen> createState() => _SettingsScreenState();
}
class _SettingsScreenState extends ConsumerState<SettingsScreen> {
  bool busy = false;
  Object? error;
  Future<void> toggle(bool enabled) async {
    final id = ref.read(profileProvider)!.id;
    setState(() { busy = true; error = null; });
    try { await ref.read(dioProvider).put('/users/$id/model/settings', data: {'autoTrainEnabled': enabled}); ref.invalidate(modelProvider(id)); }
    catch (e) { if (mounted) { setState(() => error = e); } }
    finally { if (mounted) { setState(() => busy = false); } }
  }
  @override Widget build(BuildContext context) {
    final user = ref.watch(profileProvider)!;
    final model = ref.watch(modelProvider(user.id));
    return ListView(padding: const EdgeInsets.all(22), children: [PageTitle(eyebrow: 'Do seu jeito', title: user.name, subtitle: 'Suas preferências, seu modelo e suas histórias.'), for (final item in {'/genres': 'Seus gostos', '/ratings': 'Minhas avaliações', '/model': 'Meu modelo', '/together': 'Para nós'}.entries) Card(color: surface, child: ListTile(title: Text(item.value), trailing: const Icon(Icons.chevron_right), onTap: () => context.push(item.key))), const SizedBox(height: 24), Panel(child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [Text('Atualização do modelo', style: Theme.of(context).textTheme.titleLarge), const SizedBox(height: 12), const Text('Depois do primeiro treino, retreinar a cada 10 novos filmes ou séries avaliados.', style: TextStyle(color: secondaryText)), if (model.value != null) SwitchListTile(contentPadding: EdgeInsets.zero, title: const Text('Retreino automático', style: TextStyle(fontSize: 14)), value: model.value!.autoTrainEnabled, onChanged: busy ? null : toggle), if (error != null) ErrorNotice(error!)])), const SizedBox(height: 22), OutlinedButton(onPressed: () => context.push('/connection'), child: const Text('Configurar conexão')), const SizedBox(height: 10), OutlinedButton(onPressed: () => ref.read(profileProvider.notifier).select(null), child: const Text('Trocar perfil')), const SizedBox(height: 25), const Text('LUME · Versão 1.0.0\nEste produto usa a API do TMDB, mas não é endossado ou certificado pelo TMDB.\n\nPerfis para uso em uma rede doméstica de confiança, sem senha.', style: TextStyle(fontSize: 11, color: secondaryText))]);
  }
}
class RatingsScreen extends ConsumerWidget {
  const RatingsScreen({super.key});
  @override Widget build(BuildContext context, WidgetRef ref) {
    final user = ref.watch(profileProvider)!;
    return AsyncBody(value: ref.watch(historyProvider(user.id)), retry: () => ref.invalidate(historyProvider(user.id)), data: (items) => ListView.builder(padding: const EdgeInsets.all(22), itemCount: items.length + 1, itemBuilder: (_, i) {
      if (i == 0) { return Column(children: [const PageTitle(eyebrow: 'O que já passou por você', title: 'Minhas avaliações', subtitle: 'Você pode mudar de ideia a qualquer momento.'), if (items.isEmpty) const EmptyState('Seu repertório começa com uma nota', 'Use a avaliação rápida ou busque algo que você já assistiu.')]); }
      final item = items[i-1]; final rating = Rating.fromJson(item['rating']);
      return Padding(padding: const EdgeInsets.only(bottom: 12), child: Panel(child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [Text(item['title'], style: const TextStyle(fontWeight: FontWeight.w600)), Text(labels[rating.targetType]!, style: Theme.of(context).textTheme.bodySmall), AutosaveRating(userId: user.id, type: rating.targetType, targetId: rating.targetId, value: rating.value, label: 'Avaliar ${item['title']}')])));
    }));
  }
}
