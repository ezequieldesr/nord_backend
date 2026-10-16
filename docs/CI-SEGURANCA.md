# Integração contínua e segurança

Os workflows executam ao enviar commits e abrir/atualizar pull requests. CI valida a mudança;
a publicação em hospedagem não foi automatizada, e não há credenciais de produção nesses arquivos.
As actions são fixadas por SHA, o checkout não persiste credenciais e o token começa com leitura apenas.
CodeQL recebe permissão de publicar resultados de segurança somente no job de análise.
Nenhum workflow usa pull_request_target nem executa scripts de PR com segredos de produção.

## Verificações

| Repositório | Check | O que executa |
| --- | --- | --- |
| Backend | Backend tests and Docker | Java 17, mvn verify e build do Dockerfile |
| Frontend | Frontend tests and build | Instalação congelada, testes, Next build e TypeScript |
| Frontend | Production dependency audit | pnpm audit --prod --audit-level high; falha com alertas altos/críticos |
| Frontend | Playwright with PostgreSQL | Banco PostgreSQL 16 temporário, API dev e frontend local; quatro cenários reais |
| Ambos | Dependency review | Bloqueia novas dependências vulneráveis altas/críticas introduzidas em PRs |
| Ambos | CodeQL analysis | Análise estática, consultas security-extended; push na main, PR e execução semanal |

O check de CodeQL executado com sucesso não significa ausência de alertas: consulte a aba Security.
Configure também as regras de code scanning para impedir merge de alertas relevantes.
Dependency Review usa o dependency graph do GitHub; habilite-o em Settings > Advanced Security.
Para Maven, habilite também a submissão automática de dependências se quiser melhorar a resolução transitiva.
CodeQL e Dependency Review são disponíveis para repositórios públicos; em privados podem exigir licença e configuração adicionais.
Evite habilitar a análise CodeQL padrão simultaneamente ao workflow avançado, para não duplicar execuções.

## Integração entre repositórios

O workflow e2e.yml do frontend testa a revisão do frontend do evento contra a branch main pública de
`ezequieldesr/nord_backend`. Não utiliza o computador do autor, o banco local ou um servidor de produção.
O catálogo e o administrador são criados apenas no banco descartável do runner, com credenciais dev.
Envie primeiro as alterações compatíveis do backend e depois as do frontend.

Essa integração não testa automaticamente um PR ainda não integrado do backend. Alterações coordenadas
nos contratos precisam de revisão e teste conjunto antes do merge. O CI do backend continua verificando
seu próprio código e imagem Docker de forma independente.

Relatórios de navegador, traces, vídeos de falha e logs da API não são enviados como artefatos públicos:
podem conter tokens e dados de requisições. Consulte o log dos passos no GitHub Actions.
Reproduza falhas localmente com pnpm test:e2e e o relatório HTML; use somente dados fictícios.

## Atualizações automáticas

Dependabot abre propostas semanais para GitHub Actions e bibliotecas.
O backend inclui Maven e imagens Docker; o frontend usa o ecossistema npm, compatível com pnpm.
Atualizações não são mescladas automaticamente: revise os PRs e espere os checks passarem.
Overrides de dependências transitivas do frontend estão em pnpm-workspace.yaml e devem ser revisados
quando os pacotes principais incorporarem as versões corrigidas.

## Tornar os checks obrigatórios

Após enviar os arquivos e observar a primeira execução:

1. Abra Settings > Rules > Rulesets e crie uma regra para main.
2. Exija pull requests e status checks antes de merge.
3. No backend, selecione Backend tests and Docker e Dependency review.
4. No frontend, selecione Frontend tests and build, Production dependency audit,
   Playwright with PostgreSQL e Dependency review.
5. Ative CodeQL/code scanning, alertas do Dependabot e secret scanning/push protection quando disponíveis.
6. Restrinja bypass e force push de acordo com sua forma de trabalho.

Os arquivos YAML não alteram por si só as proteções da branch. Os checks precisam aparecer no GitHub
após uma execução para poderem ser selecionados. Em PRs vindos de forks, o GitHub pode exigir aprovação
para executar Actions; não aprove alterações suspeitas sem revisão.

Não ignore um scanner que falhou nem confunda erro do serviço de auditoria com ausência de problemas.
As verificações reduzem riscos conhecidos e regressões; não substituem revisão ou auditoria de segurança.
