'use client';
import Link from 'next/link';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { ArrowLeft } from 'lucide-react';
import { api } from '@/lib/api';
import { labels, type ContentEvaluation, type PersonEvaluation, type RatedEntity } from '@/lib/types';
import { useProfile } from './providers';
import { AutosaveRating, Empty, ErrorNotice, Poster, Skeleton } from './ui';

function RefreshPerson({ tmdbId, userId }: { tmdbId: number; userId: number }) {
  const client = useQueryClient();
  const refresh = useMutation({
    mutationFn: () => api(`/discovery/import/PERSON/${tmdbId}`, { method: 'POST' }),
    onSuccess: () => client.invalidateQueries({ queryKey: ['user', userId] }),
  });
  return <div className="section"><button className="button subtle" disabled={refresh.isPending} onClick={() => refresh.mutate()}>{refresh.isPending ? 'Consultando créditos…' : 'Atualizar foto e papéis do TMDB'}</button>{refresh.error && <ErrorNotice message={refresh.error.message} retry={() => refresh.mutate()} />}</div>;
}

export function EntityRatings({ title, items, userId }: { title: string; items: RatedEntity[]; userId: number }) {
  if (!items.length) return null;
  return <section className="section"><div className="section-heading"><h2>{title}</h2><span>Suas preferências contam</span></div><div className="rating-grid">{items.map(item => <div className="rating-row" key={`${item.targetType}:${item.id}`} id={`entity-${item.id}`}><div className="entity-name">{item.targetType === 'GENRE' ? <strong>{item.name}</strong> : <Link href={`/people/${item.id}`}>{item.name}</Link>}<small>{labels[item.targetType]}</small></div><AutosaveRating userId={userId} type={item.targetType} id={item.id} value={item.rating?.value ?? null} label={`Avaliar ${item.name} em ${labels[item.targetType]}`} /></div>)}</div></section>;
}
export function ContentEvaluationPage({ id }: { id: number }) {
  const { profile } = useProfile();
  const query = useQuery({ queryKey: ['user', profile.id, 'content', id], queryFn: ({ signal }) => api<ContentEvaluation>(`/users/${profile.id}/contents/${id}/evaluation`, { signal }) });
  if (query.isPending) return <Skeleton />;
  if (query.error) return <ErrorNotice message={query.error.message} retry={() => void query.refetch()} />;
  const view = query.data;
  return <><Link className="back-link" href="/search"><ArrowLeft size={16} />Voltar à busca</Link><section className="detail-hero"><Poster path={view.content.posterPath} title={view.content.title} priority /><div className="detail-copy"><p className="eyebrow">{labels[view.content.type]}{view.content.releaseDate && ` · ${view.content.releaseDate.slice(0, 4)}`}</p><h1>{view.content.title}</h1><div className="tags">{view.genres.map(g => <span key={g.id}>{g.name}</span>)}</div><p className="overview">{view.content.overview || 'A história é sua. Conte o que achou deste título.'}</p><div className="hero-rating"><span>Sua nota, {profile.name}</span><AutosaveRating userId={profile.id} type={view.content.type} id={id} value={view.contentRating?.value ?? null} label={`Avaliar ${view.content.title}`} /><small>1 Não gosto · 3 Neutro · 5 Gosto muito</small></div></div></section><EntityRatings title="Gêneros" items={view.genres} userId={profile.id} /><EntityRatings title="O elenco" items={view.actors} userId={profile.id} /><EntityRatings title="Por trás da câmera" items={view.directors} userId={profile.id} /><EntityRatings title="Quem criou essa história" items={view.creators} userId={profile.id} />{!view.genres.length && !view.actors.length && <p className="notice">Este conteúdo está no catálogo sem créditos associados. Sua avaliação do título funciona normalmente.</p>}</>;
}
export function PersonEvaluationPage({ id }: { id: number }) {
  const { profile } = useProfile();
  const query = useQuery({ queryKey: ['user', profile.id, 'person', id], queryFn: ({ signal }) => api<PersonEvaluation>(`/users/${profile.id}/people/${id}/evaluation`, { signal }) });
  if (query.isPending) return <Skeleton />;
  if (query.error) return <ErrorNotice message={query.error.message} retry={() => void query.refetch()} />;
  const view = query.data;
  return <><Link href="/search" className="back-link"><ArrowLeft size={16} />Voltar à busca</Link><div className="detail-hero"><Poster title={view.person.title} path={view.person.posterPath} person /><div className="detail-copy"><p className="eyebrow">QUEM DÁ VIDA ÀS HISTÓRIAS</p><h1>{view.person.title}</h1><p className="overview">{view.person.overview || 'Cada papel tem a sua própria avaliação. Seu gosto por atuação e direção pode ser diferente.'}</p><div className="tags">{view.roles.map(r => <span key={r.targetType}>{labels[r.targetType]}</span>)}</div></div></div>{view.person.tmdbId && <RefreshPerson tmdbId={view.person.tmdbId} userId={profile.id} />}{view.roles.length ? <EntityRatings title="Sua opinião em cada papel" items={view.roles} userId={profile.id} /> : <Empty title="Sem papéis de avaliação confirmados" message="As relações disponíveis ainda não confirmam atuação, direção ou criação de séries para esta pessoa." />}</>;
}
export function GenrePreferencesPage() {
  const { profile } = useProfile();
  const query = useQuery({ queryKey: ['user', profile.id, 'genres'], queryFn: ({ signal }) => api<RatedEntity[]>(`/users/${profile.id}/genres`, { signal }) });
  return <><div className="page-heading"><p className="eyebrow">O SEU REPERTÓRIO</p><h1>Seus gostos</h1><p>Deixe as histórias que você ama mais perto. Essas preferências ajudam suas recomendações.</p></div>{query.isPending ? <Skeleton /> : query.error ? <ErrorNotice message={query.error.message} retry={() => void query.refetch()} /> : query.data.length ? <EntityRatings title="Uma nota para cada gênero" items={query.data} userId={profile.id} /> : <Empty title="Vamos descobrir seus gêneros" message="Busque e importe um filme ou série para trazer seus gêneros ao catálogo." />}<p className="notice">Preferências de gênero ajudam o modelo. A base de 8 / 25 considera somente notas de filmes e séries.</p></>;
}
