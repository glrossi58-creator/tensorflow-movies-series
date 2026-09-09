import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../models.dart';
import '../state.dart';
import '../widgets.dart';

class ProfileScreen extends ConsumerStatefulWidget {
  const ProfileScreen({super.key});
  @override
  ConsumerState<ProfileScreen> createState() => _ProfileScreenState();
}

class _ProfileScreenState extends ConsumerState<ProfileScreen> {
  final name = TextEditingController();
  bool creating = false, busy = false;
  Object? error;
  @override
  void dispose() {
    name.dispose();
    super.dispose();
  }

  Future<void> create() async {
    if (name.text.trim().isEmpty) {
      return;
    }
    setState(() {
      busy = true;
      error = null;
    });
    try {
      final result = await ref
          .read(dioProvider)
          .post('/profiles', data: {'name': name.text.trim()});
      if (!mounted) {
        return;
      }
      ref.invalidate(profilesProvider);
      await ref
          .read(profileProvider.notifier)
          .select(Profile.fromJson(result.data));
    } catch (e) {
      if (mounted) {
        setState(() => error = e);
      }
    } finally {
      if (mounted) {
        setState(() => busy = false);
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    final profiles = ref.watch(profilesProvider);
    return Scaffold(
      bottomNavigationBar: SafeArea(
        child: TextButton(
          onPressed: () => context.push('/connection'),
          child: const Text('Configurar conexão'),
        ),
      ),
      body: SafeArea(
        child: AsyncBody(
          value: profiles,
          retry: () => ref.invalidate(profilesProvider),
          data: (items) => ListView(
            padding: const EdgeInsets.all(28),
            children: [
              const SizedBox(height: 15),
              const Row(
                children: [
                  Icon(Icons.auto_awesome, color: lightPurple),
                  SizedBox(width: 10),
                  Text(
                    'LUME',
                    style: TextStyle(
                      fontSize: 24,
                      fontWeight: FontWeight.w700,
                      letterSpacing: 4,
                    ),
                  ),
                ],
              ),
              const SizedBox(height: 65),
              const PageTitle(
                eyebrow: 'Cada gosto, uma descoberta',
                title: 'Quem está avaliando?',
                subtitle: 'Um espaço para o seu gosto. E para o que vocês têm em comum.',
              ),
              Wrap(
                spacing: 18,
                runSpacing: 18,
                children: [
                  for (final profile in items)
                    SizedBox(
                      width: 130,
                      child: InkWell(
                        borderRadius: BorderRadius.circular(18),
                        onTap: () =>
                            ref.read(profileProvider.notifier).select(profile),
                        child: Column(
                          children: [
                            Container(
                              height: 116,
                              width: 116,
                              decoration: BoxDecoration(
                                color: profile.id.isEven
                                    ? const Color(0xFF40236B)
                                    : const Color(0xFF2B255D),
                                borderRadius: BorderRadius.circular(22),
                              ),
                              child: Center(
                                child: Text(
                                  profile.name.substring(0, 1).toUpperCase(),
                                  style: const TextStyle(
                                    fontSize: 48,
                                    color: Color(0xFFDDD0FF),
                                  ),
                                ),
                              ),
                            ),
                            const SizedBox(height: 12),
                            Text(
                              profile.name,
                              style: const TextStyle(
                                fontWeight: FontWeight.w600,
                                fontSize: 18,
                              ),
                            ),
                            const Text(
                              'Entrar no perfil',
                              style: TextStyle(
                                color: secondaryText,
                                fontSize: 11,
                              ),
                            ),
                          ],
                        ),
                      ),
                    ),
                  SizedBox(
                    width: 130,
                    child: OutlinedButton(
                      onPressed: () => setState(() => creating = true),
                      child: const Padding(
                        padding: EdgeInsets.symmetric(vertical: 25),
                        child: Column(
                          children: [
                            Icon(Icons.add, size: 32),
                            SizedBox(height: 10),
                            Text('Criar perfil'),
                          ],
                        ),
                      ),
                    ),
                  ),
                ],
              ),
              if (creating) ...[
                const SizedBox(height: 30),
                TextField(
                  controller: name,
                  maxLength: 150,
                  decoration: const InputDecoration(labelText: 'Seu nome'),
                  textInputAction: TextInputAction.done,
                  onSubmitted: (_) => create(),
                ),
                const SizedBox(height: 10),
                FilledButton(
                  onPressed: busy ? null : create,
                  child: Text(busy ? 'Criando…' : 'Criar e entrar'),
                ),
              ],
              if (error != null) ErrorNotice(error!),
              const SizedBox(height: 40),
              const Text(
                'Suas notas e seu modelo são exclusivos do seu perfil.',
                style: TextStyle(color: secondaryText, fontSize: 12),
                textAlign: TextAlign.center,
              ),
              TextButton(
                onPressed: () => context.push('/connection'),
                child: const Text('Configurar conexão'),
              ),
            ],
          ),
        ),
      ),
    );
  }
}

class ConnectionScreen extends ConsumerStatefulWidget {
  const ConnectionScreen({super.key});
  @override
  ConsumerState<ConnectionScreen> createState() => _ConnectionScreenState();
}

class _ConnectionScreenState extends ConsumerState<ConnectionScreen> {
  late final TextEditingController url;
  Object? error;
  bool busy = false;
  @override
  void initState() {
    super.initState();
    url = TextEditingController(text: ref.read(apiUrlProvider));
  }

  @override
  void dispose() {
    url.dispose();
    super.dispose();
  }

  Future<void> save() async {
    setState(() {
      busy = true;
      error = null;
    });
    try {
      await ref.read(apiUrlProvider.notifier).set(url.text);
      if (mounted) {
        context.go('/profiles');
      }
    } catch (e) {
      if (mounted) {
        setState(() => error = e);
      }
    } finally {
      if (mounted) {
        setState(() => busy = false);
      }
    }
  }

  @override
  Widget build(BuildContext context) => ListView(
    padding: const EdgeInsets.all(24),
    children: [
      const SizedBox(height: 30),
      const PageTitle(
        eyebrow: 'Vamos nos conectar',
        title: 'Seu servidor',
        subtitle: 'Use o endereço do computador na mesma rede Wi-Fi.',
      ),
      TextField(
        controller: url,
        keyboardType: TextInputType.url,
        autocorrect: false,
        decoration: const InputDecoration(
          labelText: 'Endereço da API',
          hintText: 'https://api.seu-dominio',
        ),
      ),
      const SizedBox(height: 20),
      FilledButton(
        onPressed: busy ? null : save,
        child: const Text('Salvar conexão'),
      ),
      if (error != null) ErrorNotice(error!),
      const SizedBox(height: 20),
      const Text(
        'Para mudar o IP de uma conexão HTTP local, gere o APK com o novo API_BASE_URL. Versões para produção usam HTTPS.',
        style: TextStyle(color: secondaryText),
      ),
      TextButton(
        onPressed: () => context.go('/profiles'),
        child: const Text('Voltar aos perfis'),
      ),
    ],
  );
}
