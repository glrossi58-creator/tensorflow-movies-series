import 'dart:async';
import 'dart:convert';
import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'models.dart';

const compiledApiUrl = String.fromEnvironment('API_BASE_URL', defaultValue: 'http://10.0.2.2:8080');
final preferencesProvider = Provider<SharedPreferences>((ref) => throw StateError('Armazenamento não inicializado.'));
final apiUrlProvider = NotifierProvider<ApiUrlController, String>(ApiUrlController.new);
class ApiUrlController extends Notifier<String> {
  @override String build() => ref.read(preferencesProvider).getString('api-url') ?? compiledApiUrl;
  Future<void> set(String url) async {
    final uri = Uri.tryParse(url.trim());
    if (uri == null || !uri.hasAuthority || !['http', 'https'].contains(uri.scheme)) { throw const FormatException('Informe uma URL HTTP ou HTTPS completa.'); }
    if (uri.scheme == 'http' && uri.host != Uri.parse(compiledApiUrl).host) { throw const FormatException('Para outro IP HTTP, gere o APK com API_BASE_URL apontando para esse computador.'); }
    state = url.trim().replaceFirst(RegExp(r'/$'), '');
    await ref.read(preferencesProvider).setString('api-url', state);
    await ref.read(profileProvider.notifier).select(null);
  }
}
final dioProvider = Provider<Dio>((ref) {
  final dio = Dio(BaseOptions(baseUrl: ref.watch(apiUrlProvider), connectTimeout: const Duration(seconds: 8), receiveTimeout: const Duration(seconds: 45), sendTimeout: const Duration(seconds: 15), headers: {'Accept': 'application/json'}));
  ref.onDispose(() => dio.close(force: true));
  return dio;
});
String errorMessage(Object error) {
  if (error is DioException) {
    final body = error.response?.data;
    if (body is Map && body['message'] is String) { return body['message']; }
    if ([DioExceptionType.connectionTimeout, DioExceptionType.receiveTimeout, DioExceptionType.sendTimeout].contains(error.type)) { return 'A conexão demorou demais. Tente novamente.'; }
    return 'Não foi possível conectar. Confira o Wi-Fi e se o computador está ligado.';
  }
  if (error is FormatException) { return error.message; }
  return 'Não foi possível concluir. Tente novamente.';
}
final profileProvider = NotifierProvider<ProfileController, Profile?>(ProfileController.new);
class ProfileController extends Notifier<Profile?> {
  @override Profile? build() {
    final raw = ref.read(preferencesProvider).getString('profile');
    try { return raw == null ? null : Profile.fromJson(jsonDecode(raw)); } catch (_) { return null; }
  }
  Future<void> select(Profile? profile) async {
    state = profile;
    final prefs = ref.read(preferencesProvider);
    if (profile == null) { await prefs.remove('profile'); } else { await prefs.setString('profile', jsonEncode(profile.toJson())); }
  }
}
final profilesProvider = FutureProvider<List<Profile>>((ref) async => objects((await ref.watch(dioProvider).get('/users')).data).map(Profile.fromJson).toList());
typedef EvaluationKey = ({int userId, int id});
typedef SearchKey = ({String query, String type});
final searchProvider = FutureProvider.autoDispose.family<DiscoveryResponse, SearchKey>((ref, key) async {
  final cancel = CancelToken(); ref.onDispose(cancel.cancel);
  final result = await ref.watch(dioProvider).get('/discovery/search', queryParameters: {'q': key.query, if (key.type.isNotEmpty) 'type': key.type}, cancelToken: cancel);
  final keep = ref.keepAlive(); final timer = Timer(const Duration(minutes: 2), keep.close); ref.onDispose(timer.cancel);
  return DiscoveryResponse.fromJson(result.data);
});
final contentProvider = FutureProvider.autoDispose.family<ContentEvaluation, EvaluationKey>((ref, key) async => ContentEvaluation.fromJson((await ref.watch(dioProvider).get('/users/${key.userId}/contents/${key.id}/evaluation')).data));
final personProvider = FutureProvider.autoDispose.family<PersonEvaluation, EvaluationKey>((ref, key) async => PersonEvaluation.fromJson((await ref.watch(dioProvider).get('/users/${key.userId}/people/${key.id}/evaluation')).data));
final genresProvider = FutureProvider.autoDispose.family<List<RatedEntity>, int>((ref, id) async => objects((await ref.watch(dioProvider).get('/users/$id/genres')).data).map(RatedEntity.fromJson).toList());
final modelProvider = FutureProvider.autoDispose.family<ModelStatus, int>((ref, id) async {
  final timer = Timer(const Duration(seconds: 5), ref.invalidateSelf); ref.onDispose(timer.cancel);
  return ModelStatus.fromJson((await ref.watch(dioProvider).get('/users/$id/model/status')).data);
});
final quickProvider = FutureProvider.autoDispose.family<DiscoveryResponse, int>((ref, id) async => DiscoveryResponse.fromJson((await ref.watch(dioProvider).get('/users/$id/quick-rating')).data));
final recommendationsProvider = FutureProvider.autoDispose.family<List<Recommendation>, int>((ref, id) async => objects((await ref.watch(dioProvider).get('/recommendations/users/$id')).data).map(Recommendation.fromJson).toList());
final historyProvider = FutureProvider.autoDispose.family<List<Json>, int>((ref, id) async => objects((await ref.watch(dioProvider).get('/users/$id/ratings/view')).data));

