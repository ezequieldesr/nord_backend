# Imagens do catálogo Nord

Nove imagens próprias de demonstração, criadas com a ferramenta integrada imagegen em 12/10/2026. São representações geradas por IA dos produtos fictícios do portfólio, não fotografias de mercadorias reais. Os arquivos estão em `src/main/resources/catalogo/imagens/` e são incluídos no JAR.

O backend responde `GET /api/catalogo/imagens/<arquivo>.png`, com Content-Type image/png e cache público de sete dias. A lista de arquivos é fechada em CatalogImages: o endpoint não permite leitura arbitrária de arquivos. O frontend acessa essa rota pelo proxy Next /api, sem URLs de terceiros e sem token.

Ao iniciar com perfil dev, o DevDataSeeder preenche somente imagens ausentes dos produtos conhecidos. Produtos criados em banco vazio já recebem suas imagens. Não substitui imagens personalizadas e não altera preços ou quantidades. Em produção não há carga automática: use o endpoint administrativo de produtos para definir a URL da imagem.

| Produto | Arquivo | Descrição visual usada no prompt |
|---|---|---|
| Camiseta Dry Trail | camiseta-dry-trail.png | Camiseta técnica de corrida preta, gola redonda e tecido respirável |
| Camiseta Algodão Básica | camiseta-algodao-basica.png | Camiseta de algodão encorpado branco-marfim, gola redonda |
| Camisa Polo Nord | camisa-polo-nord.png | Polo azul-marinho de piquê, gola clássica e dois botões |
| Tênis Corrida Fjord | tenis-corrida-fjord.png | Par de tênis de corrida grafite e laranja discreto, mesh e amortecimento |
| Tênis Urbano Branco | tenis-urbano-branco.png | Par de tênis urbanos brancos, cano baixo, couro sintético e sola off-white |
| Boné Aba Curva | bone-aba-curva.png | Boné verde-oliva de algodão, seis painéis, aba curva |
| Boné Trucker | bone-trucker.png | Boné trucker preto e bege com traseira de tela visível |
| Calça Jogger | calca-jogger.png | Calça de moletom cinza grafite, cordão e punhos nas barras |
| Calça Corta-vento | calca-corta-vento.png | Calça técnica preta de nylon leve, cós elástico e zíper nas barras |

Prompt base final (uma chamada por produto, com a descrição da tabela em inglês):

> Use case: product-mockup. Create one premium photorealistic ecommerce catalog product photograph: [subject]. Entire product visible and centered, realistic construction, front or suitable three-quarter view, warm off-white seamless background, diffuse studio lighting, soft natural shadow, refined fashion retailer photography. Portrait 4:5 composition, high resolution, ample margins, product occupies 75% of frame. No text, people, logos, brand symbols, props, collage or watermark.

Para a camiseta técnica: crew neck, breathable lightweight dry-fit mesh fabric, subtle athletic seams, realistic invisible mannequin shape. Não foram utilizadas fotografias de lojas nem hotlinks de CDN externa.

Após atualizar, reinicie a API para carregar as novas classes e recursos. Reinicie o Next para carregar a adaptação de URLs locais. Se substituir futuramente o conteúdo de uma imagem com o mesmo nome, considere o cache de sete dias: prefira nome versionado e atualize a lista permitida.
Validação final: 40 testes backend e 9 testes frontend aprovados; TypeScript e build Next aprovados. Os nove produtos existentes no PostgreSQL receberam suas URLs, e todos os arquivos responderam HTTP 200 com assinatura PNG válida em instância temporária da API. Evidência: docs/security/catalog-images-result.json. A instância temporária foi encerrada; o processo original do usuário não foi interrompido.
