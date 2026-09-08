'use client';
import Link from 'next/link';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { api, apiBase, jsonBody } from '@/lib/api';
import { labels, type RatingView } from '@/lib/types';
import { useProfile } from './providers';
import { useModelStatus } from './model';
import { AutosaveRating, Empty, ErrorNotice, Skeleton } from './ui';
export function RatingsPage() {
  const { profile } = useProfile();
  const query = useQuery({ queryKey: ['user', profile.id, 'ratings'], queryFn: ({ signal }) => api<RatingView[]>(`/users/${profile.id}/ratings/view`, { signal }) });
  return <><div className="page-heading"><p className="eyebrow">O QUE JÁ PASSOU POR VOCÊ</p><h1>Minhas avaliações</h1><p>Seu repertório, do seu jeito. Você pode mudar de ideia a qualquer momento.</p></div>{query.isPending ? <Skeleton /> : query.error ? <ErrorNotice message={query.error.message} retry={() => void query.refetch()} /> : query.data.length ? <div className="rating-grid">{query.data.map(item => <div className="rating-row" key={item.rating.id}><div className="entity-name"><Link href={item.rating.targetType === 'MOVIE' || item.rating.targetType === 'SERIES' ? `/content/${item.rating.targetId}` : item.rating.targetType === 'GENRE' ? '/genres' : `/people/${item.rating.targetId}`}>{item.title}</Link><small>{labels[item.rating.targetType]}</small></div><AutosaveRating userId={profile.id} type={item.rating.targetType} id={item.rating.targetId} value={item.rating.value} label={`Avaliar ${item.title}`} /></div>)}</div> : <Empty title="Seu repertório começa com uma nota" message="Use a avaliação rápida ou busque algo que você já assistiu." />}</>;
}
export function SettingsPage() {
  const { profile, switchProfile } = useProfile();
  const status = useModelStatus();
  const client = useQueryClient();
  const update = useMutation({ mutationFn: (enabled: boolean) => api(`/users/${profile.id}/model/settings`, jsonBody({ autoTrainEnabled: enabled })), onSuccess: () => { void client.invalidateQueries({ queryKey: ['user', profile.id, 'model'] }); } });
  return <><div className="page-heading"><p className="eyebrow">DO SEU JEITO</p><h1>Configurações</h1></div><div className="settings-stack"><section className="panel"><h2>Seu perfil</h2><p>Você está avaliando como <strong>{profile.name}</strong>.</p><button className="button subtle" onClick={switchProfile}>Trocar perfil</button></section><section className="panel"><h2>Atualização do modelo</h2><p>Após o primeiro treino, atualize seu modelo automaticamente a cada 10 novos filmes ou séries avaliados.</p>{status.data && <label className="toggle-label"><input type="checkbox" checked={status.data.autoTrainEnabled} disabled={update.isPending} onChange={e => update.mutate(e.target.checked)} />Retreino automático</label>}{status.error && <ErrorNotice message={status.error.message} retry={() => void status.refetch()} />}{update.error && <ErrorNotice message={update.error.message} />}</section><section className="panel"><h2>Conexão</h2><p>Servidor: <code>{apiBase}</code></p><p className="muted">Na rede local, o computador e o celular precisam estar na mesma rede Wi-Fi.</p></section><section className="panel"><h2>Sobre o Lume</h2><p>Suas histórias favoritas, com um olhar pessoal. Dados e imagens fornecidos pelo TMDB.</p><p className="muted">Os perfis são para uma rede doméstica de confiança. A seleção de perfil não exige senha.</p></section></div></>;
}
