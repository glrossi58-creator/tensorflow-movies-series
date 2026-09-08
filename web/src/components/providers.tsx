'use client';
import { createContext, useContext, useEffect, useState, useSyncExternalStore, type ReactNode } from 'react';
import { QueryClient, QueryClientProvider, useQuery } from '@tanstack/react-query';
import { api } from '@/lib/api';
import type { Profile } from '@/lib/types';
import { ratingQueue } from '@/lib/ratings';
import { ProfileSelector } from './profiles';

const ProfileContext = createContext<{ profile: Profile; profiles: Profile[]; switchProfile: () => void } | null>(null);
function subscribeProfile(callback: () => void) { window.addEventListener('storage', callback); window.addEventListener('profile-change', callback); return () => { window.removeEventListener('storage', callback); window.removeEventListener('profile-change', callback); }; }
export function selectProfile(profile: Profile | null) {
  if (profile) localStorage.setItem('lume:profile', String(profile.id)); else localStorage.removeItem('lume:profile');
  window.dispatchEvent(new Event('profile-change'));
}
function ProfileGate({ children }: { children: ReactNode }) {
  const storedId = useSyncExternalStore(subscribeProfile, () => localStorage.getItem('lume:profile'), () => null);
  const profiles = useQuery({ queryKey: ['profiles'], queryFn: ({ signal }) => api<Profile[]>('/users', { signal }) });
  const profile = profiles.data?.find(p => String(p.id) === storedId);
  if (!profile) return <ProfileSelector profiles={profiles.data || []} loading={profiles.isPending} error={profiles.error?.message} retry={() => void profiles.refetch()} onSelect={selectProfile} />;
  return <ProfileContext.Provider value={{ profile, profiles: profiles.data || [], switchProfile: () => selectProfile(null) }}>{children}</ProfileContext.Provider>;
}
export function Providers({ children }: { children: ReactNode }) {
  const [client] = useState(() => new QueryClient({ defaultOptions: { queries: { staleTime: 30_000, retry: 1, refetchOnWindowFocus: true }, mutations: { retry: false } } }));
  useEffect(() => {
    ratingQueue.connect(id => { void client.invalidateQueries({ queryKey: ['user', id] }); });
    ratingQueue.restore();
  }, [client]);
  return <QueryClientProvider client={client}><ProfileGate>{children}</ProfileGate></QueryClientProvider>;
}
export function useProfile() { const value = useContext(ProfileContext); if (!value) throw new Error('Perfil não selecionado'); return value; }
