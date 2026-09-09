import 'dart:convert';

import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:movie_recommendation/models.dart';
import 'package:movie_recommendation/state.dart';
import 'package:movie_recommendation/widgets.dart';
import 'package:movie_recommendation/screens.dart';

const modelJson = <String, dynamic>{
  'userId': 4,
  'status': 'READY',
  'directRatingCount': 8,
  'minimumDirectRatings': 8,
  'recommendedDirectRatings': 25,
  'ratingsUsedInLastTraining': 0,
  'newRatingsSinceLastTraining': 8,
  'modelVersion': 0,
  'canTrain': true,
  'autoTrainEnabled': true,
  'trainLoss': null,
  'validationLoss': null,
  'trainedAt': null,
  'lastError': null,
};
const contentJson = <String, dynamic>{
  'id': 88,
  'title': 'The Matrix',
  'type': 'MOVIE',
  'tmdbId': 603,
  'posterPath': null,
  'releaseDate': '1999-03-30',
  'overview': 'Uma nova realidade.',
};
const discoveryJson = <String, dynamic>{
  ...contentJson,
  'source': 'LOCAL',
  'roles': <String>[],
};
const evaluationJson = <String, dynamic>{
  'content': contentJson,
  'contentRating': null,
  'genres': [
    {'id': 7, 'name': 'Ação', 'targetType': 'GENRE', 'rating': null},
  ],
  'actors': [
    {'id': 91, 'name': 'Keanu Reeves', 'targetType': 'ACTOR', 'rating': null},
  ],
  'directors': [],
  'creators': [],
};

