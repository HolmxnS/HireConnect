document.addEventListener('DOMContentLoaded', () => {
    const form = document.getElementById('vacancySearchForm'); if (!form) return;
    const search = document.getElementById('searchInputAdminVacantes');
    const status = document.querySelector('[data-vacancy-request-status]');
    let request, timer;
    function filterUrl() {
        const url = new URL(form.action);
        new FormData(form).forEach((value, name) => { if (value) url.searchParams.set(name, value); });
        return url;
    }
    async function load(url) {
        clearTimeout(timer); request?.abort(); request = new AbortController(); const current = request;
        const results = document.querySelector('[data-vacancy-results]');
        results.setAttribute('aria-busy', 'true'); status.textContent = 'Cargando vacantes…';
        try {
            const target = new URL(url); target.searchParams.set('fragmento', 'true');
            const response = await fetch(target, { signal: current.signal, credentials: 'same-origin', headers: { Accept: 'text/html' } });
            if (!response.ok || response.redirected) throw Error();
            const template = document.createElement('template'); template.innerHTML = await response.text();
            const replacement = template.content.querySelector('[data-vacancy-results]');
            if (!replacement) throw Error();
            if (current !== request || current.signal.aborted) return;
            results.replaceWith(replacement); status.textContent = '';
            const clean = new URL(url); clean.searchParams.delete('fragmento'); window.history.replaceState(null, '', clean);
        } catch (error) {
            if (error.name !== 'AbortError' && current === request) status.textContent = 'No se pudo cargar la lista. Pulsa Buscar para intentarlo de nuevo.';
        } finally { if (current === request) document.querySelector('[data-vacancy-results]')?.removeAttribute('aria-busy'); }
    }
    form.addEventListener('submit', event => { event.preventDefault(); load(filterUrl()); });
    search.addEventListener('input', () => { clearTimeout(timer); request?.abort(); timer = setTimeout(() => load(filterUrl()), 300); });
    form.querySelectorAll('select').forEach(control => control.addEventListener('change', () => load(filterUrl())));
    document.addEventListener('click', event => {
        const link = event.target.closest('[data-vacancy-page]');
        if (!link || event.button !== 0 || event.ctrlKey || event.metaKey || event.shiftKey || event.altKey) return;
        event.preventDefault(); load(link.href);
    });
});
