'use client';
import Image from 'next/image';
import { Film, RefreshCw, Star } from 'lucide-react';
import { useSyncExternalStore } from 'react';
import { ratingQueue } from '@/lib/ratings';
import type { TargetType } from '@/lib/types';
export function Skeleton({ count = 4 }: { count?: number }) { return <div className="skeleton-grid" role="status" aria-label="Carregando">{Array.from({ length: count }, (_, i) => <div className="skeleton" key={i} />)}</div>; }
export function ErrorNotice({ message, retry }: { message: string; retry?: () => void }) { return <div role="alert" className="error-notice"><span>{message}</span>{retry && <button onClick={retry} className="button subtle"><RefreshCw size={16} />Tentar novamente</button>}</div>; }
export function Empty({ title, message }: { title: string; message: string }) { return <div className="empty"><Film size={32} /><h3>{title}</h3><p>{message}</p></div>; }
export function Poster({ path, title, priority = false, person = false }: { path?: string | null; title: string; priority?: boolean; person?: boolean }) {
  return <div className={`poster ${person ? 'person-poster' : ''}`}>{path ? <Image src={`https://image.tmdb.org/t/p/w500${path}`} alt={title} fill sizes="(max-width: 600px) 45vw, (max-width: 1000px) 25vw, 220px" priority={priority} /> : <div className="poster-placeholder"><Film size={32} /><span>{title}</span></div>}</div>;
}
const meanings = ['Não gosto', 'Gosto pouco', 'Neutro', 'Gosto', 'Gosto muito'];
export function StarRating({ value, onChange, label = 'Sua nota', disabled = false }: { value: number | null; onChange: (value: number) => void; label?: string; disabled?: boolean }) {
  return <div className="stars" role="group" aria-label={label}>{meanings.map((meaning, i) => <button key={meaning} type="button" disabled={disabled} aria-label={`${i + 1} ${i === 0 ? 'estrela' : 'estrelas'}: ${meaning}`} aria-pressed={value === i + 1} title={meaning} onClick={() => onChange(i + 1)}><Star size={26} fill={value !== null && i < value ? 'currentColor' : 'none'} className={value !== null && i < value ? 'star-active' : ''} /></button>)}</div>;
}
export function AutosaveRating({ userId, type, id, value, label }: { userId: number; type: TargetType; id: number; value: number | null; label: string }) {
  const key = ratingQueue.key(userId, type, id);
  const draft = useSyncExternalStore(ratingQueue.subscribe, () => ratingQueue.get(key), () => undefined);
  return <div className="autosave"><StarRating label={label} value={draft?.value ?? value} onChange={v => ratingQueue.choose(userId, type, id, v)} /><span className={`save-status ${draft?.status === 'error' ? 'save-error' : ''}`} aria-live="polite">{draft?.status === 'saving' ? 'Salvando…' : draft?.status === 'saved' ? '✓ Salvo' : draft?.status === 'error' ? <><span title={draft.error}>Não foi possível salvar</span> <button onClick={() => ratingQueue.retry(key)}>Tentar novamente</button></> : value === null ? 'Ainda não avaliado' : 'Sua avaliação'}</span></div>;
}
