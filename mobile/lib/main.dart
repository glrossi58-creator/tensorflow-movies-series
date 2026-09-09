import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:shared_preferences/shared_preferences.dart';

import 'state.dart';
import 'widgets.dart';
import 'screens.dart';

Future<void> main() async {
  WidgetsFlutterBinding.ensureInitialized();
  final preferences = await SharedPreferences.getInstance();
  runApp(
    ProviderScope(
      overrides: [preferencesProvider.overrideWithValue(preferences)],
      child: const LumeApp(),
    ),
  );
}

final routerProvider = Provider<GoRouter>((ref) {
  final router = GoRouter(
    initialLocation: '/',
    redirect: (_, state) {
      final selected = ref.read(profileProvider);
      if (state.uri.path == '/connection') {
        return null;
      }
      if (selected == null && state.uri.path != '/profiles') {
        return '/profiles';
      }
      if (selected != null && state.uri.path == '/profiles') {
        return '/';
      }
      return null;
    },
    routes: [
      GoRoute(path: '/profiles', builder: (_, _) => const ProfileScreen()),
      GoRoute(
        path: '/connection',
        builder: (_, _) =>
            const Scaffold(body: SafeArea(child: ConnectionScreen())),
      ),
      ShellRoute(
        builder: (context, state, child) =>
            AppShell(path: state.uri.path, child: child),
        routes: [
          GoRoute(path: '/', builder: (_, _) => const HomeScreen()),
          GoRoute(path: '/search', builder: (_, _) => const SearchScreen()),
          GoRoute(path: '/quick', builder: (_, _) => const QuickScreen()),
          GoRoute(
            path: '/for-you',
            builder: (_, _) => const RecommendationsScreen(),
          ),
          GoRoute(path: '/profile', builder: (_, _) => const SettingsScreen()),
          GoRoute(path: '/genres', builder: (_, _) => const GenresScreen()),
          GoRoute(path: '/model', builder: (_, _) => const ModelScreen()),
          GoRoute(path: '/together', builder: (_, _) => const JointScreen()),
          GoRoute(path: '/ratings', builder: (_, _) => const RatingsScreen()),
          GoRoute(
            path: '/content/:id',
            builder: (_, state) => ContentScreen(
              id: int.tryParse(state.pathParameters['id']!) ?? -1,
            ),
          ),
          GoRoute(
            path: '/people/:id',
            builder: (_, state) => PersonScreen(
              id: int.tryParse(state.pathParameters['id']!) ?? -1,
            ),
          ),
        ],
      ),
    ],
  );
  ref.listen(profileProvider, (_, _) => router.refresh());
  ref.onDispose(router.dispose);
  return router;
});

class LumeApp extends ConsumerWidget {
  const LumeApp({super.key});
  @override
  Widget build(BuildContext context, WidgetRef ref) => MaterialApp.router(
    title: 'Lume',
    debugShowCheckedModeBanner: false,
    theme: lumeTheme(),
    routerConfig: ref.watch(routerProvider),
  );
}

class AppShell extends ConsumerWidget {
  final String path;
  final Widget child;
  const AppShell({super.key, required this.path, required this.child});
  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final profile = ref.watch(profileProvider);
    const routes = ['/', '/search', '/quick', '/for-you', '/profile'];
    final index = routes.indexOf(path);
    return Scaffold(
      appBar: AppBar(
        title: const Row(
          mainAxisSize: MainAxisSize.min,
          children: [
            Icon(Icons.auto_awesome, color: lightPurple),
            SizedBox(width: 9),
            Text(
              'LUME',
              style: TextStyle(
                letterSpacing: 3,
                fontWeight: FontWeight.w700,
                fontSize: 20,
              ),
            ),
          ],
        ),
        leading: index < 0
            ? IconButton(
                tooltip: 'Voltar',
                icon: const Icon(Icons.arrow_back),
                onPressed: () =>
                    context.canPop() ? context.pop() : context.go('/'),
              )
            : null,
        actions: [
          PopupMenuButton<String>(
            tooltip: 'Perfil ativo: ${profile?.name}',
            onSelected: (value) {
              if (value == 'switch') {
                ref.read(profileProvider.notifier).select(null);
              } else {
                context.push(value);
              }
            },
            itemBuilder: (_) => [
              const PopupMenuItem(value: '/genres', child: Text('Seus gostos')),
              const PopupMenuItem(
                value: '/ratings',
                child: Text('Minhas avaliações'),
              ),
              const PopupMenuItem(value: '/model', child: Text('Meu modelo')),
              const PopupMenuItem(value: '/together', child: Text('Para nós')),
              const PopupMenuItem(
                value: 'switch',
                child: Text('Trocar perfil'),
              ),
            ],
            child: Padding(
              padding: const EdgeInsets.symmetric(horizontal: 16),
              child: Row(
                children: [
                  Text(
                    profile?.name ?? '',
                    style: const TextStyle(fontSize: 13),
                  ),
                  const SizedBox(width: 4),
                  const Icon(Icons.expand_more, size: 18),
                ],
              ),
            ),
          ),
        ],
      ),
      body: KeyedSubtree(key: ValueKey('${profile?.id}:$path'), child: child),
      bottomNavigationBar: NavigationBar(
        selectedIndex: index < 0 ? 4 : index,
        onDestinationSelected: (i) => context.go(routes[i]),
        destinations: const [
          NavigationDestination(
            icon: Icon(Icons.home_outlined),
            selectedIcon: Icon(Icons.home),
            label: 'Início',
          ),
          NavigationDestination(icon: Icon(Icons.search), label: 'Buscar'),
          NavigationDestination(
            icon: Icon(Icons.star_outline),
            selectedIcon: Icon(Icons.star),
            label: 'Avaliar',
          ),
          NavigationDestination(
            icon: Icon(Icons.auto_awesome_outlined),
            label: 'Para você',
          ),
          NavigationDestination(
            icon: Icon(Icons.person_outline),
            label: 'Perfil',
          ),
        ],
      ),
    );
  }
}
