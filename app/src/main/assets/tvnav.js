/* Navegação por controle remoto (setas + OK) para o site Halley Play.
   Injetado pelo app depois que cada página termina de carregar. */
(function () {
  if (window.__tvNavInstalled) { return; }
  window.__tvNavInstalled = true;

  // ---------- estilo do destaque ----------
  function addStyle() {
    if (document.getElementById('__tvnav_style')) { return; }
    var st = document.createElement('style');
    st.id = '__tvnav_style';
    st.textContent =
      '.__tvfocus{outline:4px solid #19a7ce !important;outline-offset:2px !important;' +
      'box-shadow:0 0 0 7px rgba(25,167,206,.35) !important;border-radius:4px;}';
    (document.head || document.documentElement).appendChild(st);
  }
  addStyle();

  var SEL = 'a[href],button,input:not([type=hidden]),select,textarea,' +
    '[tabindex]:not([tabindex="-1"]),[onclick],[role=button],[role=link],' +
    '[role=tab],[role=menuitem],summary,label';
  var STRICT = /^(A|BUTTON|INPUT|SELECT|TEXTAREA|SUMMARY)$/;

  var current = null;
  var rawCache = null;
  var rawCacheTime = 0;

  function safeMatches(el, sel) {
    try { return el.matches(sel); } catch (e) { return false; }
  }

  // Todos os elementos "clicáveis" (por tag/atributo ou por cursor de mãozinha).
  function rawCandidates() {
    var now = Date.now();
    if (rawCache && now - rawCacheTime < 1500) { return rawCache; }
    var out = [];
    var all = document.body ? document.body.querySelectorAll('*') : [];
    for (var i = 0; i < all.length; i++) {
      var el = all[i];
      if (safeMatches(el, SEL)) { out.push(el); continue; }
      var cs = getComputedStyle(el);
      if (cs.cursor === 'pointer') {
        var p = el.parentElement;
        if (!p || getComputedStyle(p).cursor !== 'pointer') { out.push(el); }
      }
    }
    rawCache = out;
    rawCacheTime = now;
    return out;
  }

  function isShown(el) {
    var r = el.getBoundingClientRect();
    if (r.width < 8 || r.height < 8) { return false; }
    var vw = window.innerWidth, vh = window.innerHeight;
    // horizontal: só o que está na tela; vertical: um pouco além, para poder rolar
    if (r.right < 0 || r.left > vw) { return false; }
    if (r.bottom < -vh || r.top > vh * 2) { return false; }
    var cs = getComputedStyle(el);
    if (cs.visibility === 'hidden' || cs.display === 'none' || cs.opacity === '0' ||
        cs.pointerEvents === 'none') { return false; }
    if (r.width > vw * 0.9 && r.height > vh * 0.5 && !STRICT.test(el.tagName)) { return false; }
    return true;
  }

  function candidates() {
    var raw = rawCandidates();
    var list = [];
    for (var i = 0; i < raw.length; i++) {
      if (document.documentElement.contains(raw[i]) && isShown(raw[i])) { list.push(raw[i]); }
    }
    // mantém só o elemento mais externo de cada "cartão" clicável
    var out = [];
    for (var j = 0; j < list.length; j++) {
      var el = list[j], nested = false;
      for (var k = 0; k < list.length; k++) {
        if (k !== j && list[k] !== el && list[k].contains(el)) { nested = true; break; }
      }
      if (!nested) { out.push(el); }
    }
    return out;
  }

  function setFocus(el) {
    if (current) { current.classList.remove('__tvfocus'); }
    current = el;
    el.classList.add('__tvfocus');
    try { el.focus({ preventScroll: true }); } catch (e) { /* ignora */ }
    try { el.scrollIntoView({ block: 'nearest', inline: 'nearest' }); } catch (e2) { /* ignora */ }
  }

  function firstCandidate(list) {
    var best = null, bestScore = Infinity;
    for (var i = 0; i < list.length; i++) {
      var r = list[i].getBoundingClientRect();
      if (r.bottom < 0) { continue; }
      var score = Math.max(r.top, 0) * 3 + r.left;
      if (score < bestScore) { bestScore = score; best = list[i]; }
    }
    return best || list[0] || null;
  }

  function move(dir) {
    var list = candidates();
    if (!list.length) { return; }

    if (!current || !document.documentElement.contains(current) || !isShown(current)) {
      var first = firstCandidate(list);
      if (first) { setFocus(first); }
      return;
    }

    var cr = current.getBoundingClientRect();
    var ccx = cr.left + cr.width / 2, ccy = cr.top + cr.height / 2;
    var best = null, bestScore = Infinity;

    for (var i = 0; i < list.length; i++) {
      var el = list[i];
      if (el === current || el.contains(current) || current.contains(el)) { continue; }
      var r = el.getBoundingClientRect();
      var cx = r.left + r.width / 2, cy = r.top + r.height / 2;
      var dx = cx - ccx, dy = cy - ccy, axis, perp;
      if (dir === 'right') { if (dx <= 4) { continue; } axis = dx; perp = Math.abs(dy); }
      else if (dir === 'left') { if (dx >= -4) { continue; } axis = -dx; perp = Math.abs(dy); }
      else if (dir === 'down') { if (dy <= 4) { continue; } axis = dy; perp = Math.abs(dx); }
      else { if (dy >= -4) { continue; } axis = -dy; perp = Math.abs(dx); }
      var score = axis + perp * 2.5;
      if (score < bestScore) { bestScore = score; best = el; }
    }

    if (best) {
      setFocus(best);
    } else if (dir === 'down') {
      window.scrollBy(0, Math.round(window.innerHeight * 0.6));
    } else if (dir === 'up') {
      window.scrollBy(0, -Math.round(window.innerHeight * 0.6));
    }
  }

  function isTextField(t) {
    if (!t || !t.tagName) { return false; }
    if (t.tagName === 'TEXTAREA' || t.isContentEditable) { return true; }
    if (t.tagName === 'INPUT') {
      return !/^(button|checkbox|radio|submit|reset|image|range|file|color)$/i.test(t.type || 'text');
    }
    return false;
  }

  function fire(el, type, x, y) {
    try {
      el.dispatchEvent(new MouseEvent(type, {
        bubbles: true, cancelable: true, view: window, clientX: x, clientY: y, button: 0
      }));
    } catch (e) { /* ignora */ }
  }

  function activate(el) {
    rawCache = null;
    if (isTextField(el)) {
      try { el.focus(); } catch (e) { /* ignora */ }
      if (window.TvBridge && window.TvBridge.showKeyboard) { window.TvBridge.showKeyboard(); }
      return;
    }
    var r = el.getBoundingClientRect();
    var x = r.left + r.width / 2, y = r.top + r.height / 2;
    fire(el, 'mousedown', x, y);
    fire(el, 'mouseup', x, y);
    fire(el, 'click', x, y);
  }

  function toggleVideo() {
    var v = document.querySelector('video');
    if (!v) { return false; }
    if (v.paused) { v.play(); } else { v.pause(); }
    return true;
  }

  window.addEventListener('keydown', function (e) {
    var k = e.key, kc = e.keyCode, dir = null;
    if (k === 'ArrowUp' || kc === 38) { dir = 'up'; }
    else if (k === 'ArrowDown' || kc === 40) { dir = 'down'; }
    else if (k === 'ArrowLeft' || kc === 37) { dir = 'left'; }
    else if (k === 'ArrowRight' || kc === 39) { dir = 'right'; }
    var isEnter = (k === 'Enter' || kc === 13);
    if (!dir && !isEnter) { return; }

    var fs = document.fullscreenElement || document.webkitFullscreenElement;
    if (fs) {
      // vídeo em tela cheia: OK pausa/retoma, setas ficam com o player
      if (isEnter) { e.preventDefault(); e.stopPropagation(); toggleVideo(); }
      return;
    }

    var typing = isTextField(e.target);
    if (dir) {
      if (typing && (dir === 'left' || dir === 'right')) { return; }
      e.preventDefault();
      e.stopPropagation();
      if (typing) { try { e.target.blur(); } catch (er) { /* ignora */ } }
      move(dir);
      return;
    }

    if (isEnter) {
      if (typing) { return; }
      if (current && document.documentElement.contains(current)) {
        e.preventDefault();
        e.stopPropagation();
        activate(current);
      }
    }
  }, true);
})();
