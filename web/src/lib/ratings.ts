'use client';
import { api, jsonBody } from './api';
import type { Rating, TargetType } from './types';

export interface Draft { userId: number; targetType: TargetType; targetId: number; value: number; status: 'saving' | 'saved' | 'error'; error?: string; revision: number }
type Listener = () => void;
export class RatingQueue {
  private drafts = new Map<string, Draft>();
  private timers = new Map<string, ReturnType<typeof setTimeout>>();
  private active = new Set<string>();
  private listeners = new Set<Listener>();
  private invalidate: (id: number) => void = () => {};
  subscribe = (listener: Listener) => { this.listeners.add(listener); return () => { this.listeners.delete(listener); }; };
  get = (key: string) => this.drafts.get(key);
  key = (user: number, type: TargetType, id: number) => `${user}:${type}:${id}`;
  connect(invalidate: (id: number) => void) { this.invalidate = invalidate; }
  restore() {
    try {
      const values = JSON.parse(localStorage.getItem('lume:rating-drafts') || '[]') as Draft[];
      for (const draft of values) if (draft.value >= 1 && draft.value <= 5) {
        this.drafts.set(this.key(draft.userId, draft.targetType, draft.targetId), { ...draft, status: 'error', error: 'Avaliação pendente. Tente salvar novamente.' });
      }
      this.emit();
    } catch { /* Browsers with storage disabled still support in-memory autosave. */ }
  }
  choose(userId: number, targetType: TargetType, targetId: number, value: number) {
    const key = this.key(userId, targetType, targetId);
    this.drafts.set(key, { userId, targetType, targetId, value, status: 'saving', revision: (this.drafts.get(key)?.revision || 0) + 1 });
    this.emit();
    clearTimeout(this.timers.get(key));
    this.timers.set(key, setTimeout(() => { void this.flush(key); }, 350));
  }
  retry(key: string) { const draft = this.drafts.get(key); if (draft) { this.drafts.set(key, { ...draft, status: 'saving' }); this.emit(); void this.flush(key); } }
  private emit() {
    try { localStorage.setItem('lume:rating-drafts', JSON.stringify([...this.drafts.values()].filter(d => d.status !== 'saved'))); } catch { }
    this.listeners.forEach(listener => listener());
  }
  async flush(key: string) {
    if (this.active.has(key)) return;
    const draft = this.drafts.get(key);
    if (!draft || draft.status === 'saved') return;
    this.active.add(key);
    try {
      await api<Rating>(`/users/${draft.userId}/ratings`, jsonBody({ targetType: draft.targetType, targetId: draft.targetId, value: draft.value }));
      if (this.drafts.get(key)?.revision === draft.revision) this.drafts.set(key, { ...draft, status: 'saved' });
      this.invalidate(draft.userId);
    } catch (e) {
      if (this.drafts.get(key)?.revision === draft.revision) this.drafts.set(key, { ...draft, status: 'error', error: e instanceof Error ? e.message : 'Não foi possível salvar' });
    } finally {
      this.active.delete(key);
      this.emit();
      if (this.drafts.get(key)?.revision !== draft.revision) void this.flush(key);
    }
  }
}
export const ratingQueue = new RatingQueue();
