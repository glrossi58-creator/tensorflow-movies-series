import 'package:cached_network_image/cached_network_image.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'models.dart';
import 'state.dart';

const background = Color(0xFF09090B);
const surface = Color(0xFF111116);
const alternate = Color(0xFF18181F);
const purple = Color(0xFF7C3AED);
const lightPurple = Color(0xFFA78BFA);
const secondaryText = Color(0xFFA1A1AA);
const border = Color(0xFF27272A);
ThemeData lumeTheme() => ThemeData(useMaterial3: true, brightness: Brightness.dark, scaffoldBackgroundColor: background,
  colorScheme: const ColorScheme.dark(primary: lightPurple, onPrimary: Color(0xFF20103C), secondary: lightPurple, surface: surface),
  appBarTheme: const AppBarTheme(backgroundColor: background, surfaceTintColor: Colors.transparent, centerTitle: false),
  navigationBarTheme: NavigationBarThemeData(backgroundColor: surface, indicatorColor: purple.withValues(alpha: .3)),
  filledButtonTheme: FilledButtonThemeData(style: FilledButton.styleFrom(backgroundColor: purple, foregroundColor: Colors.white, minimumSize: const Size(48, 48), shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)))),
  outlinedButtonTheme: OutlinedButtonThemeData(style: OutlinedButton.styleFrom(foregroundColor: Colors.white, minimumSize: const Size(48, 48), side: const BorderSide(color: border), shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)))),
  inputDecorationTheme: InputDecorationTheme(filled: true, fillColor: alternate, border: OutlineInputBorder(borderRadius: BorderRadius.circular(12), borderSide: const BorderSide(color: border)), enabledBorder: OutlineInputBorder(borderRadius: BorderRadius.circular(12), borderSide: const BorderSide(color: border))),
  textTheme: const TextTheme(headlineLarge: TextStyle(fontSize: 34, height: 1.12, letterSpacing: -1.2, fontWeight: FontWeight.w700, color: Colors.white), headlineMedium: TextStyle(fontSize: 27, fontWeight: FontWeight.w700), titleLarge: TextStyle(fontSize: 21, fontWeight: FontWeight.w600), bodyMedium: TextStyle(fontSize: 14, height: 1.6), bodySmall: TextStyle(fontSize: 12, color: secondaryText, height: 1.5)));
