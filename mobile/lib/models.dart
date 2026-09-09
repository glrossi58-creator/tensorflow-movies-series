typedef Json = Map<String, dynamic>;

int integer(dynamic value) => (value as num).toInt();
List<Json> objects(dynamic value) => (value as List? ?? []).cast<Json>();
const labels = {
  'MOVIE': 'Filme',
  'SERIES': 'Série',
  'PERSON': 'Pessoa',
  'ACTOR': 'Atuação',
  'DIRECTOR': 'Direção',
  'CREATOR': 'Criação',
  'GENRE': 'Gênero',
};

class Profile {
  final int id;
  final String name;
  const Profile(this.id, this.name);
  factory Profile.fromJson(Json json) =>
      Profile(integer(json['id']), json['name'] as String);
  Json toJson() => {'id': id, 'name': name};
}

class Rating {
  final int id, userId, targetId, value;
  final String targetType;
  final double normalizedValue;
  const Rating({
    required this.id,
    required this.userId,
    required this.targetId,
    required this.value,
    required this.targetType,
    required this.normalizedValue,
  });
  factory Rating.fromJson(Json j) => Rating(
    id: integer(j['id']),
    userId: integer(j['userId']),
    targetId: integer(j['targetId']),
    value: integer(j['value']),
    targetType: j['targetType'],
    normalizedValue: (j['normalizedValue'] as num).toDouble(),
  );
}

class Content {
  final int id;
  final String title, type;
  final String? posterPath, releaseDate, overview;
  const Content({
    required this.id,
    required this.title,
    required this.type,
    this.posterPath,
    this.releaseDate,
    this.overview,
  });
  factory Content.fromJson(Json j) => Content(
    id: integer(j['id']),
    title: j['title'],
    type: j['type'],
    posterPath: j['posterPath'],
    releaseDate: j['releaseDate'],
    overview: j['overview'],
  );
  String get year => releaseDate != null && releaseDate!.length >= 4
      ? releaseDate!.substring(0, 4)
      : '';
}

class DiscoveryItem {
  final int? id, tmdbId;
  final String title, type, source;
  final String? posterPath, releaseDate, overview;
  final List<String> roles;
  const DiscoveryItem({
    this.id,
    this.tmdbId,
    required this.title,
    required this.type,
    required this.source,
    this.posterPath,
    this.releaseDate,
    this.overview,
    this.roles = const [],
  });
  factory DiscoveryItem.fromJson(Json j) => DiscoveryItem(
    id: (j['id'] as num?)?.toInt(),
    tmdbId: (j['tmdbId'] as num?)?.toInt(),
    title: j['title'],
    type: j['type'],
    source: j['source'],
    posterPath: j['posterPath'],
    releaseDate: j['releaseDate'],
    overview: j['overview'],
    roles: (j['roles'] as List? ?? []).cast<String>(),
  );
  String get key => source == 'LOCAL' ? 'LOCAL:$id' : 'TMDB:$type:$tmdbId';
  String get year => releaseDate != null && releaseDate!.length >= 4
      ? releaseDate!.substring(0, 4)
      : '';
}

class DiscoveryResponse {
  final List<DiscoveryItem> results;
  final String? warning;
  const DiscoveryResponse(this.results, this.warning);
  factory DiscoveryResponse.fromJson(Json j) => DiscoveryResponse(
    objects(j['results']).map(DiscoveryItem.fromJson).toList(),
    j['warning'],
  );
}

class RatedEntity {
  final int id;
  final String name, targetType;
  final Rating? rating;
  final String? imagePath;
  const RatedEntity({
    required this.id,
    required this.name,
    required this.targetType,
    this.rating,
    this.imagePath,
  });
  factory RatedEntity.fromJson(Json j) => RatedEntity(
    id: integer(j['id']),
    name: j['name'],
    targetType: j['targetType'],
    rating: j['rating'] == null ? null : Rating.fromJson(j['rating']),
    imagePath: j['imagePath'],
  );
}

