# Publicar o Nord: Render + Vercel

O Nord pode ser apresentado como demonstracao de e-commerce. Nao processa pagamentos reais.
Use somente dados ficticios e mantenha o administrador privado.

## 1. Repositorios

- Backend: https://github.com/ezequieldesr/nord_backend
- Frontend: https://github.com/ezequieldesr/nord_frontend

Envie suas alteracoes pelo Git antes de conectar as plataformas. Nao envie .env nem credenciais.

## 2. Banco PostgreSQL no Render

Crie um PostgreSQL e um Web Service na mesma regiao. Escolha o plano conscientemente:
o PostgreSQL gratuito do Render expira em 30 dias; nao e uma base permanente de portfolio.
Confira preco, backups e retencao antes de contratar um plano.

Na conexao interna do banco, copie separadamente hostname, database, username e password.
DB_URL deve ser JDBC, sem credenciais embutidas:
`jdbc:postgresql://HOST_INTERNO:5432/DATABASE`
Nao copie `postgresql://usuario:senha@host/database` diretamente para DB_URL.

## 3. API no Render

New > Web Service > conecte nord_backend > runtime Docker.
Use Dockerfile na raiz. O Dockerfile executa os testes e inicia o perfil prod.
Defina no painel (valores reais apenas na plataforma):

| Variavel | Valor |
| --- | --- |
| SPRING_PROFILES_ACTIVE | prod |
| DB_URL | jdbc:postgresql://HOST_INTERNO:5432/DATABASE |
| DB_USER | usuario do banco |
| DB_PASSWORD | senha do banco |
| JWT_SECRET | segredo aleatorio Base64 com pelo menos 32 bytes |
| ADMIN_EMAIL | seu e-mail administrativo privado |
| ADMIN_PASSWORD | senha exclusiva e forte, diferente da senha local |
| JWT_EXPIRATION_MS | 7200000 |

Gere JWT_SECRET localmente com `openssl rand -base64 48` e cole somente no Render.
Alternativa PowerShell:
`$bytes = New-Object byte[] 48; $rng = [System.Security.Cryptography.RandomNumberGenerator]::Create(); $rng.GetBytes($bytes); $rng.Dispose(); [Convert]::ToBase64String($bytes)`
Nao use o gerador automatico de strings do provedor sem verificar Base64 e tamanho decodificado.
A aplicacao usa PORT automaticamente. Nao configure a raiz `/` como health check: ela retorna 404.
Pode usar `/api/produtos` para verificacao HTTP; o resultado inicial esperado e 200 com `[]`.
Essa verificacao tambem depende da conexao com o banco.

Copie a URL HTTPS publica, como `https://nord-api.onrender.com`.
Se o servico gratuito dormir, a primeira requisicao pode demorar; teste antes de divulgar.

## 4. Frontend na Vercel

Add New > Project > importe nord_frontend. Framework: Next.js. Root: raiz do repositorio.
Mantenha pnpm e o lockfile. Antes do build defina:
`NORD_API_URL=https://SEU-BACKEND.onrender.com`
Sem `/api` no final. Nao use NEXT_PUBLIC_ e nao coloque JWT_SECRET ou credenciais do banco na Vercel.
Configure Production; previews devem preferencialmente usar uma API/banco de testes.
Faca Deploy. Se mudar NORD_API_URL depois, faca um novo deployment para reconstruir os rewrites.

O navegador chama /api na Vercel; o Next encaminha ao Render.
Neste fluxo nao e necessario liberar CORS amplo nem usar `*`.
Se quiser chamadas diretas do navegador ao Render, configure CORS_ORIGINS com a origem HTTPS exata.

## 5. Primeiro acesso e catalogo

Abra `https://SEU-FRONTEND.vercel.app/admin` e entre com o administrador definido no Render.
O perfil prod NAO executa DevDataSeeder: cadastre categorias e produtos pelo painel.
As imagens incluidas estao em `src/main/resources/catalogo/imagens` e podem ser usadas como:
`/api/catalogo/imagens/camiseta-dry-trail.png`
Use o nome dos demais PNGs da mesma pasta. Nao copie o banco local com contas/senhas de teste.
Apos confirmar o primeiro acesso, remova ADMIN_PASSWORD do ambiente e reimplante.
Isso nao altera a senha persistida. Nunca divulgue acesso ADMIN de escrita publicamente.

## 6. Validacao antes de divulgar

- Catalogo e imagens carregam via dominio Vercel.
- Cadastro e login com conta ficticia funcionam.
- Carrinho, endereco ficticio e pedido persistem apos reiniciar o servico.
- Outra conta nao consulta pedidos ou enderecos da primeira.
- Sem token: GET /api/admin/usuarios retorna 401; com CLIENTE retorna 403.
- /admin funciona e conta desativada perde acesso.
- Interface deixa claro que pedidos e pagamento sao demonstrativos.
- Leia os logs sem publicar dados pessoais, tokens ou senhas.
- README com links reais, capturas de tela, arquitetura e limites conhecidos.

## Evolucao posterior

Antes de vender como loja real: pagamentos com webhooks verificados, valores monetarios em decimal,
migracoes versionadas (hoje Hibernate ddl-auto=update), backups com restauracao testada,
recuperacao de senha, verificacao de e-mail e monitoramento. Rate limiting atual e em memoria:
nao e compartilhado entre replicas e nao substitui protecao contra abuso na borda.
Revise dependencias e seguranca periodicamente; testes aprovados nao garantem ausencia de falhas.

## Documentacao oficial

- https://render.com/docs/docker
- https://render.com/docs/free
- https://render.com/docs/postgresql-creating-connecting
- https://vercel.com/docs/environment-variables
- https://vercel.com/docs/limits/fair-use-guidelines

O plano Hobby da Vercel e limitado a uso pessoal nao comercial. Para divulgar atividade comercial
ou hospedar trabalho de clientes, confira as regras e selecione um plano compativel.

