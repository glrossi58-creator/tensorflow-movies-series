import { act, render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import type { ReactNode } from 'react';
import { api } from '@/lib/api';
import type { ContentEvaluation, ModelStatus, Recommendation, DiscoveryItem } from '@/lib/types';
import { ProfileSelector } from '@/components/profiles';
import { StarRating } from '@/components/ui';
import { SearchPage, SearchResults } from '@/components/search';
import { ContentEvaluationPage, PersonEvaluationPage, GenrePreferencesPage } from '@/components/evaluation';
import { ModelProgress, TrainingButton } from '@/components/model';
import { QuickRatingPage } from '@/components/quick';
import { JointRecommendationPage, RecommendationCard } from '@/components/recommendations';
import { RatingQueue } from '@/lib/ratings';

const { push } = vi.hoisted(() => ({ push: vi.fn() }));
vi.mock('next/navigation', () => ({ useRouter: () => ({ push }), usePathname: () => '/search' }));
vi.mock('@/components/providers', () => ({ useProfile: () => ({ profile: { id: 4, name: 'Gil' }, profiles: [{ id: 4, name: 'Gil' }, { id: 17, name: 'Aliny' }], switchProfile: vi.fn() }) }));
vi.mock('@/lib/api', () => ({ api: vi.fn(), apiBase: 'http://localhost:8080', jsonBody: (body: unknown, method = 'PUT') => ({ method, body: JSON.stringify(body) }) }));
const request = vi.mocked(api);
function mount(child: ReactNode) { const client = new QueryClient({ defaultOptions: { queries: { retry: false, gcTime: 0 }, mutations: { retry: false } } }); return render(<QueryClientProvider client={client}>{child}</QueryClientProvider>); }
const baseStatus: ModelStatus = { userId: 4, status: 'READY', directRatingCount: 8, minimumDirectRatings: 8, recommendedDirectRatings: 25, ratingsUsedInLastTraining: 0, newRatingsSinceLastTraining: 8, canTrain: true, trainedAt: null, trainLoss: null, validationLoss: null, modelVersion: 0, autoTrainEnabled: true, lastError: null };
const movie: DiscoveryItem = { id: 88, tmdbId: 603, title: 'The Matrix', type: 'MOVIE', source: 'LOCAL', roles: [], posterPath: null };
const evaluation: ContentEvaluation = { content: { id: 88, tmdbId: 603, title: 'The Matrix', type: 'MOVIE', posterPath: null, overview: 'Uma nova realidade.', releaseDate: '1999-03-30' }, contentRating: null, genres: [{ id: 71, name: 'Ação', targetType: 'GENRE', rating: null }], actors: [{ id: 91, name: 'Keanu Reeves', targetType: 'ACTOR', rating: null }], directors: [{ id: 101, name: 'Lana Wachowski', targetType: 'DIRECTOR', rating: null }], creators: [] };
const recommendation: Recommendation = { content: evaluation.content, score: .91, strategy: 'TENSORFLOW', reasons: ['Afinidade com seus gêneros'] };
beforeEach(() => { request.mockReset(); push.mockReset(); });

describe('ProfileSelector', () => {
  it('selects the API ID without assuming one', async () => {
    const select = vi.fn(); mount(<ProfileSelector profiles={[{ id: 4, name: 'Gil' }, { id: 17, name: 'Aliny' }]} retry={vi.fn()} onSelect={select} />);
    await userEvent.click(screen.getByRole('button', { name: /Aliny/ }));
    expect(select).toHaveBeenCalledWith({ id: 17, name: 'Aliny' });
  });
  it('creates a profile using its name', async () => {
    request.mockResolvedValue({ id: 53, name: 'Visitante' });
    const select = vi.fn(); mount(<ProfileSelector profiles={[]} retry={vi.fn()} onSelect={select} />);
    await userEvent.click(screen.getByRole('button', { name: /Criar perfil/ }));
    await userEvent.type(screen.getByLabelText('Como você se chama?'), 'Visitante');
    await userEvent.click(screen.getByRole('button', { name: 'Criar e entrar' }));
    await waitFor(() => expect(select).toHaveBeenCalledWith({ id: 53, name: 'Visitante' }));
  });
});
describe('Search and SearchResults', () => {
  it('debounces and routes a local result by local ID', async () => {
    request.mockResolvedValue({ results: [movie], warning: null }); mount(<SearchPage />);
    await userEvent.type(screen.getByLabelText('Busca universal'), 'Matrix');
    expect(request).not.toHaveBeenCalled();
    await screen.findByText('No catálogo');
    expect(request).toHaveBeenCalledTimes(1);
    await userEvent.click(screen.getByRole('button', { name: /The Matrix/ }));
    expect(push).toHaveBeenCalledWith('/content/88');
  });
  it('groups movies people and genres with source labels', () => {
    mount(<SearchResults results={[movie, { ...movie, id: 3, type: 'PERSON', title: 'Pessoa', source: 'TMDB' }, { ...movie, id: 9, type: 'GENRE', title: 'Ação' }]} open={vi.fn()} />);
    expect(screen.getByRole('heading', { name: 'Filmes' })).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: 'Pessoas' })).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: 'Gêneros' })).toBeInTheDocument();
    expect(screen.getByText('Importar e avaliar')).toBeInTheDocument();
  });
  it('shows connection errors and retry', async () => {
    request.mockRejectedValue(new Error('Sem conexão')); mount(<SearchPage />);
    await userEvent.type(screen.getByLabelText('Busca universal'), 'Matrix');
    expect(await screen.findByRole('alert')).toHaveTextContent('Sem conexão');
    expect(screen.getByRole('button', { name: 'Tentar novamente' })).toBeInTheDocument();
  });
});
it('StarRating exposes meaningful keyboard accessible labels and null is unselected', async () => {
  const change = vi.fn(); mount(<StarRating value={null} onChange={change} />);
  expect(screen.getAllByRole('button').every(button => button.getAttribute('aria-pressed') === 'false')).toBe(true);
  const star = screen.getByRole('button', { name: '5 estrelas: Gosto muito' }); star.focus(); await userEvent.keyboard('{Enter}');
  expect(change).toHaveBeenCalledWith(5);
});
it('ContentEvaluation shows null separately and related independent ratings', async () => {
  request.mockResolvedValue(evaluation); mount(<ContentEvaluationPage id={88} />);
  expect(await screen.findByRole('heading', { name: 'The Matrix' })).toBeInTheDocument();
  expect(screen.getAllByText('Ainda não avaliado')).toHaveLength(4);
  expect(screen.getByRole('group', { name: /Keanu Reeves em Atuação/ })).toBeInTheDocument();
  expect(request).toHaveBeenCalledWith('/users/4/contents/88/evaluation', expect.anything());
});
it('PersonEvaluation keeps acting and directing separate', async () => {
  request.mockResolvedValue({ person: { ...movie, type: 'PERSON', title: 'Pessoa' }, roles: [{ id: 9, name: 'Pessoa', targetType: 'ACTOR', rating: { value: 2 } }, { id: 9, name: 'Pessoa', targetType: 'DIRECTOR', rating: { value: 5 } }] });
  mount(<PersonEvaluationPage id={9} />);
  const acting = await screen.findByRole('group', { name: 'Avaliar Pessoa em Atuação' });
  const directing = screen.getByRole('group', { name: 'Avaliar Pessoa em Direção' });
  expect(within(acting).getByRole('button', { name: /^2 estrelas/ })).toHaveAttribute('aria-pressed', 'true');
  expect(within(directing).getByRole('button', { name: /^5 estrelas/ })).toHaveAttribute('aria-pressed', 'true');
});
it('GenrePreferences explains that genre notes do not count as direct ratings', async () => {
  request.mockResolvedValue(evaluation.genres); mount(<GenrePreferencesPage />);
  await screen.findByText('Ação');
  expect(screen.getByText(/considera somente notas de filmes e séries/)).toBeInTheDocument();
});
describe('ModelStatus and TrainingButton', () => {
  it.each([[7, 'Faltam 1 para liberar treinamento.'], [8, 'Você já pode treinar um modelo experimental.'], [25, 'Base inicial recomendada atingida.']])('explains count %i correctly', (count, message) => {
    mount(<ModelProgress status={{ ...baseStatus, directRatingCount: count as number }} />); expect(screen.getByText(message)).toBeInTheDocument();
  });
  it('enables experimental training at eight and prevents duplicate training', () => {
    const { rerender } = mount(<TrainingButton status={baseStatus} pending={false} train={vi.fn()} />);
    expect(screen.getByRole('button')).toBeEnabled();
    rerender(<TrainingButton status={baseStatus} pending train={vi.fn()} />); expect(screen.getByRole('button')).toBeDisabled();
  });
});
it('QuickRating skip never creates a rating', async () => {
  request.mockImplementation(async path => path.includes('model') ? baseStatus : { results: [movie, { ...movie, id: 89, title: 'Próximo' }] });
  mount(<QuickRatingPage />); await screen.findByRole('heading', { name: 'The Matrix' });
  await userEvent.click(screen.getByRole('button', { name: 'Não assisti' }));
  expect(await screen.findByRole('heading', { name: 'Próximo' })).toBeInTheDocument();
  expect(request.mock.calls.every(([, init]) => !init?.method)).toBe(true);
});
it('RecommendationCard displays backend score and reasons', () => {
  mount(<RecommendationCard item={recommendation} />);
  expect(screen.getByText('91%')).toBeInTheDocument(); expect(screen.getByText('Afinidade com seus gêneros')).toBeInTheDocument();
  expect(screen.getByRole('link')).toHaveAttribute('href', '/content/88');
});
it('JointRecommendation sends selected IDs and displays shared score', async () => {
  request.mockResolvedValue([{ ...recommendation, individualScores: { '4': .91, '17': .8 } }]); mount(<JointRecommendationPage />);
  expect(screen.getByRole('button', { name: 'Encontrar algo para nós' })).toBeDisabled();
  await userEvent.click(screen.getByRole('button', { name: /Aliny/ })); await userEvent.click(screen.getByRole('button', { name: 'Encontrar algo para nós' }));
  await screen.findByText('91%');
  expect(request).toHaveBeenCalledWith('/recommendations/joint', expect.objectContaining({ method: 'POST', body: JSON.stringify({ userIds: [4, 17], limit: 20 }) }));
});
describe('Autosave queue', () => {
  it('coalesces quick edits and retains captured profile ID', async () => {
    vi.useFakeTimers(); request.mockResolvedValue({}); const queue = new RatingQueue();
    queue.choose(4, 'ACTOR', 90, 2); queue.choose(4, 'ACTOR', 90, 5); queue.choose(17, 'ACTOR', 90, 1);
    await act(() => vi.advanceTimersByTimeAsync(400));
    expect(request).toHaveBeenCalledTimes(2);
    expect(request).toHaveBeenCalledWith('/users/4/ratings', expect.objectContaining({ body: JSON.stringify({ targetType: 'ACTOR', targetId: 90, value: 5 }) }));
    expect(queue.get('17:ACTOR:90')?.value).toBe(1); vi.useRealTimers();
  });
  it('retains a failed choice and retries without losing the value', async () => {
    vi.useFakeTimers(); request.mockRejectedValueOnce(new Error('offline')).mockResolvedValue({}); const queue = new RatingQueue();
    queue.choose(4, 'GENRE', 7, 4); await act(() => vi.advanceTimersByTimeAsync(400));
    expect(queue.get('4:GENRE:7')).toMatchObject({ value: 4, status: 'error' });
    queue.retry('4:GENRE:7'); await act(() => vi.advanceTimersByTimeAsync(1));
    expect(queue.get('4:GENRE:7')?.status).toBe('saved'); vi.useRealTimers();
  });
});