class RatingDraft {
  final int userId, targetId, value, revision;
  final String targetType, status;
  const RatingDraft(this.userId, this.targetType, this.targetId, this.value, this.revision, this.status);
  String get key => '$userId:$targetType:$targetId';
  RatingDraft withStatus(String status) => RatingDraft(userId, targetType, targetId, value, revision, status);
  Json toJson() => {'userId': userId, 'targetType': targetType, 'targetId': targetId, 'value': value, 'revision': revision};
  factory RatingDraft.fromJson(Json j) => RatingDraft(integer(j['userId']), j['targetType'], integer(j['targetId']), integer(j['value']), integer(j['revision']), 'error');
}
final ratingQueueProvider = NotifierProvider<RatingQueue, Map<String, RatingDraft>>(RatingQueue.new);
class RatingQueue extends Notifier<Map<String, RatingDraft>> {
  final _timers = <String, Timer>{};
  final _active = <String>{};
  @override Map<String, RatingDraft> build() {
    ref.onDispose(() { for (final timer in _timers.values) { timer.cancel(); } });
    try { final saved = objects(jsonDecode(ref.read(preferencesProvider).getString('rating-drafts') ?? '[]')).map(RatingDraft.fromJson); return {for (final d in saved) d.key: d}; } catch (_) { return {}; }
  }
  void choose(int userId, String type, int id, int value) {
    final key = '$userId:$type:$id';
    final draft = RatingDraft(userId, type, id, value, (state[key]?.revision ?? 0) + 1, 'saving');
    state = {...state, key: draft}; _persist();
    _timers[key]?.cancel(); _timers[key] = Timer(const Duration(milliseconds: 350), () => flush(key));
  }
  void _persist() { unawaited(ref.read(preferencesProvider).setString('rating-drafts', jsonEncode(state.values.where((d) => d.status != 'saved').map((d) => d.toJson()).toList()))); }
  Future<void> flush(String key) async {
    if (_active.contains(key)) { return; }
    final draft = state[key]; if (draft == null || draft.status == 'saved') { return; }
    _active.add(key); state = {...state, key: draft.withStatus('saving')};
    try {
      await ref.read(dioProvider).put('/users/${draft.userId}/ratings', data: {'targetType': draft.targetType, 'targetId': draft.targetId, 'value': draft.value});
      if (state[key]?.revision == draft.revision) { state = {...state, key: draft.withStatus('saved')}; }
      ref.invalidate(modelProvider(draft.userId)); ref.invalidate(historyProvider(draft.userId)); ref.invalidate(recommendationsProvider(draft.userId)); ref.invalidate(genresProvider(draft.userId));
    } catch (_) { if (state[key]?.revision == draft.revision) { state = {...state, key: draft.withStatus('error')}; } }
    finally { _active.remove(key); _persist(); if (state[key]?.revision != draft.revision) { unawaited(flush(key)); } }
  }
}
