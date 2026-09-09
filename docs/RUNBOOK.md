# Executar LUME no Windows 11

Este guia parte do projeto existente e preserva seu PostgreSQL. Para um clone novo, execute `git clone URL_DO_SEU_REPOSITORIO movie-recommendation`, entre na pasta e siga os mesmos passos. Não remova volumes para resolver falhas.

## 1. Pré-requisitos e ferramentas

- JDK 21 no `JAVA_HOME` e PATH.
- Docker Desktop iniciado, usando Linux containers.
- Node.js compatível com o projeto (validado com 22.13.1) e npm.
- Flutter estável, Android SDK e licenças aceitas. Este workspace dispõe de `.tools/flutter` e `.tools/android-sdk`; essas instalações não são versionadas. Num clone novo, instale Flutter e Android SDK antes de construir o app.
- Token de leitura TMDB para procurar/importar títulos externos e carregar populares.
- PC e Android na mesma rede Wi-Fi, sem isolamento de clientes/rede de convidados.

```powershell
cd C:\dev\workspace\movie-recommendation
java -version
docker info
```

Os scripts detectam ferramentas em `.tools` e o Node instalado pelo gerenciador de versões. Para usar comandos manuais na mesma sessão:

```powershell
Set-ExecutionPolicy -Scope Process -ExecutionPolicy Bypass
. .\scripts\common.ps1
Initialize-Toolchain
node --version
flutter --version
flutter doctor
```

ExecutionPolicy acima vale apenas para essa sessão. Os comandos com `powershell -NoProfile -ExecutionPolicy Bypass -File` também funcionam sem alterar a política permanente.

## 2. Configurar o backend

```powershell
if (-not (Test-Path .env)) { Copy-Item .env.example .env }
notepad .env
```

Preencha `TMDB_READ_ACCESS_TOKEN` com seu token de leitura. Não o coloque em `web/.env.local`, `NEXT_PUBLIC_*`, Dart ou scripts versionados. Os scripts carregam `.env` sem executar seu conteúdo. Variáveis já exportadas no processo têm precedência.

Mantenha credenciais e URLs correspondentes ao banco existente. A senha em `.env` não altera automaticamente a senha de um volume PostgreSQL já inicializado.

```dotenv
DB_USERNAME=postgres
DB_PASSWORD=postgres
DB_R2DBC_URL=r2dbc:postgresql://localhost:5432/movie_recommendation
DB_JDBC_URL=jdbc:postgresql://localhost:5432/movie_recommendation
KAFKA_ENABLED=true
KAFKA_BOOTSTRAP_SERVERS=localhost:9092
MODEL_DIRECTORY=./data/models
MODEL_MIN_DIRECT_RATINGS=8
MODEL_RECOMMENDED_DIRECT_RATINGS=25
MODEL_AUTO_TRAIN_ENABLED=true
MODEL_AUTO_TRAIN_NEW_DIRECT_RATINGS=10
SERVER_ADDRESS=0.0.0.0
SERVER_PORT=8080
CORS_ALLOWED_ORIGINS=http://localhost:3000
```

Sem token, o catálogo local e as avaliações funcionam. Não será possível completar a descoberta externa apenas com os poucos títulos históricos: configure o token antes da sessão completa de 25 avaliações.

## 3. Descobrir o endereço do PC

```powershell
ipconfig
```

Use o IPv4 da interface Wi-Fi/Ethernet ativa, por exemplo `192.168.1.100`. Substitua `IP_DO_PC` nos comandos. Não use a interface Docker/WSL. No momento da validação deste workspace, o IPv4 era **10.165.120.163**; confirme se continua válido.

API: `http://IP_DO_PC:8080`. Web: `http://IP_DO_PC:3000`. `localhost` no celular refere-se ao próprio celular. O padrão Flutter `10.0.2.2` destina-se ao emulador Android.

