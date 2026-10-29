document.addEventListener('DOMContentLoaded', () => {
    const workspace = document.querySelector('[data-company-vacancy-workspace]');
    if (!workspace) return;
    const container = workspace.querySelector('.vacancies');
    const cards = Array.from(container.querySelectorAll('.vacancies__card'));
    const reader = workspace.querySelector('[data-vacancy-reader]');
    const content = workspace.querySelector('[data-vacancy-content]');
    const back = workspace.querySelector('[data-vacancy-back]');
    const list = workspace.querySelector('.vacancy-list');
    const emptyContent = content.cloneNode(true);
    const search = document.getElementById('searchInputVacantes');
    const state = document.getElementById('stateFilterVacantes');
    const sort = document.getElementById('sortFilterVacantes');
    const empty = workspace.querySelector('[data-vacancy-no-results]');
    const count = document.querySelector('[data-vacancy-count]');
    const mobile = window.matchMedia('(max-width: 900px)');
    const storageKey = 'hc-company-vacancies-' + workspace.dataset.companyId;
    let selected = null, saved = {};
    try { saved = JSON.parse(sessionStorage.getItem(storageKey) || '{}') || {}; } catch (error) {}
    workspace.classList.add('is-enhanced'); reader.hidden = false;
    if (!cards.length) return;
    const normalize = text => text.normalize('NFD').replace(/[\u0300-\u036f]/g, '').toLocaleLowerCase('es').trim();
    search.value = typeof saved.search === 'string' ? saved.search : '';
    state.value = ['ACTIVA', 'PENDIENTE', 'CERRADA'].includes(saved.state) ? saved.state : '';
    sort.value = saved.sort === 'oldest' ? 'oldest' : 'most_recent';
    function persist() {
        try { sessionStorage.setItem(storageKey, JSON.stringify({ search: search.value, state: state.value, sort: sort.value, selected: selected?.dataset.vacancyId, scroll: list.scrollTop })); } catch (error) {}
    }
    function select(card, userAction = false) {
        selected = card;
        cards.forEach(item => {
            const link = item.querySelector('[data-vacancy-open]');
            item.classList.toggle('is-selected', item === card);
            if (item === card) link.setAttribute('aria-current', 'true'); else link.removeAttribute('aria-current');
        });
        content.replaceChildren(card.querySelector('[data-vacancy-template]').content.cloneNode(true));
        content.scrollTop = 0;
        const url = new URL(window.location.href); url.searchParams.set('vacante', card.dataset.vacancyId);
        window.history.replaceState(null, '', url);
        if (userAction) {
            if (mobile.matches) { workspace.classList.add('is-detail-open'); back.focus(); }
            else content.querySelector('[data-vacancy-title]')?.focus({ preventScroll: true });
        }
        persist();
    }
    function update() {
        const query = normalize(search.value);
        cards.sort((a, b) => {
            if (!a.dataset.date) return b.dataset.date ? 1 : 0;
            if (!b.dataset.date) return -1;
            const comparison = a.dataset.date.localeCompare(b.dataset.date);
            return sort.value === 'oldest' ? comparison : -comparison;
        });
        cards.forEach(card => {
            card.hidden = !normalize(card.querySelector('h3').textContent).includes(query) || Boolean(state.value && card.dataset.state !== state.value);
            container.append(card);
        });
        const visible = cards.filter(card => !card.hidden);
        empty.hidden = visible.length > 0; count.textContent = visible.length + ' de ' + cards.length + ' vacantes';
        if (!visible.includes(selected)) {
            if (visible.length) select(visible[0]);
            else {
                selected = null; workspace.classList.remove('is-detail-open');
                cards.forEach(card => { card.classList.remove('is-selected'); card.querySelector('[data-vacancy-open]').removeAttribute('aria-current'); });
                content.replaceChildren(...Array.from(emptyContent.cloneNode(true).childNodes));
                const url = new URL(window.location.href); url.searchParams.delete('vacante'); window.history.replaceState(null, '', url);
            }
        }
        persist();
    }
    cards.forEach(card => card.querySelector('[data-vacancy-open]').addEventListener('click', event => {
        if (event.button !== 0 || event.metaKey || event.ctrlKey || event.shiftKey || event.altKey) return;
        event.preventDefault(); select(card, true);
    }));
    search.addEventListener('input', update); state.addEventListener('change', update); sort.addEventListener('change', update);
    workspace.querySelector('[data-vacancy-reset]').addEventListener('click', () => { search.value = ''; state.value = ''; sort.value = 'most_recent'; update(); search.focus(); });
    back.addEventListener('click', () => { workspace.classList.remove('is-detail-open'); selected?.querySelector('[data-vacancy-open]').focus({ preventScroll: true }); });
    mobile.addEventListener('change', () => workspace.classList.remove('is-detail-open'));
    const confirmation = document.querySelector('[data-vacancy-confirm-dialog]');
    let confirmationTrigger;
    content.addEventListener('click', event => {
        const link = event.target.closest('[data-vacancy-confirm]');
        if (!link) return;
        event.preventDefault();
        confirmationTrigger = link;
        const closing = link.dataset.vacancyConfirm === 'cerrar';
        confirmation.querySelector('#vacancy-confirm-title').textContent = closing ? '¿Cerrar esta vacante?' : '¿Reactivar esta vacante?';
        confirmation.querySelector('[data-confirm-job]').textContent = content.querySelector('[data-vacancy-title]').textContent;
        confirmation.querySelector('#vacancy-confirm-description').textContent = closing
            ? 'Dejarás de recibir nuevas postulaciones. Los candidatos actuales seguirán disponibles y podrás reactivar la oferta cuando lo necesites.'
            : 'La oferta volverá a estar activa para recibir nuevas postulaciones. Los candidatos actuales se conservarán.';
        const action = confirmation.querySelector('[data-confirm-action]');
        action.textContent = closing ? 'Sí, cerrar vacante' : 'Sí, reactivar vacante';
        action.href = link.href;
        confirmation.showModal();
    });
    confirmation.querySelectorAll('[data-confirm-cancel]').forEach(button => button.addEventListener('click', () => confirmation.close()));
    confirmation.addEventListener('close', () => confirmationTrigger?.isConnected && confirmationTrigger.focus({ preventScroll: true }));
    window.addEventListener('pagehide', persist);
    const initialId = new URL(window.location.href).searchParams.get('vacante') || saved.selected;
    selected = cards.find(card => card.dataset.vacancyId === initialId) || null;
    if (selected) select(selected);
    update();
    list.scrollTop = Number(saved.scroll) || 0;
});
