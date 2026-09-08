'use client';
import Link from 'next/link';
import { usePathname } from 'next/navigation';
import { Sparkles, ChevronDown, Settings, Heart, ListChecks } from 'lucide-react';
import type { ReactNode } from 'react';
import { useProfile } from './providers';
const links = [['/', 'Início'], ['/search', 'Buscar'], ['/quick', 'Avaliar'], ['/for-you', 'Para você'], ['/together', 'Para nós'], ['/model', 'Modelo']];
export function Shell({ children }: { children: ReactNode }) {
  const { profile, switchProfile } = useProfile();
  const path = usePathname();
  return <><a className="skip-link" href="#main">Pular para o conteúdo</a><header className="header"><Link href="/" className="brand"><span className="brand-icon"><Sparkles size={21} /></span>LUME</Link><nav aria-label="Principal">{links.map(([url, label]) => <Link key={url} href={url} className={path === url ? 'nav-active' : ''} aria-current={path === url ? 'page' : undefined}>{label}</Link>)}</nav><details className="profile-menu"><summary><span className="mini-avatar">{profile.name[0]}</span>{profile.name}<ChevronDown size={14} /></summary><div className="dropdown"><Link href="/genres"><Heart size={16} />Seus gostos</Link><Link href="/ratings"><ListChecks size={16} />Minhas avaliações</Link><Link href="/settings"><Settings size={16} />Configurações</Link><button onClick={switchProfile}>Trocar perfil</button></div></details></header><main className="main" id="main" key={profile.id}>{children}</main><footer className="footer"><span>LUME · Um próximo favorito para cada gosto.</span><span>Dados e imagens: <a href="https://www.themoviedb.org/" target="_blank" rel="noreferrer">TMDB</a>. Este produto usa a API do TMDB, mas não é endossado ou certificado pelo TMDB.</span></footer></>;
}
