# Planejamento do front-end

Este documento liga o mapa de telas do guia (Figura 11) aos endpoints que a API já oferece e aos papéis do Spring Security. O objetivo é deixar decidido o que cada tela consome, para que a implementação seja só a camada de apresentação. As telas são implementadas uma por PR; o README diz quais já estão prontas.

## Mapa de telas

```
Login
  └── Dashboard
        ├── Clientes (lista / novo / editar / detalhe) ──► chamados do cliente
        ├── Categorias (lista / cadastro)
        ├── Técnicos (lista / cadastro)
        └── Chamados (lista / filtros / novo)
              └── Detalhe do chamado (status / técnico / comentários / histórico)
```

## Tela → endpoints → papel

| Tela | Endpoints que usa | Papel mínimo | Observações |
|---|---|---|---|
| Login | qualquer rota com HTTP Basic (ex.: `GET /clientes`) | — | Hoje não há rota de login: o front envia usuário e senha e trata o 401. Ver "Decisões em aberto". |
| Dashboard | `GET /dashboard` | ADMIN | Cartões com `total`, `pendentes` e `pendentesSemTecnico`; gráficos de `porStatus` e `pendentesPorPrioridade`. Os mapas sempre trazem todas as chaves, então o layout não muda quando um status está zerado. Para USUARIO/TECNICO, a tela inicial passa a ser a lista de chamados. |
| Clientes – lista | `GET /clientes` | USUARIO | Lista sem paginação (a API devolve todos). |
| Clientes – novo / editar | `POST /clientes`, `PUT /clientes/{id}` | USUARIO | Mostrar os `detalhes` do 400 ao lado de cada campo; 409 = e-mail já cadastrado. |
| Clientes – detalhe | `GET /clientes/{id}`, `GET /clientes/{id}/chamados?page=&size=` | USUARIO | É a seta tracejada do mapa: os chamados do cliente, paginados. Botão excluir só para ADMIN (409 se houver chamados). |
| Categorias – lista / cadastro | `GET /categorias`, `POST`/`PUT`/`DELETE /categorias/{id}` | leitura: USUARIO; escrita: ADMIN | O select de categoria do "Novo chamado" também usa o `GET`. |
| Técnicos – lista / cadastro | `GET /tecnicos`, `POST`/`PUT`/`DELETE /tecnicos/{id}` | leitura: USUARIO; escrita: ADMIN | `perfil` é um select com `ADMIN`/`TECNICO`. |
| Chamados – lista / filtros | `GET /chamados?status=&prioridade=&clienteId=&page=&size=&sort=` | USUARIO | Filtros viram selects (valores dos enums no README); paginação usa `page.totalPages` da resposta. |
| Chamados – novo | `POST /chamados`, `GET /clientes`, `GET /categorias` | USUARIO | Não pede status nem técnico: o chamado nasce `ABERTO` e sem técnico. |
| Detalhe do chamado | `GET /chamados/{id}` (já traz os comentários), `GET /chamados/{id}/historico`, `POST /chamados/{id}/comentarios`, `PATCH /chamados/{id}/status`, `PATCH /chamados/{id}/atribuir`, `GET /tecnicos` | USUARIO (ver); TECNICO (status); ADMIN (atribuir) | Mostrar só as ações que o papel permite. Com status `FECHADO`, esconder todas as ações (a API devolveria 409). O histórico vira uma linha do tempo. |

## Como tratar as respostas de erro

Todas as respostas de erro têm o mesmo formato (`ErroResposta`), então um único tratador no front-end atende todas as telas:

| Status | O que a tela faz |
|---|---|
| 400 | Mostra cada item de `detalhes` (`campo: mensagem`) junto do campo |
| 401 | Volta para o login |
| 403 | Mensagem "sem permissão" (não deveria acontecer se a tela esconde as ações certas) |
| 404 | Página "não encontrado" com link para a lista |
| 409 | Mostra `erro` (duplicidade, chamado fechado, exclusão com vínculos) |

## Decisões

1. **Tecnologia: Thymeleaf (decidido).** O `pom.xml` já tinha Thymeleaf. As telas ficam em `@Controller` no pacote `web`, em `/app/...`, e chamam os **mesmos Services** da API, então nenhuma regra de negócio é duplicada. A alternativa era uma SPA (React/Vue/Angular) consumindo a API REST, que separa melhor o front, mas exigiria outro projeto e um login por token.
2. **Login: formulário com sessão (decidido).** HTTP Basic serve para a API e o Swagger, mas não para telas. Há uma segunda `SecurityFilterChain` só para as telas, com `formLogin`, sessão e CSRF ligado; a da API continua stateless. Os usuários e papéis são os mesmos.
3. **Usuários (em aberto).** Hoje os três usuários estão em memória. Para cada técnico logar com a própria conta, `Tecnico` precisaria de um campo de senha e o `UserDetailsService` passaria a ler a tabela `tecnicos`. Com isso o `autor` do comentário poderia vir do usuário logado em vez de ser digitado.