## 4. Iniciar tudo

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/start-local.ps1 -LanWeb
```

O script valida Docker, inicia PostgreSQL/Kafka, configura API e CORS para o IPv4 detectado e abre backend/Web como processos ocultos. Logs: `.run/backend.log`, `.run/backend.error.log`, `.run/web.log` e `.run/web.error.log`. Portas ocupadas são preservadas; se um processo anterior tiver outra configuração, pare-o primeiro.

Confirme a inicialização:

```powershell
Invoke-RestMethod http://localhost:8080/health
Invoke-RestMethod http://localhost:8080/users
(Invoke-WebRequest -UseBasicParsing http://localhost:3000).StatusCode
```

Gil abre **http://localhost:3000** e seleciona **Gil**. Outro dispositivo abre **http://IP_DO_PC:3000**.

Para terminais separados:

```powershell
# Terminal 1
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/start-infra.ps1
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/start-backend.ps1
```

```powershell
# Terminal 2
$env:NEXT_PUBLIC_API_BASE_URL = 'http://IP_DO_PC:8080'
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/start-web.ps1
```

Nesse modo, acrescente `http://IP_DO_PC:3000` ao `CORS_ALLOWED_ORIGINS` do backend. Para uso somente no PC, a Web pode apontar a `http://localhost:8080`.

Sem Kafka, defina `KAFKA_ENABLED=false` no `.env` e use `start-local.ps1 -WithoutKafka -LanWeb`. Ratings, embeddings e recomendações continuam ativos.

## 5. Firewall e rede local

Teste no navegador do Android: **http://IP_DO_PC:8080/health**. Se o PC responde mas o celular não conecta, confira Wi-Fi, IPv4 e firewall. Caso necessário, execute estas regras em PowerShell **como administrador**, apenas para rede Privada e sub-rede local:

```powershell
New-NetFirewallRule -DisplayName 'LUME API LAN' -Direction Inbound -Action Allow -Protocol TCP -LocalPort 8080 -Profile Private -RemoteAddress LocalSubnet
New-NetFirewallRule -DisplayName 'LUME Web LAN' -Direction Inbound -Action Allow -Protocol TCP -LocalPort 3000 -Profile Private -RemoteAddress LocalSubnet
```

Essas regras não foram criadas automaticamente. A segunda só é necessária para Web por outro dispositivo. Não abra portas no roteador para este uso local.

## 6. Gerar e instalar o aplicativo da Aliny

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/build-android.ps1 -ApiBaseUrl http://IP_DO_PC:8080
```

O script executa pub get, analyze, tests e build release. Saída: `mobile/build/app/outputs/flutter-apk/app-release.apk`.

O APK entregue neste workspace foi compilado para **http://10.165.120.163:8080**. Gere outro se o IPv4 mudar. Uma reserva DHCP no roteador pode manter o endereço do PC estável.

1. Copie `app-release.apk` para o celular.
2. Abra o APK.
3. Permita a instalação pela fonte usada, quando o Android solicitar.
4. Instale e abra **Lume**.
5. Selecione **Aliny**.

Via USB, com depuração USB habilitada e computador autorizado:

```powershell
.\.tools\android-sdk\platform-tools\adb.exe devices
.\.tools\android-sdk\platform-tools\adb.exe install -r .\mobile\build\app\outputs\flutter-apk\app-release.apk
```

Com Android SDK no PATH: `adb install -r caminho-do-apk`.

Comandos Flutter diretos após inicializar as ferramentas da seção 1:

```powershell
cd mobile
flutter pub get
flutter analyze
flutter test
flutter build apk --release --dart-define=API_BASE_URL=http://IP_DO_PC:8080
flutter devices
flutter run --dart-define=API_BASE_URL=http://IP_DO_PC:8080
```

O Android permite HTTP apenas para o host privado compilado. A configuração base bloqueia cleartext; builds HTTPS não recebem essa exceção. Em Configurações, pode-se informar uma URL HTTPS. Mudar o IP de uma API HTTP requer recompilar o APK.

## 7. Assinatura e Play Store

O APK e AAB de teste usam a chave debug quando `mobile/android/key.properties` não existe. São builds release para teste, **sem assinatura oficial de publicação**. O AAB atual não é uma release oficial da Play Store.

Para assinatura própria, crie um keystore e `mobile/android/key.properties`, ignorado pelo Git:

```properties
storePassword=SENHA_DO_KEYSTORE
keyPassword=SENHA_DA_CHAVE
keyAlias=upload
storeFile=C:/caminho/seguro/upload-keystore.jks
```

Guarde a chave e suas senhas fora do repositório. Depois:

```powershell
cd mobile
flutter build appbundle --release --dart-define=API_BASE_URL=https://api.seu-dominio
```

Saída: `mobile/build/app/outputs/bundle/release/app-release.aab`. O script `build-android.ps1 -ApiBaseUrl ... -AppBundle` gera ambos os formatos. Trocar a chave de uma instalação de teste pode exigir desinstalar o APK antigo.

## 8. Primeiro uso pelos dois

Gil seleciona Gil na Web, busca **The Matrix** e abre **No catálogo**. Avalia conteúdo, gêneros, atores e direção. As estrelas mostram Salvando e Salvo; ao reabrir, as notas continuam no backend.

Com token TMDB configurado, busque outro título, escolha **Importar e avaliar** e repita. Séries mostram creators reais. Pessoas oferecem notas separadas por papel; **Atualizar foto e papéis do TMDB** complementa créditos ausentes no catálogo.

Abra **Avaliação rápida**. Pular não cria nota. Em 8 filmes/séries o treino experimental fica disponível; tente chegar a 25 com notas que representem seu gosto. Pessoas/gêneros ajudam as features sem inflar essa contagem.

Em **Modelo**, pressione **Treinar agora** e aguarde TRAINED. Confira métricas e abra **Para você**. Aliny repete no Android selecionando Aliny, podendo dar outra nota ao mesmo filme. O treino dela é independente.

Depois do primeiro treino, notas novas deixam o modelo DIRTY. Com auto-treino ativo, dez novos conteúdos avaliados disparam atualização no ciclo periódico do servidor. Editar a mesma nota não incrementa esse total.

Abra **Para nós**, selecione Gil e Aliny e escolha **Encontrar algo para nós**. O backend combina scores individuais.

## 9. Web: desenvolvimento e produção

```powershell
cd web
npm install
$env:NEXT_PUBLIC_API_BASE_URL = 'http://IP_DO_PC:8080'
npm run dev -- --hostname 0.0.0.0
```

Produção local, depois de parar o servidor de desenvolvimento:

```powershell
npm run lint
npm test
npm run build
npm run start -- --hostname 0.0.0.0
```

Acesso: `http://localhost:3000` ou `http://IP_DO_PC:3000`. A variável pública é incorporada no build: faça novo build ao mudar a API. Alternativa: `start-local.ps1 -ProductionWeb -LanWeb`.

Para deploy, configure `NEXT_PUBLIC_API_BASE_URL=https://api.seu-dominio` antes de construir a Web. Use hospedagem compatível com Next.js e API atrás de reverse proxy com TLS (por exemplo, Caddy/Nginx). CORS deve conter a origem HTTPS exata da Web. Não exponha diretamente 8080 na internet. Adicione autenticação/controle de acesso antes de publicar perfis e avaliações; a fase atual foi desenhada para rede local confiável.

## 10. Testes e build completo

Pare Web/backend para evitar uso simultâneo dos arquivos de build. Mantenha Docker ativo:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/stop-local.ps1
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/build-all.ps1 -ApiBaseUrl http://IP_DO_PC:8080 -AppBundle
```

Comandos individuais, na raiz com ferramentas disponíveis:

```powershell
.\gradlew.bat --no-daemon clean test
.\gradlew.bat --no-daemon build
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/web.ps1 -Action install
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/web.ps1 -Action lint
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/web.ps1 -Action test
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/web.ps1 -Action build
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/mobile.ps1 -Action get
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/mobile.ps1 -Action analyze
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/mobile.ps1 -Action test
```

`--no-daemon` evita reutilizar um processo com ambiente/permissões antigos. `.\gradlew.bat clean test` e `.\gradlew.bat build` também são suportados. Integrações usam PostgreSQL/pgvector e Kafka reais em containers de teste; não o banco de Gil/Aliny. Confira se não houve skipped.

Com Web/API novamente ativas, os testes de navegador usam Edge instalado:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/start-local.ps1 -LanWeb
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/web.ps1 -Action e2e
```

Esses cenários conferem perfis, The Matrix local, notas persistidas, modelo e Para nós sem modificar ratings históricos. Relatórios: `build/reports/tests/test/index.html` e `web/test-results/`.

## 11. iOS

`mobile/ios` está preparado com código compartilhado e configuração de rede local. Nenhum IPA é gerado em Windows. Em macOS, instale Xcode, configure bundle identifier/equipe/assinatura Apple, resolva dependências e execute `flutter build ipa --release --dart-define=API_BASE_URL=https://api.seu-dominio`. Publicação/instalação conforme o canal exige a conta/assinatura Apple aplicável.

## 12. Parada e troubleshooting

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/stop-local.ps1
# Também parar containers, preservando volumes:
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/stop-local.ps1 -StopInfra
```

O script encerra apenas processos registrados por ele. Serviços abertos manualmente devem ser encerrados nos próprios terminais. Preserve o volume PostgreSQL e `data/models`. Faça backup antes de intervenções no banco; migrations são aditivas.

| Sintoma | Ação |
|---|---|
| Docker não responde | Inicie Docker Desktop, aguarde e confirme `docker info` |
| TMDB não configurado / 401 / 403 | Configure token no `.env` do backend e reinicie-o |
| TMDB 429, timeout ou indisponível | Aguarde e tente novamente; catálogo local continua disponível |
| Celular sem conexão | Teste `/health`, confira IP/Wi-Fi/firewall/rede de convidados |
| CORS na Web | Ajuste origem exata em CORS_ALLOWED_ORIGINS e reinicie backend |
| HTTP bloqueado no Android | Recompile com o IPv4 correto em API_BASE_URL |
| Porta ocupada | Confira `.run` e a configuração do processo que você iniciou |
| Autosave falhou | Mantenha a escolha e use Tentar novamente após reconectar |
| Poucos títulos | Configure TMDB e use busca/importação; não crie ratings fictícios |
| Treino indisponível | Complete 8 notas diretas; pessoas/gêneros não contam |
| Modelo ERROR / arquivo ausente | Verifique MODEL_DIRECTORY e treine novamente |
| Treino interrompido por reinício | TRAINING abandonado é recuperado após 30 minutos |
| npm sem Node ativo | Ative um runtime ou use os scripts que detectam o instalado |
| Integrações skipped | Ative Docker e repita com `--no-daemon` |

O banco histórico deste workspace já emitia aviso de versão de collation diferente (2.41/2.36). Os dados foram preservados; não houve reconstrução automática de índices nem alteração de collation. Planeje essa manutenção com backup se o aviso persistir.
