# Registro de implementação

## Estado inicial (07/09/2026)

Backend Kotlin/Spring Boot na raiz; sem clientes Web/Mobile. PostgreSQL e Kafka já ativos.
Banco preservado: 4 conteúdos, 1 perfil, 3 ratings. Gil tem ID local 4; The Matrix (TMDB 603) tem ID 8. Há três conteúdos legados sem TMDB ID. Migrations existentes: baseline 0, V1, V2.

Problemas encontrados na análise: recomendação e consumer executavam treino completo; estado do modelo não persistido; popularidade do dataset incluía o label; embedding ignorava preferências de pessoas/gêneros; busca limitada a filmes; ausência de visão consolidada de avaliação; JSON inválido do TMDB sem tradução específica; outbox ainda não utilizada.

Plano de entrega: evoluir backend com migrations aditivas, completar Web, completar Flutter/Android/iOS, scripts e runbook, executar testes e validar os fluxos reais sem criar avaliações artificiais nos perfis de Gil ou Aliny.

Referências de implementação: [Next.js](https://nextjs.org/docs/app/getting-started/installation), [Flutter](https://docs.flutter.dev/install/archive), [TMDB pessoas](https://developer.themoviedb.org/reference/person-combined-credits).
