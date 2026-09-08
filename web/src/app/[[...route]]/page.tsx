import { Routes } from '@/components/routes';
export default async function Page({ params }: { params: Promise<{ route?: string[] }> }) { const { route } = await params; return <Routes route={route || []} />; }
