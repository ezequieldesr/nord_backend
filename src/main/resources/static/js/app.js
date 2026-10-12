(function (global) {
  'use strict';
  var Nord = global.Nord;
  var Api = Nord.Api, Auth = Nord.Auth, UI = Nord.UI, esc = UI.esc;

  var renderToken = 0;   // identifica a navegação atual; respostas atrasadas de páginas antigas são descartadas
  var cats = [];
  var cartCount = 0;

  var routes = [
    [/^\/$/, 'home'],
    [/^\/produto\/(\d+)$/, 'produto'],
    [/^\/carrinho$/, 'carrinho'],
    [/^\/checkout$/, 'checkout'],
    [/^\/pedidos$/, 'pedidos'],
    [/^\/conta$/, 'conta'],
    [/^\/login$/, 'login'],
    [/^\/cadastro$/, 'cadastro'],
    [/^\/admin$/, 'admin']
  ];

  function parseHash() {
    var h = (global.location.hash || '#/').slice(1) || '/';
    var i = h.indexOf('?');
    var path = i < 0 ? h : h.slice(0, i);
    var query = {};
    if (i >= 0) new URLSearchParams(h.slice(i + 1)).forEach(function (v, k) { query[k] = v; });
    return { path: path, query: query, fullPath: h };
  }

  function go(path) {
    var target = '#' + path;
    if (global.location.hash === target) render(); else global.location.hash = target;
  }

  function updateBadge() {
    var el = document.getElementById('cart-count');
    if (el) { el.textContent = String(cartCount); el.hidden = cartCount === 0; }
  }
  function setCartCount(n) { cartCount = n; updateBadge(); }
  function setCats(c) { cats = c || []; renderHeader(); }

  async function refreshCart() {
    if (!Auth.isLogged()) { setCartCount(0); return; }
    try {
      var c = await Api.carrinho();
      setCartCount(c.itens.reduce(function (s, i) { return s + i.quantidade; }, 0));
    } catch (e) { setCartCount(0); }
  }

  function renderHeader() {
    var user = Auth.user();
    var current = parseHash().path;
    var catLinks = cats.map(function (c) {
      return '<a href="#/?cat=' + esc(c.id) + '">' + esc(c.nome) + '</a>';
    }).join('');

    var conta = Auth.isLogged()
      ? '<a href="#/pedidos"' + (current === '/pedidos' ? ' aria-current="page"' : '') + '>Pedidos</a>' +
        '<a href="#/conta"' + (current === '/conta' ? ' aria-current="page"' : '') + '>Conta</a>' +
        (Auth.isAdmin() ? '<a href="#/admin"' + (current === '/admin' ? ' aria-current="page"' : '') + '>Painel</a>' : '') +
        '<button class="link-btn" type="button" data-logout>Sair</button>'
      : '<a href="#/login"' + (current === '/login' ? ' aria-current="page"' : '') + '>Entrar</a>' +
        '<a class="btn btn-dark btn-sm" href="#/cadastro">Criar conta</a>';

    document.getElementById('topbar').innerHTML =
      '<div class="wrap topbar-in">' +
      '<a class="brand" href="#/" aria-label="Nord, página inicial">' +
      '<svg viewBox="0 0 32 32" width="28" height="28" aria-hidden="true" focusable="false"><path d="M2 27 12 10l5 8 4-5 9 14z" fill="#123047"/><circle cx="24" cy="8" r="3.4" fill="#FF5F1F"/></svg>' +
      '<span>nord</span></a>' +
      '<nav class="cats" aria-label="Categorias">' + catLinks + '</nav>' +
      '<nav class="acct" aria-label="Conta">' + conta +
      '<a class="cart" href="#/carrinho" aria-label="Carrinho"><svg viewBox="0 0 24 24" width="22" height="22" aria-hidden="true" focusable="false"><path d="M3 4h2.2l2.1 10.2a1 1 0 0 0 1 .8h8.4a1 1 0 0 0 1-.76L19.5 8H6.1" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"/><circle cx="9.5" cy="19" r="1.4" fill="currentColor"/><circle cx="16.5" cy="19" r="1.4" fill="currentColor"/></svg>' +
      '<span id="cart-count" class="count"' + (cartCount === 0 ? ' hidden' : '') + '>' + cartCount + '</span></a>' +
      '</nav></div>';
  }

  function errorView(e, retry) {
    var app = document.getElementById('app');
    var notFound = e && e.status === 404;
    app.innerHTML = '<div class="view"><div class="wrap">' +
      UI.empty(notFound ? 'Não encontramos o que você procurava' : 'Não foi possível carregar esta página',
               notFound ? 'O endereço pode ter mudado ou o produto não está mais à venda.' : (e && e.message ? e.message : ''),
               notFound ? '#/' : null, 'Voltar para o início') +
      (notFound ? '' : '<p class="center"><button class="btn btn-dark" type="button" data-retry>Tentar de novo</button></p>') +
      '</div></div>';
    var b = app.querySelector('[data-retry]');
    if (b) b.addEventListener('click', retry);
  }

  async function render() {
    var token = ++renderToken;
    var r = parseHash();
    var name = null, params = [];
    for (var i = 0; i < routes.length; i++) {
      var m = r.path.match(routes[i][0]);
      if (m) { name = routes[i][1]; params = m.slice(1); break; }
    }
    renderHeader();
    if (!name) { errorView({ status: 404 }, render); return; }

    var ctx = {
      params: params, query: r.query, fullPath: r.fullPath, path: r.path,
      alive: function () { return token === renderToken; }
    };
    try {
      await Nord.Pages[name](ctx);
    } catch (e) {
      if (token !== renderToken) return;
      if (e && e.status === 401) return; // sessão expirou: o evento nord:logout cuida do redirecionamento
      errorView(e, render);
    }
    if (token === renderToken) renderHeader();
  }

  function init() {
    // Se a imagem de um produto falhar (link quebrado), troca pelas iniciais.
    document.addEventListener('error', function (ev) {
      var t = ev.target;
      if (t && t.tagName === 'IMG' && t.parentNode && t.hasAttribute('data-ini')) {
        var s = document.createElement('span');
        s.className = 'ini'; s.setAttribute('aria-hidden', 'true');
        s.textContent = t.getAttribute('data-ini');
        t.parentNode.replaceChild(s, t);
      }
    }, true);

    document.getElementById('topbar').addEventListener('click', function (ev) {
      if (ev.target.closest('[data-logout]')) {
        Auth.clear(); setCartCount(0);
        UI.toast('Você saiu da sua conta.');
        go('/');
        renderHeader();
      }
    });

    var skip = document.querySelector('[data-skip]');
    if (skip) skip.addEventListener('click', function (ev) {
      ev.preventDefault();
      var app = document.getElementById('app');
      app.focus();
    });

    global.addEventListener('hashchange', render);
    global.addEventListener('nord:logout', function () {
      setCartCount(0);
      UI.toast('Sua sessão expirou. Entre novamente.', 'erro');
      go('/login?next=' + encodeURIComponent(parseHash().fullPath));
    });

    refreshCart();
    Api.categorias().then(setCats).catch(function () {});
    render();
  }

  Nord.App = { go: go, refreshCart: refreshCart, setCartCount: setCartCount, setCats: setCats, render: render };

  if (document.readyState === 'loading') document.addEventListener('DOMContentLoaded', init); else init();
})(window);
