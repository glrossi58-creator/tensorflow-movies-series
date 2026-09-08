'use client';
import Link from 'next/link';
import { useState } from 'react';
import { useMutation, useQuery } from '@tanstack/react-query';
import { HeartHandshake, ArrowUpRight } from 'lucide-react';
import { api, jsonBody } from '@/lib/api';
import { labels, type Recommendation } from '@/lib/types';
import { useProfile } from './providers';
import { Empty, ErrorNotice, Poster, Skeleton } from './ui';

export function RecommendationCard({ item }: { item: Recommendation }) {
  return <Link href={`/content/${item.content.id}`} className="content-card recommendation-card"><div className="recommendation-poster"><Poster path={item.content.posterPath} title={item.content.title} /><span className="score">{Math.round(item.score * 100)}%<small>afinidade</small></span></div><div className="card-copy"><p>{labels[item.content.type]}{item.content.releaseDate && ` · ${item.content.releaseDate.slice(0, 4)}`}</p><h3>{item.content.title}</h3><span className="strategy">{item.strategy === 'TENSORFLOW' ? 'Seu modelo' : item.individualScores ? 'Sintonia de vocês' : 'Explorando seu gosto'}</span><ul className="reasons">{item.reasons.slice(0, 2).map(reason => <li key={reason}>{reason}</li>)}</ul><span className="card-action">Conhecer e avaliar<ArrowUpRight size={14} /></span></div></Link>;
}
export function useRecommendations(limit = 20) {
  const { profile } = useProfile();
  return useQuery({ queryKey: ['user', profile.id, 'recommendations', limit], queryFn: ({ signal }) => api<Recommendation[]>(`/recommendations/users/${profile.id}?limit=${limit}`, { signal }), staleTime: 120_000 });
}
export function ForYouPage() {
  const { profile } = useProfile();
  const query = useRecommendations();
  return <><div className="page-heading"><p className="eyebrow">SEU GOSTO, NOVAS HISTÓRIAS</p><h1>Para você, {profile.name}.</h1><p>Escolhas que combinam com suas preferências. Só aparecem títulos que você ainda não avaliou.</p></div>{query.isPending ? <Skeleton count={6} /> : query.error ? <ErrorNotice message={query.error.message} retry={() => void query.refetch()} /> : query.data.length ? <div className="content-grid">{query.data.map(item => <RecommendationCard key={item.content.id} item={item} />)}</div> : <Empty title="Abra espaço para novas histórias" message="Busque e importe novos conteúdos. Se o TMDB estiver configurado, o catálogo também recebe candidatos populares automaticamente." />}</>;
}
export function JointRecommendationPage() {
  const { profiles, profile } = useProfile();
  const [selected, setSelected] = useState<number[]>([profile.id]);
  const [submitted, setSubmitted] = useState<number[]>([]);
  const query = useMutation({ mutationFn: (userIds: number[]) => api<Recommendation[]>('/recommendations/joint', jsonBody({ userIds, limit: 20 }, 'POST')) });
  const select = (id: number) => { setSelected(current => current.includes(id) ? current.filter(i => i !== id) : [...current, id]); query.reset(); };
  return <><div className="page-heading"><p className="eyebrow">O MELHOR DOS DOIS MUNDOS</p><h1>Quem vai assistir?</h1><p>Uma boa escolha para todo mundo. Cada modelo continua sendo individual.</p></div><div className="joint-profiles">{profiles.map(p => <button key={p.id} aria-pressed={selected.includes(p.id)} className={selected.includes(p.id) ? 'joint-profile selected' : 'joint-profile'} onClick={() => select(p.id)} disabled={query.isPending}><span className="mini-avatar">{p.name[0]}</span>{p.name}<span>{selected.includes(p.id) ? '✓' : '+'}</span></button>)}</div><button className="button" disabled={selected.length < 2 || query.isPending} onClick={() => { setSubmitted(selected); query.mutate(selected); }}><HeartHandshake size={19} />{query.isPending ? 'Buscando a sintonia…' : 'Encontrar algo para nós'}</button>{selected.length < 2 && <p className="muted">Selecione pelo menos dois perfis.</p>}{query.isPending && <Skeleton count={4} />}{query.error && <ErrorNotice message={query.error.message} retry={() => query.mutate(selected)} />}{query.data && <section className="section"><div className="section-heading"><h2>A noite de {profiles.filter(p => submitted.includes(p.id)).map(p => p.name).join(' + ')}</h2><span>Score conjunto</span></div>{query.data.length ? <div className="content-grid">{query.data.map(item => <div key={item.content.id}><RecommendationCard item={item} /><div className="individual-scores">{Object.entries(item.individualScores || {}).map(([id, score]) => <span key={id}>{profiles.find(p => p.id === Number(id))?.name}: {Math.round(score * 100)}%</span>)}</div></div>)}</div> : <Empty title="Precisamos de mais histórias" message="Importem novos conteúdos para encontrar opções que nenhum participante avaliou." />}</section>}</>;
}
