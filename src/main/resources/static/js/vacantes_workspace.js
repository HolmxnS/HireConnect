(function () {
    'use strict';

    const workspace = document.querySelector('[data-vacancy-workspace]');
    if (!workspace) return;

    const container = workspace.querySelector('#vacantesContainer');
    const list = workspace.querySelector('.vacancy-list');
    const reader = workspace.querySelector('[data-vacancy-reader]');
    const content = workspace.querySelector('[data-vacancy-content]');
    const back = workspace.querySelector('[data-vacancy-back]');
    const count = document.querySelector('[data-vacancy-count]');
    const empty = workspace.querySelector('#no-vacantes-message');
    const mobile = window.matchMedia('(max-width: 900px)');
    const emptyContent = content.cloneNode(true);
    const filterNames = [
        'filtro', 'sector', 'modalidad', 'tipoContrato', 'fechaFiltro', 'oferta',
        'salarioMin', 'salarioMax', 'pocosCandidatos'
    ];

    let selected = null;
    let request = null;
    let requestVersion = 0;
    let observer = null;
    let userScrolled = false;
    let failedRequest = null;
    let scrollFrame = null;

    function canonicalParams(source) {
        const input = source instanceof URLSearchParams ? source : new URLSearchParams(source || '');
        const result = new URLSearchParams();
        filterNames.forEach(name => {
            const value = input.get(name);
            if (value !== null && value !== '' && (name !== 'pocosCandidatos' || value === 'true')) {
                result.set(name, value);
            }
        });
        if (!result.has('filtro') && input.get('query')) result.set('filtro', input.get('query'));
        return result;
    }

    function paramsKey(params) {
        return filterNames.map(name => `${name}=${params.get(name) || ''}`).join('&');
    }

    let activeFilters = canonicalParams(new URL(window.location.href).searchParams);
    let activeKey = paramsKey(activeFilters);

    function results() {
        return container.querySelector('[data-vacancy-results]') || container;
    }

    function cards(root = results()) {
        return Array.from(root.querySelectorAll('.vacancies__card'));
    }

    function cardId(card) {
        return card.dataset.vacancyId || card.querySelector('[data-vacancy-open]')?.dataset.vacancyId || '';
    }

    function pageData() {
        const result = results();
        const loaded = cards(result).length;
        const page = Number.parseInt(result.dataset.page || '0', 10);
        const total = Number.parseInt(result.dataset.total || String(loaded), 10);
        return {
            page: Number.isFinite(page) ? page : 0,
            total: Number.isFinite(total) ? total : loaded,
            hasNext: result.dataset.hasNext === 'true'
        };
    }

    function loadStatus() {
        return list.querySelector('[data-vacancy-load-status]');
    }

    function loadButton() {
        return list.querySelector('[data-vacancy-load-more]');
    }

    function updatePagination(message = '') {
        const state = pageData();
        const loaded = cards().length;
        const button = loadButton();
        const status = loadStatus();
        const isLoading = Boolean(request);

        if (button) {
            if (!button.dataset.defaultLabel) button.dataset.defaultLabel = button.textContent.trim() || 'Cargar más';
            button.hidden = !state.hasNext && !failedRequest && !isLoading;
            button.disabled = isLoading;
            button.textContent = isLoading
                ? 'Cargando…'
                : failedRequest
                    ? 'Reintentar'
                    : button.dataset.defaultLabel;
        }
        if (status) {
            status.textContent = message || (state.total > loaded
                ? `${loaded} de ${state.total} vacantes cargadas.`
                : `${loaded} vacante${loaded === 1 ? '' : 's'} cargada${loaded === 1 ? '' : 's'}.`);
        }
    }

    function updateCount() {
        const state = pageData();
        const loaded = cards().length;
        if (count) {
            count.textContent = state.total > loaded
                ? `${loaded} de ${state.total} vacantes`
                : `${state.total} vacante${state.total === 1 ? '' : 's'} disponible${state.total === 1 ? '' : 's'}`;
        }
        if (empty) empty.style.display = loaded ? 'none' : 'block';
    }

    function clearSelection() {
        selected = null;
        workspace.classList.remove('is-detail-open');
        cards().forEach(card => {
            card.classList.remove('is-selected');
            card.querySelector('[data-vacancy-open]')?.removeAttribute('aria-current');
        });
        content.replaceChildren(...Array.from(emptyContent.cloneNode(true).childNodes));
    }

    function select(card, userAction) {
        if (!card) {
            clearSelection();
            return;
        }
        const template = card.querySelector('[data-vacancy-template]');
        if (!template) return;
        selected = card;
        cards().forEach(item => {
            const link = item.querySelector('[data-vacancy-open]');
            const current = item === card;
            item.classList.toggle('is-selected', current);
            if (current) link?.setAttribute('aria-current', 'true');
            else link?.removeAttribute('aria-current');
        });
        content.replaceChildren(template.content.cloneNode(true));
        content.scrollTop = 0;
        if (!userAction) return;
        if (mobile.matches) {
            workspace.classList.add('is-detail-open');
            back.focus();
        } else {
            content.querySelector('[data-vacancy-title]')?.focus({ preventScroll: true });
        }
    }

    function synchronize(resetSelection) {
        const currentCards = cards();
        updateCount();
        document.dispatchEvent(new CustomEvent('vacantes:actualizadas', {
            detail: { root: results() }
        }));

        if (resetSelection) {
            workspace.classList.remove('is-detail-open');
            selected = null;
        }
        if (selected?.isConnected && currentCards.includes(selected)) {
            currentCards.forEach(card => {
                const current = card === selected;
                card.classList.toggle('is-selected', current);
                if (current) card.querySelector('[data-vacancy-open]')?.setAttribute('aria-current', 'true');
                else card.querySelector('[data-vacancy-open]')?.removeAttribute('aria-current');
            });
        } else if (currentCards.length) {
            select(currentCards[0], false);
        } else {
            clearSelection();
        }
        updatePagination();
    }

    function updateAddress(params) {
        const url = new URL(window.location.href);
        [...filterNames, 'query', 'pagina', 'fragmento', 'vacante'].forEach(name => url.searchParams.delete(name));
        params.forEach((value, name) => url.searchParams.set(name, value));
        window.history.replaceState(window.history.state, '', `${url.pathname}${url.search}${url.hash}`);
    }

    function parseFragment(html) {
        const template = document.createElement('template');
        template.innerHTML = html.trim();
        return template.content.querySelector('[data-vacancy-results]');
    }

    function replaceResults(replacement) {
        const current = results();
        if (current === container) {
            current.querySelectorAll(':scope > .vacancies__card').forEach(card => card.remove());
            container.prepend(replacement);
        } else {
            current.replaceWith(replacement);
        }
    }

    function appendResults(replacement) {
        const current = results();
        const known = new Set(cards(current).map(cardId).filter(Boolean));
        cards(replacement).forEach(card => {
            const id = cardId(card);
            if (!id || !known.has(id)) {
                current.append(card);
                if (id) known.add(id);
            }
        });
        current.dataset.page = replacement.dataset.page;
        current.dataset.hasNext = replacement.dataset.hasNext;
        current.dataset.total = replacement.dataset.total;
    }

    function redirectIfSessionExpired(response) {
        const responseUrl = new URL(response.url, window.location.origin);
        const loginResponse = /\/(login|logout)(?:\/|$)/.test(responseUrl.pathname);
        if (response.status === 401 || response.status === 403 || response.redirected || loginResponse) {
            window.location.assign(response.redirected || loginResponse ? responseUrl.href : '/login');
            return true;
        }
        return false;
    }

    async function loadPage(page, reset, params = activeFilters) {
        if (!reset && request) return;
        if (reset && request) request.abort();

        const filters = canonicalParams(params);
        const controller = new AbortController();
        const version = ++requestVersion;
        let timedOut = false;
        const timeout = window.setTimeout(() => {
            timedOut = true;
            controller.abort();
        }, 20000);
        request = controller;
        failedRequest = null;
        container.setAttribute('aria-busy', 'true');
        list.setAttribute('aria-busy', 'true');
        updatePagination(page === 0 ? 'Buscando vacantes…' : 'Cargando más vacantes…');

        const url = new URL('/postulante/vacantes/resultados', window.location.origin);
        filters.forEach((value, name) => url.searchParams.set(name, value));
        url.searchParams.set('pagina', String(page));

        try {
            const response = await fetch(url, {
                signal: controller.signal,
                credentials: 'same-origin',
                headers: {
                    Accept: 'text/html',
                    'X-Requested-With': 'XMLHttpRequest'
                }
            });
            if (version !== requestVersion || controller.signal.aborted) return;
            if (redirectIfSessionExpired(response)) return;
            if (!response.ok) throw new Error(`HTTP ${response.status}`);
            const html = await response.text();
            if (version !== requestVersion || controller.signal.aborted) return;
            const replacement = parseFragment(html);
            if (!replacement) throw new Error('La respuesta no contiene resultados.');

            if (reset) {
                replaceResults(replacement);
                activeFilters = filters;
                activeKey = paramsKey(filters);
                userScrolled = false;
                list.scrollTop = 0;
                updateAddress(filters);
            } else {
                appendResults(replacement);
            }
            failedRequest = null;
            synchronize(reset);
            bindObserver();
        } catch (error) {
            if ((error.name === 'AbortError' && !timedOut) || version !== requestVersion) return;
            failedRequest = { page, reset, params: filters };
            updatePagination(timedOut
                ? 'La carga tardó demasiado. Intenta de nuevo.'
                : 'No pudimos cargar las vacantes. Intenta de nuevo.');
        } finally {
            window.clearTimeout(timeout);
            if (request === controller) {
                request = null;
                container.removeAttribute('aria-busy');
                list.removeAttribute('aria-busy');
                updatePagination(loadStatus()?.textContent || '');
            }
        }
    }

    function loadNextPage() {
        if (request) return;
        if (failedRequest) {
            const retry = failedRequest;
            failedRequest = null;
            loadPage(retry.page, retry.reset, retry.params);
            return;
        }
        const state = pageData();
        if (state.hasNext) loadPage(state.page + 1, false, activeFilters);
    }

    function bindObserver() {
        observer?.disconnect();
        const sentinel = list.querySelector('[data-vacancy-sentinel]');
        if (!sentinel || !('IntersectionObserver' in window)) return;
        observer = new IntersectionObserver(entries => {
            if (!entries.some(entry => entry.isIntersecting)) return;
            if (!userScrolled || request || failedRequest || !pageData().hasNext) return;
            loadNextPage();
        }, { root: list, rootMargin: '0px 0px 160px 0px', threshold: 0 });
        observer.observe(sentinel);
    }

    workspace.classList.add('is-enhanced');
    reader.hidden = false;
    synchronize(true);
    if (new URLSearchParams(window.location.search).has('oferta') && selected) select(selected, true);
    bindObserver();

    list.addEventListener('scroll', () => {
        if (list.scrollTop <= 0) return;
        userScrolled = true;
        if (scrollFrame !== null) return;
        scrollFrame = window.requestAnimationFrame(() => {
            scrollFrame = null;
            const remaining = list.scrollHeight - list.scrollTop - list.clientHeight;
            if (remaining <= 200 && !request && !failedRequest && pageData().hasNext) loadNextPage();
        });
    }, { passive: true });

    workspace.addEventListener('click', event => {
        const open = event.target.closest('[data-vacancy-open]');
        if (open && workspace.contains(open)) {
            if (event.button !== 0 || event.metaKey || event.ctrlKey || event.shiftKey || event.altKey) return;
            event.preventDefault();
            select(open.closest('.vacancies__card'), true);
            return;
        }
        const loadMore = event.target.closest('[data-vacancy-load-more]');
        if (loadMore && list.contains(loadMore)) {
            event.preventDefault();
            loadNextPage();
        }
    });

    back.addEventListener('click', () => {
        workspace.classList.remove('is-detail-open');
        selected?.querySelector('[data-vacancy-open]')?.focus({ preventScroll: true });
    });
    mobile.addEventListener('change', () => workspace.classList.remove('is-detail-open'));

    document.addEventListener('vacantes:buscar', event => {
        const nextFilters = canonicalParams(event.detail);
        const nextKey = paramsKey(nextFilters);
        if (nextKey === activeKey && !request && !failedRequest && pageData().page === 0) return;
        loadPage(0, true, nextFilters);
    });
})();
