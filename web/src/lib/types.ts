export type TargetType = 'MOVIE' | 'SERIES' | 'ACTOR' | 'DIRECTOR' | 'CREATOR' | 'GENRE';
export type DiscoveryType = 'MOVIE' | 'SERIES' | 'PERSON' | 'GENRE';
export interface Profile { id: number; name: string }
export interface Content { id: number; title: string; type: 'MOVIE' | 'SERIES'; tmdbId: number | null; overview: string | null; posterPath: string | null; releaseDate: string | null }
export interface Rating { id: number; userId: number; targetType: TargetType; targetId: number; value: number; normalizedValue: number; updatedAt: string }
export interface RatedEntity { id: number; name: string; targetType: TargetType; rating: Rating | null; imagePath?: string | null }
export interface ContentEvaluation { content: Content; contentRating: Rating | null; genres: RatedEntity[]; actors: RatedEntity[]; directors: RatedEntity[]; creators: RatedEntity[] }
export interface DiscoveryItem { id: number | null; tmdbId: number | null; title: string; type: DiscoveryType; source: 'LOCAL' | 'TMDB'; posterPath?: string | null; releaseDate?: string | null; overview?: string | null; roles: TargetType[] }
export interface DiscoveryResponse { results: DiscoveryItem[]; warning: string | null }
export interface PersonEvaluation { person: DiscoveryItem; roles: RatedEntity[] }
export interface ModelStatus { userId: number; status: 'COLLECTING_DATA' | 'READY' | 'TRAINING' | 'TRAINED' | 'DIRTY' | 'ERROR'; directRatingCount: number; minimumDirectRatings: number; recommendedDirectRatings: number; ratingsUsedInLastTraining: number; newRatingsSinceLastTraining: number; canTrain: boolean; trainedAt: string | null; trainLoss: number | null; validationLoss: number | null; modelVersion: number; autoTrainEnabled: boolean; lastError: string | null }
export interface Recommendation { content: Content; score: number; strategy?: string; reasons: string[]; individualScores?: Record<string, number> }
export interface RatingView { rating: Rating; title: string; posterPath: string | null }
export const labels: Record<TargetType | 'PERSON', string> = { MOVIE: 'Filme', SERIES: 'Série', ACTOR: 'Atuação', DIRECTOR: 'Direção', CREATOR: 'Criação', GENRE: 'Gênero', PERSON: 'Pessoa' };
