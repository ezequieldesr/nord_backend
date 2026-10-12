> Atualização de integração: o frontend oficial agora é o projeto Next.js e-commerce-de-moda, na porta 3000. O Nord serve somente /api na porta 8080. As referências abaixo ao frontend estático descrevem a versão anterior; siga os READMEs atuais para executar os projetos.

# Como o projeto funciona

## 1. Visão geral

```
Navegador (HTML/CSS/JS)  ──HTTP + JSON──▶  Spring Boot (API REST)  ──JPA──▶  PostgreSQL
  guarda o token JWT                         valida token, aplica regras
```

O Spring Boot faz duas coisas: serve os arquivos do site (`static/`) e responde a API em `/api/**`.
Por estarem no mesmo endereço, não há problema de CORS.

Camadas do backend (uma responsabilidade cada):

| Camada | Pasta | O que faz |
|---|---|---|
| Controller | `controller/` | Recebe a requisição HTTP, valida o corpo (`@Valid`) e chama o service |
| Service | `service/` | Regras de negócio (estoque, preço, quem é dono de quê) |
| Repository | `repository/` | Fala com o banco (Spring Data JPA) |
| Model | `model/` | As tabelas |
| DTO | `dto/` | O formato exato do que entra e sai da API |
| Security | `security/` | JWT, filtro de autenticação, regras de acesso |
| Exception | `exception/` | Erros padronizados |

## 2. Login e permissões (o coração da segurança)

1. **Cadastro** (`POST /api/auth/registrar`): a senha é transformada em hash com BCrypt (nunca é guardada em texto).
   Toda conta nova nasce `CLIENTE`. Não existe como pedir `ADMIN` por essa rota.
2. **Login** (`POST /api/auth/login`): confere e-mail/senha e devolve um **token JWT** assinado com `JWT_SECRET`.
3. **Cada requisição seguinte** leva `Authorization: Bearer <token>`. O `JwtAuthFilter` confere a assinatura e a validade,
   carrega o usuário do banco e o coloca no contexto do Spring Security.
4. O `SecurityConfig` decide quem entra em cada rota:
   - público: login, cadastro e **leitura** do catálogo;
   - `ADMIN`: criar/editar/apagar produto e categoria, e tudo em `/api/admin/**`;
   - qualquer outra rota da API: precisa estar logado.
5. Sem token ou com token inválido → **401** (o front manda para o login). Logado sem permissão → **403**.

O papel (`role`) que o front guarda serve só para mostrar/esconder menus. **Quem manda é sempre o servidor**:
mesmo que alguém edite o `localStorage`, a API continua recusando.

### O primeiro admin
Em desenvolvimento ele é criado sozinho (`admin@loja.com / admin12345`). Em produção, defina `ADMIN_EMAIL` e
`ADMIN_PASSWORD` e ele é criado na primeira subida (e troque a senha depois).

## 3. Compra: do carrinho ao pedido

- **Carrinho**: é sempre o do usuário **do token** (criado na primeira vez que ele é usado). O front nunca manda o
  "id do usuário": o servidor o descobre sozinho. Os itens são procurados *dentro* desse carrinho, então não há como
  mexer no carrinho de outra pessoa.
- **Adicionar item**: valida produto, tamanho (precisa existir na lista do produto) e estoque. Mesmo produto + mesmo
  tamanho soma a quantidade em vez de duplicar a linha.
- **Criar pedido** (`POST /api/pedidos`): numa **única transação** o servidor
  1. confere o endereço (precisa ser do próprio usuário),
  2. confere o estoque de cada item e dá baixa,
  3. grava o pedido com **cópia** de nome, imagem, tamanho e preço de cada item (o histórico não muda se o produto mudar),
  4. grava o endereço como texto no pedido,
  5. esvazia o carrinho.
  Se qualquer passo falhar, nada é gravado.
- **Estoque e concorrência**: `Produto` tem um campo `@Version`. Se duas pessoas tentam comprar a última unidade ao
  mesmo tempo, uma delas recebe um erro claro (409) em vez de o estoque ficar negativo.
- **Status**: `AGUARDANDO_PAGAMENTO → ENVIADO → ENTREGUE`. O admin só avança, nunca volta.
- **Pagamento**: **simulado**. Nenhum dado de cartão é coletado nem cobrado. Está avisado na tela de checkout.

