# Nord — API da loja NÓRD

Backend em **Java 17 + Spring Boot 3 + PostgreSQL** (API REST com JWT). O frontend oficial é o projeto Next.js paralelo **e-commerce-de-moda**. A loja abre em http://localhost:3000 e a API em http://localhost:8080. O frontend antigo de src/main/resources/static não é mais servido nem incluído no JAR.

## Rodar localmente

```bash
docker compose up -d          # sobe o PostgreSQL (porta 5432)
mvn spring-boot:run -Dspring-boot.run.profiles=dev # sobe a loja em http://localhost:8080
```

Pela IDE: rode `NordApplication`. Ative explicitamente o perfil `dev` na IDE (`SPRING_PROFILES_ACTIVE=dev`), que:
- cria categorias e produtos de exemplo (só com o banco vazio);
- cria o admin **admin@loja.com / admin12345**.

> Se você já tinha um banco de testes de versões anteriores do projeto, apague-o antes (`docker compose down -v`).
> As tabelas mudaram (ex.: `produto.version`, `pedido.endereco_entrega`) e o `ddl-auto=update` não migra dados antigos.

## Endpoints

| Método | Rota | Quem acessa |
|---|---|---|
| POST | `/api/auth/registrar`, `/api/auth/login` | público |
| GET | `/api/produtos`, `/api/produtos/{id}`, `/api/categorias` | público |
| POST/PUT/DELETE | `/api/produtos`, `/api/categorias` | ADMIN |
| GET | `/api/usuarios/me` | logado |
| GET/POST/PUT/DELETE | `/api/enderecos` | logado (só os próprios) |
| GET/POST/PUT/DELETE | `/api/carrinho`, `/api/carrinho/itens` | logado (só o próprio) |
| POST | `/api/pedidos` · GET `/api/pedidos/meus` | logado |
| GET | `/api/admin/pedidos` · PATCH `/api/admin/pedidos/{id}/status` | ADMIN |

Autenticação: header `Authorization: Bearer <token>` (o token vem do login/registro).
Erros seguem sempre `{ "status": 400, "mensagem": "...", "erros": { "campo": "motivo" } }`.

## Variáveis de ambiente (produção)

| Variável | Obrigatória | Para quê |
|---|---|---|
| `DB_URL` | sim | ex.: `jdbc:postgresql://host:5432/banco` |
| `DB_USER`, `DB_PASSWORD` | sim | credenciais do banco |
| `JWT_SECRET` | sim | segredo do token. Gere com `openssl rand -base64 48`. **Sem ele a aplicação não sobe (de propósito).** |
| `ADMIN_EMAIL`, `ADMIN_PASSWORD` | não | cria o primeiro admin (senha com 8+ caracteres) |
| `CORS_ORIGINS` | não | só se o front ficar em outro domínio. Ex.: `https://minha-loja.vercel.app` |
| `JWT_EXPIRATION_MS` | não | validade do token (padrão: 2 horas) |
| `PORT` | não | porta (plataformas de hospedagem definem sozinhas) |

## Deploy (exemplo: Render, Railway, Fly.io)

1. Crie um PostgreSQL gerenciado e copie host, usuário e senha.
2. Crie um serviço web a partir deste repositório (ele detecta o `Dockerfile`).
3. Defina as variáveis acima. Atenção: `DB_URL` precisa começar com `jdbc:postgresql://`.
4. Hospede o frontend Next.js como segundo serviço e configure NORD_API_URL com a URL do Nord antes do build. A URL pública da loja é a do frontend; as chamadas /api são encaminhadas por ele.

## Usar o front do Lovable/Next em vez do incluído

Basta apontar as chamadas para a URL da API, mandar o token no header `Authorization` e colocar o domínio do
front em `CORS_ORIGINS`. Os formatos de resposta estão em `src/main/java/com/nord/nord/dto`.

Leia também o **GUIA-COMO-FUNCIONA.md**.

O perfil padrão agora é prod e exige as variáveis de produção. Consulte AUDITORIA-SEGURANCA.md antes do deploy.

