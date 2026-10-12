(function (global) {
  'use strict';
  var Nord = global.Nord = global.Nord || {};

  var brl = new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' });

  // Todo texto vindo do servidor passa por esc() antes de entrar no HTML (proteção contra XSS).
  function esc(v) {
    return String(v == null ? '' : v).replace(/[&<>"']/g, function (c) {
      return { '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c];
    });
  }

  function money(n) { return brl.format(Number(n) || 0); }

  function dateTime(iso) {
    if (!iso) return '';
    var d = new Date(iso);
    if (isNaN(d.getTime())) return '';
    return d.toLocaleString('pt-BR', { day: '2-digit', month: '2-digit', year: 'numeric', hour: '2-digit', minute: '2-digit' });
  }

  function isHttp(u) { return typeof u === 'string' && /^https?:\/\//i.test(u); }

  function initials(name) {
    var w = String(name || '').trim().split(/\s+/);
    return (((w[0] || '')[0] || '') + ((w[1] || '')[0] || '')).toUpperCase();
  }

  // Imagem do produto; sem URL válida mostra um bloco colorido com as iniciais.
  function media(p, alt) {
    var url = p.imagem || p.imagemProduto;
    var tone = (Number(p.categoriaId != null ? p.categoriaId : p.produtoId) || 0) % 4;
    var ini = initials(p.nome || p.nomeProduto);
    var inner = isHttp(url)
      ? '<img src="' + esc(url) + '" alt="' + esc(alt || '') + '" loading="lazy" data-ini="' + esc(ini) + '">'
      : '<span class="ini" aria-hidden="true">' + esc(ini) + '</span>';
    return '<div class="media" data-tone="' + tone + '">' + inner + '</div>';
  }

  function toast(msg, kind) {
    var box = document.getElementById('toasts');
    if (!box) return;
    var el = document.createElement('div');
    el.className = 'toast' + (kind === 'erro' ? ' toast-erro' : '');
    el.textContent = msg;
    box.appendChild(el);
    setTimeout(function () { if (el.parentNode) el.parentNode.removeChild(el); }, kind === 'erro' ? 5000 : 3000);
  }

  // Mensagem de erro + lista de campos inválidos (quando o servidor devolve "erros")
  function errorHtml(err) {
    var html = '<p class="alert" role="alert">' + esc(err && err.message ? err.message : 'Algo deu errado.') + '</p>';
    if (err && err.fields) {
      var items = Object.keys(err.fields).map(function (k) {
        return '<li><strong>' + esc(k) + ':</strong> ' + esc(err.fields[k]) + '</li>';
      }).join('');
      if (items) html += '<ul class="alert-list">' + items + '</ul>';
    }
    return html;
  }

  function empty(title, text, linkHref, linkText) {
    return '<div class="empty"><h2>' + esc(title) + '</h2>' + (text ? '<p>' + esc(text) + '</p>' : '') +
      (linkHref ? '<a class="btn btn-dark" href="' + esc(linkHref) + '">' + esc(linkText) + '</a>' : '') + '</div>';
  }

  Nord.UI = { esc: esc, money: money, dateTime: dateTime, isHttp: isHttp, media: media, toast: toast, errorHtml: errorHtml, empty: empty };
})(window);
