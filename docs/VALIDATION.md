# Relatório de entrega e validação

Execução concluída em **8 de setembro de 2026**, Windows x86-64, Java 21, Node 22.13.1, Flutter 3.47.2 / Dart 3.13.2. Os resultados abaixo são de comandos realmente executados, sem interpretar testes skipped como aprovação.

## Resultado dos comandos

Comando completo executado com exit code **0**:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/build-all.ps1 -ApiBaseUrl http://10.165.120.163:8080 -AppBundle
```

| Etapa | Resultado |
|---|---|
| `gradlew.bat --no-daemon clean test` | **60 testes, 0 falhas, 0 erros, 0 skipped** |
| `gradlew.bat --no-daemon build` | **BUILD SUCCESSFUL** |
| `npm install` | Sucesso; lockfile presente |
| `npm run lint` | Sucesso |
| `npm test` | **18 testes aprovados** |
| `npm run build` | Sucesso; TypeScript e build Next.js de produção |
| `flutter pub get` | Sucesso |
| `flutter analyze` | **No issues found** |
| `flutter test` | **14 testes aprovados** |
| `flutter build apk --release` com API_BASE_URL | **APK gerado** |
| `flutter build appbundle --release` com API_BASE_URL | **AAB gerado** |
| `apksigner verify --print-certs` | Sucesso; certificado Android Debug |
| `start-local.ps1 -LanWeb` | Infraestrutura, backend e Web iniciados |
| `start-local.ps1 -ProductionWeb -LanWeb` | Build e servidores prontos, respostas HTTP 200 |
| Playwright / Edge | **4 cenários aprovados**, em desktop e viewport móvel, também no build de produção |
| `git diff --check` | Sem erros de whitespace |

Relatórios de teste ficam em `build/reports/tests/test/index.html`, `build/test-results/test/` e `web/test-results/`. Os cenários de navegador geram screenshots da avaliação em desktop/mobile.

## Backend e banco

O backend permaneceu na raiz, preservando WebFlux, Coroutines, R2DBC, Flyway, TMDB, Kafka, TensorFlow Java 1.1.0 e pgvector. Foram adicionadas busca universal/local, descoberta/importação real de pessoas, fila rápida diversificada, avaliação consolidada/batch, estado persistente dos modelos, treinamento manual/automático e outbox. O autosave usa UPSERT atômico, inclusive para duas primeiras gravações concorrentes no mesmo alvo.

Migration adicionada: `V3__profiles_discovery_and_model_state.sql`. Acrescenta papéis de pessoa, imagem/biografia, ordem de elenco, perfis ausentes, estado/revisões/amostras do modelo, trigger e índices. Migrations anteriores foram preservadas.

Verificação no banco histórico após aplicação:

- Gil continua com ID **4**; Aliny foi criada com ID **5**. Esses números são evidência deste banco, não constantes nos clientes.
- Permanecem **4 conteúdos**: IDs 1, 2 e 3 sem tmdb_id, e The Matrix no ID local **8**, tmdb_id **603**.
- Permanecem **3 ratings históricos**; testes de navegação não os alteraram.
- Flyway 0, 1, 2 e 3 com sucesso; volume PostgreSQL preservado.
- Nenhum DROP, reset de sequences ou remoção de volumes foi usado para a entrega.

## APIs adicionadas

`GET /health`, `POST /profiles`, `GET /discovery/search`, `POST /discovery/import/{type}/{tmdbId}`, `GET /users/{id}/quick-rating`, `GET/PUT /users/{id}/contents/{contentId}/evaluation`, `GET /users/{id}/people/{personId}/evaluation`, `GET /users/{id}/genres`, `GET /users/{id}/ratings/view`, `GET /users/{id}/model/status` e `PUT /users/{id}/model/settings`.

Endpoints anteriores de usuários, conteúdos, ratings, importação e recomendações foram preservados/evoluídos. Contratos e exemplos: [API.md](API.md).

## TensorFlow, pgvector e Kafka

Treino TensorFlow **real** foi executado nos testes nativos e de integração. Dataset individual, seed 42, split anterior às features, perdas de treino/validação, persistência/recarga de pesos e versões foram conferidos. Os testes verificam mínimo 8, recomendado 25, DIRTY, dez novos ratings para retreino, isolamento entre usuários, lock e recuperação de arquivo ausente.

Preferências de pessoas/gêneros não contam como labels. Foram testadas ausência de leakage de labels de validação, dimensão vector(8), atualização de embedding por preferência e remoção do vetor antigo quando neutralizado.

O Kafka real recebeu `rating.created` em um container de teste, com chave correspondente ao userId. O consumer foi testado para atualizar perfil sem iniciar outro ciclo de rating. A aplicação registra outbox transacional e mantém funcionamento com Kafka desabilitado; as integrações principais usam essa configuração opcional.

## Web, mobile e UX

Ambos implementam seleção/criação/troca de perfil, Home, busca universal, detalhes de conteúdo e pessoa, gêneros, avaliação rápida, histórico, modelo, recomendações individuais/conjuntas e configurações. A identidade própria LUME usa preto/roxo, estrelas acessíveis, feedback imediato, cache, skeletons, cancelamento de busca, retry e alvos de toque grandes.

As notas e modelos são indexados pelo ID real do perfil. A Web persiste a seleção localmente; Flutter usa SharedPreferences. Autosave agrupa toques rápidos, mantém escolhas com falha e não exige formulário geral. No mobile, rascunhos também ficam separados por servidor configurado.

Os testes Web cobrem os componentes requeridos e autosave/retry; Flutter cobre estado do perfil, DTOs/null, busca, estrelas, avaliação, fila rápida, limites de treinamento, cards de recomendação e isolamento da fila.

No Edge, usando a API real, foram validados seleção Gil/Aliny, busca local The Matrix, tela consolidada e notas após reload, 8/25, troca de perfil, Para nós, ausência de erros JavaScript e ausência de overflow horizontal. Os testes não inventaram avaliações no banco real.

## Artefatos Android

APK gerado e assinatura verificada:

```text
C:\dev\workspace\movie-recommendation\mobile\build\app\outputs\flutter-apk\app-release.apk
```

Tamanho: **54.744.863 bytes**. API compilada: **http://10.165.120.163:8080**.

AAB gerado:

```text
C:\dev\workspace\movie-recommendation\mobile\build\app\outputs\bundle\release\app-release.aab
```

Tamanho: **53.394.686 bytes**. Ambos usam a assinatura de desenvolvimento porque não foi fornecido keystore oficial. O APK é destinado à instalação direta para teste. A política Android permite HTTP somente ao host privado compilado; HTTPS não exige essa exceção.

## Limites reais da validação

1. **Token TMDB não estava configurado** no ambiente. Busca local e a mensagem de ausência do token foram verificadas ao vivo. Contratos/imports e erros TMDB foram testados com respostas controladas de teste; consulta/importação autenticada contra o TMDB real ainda requer preencher `.env` e reiniciar o backend.
2. **Nenhum Android estava conectado** em `flutter devices`. O APK foi construído e sua assinatura validada; instalação e uso no aparelho da Aliny dependem do teste físico descrito no runbook. O firewall do Windows não foi alterado automaticamente.
3. **Não houve build iOS**, pois o ambiente é Windows. O projeto iOS está presente; IPA exige macOS, Xcode e assinatura Apple apropriada.
4. Os modelos de teste foram treinados em bancos isolados. Não foram inventadas 25 notas para Gil/Aliny. A sessão completa com preferências reais dos dois, TMDB autenticado e Android físico ainda precisa ser realizada por eles.
5. O PostgreSQL histórico já apresentava aviso de collation 2.41/2.36. Nenhum dado foi removido nem houve manutenção automática de collation.

## COMO GIL E ALINY COMEÇAM A TESTAR

```powershell
cd C:\dev\workspace\movie-recommendation
if (-not (Test-Path .env)) { Copy-Item .env.example .env }
notepad .env
# Preencha TMDB_READ_ACCESS_TOKEN somente aqui.
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/stop-local.ps1
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/start-local.ps1 -LanWeb
```

Gil abre **http://localhost:3000**, seleciona Gil e busca The Matrix. Aliny instala o APK acima e seleciona Aliny, na mesma rede Wi-Fi. Confirme o IPv4 com `ipconfig`; se mudou, gere novo APK:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/build-android.ps1 -ApiBaseUrl http://IP_DO_PC:8080
```

Instalação manual, ADB, firewall, comandos Flutter/Next.js diretos e roteiro até treinar/receber recomendações: [RUNBOOK.md](RUNBOOK.md).
