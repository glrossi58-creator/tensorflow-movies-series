'use client';
export default function ErrorPage({ reset }: { reset: () => void }) { return <main className="main"><h1>Algo interrompeu a sessão</h1><p>Suas avaliações salvas continuam no servidor.</p><button className="button" onClick={reset}>Tentar novamente</button></main>; }