class ContentEvaluation {
  final Content content;
  final Rating? contentRating;
  final List<RatedEntity> genres, actors, directors, creators;
  const ContentEvaluation({
    required this.content,
    this.contentRating,
    this.genres = const [],
    this.actors = const [],
    this.directors = const [],
    this.creators = const [],
  });
  factory ContentEvaluation.fromJson(Json j) => ContentEvaluation(
    content: Content.fromJson(j['content']),
    contentRating: j['contentRating'] == null
        ? null
        : Rating.fromJson(j['contentRating']),
    genres: objects(j['genres']).map(RatedEntity.fromJson).toList(),
    actors: objects(j['actors']).map(RatedEntity.fromJson).toList(),
    directors: objects(j['directors']).map(RatedEntity.fromJson).toList(),
    creators: objects(j['creators']).map(RatedEntity.fromJson).toList(),
  );
}

class PersonEvaluation {
  final DiscoveryItem person;
  final List<RatedEntity> roles;
  const PersonEvaluation(this.person, this.roles);
  factory PersonEvaluation.fromJson(Json j) => PersonEvaluation(
    DiscoveryItem.fromJson(j['person']),
    objects(j['roles']).map(RatedEntity.fromJson).toList(),
  );
}

class ModelStatus {
  final String status;
  final int userId,
      directRatingCount,
      minimumDirectRatings,
      recommendedDirectRatings,
      ratingsUsedInLastTraining,
      newRatingsSinceLastTraining,
      modelVersion;
  final bool canTrain, autoTrainEnabled;
  final double? trainLoss, validationLoss;
  final String? trainedAt, lastError;
  const ModelStatus({
    required this.userId,
    required this.status,
    required this.directRatingCount,
    required this.minimumDirectRatings,
    required this.recommendedDirectRatings,
    required this.ratingsUsedInLastTraining,
    required this.newRatingsSinceLastTraining,
    required this.modelVersion,
    required this.canTrain,
    required this.autoTrainEnabled,
    this.trainLoss,
    this.validationLoss,
    this.trainedAt,
    this.lastError,
  });
  factory ModelStatus.fromJson(Json j) => ModelStatus(
    userId: integer(j['userId']),
    status: j['status'],
    directRatingCount: integer(j['directRatingCount']),
    minimumDirectRatings: integer(j['minimumDirectRatings']),
    recommendedDirectRatings: integer(j['recommendedDirectRatings']),
    ratingsUsedInLastTraining: integer(j['ratingsUsedInLastTraining']),
    newRatingsSinceLastTraining: integer(j['newRatingsSinceLastTraining']),
    modelVersion: integer(j['modelVersion']),
    canTrain: j['canTrain'],
    autoTrainEnabled: j['autoTrainEnabled'],
    trainLoss: (j['trainLoss'] as num?)?.toDouble(),
    validationLoss: (j['validationLoss'] as num?)?.toDouble(),
    trainedAt: j['trainedAt'],
    lastError: j['lastError'],
  );
  String get label =>
      {
        'COLLECTING_DATA': 'Conhecendo seu gosto',
        'READY': 'Pronto para experimentar',
        'TRAINING': 'Treinando modelo…',
        'TRAINED': 'Modelo atualizado',
        'DIRTY': 'Novas preferências',
        'ERROR': 'Treino precisa de atenção',
      }[status] ??
      status;
  int get target => directRatingCount < minimumDirectRatings
      ? minimumDirectRatings
      : recommendedDirectRatings;
  String get progressMessage => directRatingCount < minimumDirectRatings
      ? 'Faltam ${minimumDirectRatings - directRatingCount} para liberar treinamento.'
      : directRatingCount < recommendedDirectRatings
      ? 'Você já pode treinar um modelo experimental.'
      : 'Base inicial recomendada atingida.';
}

class Recommendation {
  final Content content;
  final double score;
  final String? strategy;
  final List<String> reasons;
  final Map<String, double> individualScores;
  const Recommendation({
    required this.content,
    required this.score,
    this.strategy,
    this.reasons = const [],
    this.individualScores = const {},
  });
  factory Recommendation.fromJson(Json j) => Recommendation(
    content: Content.fromJson(j['content']),
    score: (j['score'] as num).toDouble(),
    strategy: j['strategy'],
    reasons: (j['reasons'] as List? ?? []).cast<String>(),
    individualScores: (j['individualScores'] as Json? ?? {}).map(
      (k, v) => MapEntry(k, (v as num).toDouble()),
    ),
  );
}