void main() {
  late SharedPreferences preferences;
  late Dio dio;
  late List<RequestOptions> requests;
  late Map<String, dynamic> responses;
  setUp(() async {
    SharedPreferences.setMockInitialValues({
      'profile': jsonEncode({'id': 4, 'name': 'Gil'}),
    });
    preferences = await SharedPreferences.getInstance();
    requests = [];
    responses = {
      '/users': [
        {'id': 4, 'name': 'Gil'},
        {'id': 17, 'name': 'Aliny'},
      ],
      '/users/4/model/status': modelJson,
      '/users/4/contents/88/evaluation': evaluationJson,
      '/users/4/quick-rating': {
        'results': [
          discoveryJson,
          {...discoveryJson, 'id': 89, 'title': 'Próximo'},
        ],
      },
      '/users/4/genres': evaluationJson['genres'],
      '/recommendations/users/4': <dynamic>[],
      '/users/4/ratings/view': <dynamic>[],
    };
    dio = Dio(BaseOptions(baseUrl: 'http://localhost:8080'));
    dio.interceptors.add(
      InterceptorsWrapper(
        onRequest: (options, handler) {
          requests.add(options);
          handler.resolve(
            Response(
              requestOptions: options,
              data: responses[options.path] ?? <String, dynamic>{},
              statusCode: 200,
            ),
          );
        },
      ),
    );
  });
  Future<void> mount(WidgetTester tester, Widget widget) async {
    await tester.pumpWidget(
      ProviderScope(
        overrides: [
          preferencesProvider.overrideWithValue(preferences),
          dioProvider.overrideWithValue(dio),
        ],
        child: MaterialApp(
          theme: lumeTheme(),
          home: Scaffold(body: widget),
        ),
      ),
    );
    await tester.pumpAndSettle();
  }

  test(
    'profile state persists the real API identifier and allows switching',
    () async {
      final container = ProviderContainer(
        overrides: [preferencesProvider.overrideWithValue(preferences)],
      );
      addTearDown(container.dispose);
      expect(container.read(profileProvider)?.id, 4);
      await container
          .read(profileProvider.notifier)
          .select(const Profile(17, 'Aliny'));
      expect(container.read(profileProvider)?.name, 'Aliny');
      expect(jsonDecode(preferences.getString('profile')!)['id'], 17);
      await container.read(profileProvider.notifier).select(null);
      expect(container.read(profileProvider), isNull);
    },
  );
  test('DTO parsing preserves null ratings and server normalization', () {
    final view = ContentEvaluation.fromJson(evaluationJson);
    expect(view.content.id, 88);
    expect(view.content.year, '1999');
    expect(view.contentRating, isNull);
    expect(view.actors.single.rating, isNull);
    final rating = Rating.fromJson({
      'id': 101,
      'userId': 17,
      'targetType': 'ACTOR',
      'targetId': 91,
      'value': 2,
      'normalizedValue': .25,
    });
    expect(rating.normalizedValue, .25);
    expect(rating.userId, 17);
    expect(
      DiscoveryResponse.fromJson({
        'results': [discoveryJson],
      }).results.single.key,
      'LOCAL:88',
    );
  });
  test('model status distinguishes eight and twenty five', () {
    expect(
      ModelStatus.fromJson({...modelJson, 'directRatingCount': 7}).target,
      8,
    );
    expect(
      ModelStatus.fromJson(modelJson).progressMessage,
      contains('experimental'),
    );
    expect(
      ModelStatus.fromJson({...modelJson, 'directRatingCount': 25})
          .progressMessage,
      'Base inicial recomendada atingida.',
    );
  });
  testWidgets('profile selection shows the existing profiles', (tester) async {
    await mount(tester, const ProfileScreen());
    expect(find.text('Quem está avaliando?'), findsOneWidget);
    await tester.tap(find.text('Aliny'));
    await tester.pumpAndSettle();
    expect(jsonDecode(preferences.getString('profile')!)['id'], 17);
  });
  testWidgets('rating widget has semantic labels and forwards exact stars', (
    tester,
  ) async {
    int? choice;
    await mount(
      tester,
      StarRating(value: null, onChanged: (value) => choice = value),
    );
    final semantics = tester.ensureSemantics();
    expect(find.bySemanticsLabel('5 estrelas: Gosto muito'), findsOneWidget);
    await tester.tap(find.byTooltip('Gosto muito'));
    expect(choice, 5);
    semantics.dispose();
  });
  testWidgets(
    'content evaluation loads consolidated view and unfilled ratings',
    (tester) async {
      await mount(tester, const ContentScreen(id: 88));
      expect(find.text('The Matrix'), findsWidgets);
      await tester.scrollUntilVisible(find.text('Sua nota, Gil'), 150);
      expect(find.text('Ainda não avaliado'), findsWidgets);
      expect(
        requests
            .where((r) => r.path == '/users/4/contents/88/evaluation')
            .length,
        1,
      );
      expect(requests.where((r) => r.method == 'PUT'), isEmpty);
    },
  );
  testWidgets('search debounces and shows source and people groups', (
    tester,
  ) async {
    responses['/discovery/search'] = {
      'results': [
        discoveryJson,
        {...discoveryJson, 'id': 91, 'type': 'PERSON', 'title': 'Keanu Reeves'},
      ],
    };
    await mount(tester, const SearchScreen());
    await tester.enterText(find.byType(TextField), 'Matrix');
    await tester.pump(const Duration(milliseconds: 200));
    expect(requests.where((r) => r.path == '/discovery/search'), isEmpty);
    await tester.pump(const Duration(milliseconds: 200));
    await tester.pumpAndSettle();
    expect(requests.where((r) => r.path == '/discovery/search').length, 1);
    expect(find.text('No catálogo'), findsWidgets);
  });
  testWidgets('quick rating skip does not create a rating', (tester) async {
    await mount(tester, const QuickScreen());
    await tester.drag(find.byType(ListView).first, const Offset(0, -500));
    await tester.pumpAndSettle();
    await tester.tap(find.text('Não assisti'));
    await tester.pumpAndSettle();
    expect(requests.where((r) => r.method == 'PUT'), isEmpty);
    expect(find.text('Próximo'), findsWidgets);
  });
  testWidgets('quick rating saves for active profile then advances', (
    tester,
  ) async {
    await mount(tester, const QuickScreen());
    await tester.drag(find.byType(ListView).first, const Offset(0, -500));
    await tester.pumpAndSettle();
    await tester.tap(find.byTooltip('Gosto muito'));
    await tester.pumpAndSettle();
    final sent = requests.singleWhere((r) => r.method == 'PUT');
    expect(sent.path, '/users/4/ratings');
    expect(sent.data, {'targetType': 'MOVIE', 'targetId': 88, 'value': 5});
    expect(find.text('Próximo'), findsWidgets);
  });
  testWidgets(
    'model training is available at eight and shows nullable losses',
    (tester) async {
      await mount(tester, const ModelScreen());
      final button = tester.widget<FilledButton>(
        find.widgetWithText(FilledButton, 'Treinar agora'),
      );
      expect(button.onPressed, isNotNull);
      expect(find.text('8 / 25'), findsOneWidget);
    },
  );
  testWidgets('training is disabled before minimum', (tester) async {
    responses['/users/4/model/status'] = {
      ...modelJson,
      'directRatingCount': 7,
      'status': 'COLLECTING_DATA',
      'canTrain': false,
    };
    await mount(tester, const ModelScreen());
    expect(
      tester
          .widget<FilledButton>(
            find.widgetWithText(FilledButton, 'Treinar agora'),
          )
          .onPressed,
      isNull,
    );
    expect(find.text('7 / 8'), findsOneWidget);
  });
  testWidgets('recommendation card renders backend score and reasons', (
    tester,
  ) async {
    final item = Recommendation.fromJson({
      'content': contentJson,
      'score': .91,
      'strategy': 'TENSORFLOW',
      'reasons': ['Afinidade com seus gêneros'],
    });
    await mount(tester, RecommendationCard(item));
    expect(find.text('91% de afinidade'), findsOneWidget);
    expect(find.text('Afinidade com seus gêneros'), findsOneWidget);
  });
  test('pending ratings remain isolated when changing API servers', () async {
    final container = ProviderContainer(
      overrides: [
        preferencesProvider.overrideWithValue(preferences),
        dioProvider.overrideWithValue(dio),
      ],
    );
    addTearDown(container.dispose);
    final queue = container.read(ratingQueueProvider.notifier);
    queue.choose(4, 'MOVIE', 88, 5);
    expect(container.read(ratingQueueProvider).length, 1);
    await container
        .read(apiUrlProvider.notifier)
        .set('https://api.example.test');
    expect(container.read(ratingQueueProvider), isEmpty);
    expect(container.read(profileProvider), isNull);
    await Future<void>.delayed(const Duration(milliseconds: 400));
    expect(requests.where((r) => r.method == 'PUT'), isEmpty);
  });
  testWidgets('autosave coalesces edits and isolates profiles', (tester) async {
    await mount(
      tester,
      const AutosaveRating(
        userId: 17,
        type: 'ACTOR',
        targetId: 91,
        label: 'Atuação',
      ),
    );
    await tester.tap(find.byTooltip('Gosto pouco'));
    await tester.pump(const Duration(milliseconds: 100));
    await tester.tap(find.byTooltip('Gosto muito'));
    await tester.pump(const Duration(milliseconds: 400));
    await tester.pumpAndSettle();
    final writes = requests.where((r) => r.method == 'PUT').toList();
    expect(writes.length, 1);
    expect(writes.single.path, '/users/17/ratings');
    expect(writes.single.data['value'], 5);
    expect(find.text('✓ Salvo'), findsOneWidget);
  });
}
