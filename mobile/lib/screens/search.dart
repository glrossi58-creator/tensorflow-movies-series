import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../models.dart';
import '../state.dart';
import '../widgets.dart';

class SearchScreen extends ConsumerStatefulWidget {
  const SearchScreen({super.key});
  @override
  ConsumerState<SearchScreen> createState() => _SearchScreenState();
}

class _SearchScreenState extends ConsumerState<SearchScreen> {
  Timer? timer;
  String query = '', type = '';
  bool importing = false;
  Object? error;
  @override
  void dispose() {
    timer?.cancel();
    super.dispose();
  }

  void changed(String value) {
    timer?.cancel();
    timer = Timer(const Duration(milliseconds: 350), () {
      if (mounted) {
        setState(() => query = value.trim());
      }
    });
  }

  Future<void> open(DiscoveryItem item) async {
    if (importing) {
      return;
    }
    int? id = item.id;
    setState(() {
      importing = true;
      error = null;
    });
    try {
      if (item.source == 'TMDB') {
        id = integer(
          (await ref
                  .read(dioProvider)
                  .post('/discovery/import/${item.type}/${item.tmdbId}'))
              .data['id'],
        );
      }
      if (!mounted) {
        return;
      }
      context.push(
        item.type == 'PERSON'
            ? '/people/$id'
            : item.type == 'GENRE'
            ? '/genres'
            : '/content/$id',
      );
    } catch (e) {
      if (mounted) {
        setState(() => error = e);
      }
    } finally {
      if (mounted) {
        setState(() => importing = false);
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    final key = (query: query, type: type);
    final results = query.length >= 2 ? ref.watch(searchProvider(key)) : null;
    return Column(
      children: [
        Padding(
          padding: const EdgeInsets.fromLTRB(20, 15, 20, 0),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(
                'Siga sua curiosidade.',
                style: Theme.of(context).textTheme.headlineMedium,
              ),
              const SizedBox(height: 18),
              TextField(
                onChanged: changed,
                maxLength: 150,
                decoration: const InputDecoration(
                  prefixIcon: Icon(Icons.search),
                  hintText: 'Filmes, séries, atores, diretores, creators…',
                  counterText: '',
                ),
                textInputAction: TextInputAction.search,
              ),
              const SizedBox(height: 12),
              SingleChildScrollView(
                scrollDirection: Axis.horizontal,
                child: Row(
                  children: [
                    for (final filter in {
                      '': 'Tudo',
                      'MOVIE': 'Filmes',
                      'SERIES': 'Séries',
                      'PERSON': 'Pessoas',
                      'GENRE': 'Gêneros',
                    }.entries)
                      Padding(
                        padding: const EdgeInsets.only(right: 8),
                        child: ChoiceChip(
                          label: Text(filter.value),
                          selected: type == filter.key,
                          onSelected: (_) => setState(() => type = filter.key),
                        ),
                      ),
                  ],
                ),
              ),
              if (importing)
                const Padding(
                  padding: EdgeInsets.only(top: 10),
                  child: Text(
                    'Importando detalhes e créditos…',
                    style: TextStyle(color: lightPurple),
                  ),
                ),
              if (error != null) ErrorNotice(error!),
            ],
          ),
        ),
        Expanded(
          child: results == null
              ? ListView(
                  children: const [
                    EmptyState(
                      'Sua próxima descoberta começa aqui',
                      'Digite pelo menos 2 caracteres. O catálogo local tem prioridade.',
                    ),
                  ],
                )
              : AsyncBody(
                  value: results,
                  retry: () => ref.invalidate(searchProvider(key)),
                  data: (response) {
                    final groups = <Widget>[];
                    if (response.warning != null) {
                      groups.add(
                        ErrorNotice(
                          response.warning!,
                          retry: () => ref.invalidate(searchProvider(key)),
                        ),
                      );
                    }
                    for (final group in {
                      'MOVIE': 'Filmes',
                      'SERIES': 'Séries',
                      'PERSON': 'Pessoas',
                      'GENRE': 'Gêneros',
                    }.entries) {
                      final items = response.results.where(
                        (i) => i.type == group.key,
                      );
                      if (items.isNotEmpty) {
                        groups.add(
                          Padding(
                            padding: const EdgeInsets.symmetric(vertical: 16),
                            child: Text(
                              group.value,
                              style: Theme.of(context).textTheme.titleLarge,
                            ),
                          ),
                        );
                      }
                      for (final item in items) {
                        groups.add(
                          Padding(
                            padding: const EdgeInsets.only(bottom: 12),
                            child: InkWell(
                              onTap: importing ? null : () => open(item),
                              borderRadius: BorderRadius.circular(14),
                              child: Panel(
                                padding: const EdgeInsets.all(12),
                                child: Row(
                                  children: [
                                    Poster(
                                      title: item.title,
                                      path: item.posterPath,
                                      width: 65,
                                    ),
                                    const SizedBox(width: 16),
                                    Expanded(
                                      child: Column(
                                        crossAxisAlignment:
                                            CrossAxisAlignment.start,
                                        children: [
                                          Text(
                                            item.source == 'LOCAL'
                                                ? 'No catálogo'
                                                : 'TMDB',
                                            style: const TextStyle(
                                              color: lightPurple,
                                              fontSize: 11,
                                            ),
                                          ),
                                          const SizedBox(height: 5),
                                          Text(
                                            item.title,
                                            style: const TextStyle(
                                              fontWeight: FontWeight.w600,
                                            ),
                                          ),
                                          if (item.year.isNotEmpty)
                                            Text(
                                              item.year,
                                              style: Theme.of(context)
                                                  .textTheme
                                                  .bodySmall,
                                            ),
                                          if (item.roles.isNotEmpty)
                                            Text(
                                              item.roles
                                                  .map((r) => labels[r])
                                                  .join(' · '),
                                              style: Theme.of(context)
                                                  .textTheme
                                                  .bodySmall,
                                            ),
                                          const SizedBox(height: 8),
                                          Text(
                                            item.source == 'LOCAL'
                                                ? 'Abrir e avaliar →'
                                                : 'Importar e avaliar →',
                                            style: const TextStyle(
                                              fontSize: 12,
                                              color: lightPurple,
                                            ),
                                          ),
                                        ],
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
                    if (response.results.isEmpty) {
                      groups.add(
                        const EmptyState(
                          'Nenhum resultado por aqui',
                          'Tente outro nome ou ajuste o filtro.',
                        ),
                      );
                    }
                    return ListView.builder(
                      padding: const EdgeInsets.all(20),
                      itemCount: groups.length,
                      itemBuilder: (_, i) => groups[i],
                    );
                  },
                ),
        ),
      ],
    );
  }
}
