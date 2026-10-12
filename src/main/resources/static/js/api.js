(function (global) {
  'use strict';
  var Nord = global.Nord = global.Nord || {};
  var BASE = ((global.NORD_CONFIG && global.NORD_CONFIG.API_URL) || '').replace(/\/$/, '');
  var KEY = 'nord.auth';

  function ApiError(status, message, fields) {
    this.name = 'ApiError';
    this.status = status;
    this.message = message;
    this.fields = fields || null;
  }
  ApiError.prototype = Object.create(Error.prototype);

  // Guarda token + dados básicos do usuário. O servidor é quem decide o que cada um pode fazer;
  // o "role" aqui serve apenas para mostrar ou esconder menus.
  var Auth = {
    get: function () {
      try { return JSON.parse(localStorage.getItem(KEY)); } catch (e) { return null; }
    },
    set: function (data) { localStorage.setItem(KEY, JSON.stringify(data)); },
    clear: function () { localStorage.removeItem(KEY); },
    token: function () { var a = this.get(); return a ? a.token : null; },
    user: function () { return this.get(); },
    isLogged: function () { return !!this.token(); },
    isAdmin: function () { var a = this.get(); return !!a && a.role === 'ADMIN'; }
  };

  async function request(path, opts) {
    opts = opts || {};
    var method = opts.method || 'GET';
    var useAuth = opts.auth !== false;
    var headers = {};
    var token = Auth.token();
    if (opts.body !== undefined) headers['Content-Type'] = 'application/json';
    if (useAuth && token) headers['Authorization'] = 'Bearer ' + token;

    var res;
    try {
      res = await fetch(BASE + path, {
        method: method,
        headers: headers,
        body: opts.body !== undefined ? JSON.stringify(opts.body) : undefined
      });
    } catch (e) {
      throw new ApiError(0, 'Não foi possível falar com o servidor. Verifique sua conexão e tente de novo.');
    }

    if (res.status === 204) return null;

    var data = null;
    var text = await res.text();
    if (text) { try { data = JSON.parse(text); } catch (e) { data = null; } }

    if (!res.ok) {
      if (res.status === 401 && useAuth && token) {
        Auth.clear();
        global.dispatchEvent(new Event('nord:logout'));
      }
      var msg = (data && data.mensagem) ||
        (res.status === 403 ? 'Você não tem permissão para isso.' : 'Algo deu errado (código ' + res.status + ').');
      throw new ApiError(res.status, msg, data && data.erros);
    }
    return data;
  }

  var enc = encodeURIComponent;

  var Api = {
    // autenticação
    login: function (email, senha) { return request('/api/auth/login', { method: 'POST', body: { email: email, senha: senha }, auth: false }); },
    registrar: function (nome, email, senha) { return request('/api/auth/registrar', { method: 'POST', body: { nome: nome, email: email, senha: senha }, auth: false }); },
    // catálogo (público)
    categorias: function () { return request('/api/categorias', { auth: false }); },
    produtos: function (categoriaId) { return request('/api/produtos' + (categoriaId ? '?categoriaId=' + enc(categoriaId) : ''), { auth: false }); },
    produto: function (id) { return request('/api/produtos/' + enc(id), { auth: false }); },
    // carrinho
    carrinho: function () { return request('/api/carrinho'); },
    addItem: function (produtoId, tamanho, quantidade) { return request('/api/carrinho/itens', { method: 'POST', body: { produtoId: produtoId, tamanho: tamanho, quantidade: quantidade } }); },
    setQuantidade: function (itemId, quantidade) { return request('/api/carrinho/itens/' + enc(itemId), { method: 'PUT', body: { quantidade: quantidade } }); },
    removeItem: function (itemId) { return request('/api/carrinho/itens/' + enc(itemId), { method: 'DELETE' }); },
    // endereços
    enderecos: function () { return request('/api/enderecos'); },
    criarEndereco: function (dados) { return request('/api/enderecos', { method: 'POST', body: dados }); },
    atualizarEndereco: function (id, dados) { return request('/api/enderecos/' + enc(id), { method: 'PUT', body: dados }); },
    removerEndereco: function (id) { return request('/api/enderecos/' + enc(id), { method: 'DELETE' }); },
    // pedidos
    criarPedido: function (dados) { return request('/api/pedidos', { method: 'POST', body: dados }); },
    meusPedidos: function () { return request('/api/pedidos/meus'); },
    // admin
    adminPedidos: function () { return request('/api/admin/pedidos'); },
    adminStatus: function (id, status) { return request('/api/admin/pedidos/' + enc(id) + '/status', { method: 'PATCH', body: { status: status } }); },
    salvarProduto: function (id, dados) { return id ? request('/api/produtos/' + enc(id), { method: 'PUT', body: dados }) : request('/api/produtos', { method: 'POST', body: dados }); },
    removerProduto: function (id) { return request('/api/produtos/' + enc(id), { method: 'DELETE' }); },
    salvarCategoria: function (id, nome) { return id ? request('/api/categorias/' + enc(id), { method: 'PUT', body: { nome: nome } }) : request('/api/categorias', { method: 'POST', body: { nome: nome } }); },
    removerCategoria: function (id) { return request('/api/categorias/' + enc(id), { method: 'DELETE' }); }
  };

  Nord.ApiError = ApiError;
  Nord.Auth = Auth;
  Nord.Api = Api;
})(window);
