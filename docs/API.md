# Contrato dos clientes

Todos os IDs abaixo são locais e devem vir das respostas da API. Nenhum cliente pressupõe IDs para Gil, Aliny ou conteúdos. A raiz da API é configurável; o TMDB é acessado exclusivamente pelo backend.

| Método | Endpoint | Resposta / uso |
|---|---|---|
| GET | `/health` | processo HTTP ativo; confirma-se banco também com `/users` |
| GET | `/users` | perfis existentes |
| POST | `/profiles` | `{ "name": "Nome" }`; perfil criado com ID real |
| GET | `/discovery/search?q=Matrix&type=MOVIE` | `{results,warning}`; tipos opcionais MOVIE, SERIES, PERSON, GENRE |
| POST | `/discovery/import/{type}/{tmdbId}` | importa MOVIE/SERIES/PERSON e retorna ID local |
| GET | `/users/{userId}/contents/{contentId}/evaluation` | conteúdo, `contentRating`, gêneros, atores, diretores e creators com notas |
| PUT | `/users/{userId}/contents/{contentId}/evaluation` | batch transacional e parcial |
| GET | `/users/{userId}/people/{personId}/evaluation` | pessoa e ratings separados por papel comprovado |
| GET | `/users/{userId}/genres` | gêneros e notas do perfil |
| PUT/POST | `/users/{userId}/ratings` | `{targetType,targetId,value}`; upsert e normalização central |
| GET | `/users/{userId}/ratings/view` | histórico com título e imagem de cada alvo |
| GET | `/users/{userId}/quick-rating` | fila local primeiro, depois populares TMDB; exclui avaliados |
| GET | `/users/{userId}/model/status` | estado, limites, perdas, versão, datas e contagens |
| PUT | `/users/{userId}/model/settings` | `{ "autoTrainEnabled": true }` |
| POST | `/recommendations/users/{userId}/train` | treino real; 400 antes do mínimo, 409 se já há treino |
| GET | `/recommendations/users/{userId}?limit=20` | scores individuais, estratégia e motivos |
| POST | `/recommendations/joint` | `{ "userIds": [idGil,idAliny], "limit": 20 }` |

Os endpoints anteriores de `/contents`, `/users`, ratings e importação específica continuam disponíveis.

Uma busca sem resultado local consulta o TMDB. Uma falha externa retorna `warning` compreensível e mantém resultados locais utilizáveis. Imports retornam os erros HTTP apropriados. Nenhuma resposta contém token ou corpo bruto de erro externo.

Exemplo de batch (todos os campos opcionais):

```json
{
  "contentRating": 5,
  "genres": [{"targetId": 71, "value": 4}],
  "actors": [{"targetId": 91, "value": 5}],
  "directors": [],
  "creators": []
}
```

Os alvos devem pertencer às relações daquele conteúdo. Uma falha invalida todo o batch. A mesma pessoa pode receber notas diferentes em ACTOR, DIRECTOR e CREATOR. CREATOR exige crédito real de criação, confirmado em `created_by`; Writer não é convertido em Creator.

`rating: null` significa não avaliado. Uma nota 1 retorna `normalizedValue: 0.0` e continua sendo uma avaliação válida. Escala: 1→0; 2→0,25; 3→0,5; 4→0,75; 5→1. Fora de 1..5, HTTP 400.

Recomendações retornam `score` em 0..1. O percentual visual representa afinidade do recomendador, não uma probabilidade calibrada. Na recomendação conjunta, `individualScores` é um mapa de ID do perfil para score. Conteúdos avaliados por qualquer participante são excluídos.