class PageTitle extends StatelessWidget {
  final String eyebrow, title, subtitle;
  const PageTitle({super.key, required this.eyebrow, required this.title, this.subtitle = ''});
  @override Widget build(BuildContext context) => Padding(padding: const EdgeInsets.only(bottom: 24), child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [Text(eyebrow.toUpperCase(), style: const TextStyle(color: lightPurple, fontSize: 10, fontWeight: FontWeight.w700, letterSpacing: 2)), const SizedBox(height: 12), Text(title, style: Theme.of(context).textTheme.headlineLarge), if (subtitle.isNotEmpty) ...[const SizedBox(height: 12), Text(subtitle, style: const TextStyle(color: secondaryText))]]));
}
class Panel extends StatelessWidget {
  final Widget child;
  final EdgeInsets padding;
  const Panel({super.key, required this.child, this.padding = const EdgeInsets.all(20)});
  @override Widget build(BuildContext context) => Container(padding: padding, decoration: BoxDecoration(color: surface, border: Border.all(color: border), borderRadius: BorderRadius.circular(16)), child: child);
}
class LoadingTiles extends StatelessWidget {
  const LoadingTiles({super.key});
  @override Widget build(BuildContext context) => Semantics(label: 'Carregando', child: ListView.builder(padding: const EdgeInsets.all(20), itemCount: 4, itemBuilder: (_, index) => Container(height: index == 0 ? 180 : 95, margin: const EdgeInsets.only(bottom: 16), decoration: BoxDecoration(color: alternate, borderRadius: BorderRadius.circular(14)))));
}
class ErrorNotice extends StatelessWidget {
  final Object error;
  final VoidCallback? retry;
  const ErrorNotice(this.error, {super.key, this.retry});
  @override Widget build(BuildContext context) => Semantics(liveRegion: true, child: Padding(padding: const EdgeInsets.symmetric(vertical: 12), child: Panel(child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [Text(error is String ? error as String : errorMessage(error), style: const TextStyle(color: Color(0xFFE3CFF6))), if (retry != null) ...[const SizedBox(height: 10), OutlinedButton.icon(onPressed: retry, icon: const Icon(Icons.refresh, size: 18), label: const Text('Tentar novamente'))]]))));
}
class AsyncBody<T> extends StatelessWidget {
  final AsyncValue<T> value;
  final Widget Function(T data) data;
  final VoidCallback retry;
  const AsyncBody({super.key, required this.value, required this.data, required this.retry});
  @override Widget build(BuildContext context) => value.when(data: data, loading: () => const LoadingTiles(), error: (error, _) => ListView(padding: const EdgeInsets.all(24), children: [ErrorNotice(error, retry: retry)]));
}
class EmptyState extends StatelessWidget {
  final String title, message;
  const EmptyState(this.title, this.message, {super.key});
  @override Widget build(BuildContext context) => Padding(padding: const EdgeInsets.symmetric(vertical: 36, horizontal: 15), child: Column(children: [const Icon(Icons.movie_outlined, size: 38, color: lightPurple), const SizedBox(height: 18), Text(title, style: Theme.of(context).textTheme.titleLarge, textAlign: TextAlign.center), const SizedBox(height: 10), Text(message, style: const TextStyle(color: secondaryText), textAlign: TextAlign.center)]));
}
class Poster extends StatelessWidget {
  final String? path;
  final String title;
  final double? width;
  const Poster({super.key, this.path, required this.title, this.width});
  @override Widget build(BuildContext context) => SizedBox(width: width, child: AspectRatio(aspectRatio: 2/3, child: ClipRRect(borderRadius: BorderRadius.circular(12), child: path == null ? _fallback() : CachedNetworkImage(imageUrl: 'https://image.tmdb.org/t/p/w500$path', fit: BoxFit.cover, memCacheWidth: 500, placeholder: (_, _) => Container(color: alternate), errorWidget: (_, _, _) => _fallback(), imageBuilder: (_, provider) => Semantics(label: title, image: true, child: Image(image: provider, fit: BoxFit.cover))))));
  Widget _fallback() => Container(color: alternate, padding: const EdgeInsets.all(12), child: Center(child: Column(mainAxisSize: MainAxisSize.min, children: [const Icon(Icons.movie_outlined, size: 30, color: lightPurple), const SizedBox(height: 14), Text(title, textAlign: TextAlign.center, maxLines: 4, overflow: TextOverflow.ellipsis, style: const TextStyle(color: secondaryText, fontSize: 12))])));
}
class StarRating extends StatelessWidget {
  final int? value;
  final ValueChanged<int>? onChanged;
  final String label;
  const StarRating({super.key, required this.value, required this.onChanged, this.label = 'Sua nota'});
  @override Widget build(BuildContext context) => Semantics(label: label, child: Wrap(spacing: 0, children: List.generate(5, (i) {
    const meanings = ['Não gosto', 'Gosto pouco', 'Neutro', 'Gosto', 'Gosto muito'];
    return Semantics(label: '${i+1} ${i == 0 ? 'estrela' : 'estrelas'}: ${meanings[i]}', selected: value == i+1, button: true, child: ExcludeSemantics(child: IconButton(tooltip: meanings[i], onPressed: onChanged == null ? null : () => onChanged!(i+1), constraints: const BoxConstraints(minWidth: 48, minHeight: 48), icon: Icon(value != null && i < value! ? Icons.star_rounded : Icons.star_outline_rounded, size: 32, color: value != null && i < value! ? lightPurple : const Color(0xFF696174)))));
  })));
}
class AutosaveRating extends ConsumerWidget {
  final int userId, targetId;
  final String type, label;
  final int? value;
  const AutosaveRating({super.key, required this.userId, required this.type, required this.targetId, required this.label, this.value});
  @override Widget build(BuildContext context, WidgetRef ref) {
    final key = '$userId:$type:$targetId';
    final draft = ref.watch(ratingQueueProvider.select((state) => state[key]));
    return Column(crossAxisAlignment: CrossAxisAlignment.start, children: [StarRating(value: draft?.value ?? value, label: label, onChanged: (v) => ref.read(ratingQueueProvider.notifier).choose(userId, type, targetId, v)), Semantics(liveRegion: true, child: Text(draft?.status == 'saving' ? 'Salvando…' : draft?.status == 'saved' ? '✓ Salvo' : draft?.status == 'error' ? 'Não foi possível salvar' : value == null ? 'Ainda não avaliado' : 'Sua avaliação', style: const TextStyle(color: secondaryText, fontSize: 11))), if (draft?.status == 'error') TextButton(onPressed: () => ref.read(ratingQueueProvider.notifier).flush(key), child: const Text('Tentar novamente'))]);
  }
}
class EntityRatingTile extends StatelessWidget {
  final RatedEntity entity;
  final int userId;
  const EntityRatingTile({super.key, required this.entity, required this.userId});
  @override Widget build(BuildContext context) => Padding(padding: const EdgeInsets.only(bottom: 12), child: Panel(padding: const EdgeInsets.all(16), child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [InkWell(onTap: entity.targetType == 'GENRE' ? null : () => context.push('/people/${entity.id}'), child: Padding(padding: const EdgeInsets.only(bottom: 6), child: Row(children: [Expanded(child: Text(entity.name, style: const TextStyle(fontWeight: FontWeight.w600, fontSize: 16))), Text(labels[entity.targetType]!, style: const TextStyle(color: secondaryText, fontSize: 11))]))), AutosaveRating(userId: userId, type: entity.targetType, targetId: entity.id, value: entity.rating?.value, label: 'Avaliar ${entity.name} em ${labels[entity.targetType]}')])));
}
class ModelProgress extends StatelessWidget {
  final ModelStatus status;
  const ModelProgress(this.status, {super.key});
  @override Widget build(BuildContext context) => Column(crossAxisAlignment: CrossAxisAlignment.start, children: [Text(status.label, style: const TextStyle(color: lightPurple, fontSize: 12)), const SizedBox(height: 8), Text('${status.directRatingCount} / ${status.target}', style: Theme.of(context).textTheme.headlineMedium), const SizedBox(height: 14), Semantics(label: 'Progresso de avaliações diretas', child: LinearProgressIndicator(value: (status.directRatingCount/status.target).clamp(0, 1), minHeight: 6, borderRadius: BorderRadius.circular(5), color: lightPurple, backgroundColor: border)), const SizedBox(height: 14), Text(status.progressMessage), const SizedBox(height: 5), Text('Mínimo ${status.minimumDirectRatings} · Recomendado ${status.recommendedDirectRatings}', style: Theme.of(context).textTheme.bodySmall)]);
}
class RecommendationCard extends StatelessWidget {
  final Recommendation item;
  const RecommendationCard(this.item, {super.key});
  @override Widget build(BuildContext context) => InkWell(borderRadius: BorderRadius.circular(14), onTap: () => context.push('/content/${item.content.id}'), child: Panel(padding: const EdgeInsets.all(14), child: Row(crossAxisAlignment: CrossAxisAlignment.start, children: [Poster(path: item.content.posterPath, title: item.content.title, width: 92), const SizedBox(width: 16), Expanded(child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [Text('${(item.score*100).round()}% de afinidade', style: const TextStyle(color: lightPurple, fontWeight: FontWeight.w600)), const SizedBox(height: 7), Text(item.content.title, style: const TextStyle(fontSize: 17, fontWeight: FontWeight.w600)), Text('${labels[item.content.type]} · ${item.content.year}', style: Theme.of(context).textTheme.bodySmall), const SizedBox(height: 10), Text(item.strategy == 'TENSORFLOW' ? 'Seu modelo' : item.individualScores.isNotEmpty ? 'Sintonia de vocês' : 'Explorando seu gosto', style: const TextStyle(fontSize: 10, color: lightPurple)), for (final reason in item.reasons.take(2)) Padding(padding: const EdgeInsets.only(top: 5), child: Text(reason, style: const TextStyle(fontSize: 11, color: secondaryText)))]))])));
}
