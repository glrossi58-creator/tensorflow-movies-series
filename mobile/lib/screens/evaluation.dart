import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../models.dart';
import '../state.dart';
import '../widgets.dart';

class ContentScreen extends ConsumerWidget {
  final int id;
  const ContentScreen({super.key, required this.id});
  @override Widget build(BuildContext context, WidgetRef ref) {
    final user = ref.watch(profileProvider)!;
    final key = (userId: user.id, id: id);
    return AsyncBody(value: ref.watch(contentProvider(key)), retry: () => ref.invalidate(contentProvider(key)), data: (view) {
      final entries = <Widget>[Center(child: Poster(path: view.content.posterPath, title: view.content.title, width: 180)), const SizedBox(height: 25), PageTitle(eyebrow: '${labels[view.content.type]} · ${view.content.year}', title: view.content.title), if (view.content.overview != null) Padding(padding: const EdgeInsets.only(bottom: 20), child: Text(view.content.overview!, style: const TextStyle(color: secondaryText))), Panel(child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [Text('Sua nota, ${user.name}', style: Theme.of(context).textTheme.titleLarge), const SizedBox(height: 10), AutosaveRating(userId: user.id, type: view.content.type, targetId: id, value: view.contentRating?.value, label: 'Avaliar ${view.content.title}'), const SizedBox(height: 7), const Text('1 Não gosto · 3 Neutro · 5 Gosto muito', style: TextStyle(fontSize: 10, color: secondaryText))]))];
      for (final section in {'Gêneros': view.genres, 'O elenco': view.actors, 'Por trás da câmera': view.directors, 'Quem criou essa história': view.creators}.entries) {
        if (section.value.isNotEmpty) { entries.add(Padding(padding: const EdgeInsets.only(top: 30, bottom: 16), child: Text(section.key, style: Theme.of(context).textTheme.titleLarge))); }
        entries.addAll(section.value.map((e) => EntityRatingTile(entity: e, userId: user.id)));
      }
      return ListView.builder(padding: const EdgeInsets.all(22), itemCount: entries.length, itemBuilder: (_, i) => entries[i]);
    });
  }
}
class PersonScreen extends ConsumerWidget {
  final int id;
  const PersonScreen({super.key, required this.id});
  @override Widget build(BuildContext context, WidgetRef ref) {
    final user = ref.watch(profileProvider)!;
    final key = (userId: user.id, id: id);
    return AsyncBody(value: ref.watch(personProvider(key)), retry: () => ref.invalidate(personProvider(key)), data: (view) => ListView(padding: const EdgeInsets.all(22), children: [Center(child: Poster(title: view.person.title, path: view.person.posterPath, width: 160)), const SizedBox(height: 25), PageTitle(eyebrow: 'Quem dá vida às histórias', title: view.person.title, subtitle: 'Cada papel tem sua própria nota.'), if (view.person.overview?.isNotEmpty ?? false) Text(view.person.overview!, style: const TextStyle(color: secondaryText)), const SizedBox(height: 22), if (view.roles.isEmpty) const EmptyState('Sem papéis confirmados', 'As relações disponíveis ainda não confirmam atuação, direção ou criação de séries.'), ...view.roles.map((e) => EntityRatingTile(entity: e, userId: user.id))]));
  }
}
class GenresScreen extends ConsumerWidget {
  const GenresScreen({super.key});
  @override Widget build(BuildContext context, WidgetRef ref) {
    final user = ref.watch(profileProvider)!;
    return AsyncBody(value: ref.watch(genresProvider(user.id)), retry: () => ref.invalidate(genresProvider(user.id)), data: (genres) => ListView.builder(padding: const EdgeInsets.all(22), itemCount: genres.length + 2, itemBuilder: (_, i) {
      if (i == 0) { return const PageTitle(eyebrow: 'Seu repertório', title: 'Seus gostos', subtitle: 'Uma nota para cada gênero. Deixe as histórias que você ama mais perto.'); }
      if (i == genres.length + 1) { return genres.isEmpty ? const EmptyState('Vamos descobrir seus gêneros', 'Importe um filme ou série para adicionar seus gêneros ao catálogo.') : const Padding(padding: EdgeInsets.only(top: 15), child: Text('Gêneros ajudam as preferências. A base de 8 / 25 conta apenas filmes e séries.', style: TextStyle(color: secondaryText, fontSize: 12))); }
      return EntityRatingTile(entity: genres[i-1], userId: user.id);
    }));
  }
}