## 4. O que foi acrescentado para ficar seguro (e por quê)

| Problema | Correção |
|---|---|
| `/api/produtos/**` e `/api/categorias/**` estavam liberados para **todos os métodos**: qualquer pessoa podia criar ou apagar produto | GET público; POST/PUT/DELETE só ADMIN |
| Alterar/remover item do carrinho usava só o id do item: dava para mexer no carrinho de outro usuário | Itens são buscados dentro do carrinho do usuário do token |
| Editar/apagar endereço sem checar o dono | Busca sempre por `(id do endereço + id do usuário)`; se não for dele, responde 404 |
| Segredo do JWT escrito no código | Vem de variável de ambiente; sem ele a app não sobe em produção |
| Cadastro recebia a entidade `Usuario` inteira | DTO próprio (`RegistroRequest`); e-mail único e normalizado; senha de 8 a 72 caracteres |
| Sem validação de entrada | `@Valid` + regras nos DTOs (tamanhos, números positivos, CEP, etc.) |
| Respostas devolviam entidades JPA (vazam campos, JSON gigante, loops) | DTOs de resposta em tudo; `senha` também marcada com `@JsonIgnore` |
| Erro inesperado devolvia stack trace | `GlobalExceptionHandler`: mensagem genérica ao cliente, detalhe só no log |
| Token expirado/malformado derrubava a requisição com erro 500 | Filtro trata e segue como visitante (rotas protegidas respondem 401) |
| Tentativas ilimitadas de senha | 5 erros por e-mail em 15 min bloqueiam novas tentativas (429) |
| Sem cabeçalhos de segurança | CSP restritiva (só scripts do próprio site), proibição de iframe, etc. |
| Sessão aberta durante toda a requisição (`open-in-view`) | Desligado; respostas são montadas dentro de transações |

Como o frontend monta HTML a partir de dados do servidor, **todo texto passa por `esc()`**. Há um teste que cadastra
um produto com nome `<img src=x onerror=...>` e confirma que aparece como texto, sem executar.

## 5. Decisões que você deve conhecer

- **Tamanhos**: o campo `tamanho` do produto guarda a lista separada por vírgula (`P,M,G` ou `38,39,40`); vazio =
  tamanho único. O estoque (`quantidade`) é do produto todo, não por tamanho. É a simplificação da v1; para estoque
  por tamanho, o próximo passo é uma tabela de variações.
- **Endereço no pedido** é texto copiado (não uma chave para `Endereco`): apagar ou editar o endereço depois não
  altera pedidos antigos.
- **Dinheiro em `Double`** por simplicidade, com arredondamento a 2 casas. Em um sistema real, use `BigDecimal`.

## 6. Limites honestos da v1 (o que ainda NÃO está coberto)

- **Token no `localStorage`**: simples, mas um XSS poderia roubá-lo. Mitigado por escape de HTML + CSP, mas a opção
  mais robusta é cookie `HttpOnly`.
- **Sem logout no servidor / refresh token**: o token vale até expirar (2h em produção).
- **Limite de login em memória e por e-mail**: reinicia com a aplicação e, em contrapartida, permite que alguém
  trave o login de outra pessoa por 15 minutos digitando senhas erradas. Em escala, use um limitador por IP/Redis.
- **Sem recuperação de senha nem confirmação de e-mail.**
- **`ddl-auto=update`**: o Hibernate altera o banco sozinho. Para produção séria, use migrations (Flyway).
- **Sem testes automatizados no backend.** O frontend foi exercitado com uma bateria de 85 verificações contra uma
  API simulada, mas o backend precisa de testes (`@SpringBootTest` + MockMvc para as regras de acesso são o melhor começo).
- **Sem upload de imagem**: o admin cola a URL de uma imagem.

## 7. O frontend

HTML + CSS + JS puro, sem build. Páginas por *hash* (`#/produto/3`):
início (busca, filtro, ordenação), produto (tamanho e quantidade), carrinho, checkout (endereço + pagamento
simulado), meus pedidos (com andamento), conta (endereços), login/cadastro e **painel** (pedidos com troca de status,
produtos, categorias).

Arquivos: `js/api.js` (todas as chamadas e o token), `js/ui.js` (escape, formatação, avisos), `js/pages.js` (as telas),
`js/app.js` (rotas e cabeçalho), `css/styles.css`.
