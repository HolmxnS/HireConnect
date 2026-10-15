(function () {
    'use strict';

    var script = document.currentScript;
    var ENDPOINT = (script && script.dataset.endpoint) || '/postulante/chat';
    var STORAGE_KEY = 'hcChatHistorial';
    var MAX_TURNOS = 10;

    var toggle = document.getElementById('hcChatToggle');
    var panel = document.getElementById('hcChatPanel');
    var messages = document.getElementById('hcChatMessages');
    var input = document.getElementById('hcChatInput');
    var sendBtn = document.getElementById('hcChatSend');
    var closeBtn = document.getElementById('hcChatClose');
    var clearBtn = document.getElementById('hcChatClear');
    var chips = document.getElementById('hcChatChips');
    if (!toggle || !panel) return;

    var historial = cargarHistorial();
    var enviando = false;

    function escapeHtml(s) {
        return s.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;').replace(/'/g, '&#39;');
    }

    function inline(s) {
        s = escapeHtml(s);
        s = s.replace(/\*\*([^*]+)\*\*/g, '<strong>$1</strong>');
        s = s.replace(/\[([^\]]+)\]\((\/postulante\/vacante\/[A-Za-z0-9_-]+)\)/g,
            '<a href="$2">$1</a>');
        return s;
    }

    function render(texto) {
        var lines = texto.split(/\r?\n/);
        var html = '';
        var enLista = false;
        lines.forEach(function (raw) {
            var line = raw.trim();
            var m = line.match(/^[-*•]\s+(.*)$/);
            if (m) {
                if (!enLista) { html += '<ul>'; enLista = true; }
                html += '<li>' + inline(m[1]) + '</li>';
            } else {
                if (enLista) { html += '</ul>'; enLista = false; }
                if (line) html += '<p>' + inline(line) + '</p>';
            }
        });
        if (enLista) html += '</ul>';
        return html;
    }

    function addMessage(rol, texto) {
        var div = document.createElement('div');
        div.className = 'hc-msg ' + (rol === 'user' ? 'hc-msg--user' : 'hc-msg--bot');
        if (rol === 'user') {
            div.textContent = texto;
        } else {
            div.innerHTML = render(texto);
        }
        messages.appendChild(div);
        messages.scrollTop = messages.scrollHeight;
        return div;
    }

    function mostrarBienvenida() {
        addMessage('model', '¡Hola! Soy tu asistente de HireConnect. Puedo ayudarte a **mejorar tu perfil** ' +
            'y recomendarte **vacantes** según tus habilidades. ¿Por dónde empezamos?');
    }

    function restaurar() {
        messages.innerHTML = '';
        if (historial.length === 0) {
            mostrarBienvenida();
            chips.style.display = '';
            return;
        }
        historial.forEach(function (t) { addMessage(t.rol, t.texto); });
        chips.style.display = 'none';
    }

    function abrir(abierto) {
        panel.hidden = !abierto;
        toggle.setAttribute('aria-expanded', String(abierto));
        if (abierto) {
            messages.scrollTop = messages.scrollHeight;
            input.focus();
        }
    }

    function cargarHistorial() {
        try {
            var raw = sessionStorage.getItem(STORAGE_KEY);
            var arr = raw ? JSON.parse(raw) : [];
            return Array.isArray(arr) ? arr : [];
        } catch (e) { return []; }
    }

    function guardarHistorial() {
        try {
            sessionStorage.setItem(STORAGE_KEY, JSON.stringify(historial.slice(-MAX_TURNOS * 2)));
        } catch (e) { }
    }

    function enviar(texto) {
        texto = (texto || '').trim();
        if (!texto || enviando) return;

        enviando = true;
        sendBtn.disabled = true;
        chips.style.display = 'none';
        input.value = '';
        addMessage('user', texto);

        var typing = document.createElement('div');
        typing.className = 'hc-msg hc-msg--bot hc-msg--typing';
        typing.innerHTML = '<span></span><span></span><span></span>';
        messages.appendChild(typing);
        messages.scrollTop = messages.scrollHeight;

        var historialPrevio = historial.slice(-MAX_TURNOS);

        fetch(ENDPOINT, {
            method: 'POST',
            credentials: 'same-origin',
            headers: { 'Content-Type': 'application/json', 'Accept': 'application/json' },
            body: JSON.stringify({ mensaje: texto, historial: historialPrevio })
        })
            .then(function (res) {
                var ct = res.headers.get('content-type') || '';
                if (res.redirected || ct.indexOf('application/json') === -1) {
                    throw new Error('sesion');
                }
                return res.json();
            })
            .then(function (data) {
                var respuesta = (data && data.respuesta) || 'No pude generar una respuesta.';
                typing.remove();
                addMessage('model', respuesta);
                historial.push({ rol: 'user', texto: texto });
                historial.push({ rol: 'model', texto: respuesta });
                guardarHistorial();
            })
            .catch(function (err) {
                typing.remove();
                addMessage('model', err.message === 'sesion'
                    ? 'Tu sesión expiró. Recarga la página e inicia sesión de nuevo.'
                    : 'No pude conectarme con el servidor. Inténtalo de nuevo.');
            })
            .finally(function () {
                enviando = false;
                sendBtn.disabled = false;
                input.focus();
            });
    }

    toggle.addEventListener('click', function () { abrir(panel.hidden); });
    closeBtn.addEventListener('click', function () { abrir(false); });
    clearBtn.addEventListener('click', function () {
        historial = [];
        guardarHistorial();
        restaurar();
    });
    sendBtn.addEventListener('click', function () { enviar(input.value); });
    input.addEventListener('keydown', function (e) {
        if (e.key === 'Enter' && !e.shiftKey) { e.preventDefault(); enviar(input.value); }
    });
    chips.addEventListener('click', function (e) {
        var b = e.target.closest('button[data-msg]');
        if (b) enviar(b.dataset.msg);
    });

    restaurar();
})();