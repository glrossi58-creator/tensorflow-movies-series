# Lume Web

Next.js 16.3.4, App Router, TypeScript, Tailwind CSS e TanStack Query. O backend é a única fonte de regras e normalização.

```powershell
Copy-Item .env.example .env.local
npm install
npm run dev -- --hostname 0.0.0.0
```

No próprio PC: `http://localhost:3000`. Na rede: `http://IP_DO_PC:3000`. Para acessar pela LAN, configure `NEXT_PUBLIC_API_BASE_URL=http://IP_DO_PC:8080` e acrescente a origem Web exata em `CORS_ALLOWED_ORIGINS` do backend.

```powershell
npm run lint
npm test
npm run build
npm run start -- --hostname 0.0.0.0
```

`NEXT_PUBLIC_API_BASE_URL` é incorporado ao bundle no build; alterá-lo exige novo build de produção. Para deploy: `NEXT_PUBLIC_API_BASE_URL=https://api.seu-dominio`. O Next.js está preparado com build de produção compatível com npm run start e otimização de imagens restrita a `image.tmdb.org`. Não configure segredo TMDB no cliente.

Os testes Vitest cobrem perfis, busca, agrupamento, estrelas, avaliação de conteúdo/pessoa/gênero, fila rápida, limites do modelo, bloqueio de treino duplicado, recomendações conjuntas e autosave com coalescência/retry. `npm run test:e2e` usa Edge instalado e verifica o produto ativo em desktop e viewport móvel, sem alterar ratings históricos. Requer API/Web ativos com os perfis e The Matrix presentes.
