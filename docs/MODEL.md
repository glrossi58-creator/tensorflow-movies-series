# Modelos individuais

TensorFlow Java **1.1.0** foi mantido, incluindo seu binário nativo Windows x86-64. O backend executa treinamento de regressão logística em um grafo TensorFlow com gradient descent e MSE, 150 épocas e seed 42. Os clientes apenas enviam avaliações e exibem respostas.

Cada usuário possui pesos, versão, lock, métricas e histórico de amostras próprios. Os arquivos versionados ficam em `MODEL_DIRECTORY/user-{id}-v{version}.properties`. A gravação usa arquivo temporário e move atômico quando suportado. A inferência aplica a função logística com esses pesos aprendidos.

## Labels e features

Somente avaliações MOVIE/SERIES formam labels supervisionados. ACTOR/DIRECTOR/CREATOR/GENRE expressam preferências e não contam para os limites.

As oito features preservadas são afinidade com gêneros, atores e direção/criação; médias de filmes, séries e histórico direto; popularidade; similaridade do embedding. Todas são limitadas a 0..1.

O split 80/20 acontece **antes** de calcular features. Features de treino usam apenas os ratings da partição de treino, removendo o próprio conteúdo-label. Features da validação usam apenas o histórico de treino. A popularidade do dataset exclui as notas do usuário treinado. O embedding de cada exemplo é reconstruído com esse mesmo histórico permitido. Um teste altera os labels de validação e confirma que nenhuma feature de treino muda.

O loss de validação em uma base de oito avaliações é uma medida inicial com poucas amostras. As preferências explícitas e o baseline continuam úteis nessa fase.

## Ciclo e persistência

| Contagem / evento | Estado |
|---|---|
| 0–7 ratings diretos | COLLECTING_DATA |
| 8+ sem modelo | READY |
| treino em andamento | TRAINING |
| modelo concluído sem mudanças concorrentes | TRAINED |
| mudanças após o treino | DIRTY |
| falha do treino ou arquivo inválido | ERROR |

25 é a base inicial recomendada, não um bloqueio ao treinamento. A configuração aceita `MODEL_MIN_DIRECT_RATINGS`, `MODEL_RECOMMENDED_DIRECT_RATINGS`, `MODEL_AUTO_TRAIN_ENABLED` e `MODEL_AUTO_TRAIN_NEW_DIRECT_RATINGS`.

A migration V3 cria `user_model_state` e `user_model_sample`. Um trigger aditivo registra revisões em inserções, alterações e exclusões de rating. As amostras persistidas por versão permitem distinguir dez **novos** ratings de dez alterações de uma nota antiga. Um usuário pode continuar avaliando durante o treino: mudanças de revisão deixam o resultado DIRTY.

O lock é adquirido por UPDATE condicional no PostgreSQL. Duas requisições do mesmo usuário não iniciam dois treinos. Uma execução interrompida por queda do processo é marcada ERROR após a expiração de 30 minutos. O agendador verifica a necessidade de retreino a cada 15 segundos; somente um modelo já treinado acumula retreino automático. Quando desabilitado, ele permanece DIRTY até o treino manual.

## pgvector e Kafka

Embeddings continuam `vector(8)`. O vetor de conteúdo é determinístico. Preferências diretas e de gêneros/pessoas contribuem para o vetor de usuário, com peso positivo ou negativo conforme a nota normalizada. Um perfil sem sinal, ou totalmente neutro, limpa o embedding anterior. A similaridade cosseno calculada pelo pgvector entra como sinal complementar.

O salvamento do rating, sua revisão de modelo, o embedding e a inclusão na outbox ocorrem em transação. Com Kafka habilitado, a outbox publica `rating.created` com chave `userId` e tenta novamente quando o broker está indisponível. A entrega admite repetição; o consumer recalcula o perfil sem criar outro rating nem executar treino por evento. O agendador controla o retreino. Com Kafka desabilitado, o caminho síncrono mantém ratings, embeddings e recomendações funcionais.

Recomendar carrega o modelo existente e **não treina**. Sem modelo, há baseline e cold start. Quando faltam candidatos locais, o backend tenta importar uma seleção popular real do TMDB. A recomendação conjunta usa 70% da média harmônica mais 30% do menor score individual, sem criar um terceiro modelo.
