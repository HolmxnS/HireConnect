(function () {
    'use strict';
    var root = document.querySelector('.hc-chat-root');
    if (!root || root.dataset.initialized) return;
    root.dataset.initialized = 'true';
    var script = document.currentScript;
    var endpoint = script && script.dataset.endpoint || '/postulante/chat';
    var panel = root.querySelector('#hcChatPanel'), toggle = root.querySelector('#hcChatToggle');
    var messages = root.querySelector('#hcChatMessages'), input = root.querySelector('#hcChatInput');
    var send = root.querySelector('#hcChatSend'), stop = root.querySelector('#hcChatStop');
    var chips = root.querySelector('#hcChatChips'), status = root.querySelector('#hcChatStatus');
    var counter = root.querySelector('#hcChatCounter'), expand = root.querySelector('#hcChatExpand');
    var storageKey = root.dataset.chatUser ? 'hcChat:' + root.dataset.chatUser : null;
    var conversation = [], busy = false, controller, generation = 0, pending, retryBubble, lastQuestion;
    var starters = ['Revisa mi perfil', 'Busca vacantes para mí', 'Practicar una entrevista', 'Analiza esta vacante'];
    try {
        var saved = storageKey && JSON.parse(sessionStorage.getItem(storageKey) || '[]');
        if (Array.isArray(saved)) conversation = saved.filter(function (t) {
            return t && ['user', 'model'].includes(t.rol) && typeof t.texto === 'string' && t.texto.length <= 6000;
        }).slice(-24);
    } catch (error) {}
    function save() {
        if (!storageKey) return;
        try { sessionStorage.setItem(storageKey, JSON.stringify(conversation.slice(-24))); } catch (error) {}
    }
    function el(tag, className, text) {
        var node = document.createElement(tag);
        if (className) node.className = className;
        if (text !== undefined) node.textContent = text;
        return node;
    }
    function safeLink(path) {
        if (typeof path !== 'string' || !path.startsWith('/postulante/') || path.startsWith('//')) return null;
        try {
            var url = new URL(path, location.origin);
            return url.origin === location.origin && /^\/postulante\/(?:vacantes|perfil|postulaciones|entrevistas|proyectos|(?:vacante|empresas)\/[a-f\d]{24})$/i.test(url.pathname) ? url.pathname + url.search : null;
        } catch (error) { return null; }
    }
    function inline(parent, text) {
        var pattern = /\*\*([^*]+)\*\*|\[([^\]]+)\]\(([^)]+)\)/g, match, last = 0;
        while ((match = pattern.exec(text))) {
            parent.append(document.createTextNode(text.slice(last, match.index)));
            if (match[1]) parent.append(el('strong', '', match[1]));
            else {
                var path = safeLink(match[3]);
                if (path) { var link = el('a', '', match[2]); link.href = path; parent.append(link); }
                else parent.append(document.createTextNode(match[2]));
            }
            last = pattern.lastIndex;
        }
        parent.append(document.createTextNode(text.slice(last)));
    }
    function renderText(parent, text) {
        var list;
        String(text).split(/\r?\n/).forEach(function (line) {
            var item = line.trim().match(/^(?:[-*•]|\d+[.)])\s+(.*)$/);
            if (item) {
                if (!list) { list = el('ul'); parent.append(list); }
                var li = el('li'); inline(li, item[1]); list.append(li);
            } else if (line.trim()) {
                list = null; var p = el('p'); inline(p, line.replace(/^#{1,6}\s*/, '')); parent.append(p);
            }
        });
    }
    function scroll() { messages.scrollTop = messages.scrollHeight; }
    function message(turn) {
        var wrap = el('article', 'hc-msg ' + (turn.rol === 'user' ? 'hc-msg--user' : 'hc-msg--bot'));
        if (turn.rol === 'user') wrap.append(el('p', '', turn.texto));
        else {
            wrap.append(el('span', 'hc-msg__author', turn.modo === 'ia' ? 'HireConnect · IA' : 'HireConnect'));
            renderText(wrap, turn.texto);
            if (Array.isArray(turn.vacantes)) turn.vacantes.slice(0, 4).forEach(function (v) {
                if (!v || !safeLink(v.url)) return;
                var a = el('a', 'hc-chat__job'); a.href = safeLink(v.url);
                a.append(el('small', '', String(v.empresa || 'Empresa').slice(0, 100)), el('strong', '', String(v.titulo || 'Vacante').slice(0, 100)), el('span', '', String(v.modalidad || '') + ' · ' + String(v.salario || 'A convenir')), el('p', '', String(v.motivo || '').slice(0, 300)), el('b', '', 'Ver vacante ↗'));
                wrap.append(a);
            });
            if (Array.isArray(turn.acciones)) {
                var actions = el('div', 'hc-chat__links');
                turn.acciones.slice(0, 3).forEach(function (a) {
                    if (!a || !safeLink(a.url)) return;
                    var link = el('a', '', String(a.texto || 'Abrir').slice(0, 80)); link.href = safeLink(a.url); actions.append(link);
                });
                wrap.append(actions);
            }
            var copy = el('button', 'hc-msg__copy', 'Copiar'); copy.type = 'button'; copy.setAttribute('aria-label', 'Copiar respuesta');
            copy.addEventListener('click', async function () {
                try {
                    if (!navigator.clipboard) throw new Error('unavailable');
                    await navigator.clipboard.writeText(turn.texto); status.textContent = 'Respuesta copiada.';
                } catch (error) { status.textContent = 'No pude copiar automáticamente. Puedes seleccionar el texto de la respuesta.'; }
            });
            wrap.append(copy);
        }
        messages.append(wrap);
        while (messages.childElementCount > 40) messages.firstElementChild.remove(); scroll(); return wrap;
    }
    function suggestions(items) {
        chips.replaceChildren();
        (Array.isArray(items) ? items : starters).slice(0, 4).forEach(function (text) {
            if (typeof text !== 'string') return;
            var button = el('button', '', text.slice(0, 120)); button.type = 'button';
            button.disabled = busy; button.addEventListener('click', function () { sendMessage(text); }); chips.append(button);
        });
    }
    function welcome() {
        var box = el('div', 'hc-chat__welcome');
        box.append(el('span', 'hc-chat__welcome-spark', '✦'), el('h2', '', 'Hola' + (root.dataset.chatName ? ', ' + root.dataset.chatName.split(' ')[0] : '') + '.'), el('p', '', 'Hagamos más fácil tu búsqueda de empleo.'), el('small', '', 'Podemos revisar tu perfil, explorar ofertas o preparar tu próxima entrevista.'));
        messages.append(box); suggestions(starters);
    }
    function syncInput() {
        send.disabled = busy || !input.value.trim(); stop.hidden = !busy; send.hidden = busy;
        counter.textContent = input.value.length + '/1000';
        input.style.height = 'auto'; input.style.height = Math.min(input.scrollHeight, 120) + 'px';
    }
    function setBusy(value) { busy = value; messages.setAttribute('aria-busy', String(value)); syncInput(); chips.querySelectorAll('button').forEach(function (b) { b.disabled = value; }); }
    function open(value) {
        panel.hidden = !value; toggle.setAttribute('aria-expanded', String(value));
        if (value) { scroll(); input.focus({ preventScroll: true }); } else toggle.focus({ preventScroll: true });
    }
    function cancel() {
        generation++;
        if (controller) controller.abort();
        if (busy && lastQuestion) input.value = lastQuestion;
        if (pending) pending.remove();
        pending = null; setBusy(false); status.textContent = 'Espera detenida. Puedes volver a enviar tu pregunta.';
    }
    function selectedVacancy() {
        var current = document.querySelector('[data-vacancy-open][aria-current="true"]');
        var id = current && current.dataset.vacancyId;
        var path = location.pathname.match(/^\/postulante\/vacante\/([a-f\d]{24})$/i);
        return id && /^[a-f\d]{24}$/i.test(id) ? id : path ? path[1] : null;
    }
    async function sendMessage(text, retry) {
        text = String(text || '').trim(); if (!text || busy || text.length > 1000) return;
        if (retryBubble) retryBubble.remove(); retryBubble = null;
        var history = conversation.slice(-12).map(function (t) { return { rol: t.rol, texto: t.texto.slice(0, 3000) }; });
        if (retry && history.length && history[history.length - 1].rol === 'user' && history[history.length - 1].texto === text) history.pop();
        if (!retry) { var user = { rol: 'user', texto: text }; conversation.push(user); message(user); save(); }
        input.value = ''; setBusy(true); status.textContent = 'Preparando tu respuesta…';
        pending = el('div', 'hc-chat__typing'); pending.setAttribute('aria-hidden', 'true');
        pending.append(el('span'), el('span'), el('span')); messages.append(pending); scroll();
        lastQuestion = text;
        var ticket = ++generation, activeController = new AbortController(); controller = activeController;
        var timeout = setTimeout(function () { activeController.abort(); }, 35000);
        try {
            var headers = { 'Content-Type': 'application/json', 'Accept': 'application/json' };
            var csrf = document.querySelector('meta[name="_csrf"]'), csrfHeader = document.querySelector('meta[name="_csrf_header"]');
            if (csrf && csrfHeader) headers[csrfHeader.content] = csrf.content;
            var response = await fetch(endpoint, { method: 'POST', credentials: 'same-origin', signal: activeController.signal, headers: headers, body: JSON.stringify({ mensaje: text, historial: history, vacanteId: selectedVacancy() }) });
            if (ticket !== generation) return;
            if (response.redirected || response.status === 401 || response.status === 403) throw new Error('Tu sesión expiró o ya no tienes acceso. Inicia sesión de nuevo.');
            if (!(response.headers.get('content-type') || '').includes('application/json')) throw new Error('No pude comunicarme con el asistente. Inténtalo de nuevo.');
            var data = await response.json();
            if (ticket !== generation) return;
            if (!response.ok) throw new Error(typeof data.respuesta === 'string' ? data.respuesta : 'No pude responder ahora. Inténtalo de nuevo.');
            if (typeof data.respuesta !== 'string' || !data.respuesta.trim()) throw new Error('No recibí una respuesta. Vuelve a intentarlo.');
            pending.remove(); pending = null;
            var turn = { rol: 'model', texto: data.respuesta.slice(0, 6000), vacantes: data.vacantes, acciones: data.acciones, modo: data.modo };
            conversation.push(turn); conversation = conversation.slice(-24); save(); message(turn); suggestions(data.sugerencias);
            status.textContent = data.modo === 'ia' ? 'Respuesta con IA' : 'Orientación de HireConnect';
        } catch (error) {
            if (ticket !== generation) return;
            if (pending) pending.remove(); pending = null;
            retryBubble = el('div', 'hc-chat__error'); retryBubble.setAttribute('role', 'alert');
            retryBubble.append(el('p', '', error.name === 'AbortError' ? 'La respuesta está tardando demasiado. Puedes volver a intentarlo.' : error.message));
            var button = el('button', '', 'Reintentar'); button.type = 'button'; button.addEventListener('click', function () { sendMessage(text, true); }); retryBubble.append(button); messages.append(retryBubble); scroll(); status.textContent = '';
        } finally {
            clearTimeout(timeout);
            if (ticket === generation) { setBusy(false); if (!panel.hidden) input.focus({ preventScroll: true }); }
        }
    }
    toggle.addEventListener('click', function () { open(panel.hidden); });
    root.querySelector('#hcChatClose').addEventListener('click', function () { open(false); });
    root.querySelector('#hcChatClear').addEventListener('click', function () { cancel(); conversation = []; save(); messages.replaceChildren(); retryBubble = null; status.textContent = ''; welcome(); input.value = ''; syncInput(); input.focus(); });
    expand.addEventListener('click', function () { var value = panel.classList.toggle('is-expanded'); expand.setAttribute('aria-pressed', String(value)); expand.title = value ? 'Reducir conversación' : 'Ampliar conversación'; expand.setAttribute('aria-label', value ? 'Reducir conversación' : 'Ampliar conversación'); });
    root.querySelector('#hcChatForm').addEventListener('submit', function (event) { event.preventDefault(); sendMessage(input.value); });
    input.addEventListener('input', syncInput);
    input.addEventListener('keydown', function (event) { if (event.key === 'Enter' && !event.shiftKey && !event.isComposing) { event.preventDefault(); sendMessage(input.value); } });
    stop.addEventListener('click', cancel);
    panel.addEventListener('keydown', function (event) {
        if (event.key === 'Escape') { event.preventDefault(); open(false); }
        if (event.key === 'Tab') {
            var focusable = Array.from(panel.querySelectorAll('button:not(:disabled),a[href],textarea')).filter(function (n) { return n.getClientRects().length; });
            var first = focusable[0], last = focusable[focusable.length - 1];
            if (event.shiftKey && document.activeElement === first) { event.preventDefault(); last.focus(); }
            else if (!event.shiftKey && document.activeElement === last) { event.preventDefault(); first.focus(); }
        }
    });
    if (conversation.length) { conversation.forEach(message); suggestions(starters); } else welcome();
    syncInput();
})();
