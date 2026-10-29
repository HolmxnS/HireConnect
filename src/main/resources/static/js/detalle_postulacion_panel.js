(function () {
    'use strict';
    var drawer = document.getElementById('application-drawer');
    if (!drawer || typeof drawer.showModal !== 'function') return;
    var content = drawer.querySelector('[data-application-content]');
    var previous = drawer.querySelector('[data-application-previous]');
    var next = drawer.querySelector('[data-application-next]');
    var opener = null;
    var request = null;

    function links() {
        return Array.from(document.querySelectorAll('[data-application-open]')).filter(function (link) {
            return !link.closest('[data-application-card]').hidden;
        });
    }
    async function open(link) {
        if (request) request.abort();
        request = new AbortController();
        var currentRequest = request;
        opener = link;
        var pageUrl = new URL(window.location.href);
        pageUrl.searchParams.set('postulacion', link.dataset.applicationId);
        window.history.replaceState(null, '', pageUrl);
        var visible = links();
        var index = visible.indexOf(link);
        previous.disabled = index <= 0;
        next.disabled = index < 0 || index === visible.length - 1;
        content.textContent = 'Cargando postulación…';
        content.setAttribute('aria-busy', 'true');
        if (!drawer.open) {
            drawer.showModal();
            document.body.classList.add('application-detail-open');
        }
        var url = new URL(link.href, window.location.href);
        url.searchParams.set('panel', 'true');
        try {
            var response = await fetch(url, { signal: currentRequest.signal, headers: { Accept: 'text/html' } });
            if (!response.ok || response.redirected) throw new Error('Detalle no disponible');
            var html = await response.text();
            var doc = new DOMParser().parseFromString(html, 'text/html');
            var detail = doc.querySelector('[data-application-detail]');
            if (!detail) throw new Error('Detalle no disponible');
            // Una respuesta anterior nunca reemplaza la postulación abierta más recientemente.
            if (request !== currentRequest || !drawer.open) return;
            content.replaceChildren(document.importNode(detail, true));
            content.scrollTop = 0;
        } catch (error) {
            if (error.name === 'AbortError' || request !== currentRequest || !drawer.open) return;
            var message = document.createElement('p');
            message.textContent = 'No pudimos cargar el detalle. Intenta de nuevo o ábrelo en su página.';
            var fallback = document.createElement('a');
            fallback.href = link.href;
            fallback.className = 'applications__detail';
            fallback.textContent = 'Abrir detalle';
            content.replaceChildren(message, fallback);
        } finally {
            if (request === currentRequest) content.removeAttribute('aria-busy');
        }
    }
    document.querySelectorAll('[data-application-open]').forEach(function (link) {
        link.addEventListener('click', function (event) {
            if (event.ctrlKey || event.metaKey || event.shiftKey || event.altKey || event.button !== 0) return;
            event.preventDefault();
            open(link);
        });
    });
    drawer.querySelector('[data-application-close]').addEventListener('click', function () { drawer.close(); });
    previous.addEventListener('click', function () {
        var visible = links(), index = visible.indexOf(opener);
        if (index > 0) open(visible[index - 1]);
    });
    next.addEventListener('click', function () {
        var visible = links(), index = visible.indexOf(opener);
        if (index >= 0 && index < visible.length - 1) open(visible[index + 1]);
    });
    drawer.addEventListener('click', function (event) {
        var rect = drawer.getBoundingClientRect();
        if (event.target === drawer && (event.clientX < rect.left || event.clientX > rect.right || event.clientY < rect.top || event.clientY > rect.bottom)) drawer.close();
    });
    drawer.addEventListener('close', function () {
        if (request) request.abort();
        document.body.classList.remove('application-detail-open');
        var pageUrl = new URL(window.location.href);
        pageUrl.searchParams.delete('postulacion');
        window.history.replaceState(null, '', pageUrl);
        content.removeAttribute('aria-busy');
        if (opener && opener.isConnected) opener.focus();
    });

    // Las notificaciones enlazan a la lista con el identificador del panel que debe abrirse.
    var requested = new URL(window.location.href).searchParams.get('postulacion');
    if (requested) {
        var target = Array.from(document.querySelectorAll('[data-application-open]')).find(function (link) {
            return link.dataset.applicationId === requested;
        });
        if (target) {
            open(target);
        } else {
            var message = document.createElement('p');
            message.className = 'applications__alert';
            message.setAttribute('role', 'status');
            message.textContent = 'Esta postulación ya no está disponible en tu lista.';
            document.querySelector('main.applications').prepend(message);
            var pageUrl = new URL(window.location.href);
            pageUrl.searchParams.delete('postulacion');
            window.history.replaceState(null, '', pageUrl);
        }
    }
})();
