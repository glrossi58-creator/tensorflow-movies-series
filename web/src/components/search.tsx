'use client';
import { useEffect, useState } from 'react';
import { useQuery, useMutation } from '@tanstack/react-query';
import { useRouter } from 'next/navigation';
import { Search as SearchIcon, ArrowUpRight } from 'lucide-react';
import { api } from '@/lib/api';
import type { DiscoveryItem, DiscoveryResponse, DiscoveryType } from '@/lib/types';
import { labels } from '@/lib/types';
import { Empty, ErrorNotice, Poster, Skeleton } from './ui';

export function SearchResults({ results, open, importing }: { results: DiscoveryItem[]; open: (item: DiscoveryItem) => void; importing?: boolean }) {
  const groups: [DiscoveryType, string][] = [['MOVIE', 'Filmes'], ['SERIES', 'Séries'], ['PERSON', 'Pessoas'], ['GENRE', 'Gêneros']];
  return <>{groups.map(([type, title]) => {
    const items = results.filter(item => item.type === type);
    return items.length ? <section key={type} className="section"><div className="section-heading"><h2>{title}</h2><span>{items.length} resultados</span></div><div className="content-grid">{items.map(item => <button className="content-card" key={`${item.source}:${item.type}:${item.id || item.tmdbId}`} onClick={() => open(item)} disabled={importing}><Poster path={item.posterPath} title={item.title} person={type === 'PERSON'} /><div className="card-copy"><span className={`source ${item.source === 'LOCAL' ? 'local-source' : ''}`}>{item.source === 'LOCAL' ? 'No catálogo' : 'TMDB'}</span><h3>{item.title}</h3><p>{item.releaseDate?.slice(0, 4) || labels[type]}{item.roles?.length > 0 && ` · ${item.roles.map(role => labels[role]).join(', ')}`}</p><span className="card-action">{item.source === 'TMDB' ? 'Importar e avaliar' : 'Abrir e avaliar'}<ArrowUpRight size={14} /></span></div></button>)}</div></section> : null;
  })}</>;
}
export function SearchPage() {
  const router = useRouter();
  const [query, setQuery] = useState('');
  const [debounced, setDebounced] = useState('');
  const [type, setType] = useState('');
  useEffect(() => { const timer = setTimeout(() => setDebounced(query.trim()), 350); return () => clearTimeout(timer); }, [query]);
  const result = useQuery({ queryKey: ['search', debounced, type], enabled: debounced.length >= 2, queryFn: ({ signal }) => api<DiscoveryResponse>(`/discovery/search?q=${encodeURIComponent(debounced)}${type ? `&type=${type}` : ''}`, { signal }), staleTime: 120_000 });
  const go = (item: DiscoveryItem) => router.push(item.type === 'PERSON' ? `/people/${item.id}` : item.type === 'GENRE' ? `/genres?focus=${item.id}` : `/content/${item.id}`);
  const importer = useMutation({ mutationFn: async (item: DiscoveryItem) => {
    const imported = await api<{ id: number }>(`/discovery/import/${item.type}/${item.tmdbId}`, { method: 'POST' });
    return { ...item, id: imported.id, source: 'LOCAL' as const };
  }, onSuccess: go });
  return <><div className="page-heading"><p className="eyebrow">SIGA A SUA CURIOSIDADE</p><h1>O que você tem em mente?</h1><p>Encontre uma história, um rosto conhecido ou um novo gosto.</p></div><label className="search-field"><SearchIcon size={23} /><input aria-label="Busca universal" placeholder="Busque filmes, séries, atores, diretores, creators..." value={query} maxLength={150} onChange={e => setQuery(e.target.value)} autoFocus /><kbd>BUSCAR</kbd></label><div className="filters" aria-label="Filtrar busca">{[['', 'Tudo'], ['MOVIE', 'Filmes'], ['SERIES', 'Séries'], ['PERSON', 'Pessoas'], ['GENRE', 'Gêneros']].map(([value, text]) => <button key={value} className={type === value ? 'filter-active' : ''} aria-pressed={type === value} onClick={() => setType(value)}>{text}</button>)}</div>
    {importer.isPending && <p role="status" className="notice">Importando detalhes e créditos…</p>}{importer.error && <ErrorNotice message={importer.error.message} />}
    {debounced.length < 2 ? <Empty title="Sua próxima descoberta começa aqui" message="Digite pelo menos 2 caracteres. O catálogo local tem prioridade." /> : result.isPending || query.trim() !== debounced ? <Skeleton count={6} /> : result.error ? <ErrorNotice message={result.error.message} retry={() => void result.refetch()} /> : <>{result.data?.warning && <ErrorNotice message={result.data.warning} retry={() => void result.refetch()} />}{result.data?.results.length ? <SearchResults results={result.data.results} importing={importer.isPending} open={item => item.source === 'TMDB' ? importer.mutate(item) : go(item)} /> : <Empty title="Nenhum resultado por aqui" message="Tente outro nome ou ajuste o filtro." />}</>}
  </>;
}
