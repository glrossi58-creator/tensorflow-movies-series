import type { Metadata } from 'next';
import { Providers } from '@/components/providers';
import './globals.css';
export const metadata: Metadata = { title: 'Lume · Seu próximo favorito', description: 'Filmes, séries e histórias que combinam com você. E com vocês.' };
export default function RootLayout({ children }: Readonly<{ children: React.ReactNode }>) { return <html lang="pt-BR"><body><Providers>{children}</Providers></body></html>; }
