(function (global) {
  'use strict';
  var Nord = global.Nord;
  var Api = Nord.Api, Auth = Nord.Auth, UI = Nord.UI;
  var esc = UI.esc, money = UI.money;
  var Pages = Nord.Pages = {};

  // ---------- utilidades ----------

  // Desenha a página. Devolve o elemento raiz da view (ou null se o usuário já navegou para outra rota).
  function view(ctx, html) {
    if (ctx && ctx.alive && !ctx.alive()) return null;
    var app = document.getElementById('app');
    app.innerHTML = '<div class="view">' + html + '</div>';
    global.scrollTo(0, 0);
    return app.firstElementChild;
  }
  function loading(ctx) { view(ctx, '<p class="wrap muted pad">Carregando…</p>'); }

  function needAuth(ctx) {
    if (Auth.isLogged()) return true;
    Nord.App.go('/login?next=' + encodeURIComponent(ctx.fullPath));
    return false;
  }

  // Liga um formulário: bloqueia clique duplo, mostra erros do servidor dentro do próprio form.
  function onSubmit(form, handler) {
    form.addEventListener('submit', async function (ev) {
      ev.preventDefault();
      if (!form.checkValidity()) { form.reportValidity(); return; }
      var btn = form.querySelector('[type=submit]');
      var box = form.querySelector('[data-error]');
      if (box) box.innerHTML = '';
      if (btn) btn.disabled = true;
      try {
        await handler(ev);
      } catch (e) {
        if (box) { box.innerHTML = UI.errorHtml(e); } else { UI.toast(e.message, 'erro'); }
      } finally {
        if (btn && btn.isConnected) btn.disabled = false;
      }
    });
  }

  function fail(e) { UI.toast(e && e.message ? e.message : 'Algo deu errado.', 'erro'); }

  var STATUS = { AGUARDANDO_PAGAMENTO: 'Aguardando pagamento', ENVIADO: 'Enviado', ENTREGUE: 'Entregue' };
  var STATUS_ORDEM = ['AGUARDANDO_PAGAMENTO', 'ENVIADO', 'ENTREGUE'];

  function pagamentoTexto(p) {
    if (p.formaPagamento === 'PIX') return 'Pix';
    if (p.formaPagamento === 'BOLETO') return 'Boleto';
    if (p.formaPagamento === 'CARTAO') {
      return p.quantidadeParcelas > 1
        ? 'Cartão de crédito em ' + p.quantidadeParcelas + 'x de ' + money(p.valorParcela)
        : 'Cartão de crédito à vista';
    }
    return p.formaPagamento || '';
  }

  function tamanhosDe(p) {
    var lista = String(p.tamanho || '').split(',').map(function (t) { return t.trim(); }).filter(Boolean);
    return lista.length ? lista : ['Único'];
  }

  function addressFields(a) {
    a = a || {};
    return '' +
      '<div class="grid2">' +
      '<label class="f">CEP<input name="cep" value="' + esc(a.cep) + '" required pattern="\\d{5}-?\\d{3}" inputmode="numeric" autocomplete="postal-code" placeholder="00000-000"></label>' +
      '<label class="f">Estado (sigla)<input name="estado" value="' + esc(a.estado) + '" required minlength="2" maxlength="2" autocomplete="address-level1" placeholder="SP"></label>' +
      '</div>' +
      '<label class="f">Rua<input name="rua" value="' + esc(a.rua) + '" required maxlength="120" autocomplete="address-line1"></label>' +
      '<div class="grid2">' +
      '<label class="f">Número<input name="numero" type="number" min="1" value="' + esc(a.numero) + '" required inputmode="numeric"></label>' +
      '<label class="f">Complemento (opcional)<input name="complemento" value="' + esc(a.complemento) + '" maxlength="100" autocomplete="address-line2"></label>' +
      '</div>' +
      '<div class="grid2">' +
      '<label class="f">Bairro<input name="bairro" value="' + esc(a.bairro) + '" required maxlength="80"></label>' +
      '<label class="f">Cidade<input name="cidade" value="' + esc(a.cidade) + '" required maxlength="80" autocomplete="address-level2"></label>' +
      '</div>';
  }

  function readAddress(form) {
    var fd = new FormData(form);
    function s(k) { return String(fd.get(k) || '').trim(); }
    return {
      cep: s('cep'), estado: s('estado').toUpperCase(), cidade: s('cidade'), bairro: s('bairro'),
      rua: s('rua'), numero: parseInt(s('numero'), 10), complemento: s('complemento')
    };
  }

  function addressLine(a) {
    return esc(a.rua) + ', ' + esc(a.numero) + (a.complemento ? ' (' + esc(a.complemento) + ')' : '') +
      ' &middot; ' + esc(a.bairro) + ', ' + esc(a.cidade) + '/' + esc(a.estado) + ' &middot; CEP ' + esc(a.cep);
  }

  // ---------- início ----------

  function heroHtml() {
    return '' +
      '<section class="hero">' +
      '<div class="wrap hero-in">' +
      '<h1>Roupas, calçados e acessórios para o dia a dia.</h1>' +
      '<p>Escolha o tamanho, monte o carrinho e finalize em poucos passos.</p>' +
      '<button class="btn btn-signal" type="button" data-go="catalogo">Ver produtos</button>' +
      '</div>' +
      '<svg class="ridge" viewBox="0 0 1200 220" preserveAspectRatio="none" aria-hidden="true" focusable="false">' +
      '<circle cx="960" cy="78" r="34" fill="#FF5F1F"/>' +
      '<polygon fill="#C9D9E5" points="0,220 0,120 120,72 220,112 340,42 470,122 560,92 700,152 820,62 940,112 1060,52 1200,122 1200,220"/>' +
      '<polygon fill="#8FB0C7" points="0,220 0,150 90,118 210,160 380,96 520,158 690,110 860,168 1020,120 1200,166 1200,220"/>' +
      '<polygon fill="#123047" points="0,220 0,184 150,146 300,186 480,138 650,190 820,152 1000,194 1200,160 1200,220"/>' +
      '</svg>' +
      '</section>';
  }

  function tileHtml(p) {
    var nota = '';
    if (p.quantidade <= 0) nota = '<p class="stock stock-out">Esgotado</p>';
    else if (p.quantidade <= 3) nota = '<p class="stock">Restam ' + esc(p.quantidade) + '</p>';
    return '<a class="tile" href="#/produto/' + esc(p.id) + '">' + UI.media(p, '') +
      '<div class="tile-body"><h3>' + esc(p.nome) + '</h3>' +
      '<p class="muted">' + esc(p.categoriaNome) + '</p>' +
      '<p class="price">' + money(p.preco) + '</p>' + nota + '</div></a>';
  }

  Pages.home = async function (ctx) {
    loading(ctx);
    var catId = ctx.query.cat || '';
    var res = await Promise.all([Api.categorias(), Api.produtos(catId)]);
    var cats = res[0], produtos = res[1];
    Nord.App.setCats(cats);
    var atual = cats.filter(function (c) { return String(c.id) === String(catId); })[0];

    var pills = '<a class="pill" href="#/"' + (!catId ? ' aria-current="page"' : '') + '>Todos</a>' +
      cats.map(function (c) {
        return '<a class="pill" href="#/?cat=' + esc(c.id) + '"' + (String(c.id) === String(catId) ? ' aria-current="page"' : '') + '>' + esc(c.nome) + '</a>';
      }).join('');

    var v = view(ctx, (catId ? '' : heroHtml()) +
      '<section class="wrap catalog" id="catalogo">' +
      '<div class="catalog-head"><h2>' + esc(atual ? atual.nome : 'Todos os produtos') + '</h2>' +
      '<div class="tools">' +
      '<label class="sr" for="q">Buscar produto</label><input id="q" type="search" placeholder="Buscar produto" autocomplete="off">' +
      '<label class="sr" for="ord">Ordenar por</label>' +
      '<select id="ord"><option value="novos">Novidades</option><option value="menor">Menor preço</option><option value="maior">Maior preço</option><option value="nome">Nome (A a Z)</option></select>' +
      '</div></div>' +
      '<nav class="pills" aria-label="Categorias">' + pills + '</nav>' +
      '<div id="grid" class="grid"></div>' +
      '</section>');
    if (!v) return;

    var grid = v.querySelector('#grid');
    var q = v.querySelector('#q');
    var ord = v.querySelector('#ord');

    function draw() {
      var termo = q.value.trim().toLowerCase();
      var lista = produtos.filter(function (p) {
        return !termo || (p.nome + ' ' + (p.descricao || '')).toLowerCase().indexOf(termo) !== -1;
      });
      var o = ord.value;
      lista.sort(function (a, b) {
        if (o === 'menor') return a.preco - b.preco;
        if (o === 'maior') return b.preco - a.preco;
        if (o === 'nome') return a.nome.localeCompare(b.nome, 'pt-BR');
        return b.id - a.id;
      });
      grid.innerHTML = lista.length
        ? lista.map(tileHtml).join('')
        : UI.empty('Nenhum produto encontrado', 'Tente outro termo de busca ou escolha outra categoria.');
    }
    q.addEventListener('input', draw);
    ord.addEventListener('change', draw);
    draw();

    v.addEventListener('click', function (ev) {
      var b = ev.target.closest('[data-go="catalogo"]');
      if (b) { var el = v.querySelector('#catalogo'); if (el && el.scrollIntoView) el.scrollIntoView({ behavior: 'smooth' }); }
    });
  };

  // ---------- produto ----------

  Pages.produto = async function (ctx) {
    loading(ctx);
    var p = await Api.produto(ctx.params[0]);
    var tamanhos = tamanhosDe(p);
    var unico = tamanhos.length === 1;
    var esgotado = p.quantidade <= 0;

    var chips = tamanhos.map(function (t, i) {
      return '<label class="chip"><input type="radio" name="tam" value="' + esc(t) + '"' + (unico ? ' checked' : '') + '><span>' + esc(t) + '</span></label>';
    }).join('');

    var v = view(ctx,
      '<section class="wrap detail">' +
      '<a class="back" href="#/?cat=' + esc(p.categoriaId) + '">Voltar para ' + esc(p.categoriaNome) + '</a>' +
      '<div class="detail-grid">' +
      UI.media(p, p.nome) +
      '<div class="detail-info">' +
      '<p class="muted">' + esc(p.categoriaNome) + '</p>' +
      '<h1>' + esc(p.nome) + '</h1>' +
      '<p class="price price-lg">' + money(p.preco) + '</p>' +
      (p.descricao ? '<p class="desc">' + esc(p.descricao) + '</p>' : '') +
      '<form id="buy" novalidate>' +
      '<fieldset class="sizes"><legend>Tamanho</legend><div class="chips">' + chips + '</div></fieldset>' +
      '<div class="qty-row"><span class="lbl" id="qlbl">Quantidade</span>' +
      '<div class="stepper" role="group" aria-labelledby="qlbl">' +
      '<button type="button" data-d="-1" aria-label="Diminuir">&minus;</button>' +
      '<output id="qty" aria-live="polite">1</output>' +
      '<button type="button" data-d="1" aria-label="Aumentar">+</button></div></div>' +
      '<p class="stock' + (esgotado ? ' stock-out' : '') + '">' + (esgotado ? 'Esgotado' : (p.quantidade <= 3 ? 'Restam ' + esc(p.quantidade) + ' unidade(s)' : '')) + '</p>' +
      '<div data-error></div>' +
      '<button class="btn btn-signal btn-lg" type="submit"' + (esgotado ? ' disabled' : '') + '>Adicionar ao carrinho</button>' +
      '</form>' +
      '</div></div></section>');
    if (!v) return;

    var qty = 1;
    var out = v.querySelector('#qty');
    v.querySelectorAll('[data-d]').forEach(function (b) {
      b.addEventListener('click', function () {
        var n = qty + parseInt(b.getAttribute('data-d'), 10);
        if (n < 1) n = 1;
        if (n > Math.min(20, Math.max(1, p.quantidade))) n = Math.min(20, Math.max(1, p.quantidade));
        qty = n; out.textContent = String(n);
      });
    });

    onSubmit(v.querySelector('#buy'), async function () {
      var sel = v.querySelector('input[name=tam]:checked');
      if (!sel) throw new Nord.ApiError(0, 'Escolha um tamanho antes de adicionar.');
      if (!Auth.isLogged()) {
        UI.toast('Entre na sua conta para adicionar ao carrinho.');
        Nord.App.go('/login?next=' + encodeURIComponent(ctx.fullPath));
        return;
      }
      await Api.addItem(p.id, sel.value, qty);
      UI.toast('Adicionado ao carrinho.');
      Nord.App.refreshCart();
    });
  };

  // ---------- carrinho ----------

  Pages.carrinho = async function (ctx) {
    if (!needAuth(ctx)) return;
    loading(ctx);
    var cart = await Api.carrinho();
    var v = view(ctx, '<section class="wrap" id="cart-root"></section>');
    if (!v) return;
    var root = v.querySelector('#cart-root');

    function draw() {
      Nord.App.setCartCount(cart.itens.reduce(function (s, i) { return s + i.quantidade; }, 0));
      if (!cart.itens.length) {
        root.innerHTML = '<h1>Carrinho</h1>' + UI.empty('Seu carrinho está vazio', 'Escolha um produto e o tamanho para começar.', '#/', 'Ver produtos');
        return;
      }
      root.innerHTML = '<h1>Carrinho</h1><div class="split"><ul class="lines">' + cart.itens.map(function (i) {
        return '<li class="line">' + UI.media({ imagem: i.imagemProduto, nome: i.nomeProduto, produtoId: i.produtoId }, '') +
          '<div class="line-info"><a href="#/produto/' + esc(i.produtoId) + '"><strong>' + esc(i.nomeProduto) + '</strong></a>' +
          '<p class="muted">Tamanho ' + esc(i.tamanho) + ' &middot; ' + money(i.preco) + ' cada</p>' +
          '<div class="stepper" role="group" aria-label="Quantidade de ' + esc(i.nomeProduto) + '">' +
          '<button type="button" data-q="' + esc(i.id) + '" data-d="-1" aria-label="Diminuir">&minus;</button>' +
          '<output>' + esc(i.quantidade) + '</output>' +
          '<button type="button" data-q="' + esc(i.id) + '" data-d="1" aria-label="Aumentar">+</button></div></div>' +
          '<div class="line-end"><p class="price">' + money(i.subtotal) + '</p>' +
          '<button class="link-btn" type="button" data-rm="' + esc(i.id) + '">Remover</button></div></li>';
      }).join('') + '</ul>' +
        '<aside class="summary"><h2>Resumo</h2>' +
        '<p class="sum-row"><span>Total</span><strong class="price">' + money(cart.total) + '</strong></p>' +
        '<a class="btn btn-signal btn-block" href="#/checkout">Continuar para o pagamento</a></aside></div>';
    }
    draw();

    // Ignora cliques enquanto uma alteração está em andamento: evita ler a quantidade antiga e perder cliques.
    var ocupado = false;
    root.addEventListener('click', async function (ev) {
      var q = ev.target.closest('[data-q]');
      var rm = ev.target.closest('[data-rm]');
      if ((!q && !rm) || ocupado) return;
      ocupado = true;
      try {
        if (q) {
          var item = cart.itens.filter(function (i) { return String(i.id) === q.getAttribute('data-q'); })[0];
          var n = item.quantidade + parseInt(q.getAttribute('data-d'), 10);
          if (n < 1) return;
          cart = await Api.setQuantidade(item.id, n);
          draw();
        } else {
          cart = await Api.removeItem(rm.getAttribute('data-rm'));
          draw();
        }
      } catch (e) { fail(e); } finally { ocupado = false; }
    });
  };

  // ---------- checkout ----------

  Pages.checkout = async function (ctx) {
    if (!needAuth(ctx)) return;
    loading(ctx);
    var res = await Promise.all([Api.carrinho(), Api.enderecos()]);
    var cart = res[0], enderecos = res[1];
    if (!cart.itens.length) { Nord.App.go('/carrinho'); return; }

    var lista = enderecos.map(function (a, i) {
      return '<label class="choice"><input type="radio" name="end" value="' + esc(a.id) + '"' + (i === 0 ? ' checked' : '') + '><span>' + addressLine(a) + '</span></label>';
    }).join('');
    var semEnderecos = !enderecos.length;

    var resumo = cart.itens.map(function (i) {
      return '<li class="sum-line"><span>' + esc(i.quantidade) + ' &times; ' + esc(i.nomeProduto) + ' <span class="muted">(' + esc(i.tamanho) + ')</span></span><span>' + money(i.subtotal) + '</span></li>';
    }).join('');

    var parcelas = '';
    for (var n = 1; n <= 6; n++) {
      parcelas += '<option value="' + n + '">' + (n === 1 ? 'À vista, ' + money(cart.total) : n + 'x de ' + money(cart.total / n)) + '</option>';
    }

    var v = view(ctx,
      '<section class="wrap"><h1>Finalizar compra</h1>' +
      '<form id="co" class="split" novalidate>' +
      '<div class="co-main">' +
      '<fieldset><legend>Endereço de entrega</legend>' + lista +
      '<label class="choice"><input type="radio" name="end" value="novo"' + (semEnderecos ? ' checked' : '') + '><span>Usar um novo endereço</span></label>' +
      '<div id="novo-end" class="subform"' + (semEnderecos ? '' : ' hidden') + '>' + addressFields() + '</div>' +
      '</fieldset>' +
      '<fieldset><legend>Pagamento</legend>' +
      '<label class="choice"><input type="radio" name="pag" value="PIX" checked><span>Pix</span></label>' +
      '<label class="choice"><input type="radio" name="pag" value="CARTAO"><span>Cartão de crédito</span></label>' +
      '<div id="parc" class="subform" hidden><label class="f">Parcelas<select name="parcelas">' + parcelas + '</select></label></div>' +
      '<label class="choice"><input type="radio" name="pag" value="BOLETO"><span>Boleto</span></label>' +
      '<p class="note">Nesta versão o pagamento é simulado: nenhum valor é cobrado e nenhum dado de cartão é pedido.</p>' +
      '</fieldset>' +
      '<div data-error></div>' +
      '</div>' +
      '<aside class="summary"><h2>Seu pedido</h2><ul class="sum-lines">' + resumo + '</ul>' +
      '<p class="sum-row"><span>Total</span><strong class="price">' + money(cart.total) + '</strong></p>' +
      '<button class="btn btn-signal btn-block" type="submit">Confirmar pedido</button></aside>' +
      '</form></section>');
    if (!v) return;

    var form = v.querySelector('#co');
    var novoBox = v.querySelector('#novo-end');
    var parcBox = v.querySelector('#parc');

    function syncAddress() {
      var escolhido = form.querySelector('input[name=end]:checked');
      var novo = !!escolhido && escolhido.value === 'novo';
      novoBox.hidden = !novo;
      novoBox.querySelectorAll('input').forEach(function (i) { i.disabled = !novo; });
    }
    function syncPay() {
      var p = form.querySelector('input[name=pag]:checked');
      parcBox.hidden = !(p && p.value === 'CARTAO');
    }
    form.addEventListener('change', function () { syncAddress(); syncPay(); });
    syncAddress(); syncPay();

    onSubmit(form, async function () {
      var end = form.querySelector('input[name=end]:checked');
      if (!end) throw new Nord.ApiError(0, 'Escolha ou cadastre um endereço de entrega.');
      var enderecoId = end.value;
      if (enderecoId === 'novo') {
        var criado = await Api.criarEndereco(readAddress(form));
        enderecoId = criado.id;
      }
      var pag = form.querySelector('input[name=pag]:checked').value;
      var parc = pag === 'CARTAO' ? parseInt(form.querySelector('select[name=parcelas]').value, 10) : 1;
      var pedido = await Api.criarPedido({ enderecoId: Number(enderecoId), formaPagamento: pag, quantidadeParcelas: parc });
      Nord.App.setCartCount(0);
      view(ctx, '<section class="wrap done"><h1>Pedido #' + esc(pedido.id) + ' recebido</h1>' +
        '<p>Total de <strong>' + money(pedido.valorTotal) + '</strong> &middot; ' + esc(pagamentoTexto(pedido)) + '.</p>' +
        '<p class="muted">Status atual: ' + esc(STATUS[pedido.status] || pedido.status) + '. Você acompanha as próximas etapas em Meus pedidos.</p>' +
        '<p><a class="btn btn-dark" href="#/pedidos">Ver meus pedidos</a> <a class="btn btn-ghost" href="#/">Continuar comprando</a></p></section>');
    });
  };

  // ---------- pedidos ----------

  function stepsHtml(status) {
    var idx = STATUS_ORDEM.indexOf(status);
    return '<ol class="steps" aria-label="Andamento do pedido">' + STATUS_ORDEM.map(function (s, i) {
      return '<li class="' + (i < idx ? 'done' : i === idx ? 'now' : '') + '"' + (i === idx ? ' aria-current="step"' : '') + '>' + esc(STATUS[s]) + '</li>';
    }).join('') + '</ol>';
  }

  Pages.pedidos = async function (ctx) {
    if (!needAuth(ctx)) return;
    loading(ctx);
    var pedidos = await Api.meusPedidos();
    if (!pedidos.length) {
      view(ctx, '<section class="wrap"><h1>Meus pedidos</h1>' + UI.empty('Você ainda não fez pedidos', 'Quando finalizar uma compra, ela aparece aqui.', '#/', 'Ver produtos') + '</section>');
      return;
    }
    view(ctx, '<section class="wrap"><h1>Meus pedidos</h1>' + pedidos.map(function (p) {
      return '<article class="order"><header class="order-head"><h2>Pedido #' + esc(p.id) + '</h2><p class="muted">' + esc(UI.dateTime(p.dataPedido)) + '</p></header>' +
        stepsHtml(p.status) +
        '<ul class="order-items">' + p.itens.map(function (i) {
          return '<li>' + esc(i.quantidade) + ' &times; ' + esc(i.nomeProduto) + ' <span class="muted">(' + esc(i.tamanho) + ')</span> <span class="push">' + money(i.precoUnitario * i.quantidade) + '</span></li>';
        }).join('') + '</ul>' +
        '<p class="sum-row"><span>Total</span><strong class="price">' + money(p.valorTotal) + '</strong></p>' +
        '<p class="muted">' + esc(pagamentoTexto(p)) + '</p>' +
        '<p class="muted">Entrega em ' + esc(p.enderecoEntrega) + '</p></article>';
    }).join('') + '</section>');
  };

  // ---------- conta ----------

  Pages.conta = async function (ctx) {
    if (!needAuth(ctx)) return;
    loading(ctx);
    var user = Auth.user();
    var enderecos = await Api.enderecos();
    var editando = null;

    var v = view(ctx, '<section class="wrap account"><h1>Minha conta</h1>' +
      '<p><strong>' + esc(user.nome) + '</strong><br><span class="muted">' + esc(user.email) + '</span></p>' +
      '<h2>Endereços</h2><div id="end-list"></div>' +
      '<h3 id="end-title">Novo endereço</h3>' +
      '<form id="end-form" class="form-narrow" novalidate>' + addressFields() +
      '<div data-error></div>' +
      '<p><button class="btn btn-dark" type="submit">Salvar endereço</button> <button class="btn btn-ghost" type="button" id="end-cancel" hidden>Cancelar edição</button></p></form></section>');
    if (!v) return;

    var listBox = v.querySelector('#end-list');
    var form = v.querySelector('#end-form');
    var title = v.querySelector('#end-title');
    var cancel = v.querySelector('#end-cancel');

    function drawList() {
      listBox.innerHTML = enderecos.length ? '<ul class="addr-list">' + enderecos.map(function (a) {
        return '<li><span>' + addressLine(a) + '</span><span class="addr-actions"><button class="link-btn" type="button" data-edit="' + esc(a.id) + '">Editar</button><button class="link-btn danger" type="button" data-del="' + esc(a.id) + '">Excluir</button></span></li>';
      }).join('') + '</ul>' : '<p class="muted">Você ainda não tem endereços salvos.</p>';
    }
    function setForm(a) {
      editando = a ? a.id : null;
      var vals = ['cep', 'estado', 'rua', 'numero', 'complemento', 'bairro', 'cidade'];
      vals.forEach(function (k) { form.elements[k].value = a && a[k] != null ? a[k] : ''; });
      title.textContent = a ? 'Editar endereço' : 'Novo endereço';
      cancel.hidden = !a;
    }
    drawList();

    listBox.addEventListener('click', async function (ev) {
      var ed = ev.target.closest('[data-edit]');
      var del = ev.target.closest('[data-del]');
      try {
        if (ed) {
          var a = enderecos.filter(function (x) { return String(x.id) === ed.getAttribute('data-edit'); })[0];
          setForm(a);
          form.elements.cep.focus();
        } else if (del && global.confirm('Excluir este endereço?')) {
          await Api.removerEndereco(del.getAttribute('data-del'));
          enderecos = await Api.enderecos();
          drawList();
          UI.toast('Endereço excluído.');
        }
      } catch (e) { fail(e); }
    });
    cancel.addEventListener('click', function () { setForm(null); });

    onSubmit(form, async function () {
      var dados = readAddress(form);
      if (editando) await Api.atualizarEndereco(editando, dados); else await Api.criarEndereco(dados);
      enderecos = await Api.enderecos();
      drawList();
      setForm(null);
      UI.toast('Endereço salvo.');
    });
  };

  // ---------- login e cadastro ----------

  function afterLogin(ctx, data) {
    Auth.set({ token: data.token, id: data.id, nome: data.nome, email: data.email, role: data.role });
    Nord.App.refreshCart();
    var next = ctx.query.next;
    // só aceita caminhos internos (evita redirecionar para site externo)
    if (next && next.charAt(0) === '/' && next.charAt(1) !== '/') Nord.App.go(next);
    else Nord.App.go(data.role === 'ADMIN' ? '/admin' : '/');
  }

  Pages.login = async function (ctx) {
    if (Auth.isLogged()) { Nord.App.go('/'); return; }
    var v = view(ctx, '<section class="wrap auth"><h1>Entrar</h1>' +
      '<form id="f" class="form-narrow" novalidate>' +
      '<label class="f">E-mail<input name="email" type="email" required autocomplete="email"></label>' +
      '<label class="f">Senha<input name="senha" type="password" required autocomplete="current-password"></label>' +
      '<div data-error></div>' +
      '<button class="btn btn-dark btn-block" type="submit">Entrar</button></form>' +
      '<p class="muted">Ainda não tem conta? <a href="#/cadastro' + (ctx.query.next ? '?next=' + encodeURIComponent(ctx.query.next) : '') + '">Criar conta</a></p></section>');
    if (!v) return;
    onSubmit(v.querySelector('#f'), async function () {
      var f = v.querySelector('#f');
      var data = await Api.login(f.elements.email.value.trim(), f.elements.senha.value);
      afterLogin(ctx, data);
    });
  };

  Pages.cadastro = async function (ctx) {
    if (Auth.isLogged()) { Nord.App.go('/'); return; }
    var v = view(ctx, '<section class="wrap auth"><h1>Criar conta</h1>' +
      '<form id="f" class="form-narrow" novalidate>' +
      '<label class="f">Nome<input name="nome" required maxlength="100" autocomplete="name"></label>' +
      '<label class="f">E-mail<input name="email" type="email" required autocomplete="email"></label>' +
      '<label class="f">Senha<input name="senha" type="password" required minlength="8" maxlength="72" autocomplete="new-password"><span class="hint">Mínimo de 8 caracteres.</span></label>' +
      '<div data-error></div>' +
      '<button class="btn btn-dark btn-block" type="submit">Criar conta</button></form>' +
      '<p class="muted">Já tem conta? <a href="#/login' + (ctx.query.next ? '?next=' + encodeURIComponent(ctx.query.next) : '') + '">Entrar</a></p></section>');
    if (!v) return;
    onSubmit(v.querySelector('#f'), async function () {
      var f = v.querySelector('#f');
      var data = await Api.registrar(f.elements.nome.value.trim(), f.elements.email.value.trim(), f.elements.senha.value);
      UI.toast('Conta criada. Bem-vindo(a)!');
      afterLogin(ctx, data);
    });
  };

  // ---------- admin ----------

  function adminTabs(aba) {
    var abas = [['pedidos', 'Pedidos'], ['produtos', 'Produtos'], ['categorias', 'Categorias']];
    return '<nav class="tabs" aria-label="Painel">' + abas.map(function (a) {
      return '<a href="#/admin?aba=' + a[0] + '"' + (a[0] === aba ? ' aria-current="page"' : '') + '>' + a[1] + '</a>';
    }).join('') + '</nav>';
  }

  Pages.admin = async function (ctx) {
    if (!needAuth(ctx)) return;
    if (!Auth.isAdmin()) { UI.toast('Área restrita a administradores.', 'erro'); Nord.App.go('/'); return; }
    var aba = ctx.query.aba || 'pedidos';
    var v = view(ctx, '<section class="wrap"><h1>Painel</h1>' + adminTabs(aba) + '<div id="body"><p class="muted">Carregando…</p></div></section>');
    if (!v) return;
    var body = v.querySelector('#body');
    if (aba === 'produtos') await adminProdutos(ctx, body);
    else if (aba === 'categorias') await adminCategorias(ctx, body);
    else await adminPedidos(ctx, body);
  };

  async function adminPedidos(ctx, body) {
    async function carregar() {
      var pedidos = await Api.adminPedidos();
      if (!ctx.alive()) return;
      if (!pedidos.length) { body.innerHTML = UI.empty('Nenhum pedido ainda', 'Os pedidos feitos pelos clientes aparecem aqui.'); return; }

      body.innerHTML = '<div class="tablewrap"><table class="table"><thead><tr><th>Pedido</th><th>Cliente</th><th>Total</th><th>Status</th></tr></thead><tbody>' +
        pedidos.map(function (p) {
          var opts = STATUS_ORDEM.map(function (s) {
            return '<option value="' + s + '"' + (s === p.status ? ' selected' : '') + '>' + esc(STATUS[s]) + '</option>';
          }).join('');
          return '<tr><td>#' + esc(p.id) + '<br><span class="muted">' + esc(UI.dateTime(p.dataPedido)) + '</span></td>' +
            '<td>' + esc(p.clienteNome) + '<br><span class="muted">' + esc(p.clienteEmail) + '</span></td>' +
            '<td>' + money(p.valorTotal) + '<br><span class="muted">' + esc(pagamentoTexto(p)) + '</span></td>' +
            '<td><div class="inline"><label class="sr" for="st-' + esc(p.id) + '">Status do pedido ' + esc(p.id) + '</label><select id="st-' + esc(p.id) + '">' + opts + '</select>' +
            '<button class="btn btn-dark btn-sm" type="button" data-st="' + esc(p.id) + '">Salvar</button></div></td></tr>' +
            '<tr class="sub"><td colspan="4"><details><summary>Itens e endereço</summary><ul class="order-items">' + p.itens.map(function (i) {
              return '<li>' + esc(i.quantidade) + ' &times; ' + esc(i.nomeProduto) + ' (' + esc(i.tamanho) + ') <span class="push">' + money(i.precoUnitario * i.quantidade) + '</span></li>';
            }).join('') + '</ul><p class="muted">Entrega em ' + esc(p.enderecoEntrega) + '</p></details></td></tr>';
        }).join('') + '</tbody></table></div>';
    }

    // O listener é ligado UMA vez; recarregar a tabela só troca o HTML interno.
    body.addEventListener('click', async function (ev) {
      var b = ev.target.closest('[data-st]');
      if (!b) return;
      var id = b.getAttribute('data-st');
      b.disabled = true;
      try {
        await Api.adminStatus(id, body.querySelector('#st-' + id).value);
        UI.toast('Status atualizado.');
        await carregar();
      } catch (e) { fail(e); b.disabled = false; }
    });

    await carregar();
  }

  async function adminProdutos(ctx, body) {
    var res = await Promise.all([Api.produtos(''), Api.categorias()]);
    if (!ctx.alive()) return;
    var produtos = res[0], cats = res[1];
    var editando = null;

    var catOpts = cats.map(function (c) { return '<option value="' + esc(c.id) + '">' + esc(c.nome) + '</option>'; }).join('');
    body.innerHTML = (cats.length ? '' : '<p class="alert">Crie ao menos uma categoria antes de cadastrar produtos.</p>') +
      '<div id="plist"></div>' +
      '<h2 id="ptitle">Novo produto</h2>' +
      '<form id="pform" class="form-narrow" novalidate>' +
      '<label class="f">Nome<input name="nome" required maxlength="150"></label>' +
      '<label class="f">Categoria<select name="categoriaId" required>' + catOpts + '</select></label>' +
      '<div class="grid2"><label class="f">Preço (R$)<input name="preco" type="number" step="0.01" min="0.01" required></label>' +
      '<label class="f">Estoque<input name="quantidade" type="number" min="0" step="1" required></label></div>' +
      '<label class="f">Tamanhos<input name="tamanho" maxlength="100" placeholder="P, M, G, GG (deixe vazio se for tamanho único)"></label>' +
      '<label class="f">Endereço da imagem (opcional)<input name="imagem" type="url" maxlength="1000" placeholder="https://..."></label>' +
      '<label class="f">Descrição<textarea name="descricao" rows="3" maxlength="1000"></textarea></label>' +
      '<div data-error></div>' +
      '<p><button class="btn btn-dark" type="submit">Salvar produto</button> <button class="btn btn-ghost" type="button" id="pcancel" hidden>Cancelar edição</button></p></form>';

    var list = body.querySelector('#plist');
    var form = body.querySelector('#pform');
    var title = body.querySelector('#ptitle');
    var cancel = body.querySelector('#pcancel');

    function drawList() {
      list.innerHTML = produtos.length ? '<div class="tablewrap"><table class="table"><thead><tr><th>Produto</th><th>Categoria</th><th>Preço</th><th>Estoque</th><th></th></tr></thead><tbody>' +
        produtos.map(function (p) {
          return '<tr><td>' + esc(p.nome) + '<br><span class="muted">' + esc(p.tamanho || 'Tamanho único') + '</span></td><td>' + esc(p.categoriaNome) + '</td><td>' + money(p.preco) + '</td><td>' + esc(p.quantidade) + '</td>' +
            '<td><button class="link-btn" type="button" data-edit="' + esc(p.id) + '">Editar</button> <button class="link-btn danger" type="button" data-del="' + esc(p.id) + '">Excluir</button></td></tr>';
        }).join('') + '</tbody></table></div>' : UI.empty('Nenhum produto cadastrado', 'Use o formulário abaixo para cadastrar o primeiro.');
    }
    function setForm(p) {
      editando = p ? p.id : null;
      form.elements.nome.value = p ? p.nome : '';
      if (p) form.elements.categoriaId.value = String(p.categoriaId);
      form.elements.preco.value = p ? p.preco : '';
      form.elements.quantidade.value = p ? p.quantidade : '';
      form.elements.tamanho.value = p && p.tamanho ? p.tamanho : '';
      form.elements.imagem.value = p && p.imagem ? p.imagem : '';
      form.elements.descricao.value = p && p.descricao ? p.descricao : '';
      title.textContent = p ? 'Editar produto #' + p.id : 'Novo produto';
      cancel.hidden = !p;
    }
    drawList();

    list.addEventListener('click', async function (ev) {
      var ed = ev.target.closest('[data-edit]');
      var del = ev.target.closest('[data-del]');
      try {
        if (ed) {
          var p = produtos.filter(function (x) { return String(x.id) === ed.getAttribute('data-edit'); })[0];
          setForm(p); form.elements.nome.focus();
        } else if (del) {
          var alvo = produtos.filter(function (x) { return String(x.id) === del.getAttribute('data-del'); })[0];
          if (!global.confirm('Excluir "' + alvo.nome + '"?')) return;
          await Api.removerProduto(alvo.id);
          produtos = await Api.produtos('');
          drawList(); UI.toast('Produto excluído.');
        }
      } catch (e) { fail(e); }
    });
    cancel.addEventListener('click', function () { setForm(null); });

    onSubmit(form, async function () {
      var f = form.elements;
      var dados = {
        nome: f.nome.value.trim(), categoriaId: Number(f.categoriaId.value),
        preco: parseFloat(f.preco.value), quantidade: parseInt(f.quantidade.value, 10),
        tamanho: f.tamanho.value.trim(), imagem: f.imagem.value.trim(), descricao: f.descricao.value.trim()
      };
      await Api.salvarProduto(editando, dados);
      produtos = await Api.produtos('');
      drawList(); setForm(null); UI.toast('Produto salvo.');
    });
  }

  async function adminCategorias(ctx, body) {
    var cats = await Api.categorias();
    if (!ctx.alive()) return;

    function draw() {
      body.innerHTML = '<ul class="addr-list">' + (cats.length ? cats.map(function (c) {
        return '<li><form class="inline cat-form" data-id="' + esc(c.id) + '"><label class="sr" for="cn-' + esc(c.id) + '">Nome da categoria</label>' +
          '<input id="cn-' + esc(c.id) + '" name="nome" value="' + esc(c.nome) + '" required maxlength="60">' +
          '<button class="btn btn-dark btn-sm" type="submit">Renomear</button>' +
          '<button class="link-btn danger" type="button" data-del="' + esc(c.id) + '">Excluir</button></form></li>';
      }).join('') : '<li class="muted">Nenhuma categoria ainda.</li>') + '</ul>' +
        '<h2>Nova categoria</h2><form id="cnew" class="inline"><label class="sr" for="cnew-n">Nome</label><input id="cnew-n" name="nome" required maxlength="60" placeholder="Ex.: Acessórios"><button class="btn btn-dark" type="submit">Adicionar</button></form>' +
        '<div data-error></div>';
    }
    draw();

    async function reload() { cats = await Api.categorias(); draw(); }

    body.addEventListener('submit', async function (ev) {
      ev.preventDefault();
      var f = ev.target;
      try {
        if (f.id === 'cnew') { await Api.salvarCategoria(null, f.elements.nome.value.trim()); UI.toast('Categoria criada.'); }
        else { await Api.salvarCategoria(f.getAttribute('data-id'), f.elements.nome.value.trim()); UI.toast('Categoria atualizada.'); }
        await reload();
      } catch (e) { fail(e); }
    });
    body.addEventListener('click', async function (ev) {
      var del = ev.target.closest('[data-del]');
      if (!del || !global.confirm('Excluir esta categoria?')) return;
      try { await Api.removerCategoria(del.getAttribute('data-del')); UI.toast('Categoria excluída.'); await reload(); }
      catch (e) { fail(e); }
    });
  }

})(window);