## Frontend oficial integrado

O projeto paralelo está em C:\Users\ezequ\Downloads\e-commerce-de-moda. Consulte o README dessa pasta. Execute pnpm install --frozen-lockfile e pnpm dev para abrir a loja em http://localhost:3000. NORD_API_URL no ambiente do Next aponta para http://127.0.0.1:8080 por padrão. O Nord agora serve somente a API; acessar / ou /index.html na porta 8080 retorna 404.

Não edite target/classes/static: target é saída gerada pelo Maven. Execute mvn clean verify para remover artefatos antigos. O diretório fonte static foi preservado como referência, mas excluído dos recursos empacotados e desativado na configuração Spring.

Validação da integração: 29 testes backend aprovados e JAR sem BOOT-INF/classes/static; frontend com build, TypeScript, 8 testes de contrato e smoke test do proxy aprovados. A integração com PostgreSQL real ainda requer execução local com Docker Desktop ativo.

## Erro JWT_SECRET ao iniciar pela IDE

Se o log mostrar perfil `prod` e `Could not resolve placeholder JWT_SECRET`, ative o perfil local `dev`. O padrão prod é intencional para evitar credenciais de desenvolvimento online.

- IntelliJ: selecione a configuração compartilhada **Nord Local (dev)**. Em uma configuração existente, abra Run > Edit Configurations e defina a variável de ambiente `SPRING_PROFILES_ACTIVE=dev` ou o argumento `--spring.profiles.active=dev`.
- VS Code: abra a pasta Nord e selecione **Nord Local (dev)** em Executar e Depurar.
- Terminal: `mvn spring-boot:run "-Dspring-boot.run.profiles=dev"`.
- JAR já compilado: `java -jar target/nord-0.0.1-SNAPSHOT.jar --spring.profiles.active=dev`.

Primeiro mantenha o PostgreSQL local ativo. Depois execute a API (8080) e `pnpm dev` no frontend (3000). Abra a loja na porta 3000; a raiz da API na porta 8080 retorna 404 por projeto. Evite iniciar uma segunda cópia enquanto a porta 8080 estiver ocupada.

Validação local posterior em 11/10/2026: frontend Next, API Nord e PostgreSQL 16.15 executados juntos. Cadastro, login, perfil, carrinho, endereço, pedido, persistência, baixa de estoque e isolamento entre contas passaram através do proxy real. Evidência em docs/security/local-integration-result.json. Foram criadas contas, endereços e dois pedidos fictícios no banco local, consumindo duas unidades do produto 1.
## Imagens dos produtos

Os nove produtos de demonstração possuem imagens próprias geradas por IA e incluídas no backend, em src/main/resources/catalogo/imagens. A API serve /api/catalogo/imagens/<arquivo>.png; o frontend acessa pelo proxy Next. O perfil dev completa imagens ausentes em produtos existentes sem alterar estoque ou imagens personalizadas. Consulte docs/IMAGENS-CATALOGO.md para arquivos, origem e prompts. Validação: 40 testes backend, 9 testes frontend, builds aprovados e nove PNGs com HTTP 200 no catálogo PostgreSQL local. Reinicie os projetos para carregar a nova versão.


## Painel administrativo

Acesse `http://localhost:3000/admin` com uma conta ADMIN. A loja continua em `/`.
O painel possui login e sessão próprios, gest?o de usuários (nome, e-mail, permissão e ativação),
exclusão de contas sem pedidos, produtos, estoque, categorias e acompanhamento dos pedidos.
Usuários com pedidos devem ser desativados para preservar o histórico. Não é permitido excluir,
bloquear ou rebaixar a própria conta administrativa. Alterações em usuários invalidam seus tokens;
após esta atualização, fa?a login novamente também na loja.

A autorização ADMIN ? aplicada pela API. `/admin` é uma URL separada no mesmo domínio;
para isolamento por domínio/infraestrutura é necessário configurar uma implantação própria.
O status do pedido não comprova pagamento: a confirmação financeira continua fora deste projeto.
