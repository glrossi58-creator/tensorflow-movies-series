import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import '../state.dart';
import '../widgets.dart';

class ModelScreen extends ConsumerStatefulWidget {
  const ModelScreen({super.key});
  @override ConsumerState<ModelScreen> createState() => _ModelScreenState();
}
class _ModelScreenState extends ConsumerState<ModelScreen> {
  bool busy = false, success = false;
  Object? error;
  Future<void> train() async {
    final userId = ref.read(profileProvider)!.id;
    setState(() { busy = true; error = null; success = false; });
    try {
      await ref.read(dioProvider).post('/recommendations/users/$userId/train', options: Options(receiveTimeout: const Duration(minutes: 3)));
      if (mounted) { setState(() => success = true); }
    } catch (e) { if (mounted) { setState(() => error = e); } }
    finally { if (mounted) { ref.invalidate(modelProvider(userId)); ref.invalidate(recommendationsProvider(userId)); setState(() => busy = false); } }
  }
  @override Widget build(BuildContext context) {
    final user = ref.watch(profileProvider)!;
    return AsyncBody(value: ref.watch(modelProvider(user.id)), retry: () => ref.invalidate(modelProvider(user.id)), data: (status) => ListView(padding: const EdgeInsets.all(22), children: [PageTitle(eyebrow: 'Aprende com você', title: 'Modelo de ${user.name}', subtitle: 'Suas escolhas constroem recomendações pessoais.'), Panel(child: ModelProgress(status)), const SizedBox(height: 20), FilledButton.icon(onPressed: busy || !status.canTrain ? null : train, icon: const Icon(Icons.auto_awesome, size: 18), label: Text(busy || status.status == 'TRAINING' ? 'Treinando modelo…' : 'Treinar agora')), const SizedBox(height: 10), OutlinedButton(onPressed: () => context.push('/quick'), child: const Text('Continuar avaliando')), if (success) const Padding(padding: EdgeInsets.symmetric(vertical: 15), child: Text('✓ Modelo atualizado.', style: TextStyle(color: lightPurple))), if (error != null) ErrorNotice(error!), if (status.lastError != null) ErrorNotice(status.lastError!), const SizedBox(height: 20), Panel(child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [Text('Último treinamento', style: Theme.of(context).textTheme.titleLarge), const SizedBox(height: 18), for (final entry in {'Ratings utilizados': '${status.ratingsUsedInLastTraining}', 'Novos desde o treino': '${status.newRatingsSinceLastTraining}', 'Último treino': status.trainedAt == null ? 'Ainda não treinado' : DateTime.parse(status.trainedAt!).toLocal().toString().split('.').first, 'Train loss': status.trainLoss?.toStringAsFixed(4) ?? '—', 'Validation loss': status.validationLoss?.toStringAsFixed(4) ?? '—', 'Versão': '${status.modelVersion}'}.entries) Padding(padding: const EdgeInsets.symmetric(vertical: 8), child: Row(crossAxisAlignment: CrossAxisAlignment.start, children: [Expanded(child: Text(entry.key, style: const TextStyle(color: secondaryText, fontSize: 12))), Expanded(child: Text(entry.value, textAlign: TextAlign.right, style: const TextStyle(fontSize: 12)))]))])), const SizedBox(height: 22), const Text('Depois do primeiro treino, o retreino automático considera 10 novos filmes ou séries. Preferências de pessoas e gêneros continuam independentes.', style: TextStyle(color: secondaryText, fontSize: 12))]));
  }
}
