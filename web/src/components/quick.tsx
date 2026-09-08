'use client';
import { useState } from 'react';
import Link from 'next/link';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { SkipForward, Zap } from 'lucide-react';
import { api, jsonBody } from '@/lib/api';
import { labels, type DiscoveryItem, type DiscoveryResponse } from '@/lib/types';
import { useProfile } from './providers';
import { Empty, ErrorNotice, Poster, Skeleton, StarRating } from './ui';
import { ModelProgress, useModelStatus } from './model';
export const quickKey = (item: DiscoveryItem) => item.source === 'LOCAL' ? `LOCAL:${item.id}` : `TMDB:${item.type}:${item.tmdbId}`;
export function QuickRatingPage() {
  const { profile } = useProfile();
  const client = useQueryClient();
  const [skipped, setSkipped] = useState<string[]>(() => { try { return JSON.parse(sessionStorage.getItem(`lume:quick:${profile.id}`) || '[]'); } catch { return []; } });
  const [choice, setChoice] = useState<number | null>(null);
  const status = useModelStatus();
  const query = useQuery({ queryKey: ['user', profile.id, 'quick'], queryFn: ({ signal }) => api<DiscoveryResponse>(`/users/${profile.id}/quick-rating`, { signal }), staleTime: 60_000 });
  const item = query.data?.results.find(i => !skipped.includes(quickKey(i)));
  const skip = (target: DiscoveryItem) => { const next = [...skipped, quickKey(target)]; setSkipped(next); sessionStorage.setItem(`lume:quick:${profile.id}`, JSON.stringify(next)); setChoice(null); };
  const save = useMutation({ mutationFn: async ({ target, value }: { target: DiscoveryItem; value: number }) => {
    const id = target.source === 'LOCAL' ? target.id : (await api<{ id: number }>(`/discovery/import/${target.type}/${target.tmdbId}`, { method: 'POST' })).id;
    await api(`/users/${profile.id}/ratings`, jsonBody({ targetType: target.type, targetId: id, value }));
    return target;
  }, onSuccess: target => { skip(target); void client.invalidateQueries({ queryKey: ['user', profile.id] }); } });
  return <><div className="page-heading"><p className="eyebrow"><Zap size={14} />POUCOS TOQUES, NOVAS DESCOBERTAS</p><h1>Avaliação rápida</h1><p>Já assistiu? Deixe sua nota. Cada história ajuda a conhecer seu gosto.</p></div><div className="quick-layout"><div>{query.isPending ? <Skeleton count={1} /> : query.error ? <ErrorNotice message={query.error.message} retry={() => void query.refetch()} /> : item ? <article className="quick-card"><Poster path={item.posterPath} title={item.title} priority /><div><span className="eyebrow">{labels[item.type]} · {item.releaseDate?.slice(0, 4) || 'NO SEU RITMO'}</span><h2>{item.title}</h2><StarRating value={choice} disabled={save.isPending} label={`Avaliar ${item.title}`} onChange={value => { setChoice(value); save.mutate({ target: item, value }); }} /><p className="save-status" aria-live="polite">{save.isPending ? 'Salvando…' : save.isSuccess ? '✓ Salvo. Vamos para a próxima.' : '1 Não gosto · 3 Neutro · 5 Gosto muito'}</p><button className="button subtle" disabled={save.isPending} onClick={() => { skip(item); save.reset(); }}><SkipForward size={17} />Não assisti</button>{save.error && <ErrorNotice message={`Não foi possível salvar. ${save.error.message}`} retry={() => choice && save.mutate({ target: item, value: choice })} />}</div></article> : <Empty title="Você chegou ao fim desta seleção" message="Explore outros títulos na busca ou volte depois para mais descobertas." />}{query.data?.warning && <ErrorNotice message={query.data.warning} retry={() => void query.refetch()} />}</div><aside className="panel quick-progress"><h2>Um pouco mais sobre você</h2>{status.data && <><ModelProgress status={status.data} /><p>{Math.max(0, status.data.recommendedDirectRatings - status.data.directRatingCount)} para a base inicial recomendada.</p></>}<p className="muted">Vale gostar muito, pouco ou não gostar. Dê a nota que faz sentido para você.</p><Link className="button subtle" href="/model">Ver meu modelo</Link></aside></div></>;
}
