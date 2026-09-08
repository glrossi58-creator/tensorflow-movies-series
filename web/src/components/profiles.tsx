'use client';
import { useState } from 'react';
import Link from 'next/link';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { Plus, Sparkles } from 'lucide-react';
import { api, jsonBody } from '@/lib/api';
import type { Profile } from '@/lib/types';
import { ErrorNotice, Skeleton } from './ui';

export function ProfileSelector({ profiles, loading, error, retry, onSelect }: { profiles: Profile[]; loading?: boolean; error?: string; retry: () => void; onSelect: (p: Profile) => void }) {
  const [creating, setCreating] = useState(false);
  const [name, setName] = useState('');
  const client = useQueryClient();
  const create = useMutation({ mutationFn: () => api<Profile>('/profiles', jsonBody({ name: name.trim() }, 'POST')), onSuccess: async p => { await client.invalidateQueries({ queryKey: ['profiles'] }); onSelect(p); } });
  return <main className="profile-screen"><Link className="brand" href="/"><span className="brand-icon"><Sparkles size={22} /></span>LUME<span className="brand-caption">seu próximo favorito</span></Link>
    <div className="profile-intro"><p className="eyebrow">CADA GOSTO, UMA DESCOBERTA</p><h1>Quem está avaliando?</h1><p>Um espaço para o seu gosto. E para o que vocês têm em comum.</p></div>
    {loading ? <Skeleton count={2} /> : error ? <ErrorNotice message={error} retry={retry} /> : <div className="profile-grid">{profiles.map((p, i) => <button className="profile-card" key={p.id} onClick={() => onSelect(p)}><span className={`avatar avatar-${i % 3}`}>{p.name.slice(0, 1).toUpperCase()}</span><strong>{p.name}</strong><span>Entrar no perfil</span></button>)}<button className="profile-card new-profile" onClick={() => setCreating(true)}><span className="avatar"><Plus size={38} /></span><strong>Criar perfil</strong><span>Um novo olhar</span></button></div>}
    {creating && <form className="create-profile panel" onSubmit={e => { e.preventDefault(); if (name.trim()) create.mutate(); }}><label htmlFor="profile-name">Como você se chama?</label><div className="inline-form"><input autoFocus id="profile-name" maxLength={150} value={name} onChange={e => setName(e.target.value)} placeholder="Seu nome" required /><button className="button" disabled={create.isPending || !name.trim()}>{create.isPending ? 'Criando…' : 'Criar e entrar'}</button></div>{create.error && <ErrorNotice message={create.error.message} />}</form>}
    <p className="profile-footnote">Suas notas e seu modelo são exclusivos do seu perfil.</p>
  </main>;
}
