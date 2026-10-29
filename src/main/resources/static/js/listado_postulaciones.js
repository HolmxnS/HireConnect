document.addEventListener('DOMContentLoaded', () => {
    const board = document.querySelector('[data-recruit-workspace]');
    if (!board) return;
    const jobs = Array.from(board.querySelectorAll('[data-job-open]'));
    const search = board.querySelector('[data-job-search]');
    const state = board.querySelector('[data-job-state]');
    const jobCount = board.querySelector('[data-job-count]');
    const jobEmpty = board.querySelector('[data-job-empty]');
    const pane = board.querySelector('[data-candidate-pane]');
    const dialog = document.querySelector('[data-candidate-dialog]');
    const detail = dialog.querySelector('[data-candidate-detail]');
    const mobile = window.matchMedia('(max-width: 900px)');
    const storageKey = 'hc-recruit-' + board.dataset.companyId;
    let selected = board.dataset.selectedId || '', lastCandidate, listRequest, detailRequest;
    const normalize = text => text.normalize('NFD').replace(/[\u0300-\u036f]/g, '').toLocaleLowerCase('es').trim();
    function save() {
        try { sessionStorage.setItem(storageKey, JSON.stringify({ search: search.value, state: state.value, selected })); } catch (error) {}
    }
    function setUrl(application) {
        const url = new URL(window.location.href);
        if (selected) url.searchParams.set('vacante', selected); else url.searchParams.delete('vacante');
        if (application) url.searchParams.set('postulacion', application); else url.searchParams.delete('postulacion');
        window.history.replaceState(null, '', url); save();
    }
    function mark() {
        jobs.forEach(job => { const active = job.dataset.jobId === selected; job.classList.toggle('is-selected', active); if (active) job.setAttribute('aria-current', 'true'); else job.removeAttribute('aria-current'); });
    }
    function message(container, text, retry) {
        const wrapper = document.createElement('div'); wrapper.className = 'recruit-loading';
        const paragraph = document.createElement('p'); paragraph.textContent = text; wrapper.append(paragraph);
        if (retry) { const button = document.createElement('button'); button.type = 'button'; button.className = 'recruit-secondary'; button.textContent = 'Reintentar'; button.addEventListener('click', retry); wrapper.append(button); }
        container.replaceChildren(wrapper);
    }
    async function fragment(url, selector, signal) {
        const target = new URL(url, window.location.origin); target.searchParams.set('panel', 'true');
        const response = await fetch(target, { signal, credentials: 'same-origin', headers: { Accept: 'text/html' } });
        if (!response.ok || response.redirected) throw new Error(response.status === 403 ? 'No tienes acceso a esta información.' : 'No se pudo cargar la información. Revisa tu sesión e inténtalo de nuevo.');
        const template = document.createElement('template'); template.innerHTML = await response.text();
        const result = template.content.querySelector(selector);
        if (!result) throw new Error('No se pudo cargar la información. Revisa tu sesión.');
        return result;
    }
    async function selectJob(job, userAction = false) {
        listRequest?.abort(); listRequest = new AbortController();
        const request = listRequest;
        selected = job.dataset.jobId; mark(); setUrl();
        if (mobile.matches && userAction) board.classList.add('is-candidates-open');
        pane.setAttribute('aria-busy', 'true'); message(pane, 'Cargando postulantes…');
        try {
            const result = await fragment(job.href, '.recruit-candidates', request.signal);
            if (request !== listRequest) return;
            pane.replaceChildren(result); pane.scrollTop = 0; filterCandidates();
            if (mobile.matches && userAction) pane.querySelector('[data-recruit-back]')?.focus();
        } catch (error) { if (error.name !== 'AbortError' && request === listRequest) message(pane, error.message, () => selectJob(job, userAction)); }
        finally { if (request === listRequest) pane.removeAttribute('aria-busy'); }
    }
    function filterJobs() {
        jobs.forEach(job => { job.hidden = !normalize(job.querySelector('[data-job-title]').textContent).includes(normalize(search.value)) || Boolean(state.value && job.dataset.state !== state.value); });
        const visible = jobs.filter(job => !job.hidden);
        jobCount.textContent = visible.length + ' de ' + jobs.length + ' vacantes'; jobEmpty.hidden = Boolean(visible.length) || jobs.length === 0;
        if (!visible.some(job => job.dataset.jobId === selected)) {
            if (visible.length) selectJob(visible[0]);
            else if (jobs.length) { listRequest?.abort(); selected = ''; mark(); setUrl(); message(pane, 'Ajusta los filtros para seleccionar una vacante.'); board.classList.remove('is-candidates-open'); }
        }
        save();
    }
    function filterCandidates() {
        const query = normalize(pane.querySelector('[data-candidate-search]')?.value || '');
        const status = pane.querySelector('[data-candidate-state]')?.value || '';
        const rows = Array.from(pane.querySelectorAll('[data-candidate-open]'));
        rows.forEach(row => { row.hidden = !normalize(row.dataset.name).includes(query) || Boolean(status && row.dataset.state !== status); });
        const empty = pane.querySelector('[data-candidate-empty]'); if (empty) empty.hidden = rows.some(row => !row.hidden) || rows.length === 0;
    }
    async function openCandidate(link) {
        detailRequest?.abort(); detailRequest = new AbortController(); const request = detailRequest;
        lastCandidate = link;
        if (!dialog.open) dialog.showModal();
        message(detail, 'Cargando perfil…'); setUrl(link.dataset.applicationId);
        try {
            const result = await fragment(link.href, '.recruit-profile', request.signal);
            if (request !== detailRequest) return;
            detail.replaceChildren(result); dialog.scrollTop = 0; detail.querySelector('h2')?.focus({ preventScroll: true });
        } catch (error) { if (error.name !== 'AbortError' && request === detailRequest) message(detail, error.message, () => openCandidate(link)); }
    }
    jobs.forEach(job => job.addEventListener('click', event => {
        if (event.button !== 0 || event.ctrlKey || event.metaKey || event.shiftKey || event.altKey) return;
        event.preventDefault(); selectJob(job, true);
    }));
    search.addEventListener('input', filterJobs); state.addEventListener('change', filterJobs);
    pane.addEventListener('input', event => { if (event.target.matches('[data-candidate-search]')) filterCandidates(); });
    pane.addEventListener('change', event => { if (event.target.matches('[data-candidate-state]')) filterCandidates(); });
    pane.addEventListener('click', event => {
        if (event.target.closest('[data-recruit-back]')) { board.classList.remove('is-candidates-open'); jobs.find(job => job.dataset.jobId === selected)?.focus({ preventScroll: true }); return; }
        const link = event.target.closest('[data-candidate-open]');
        if (!link || event.button !== 0 || event.ctrlKey || event.metaKey || event.shiftKey || event.altKey) return;
        event.preventDefault(); openCandidate(link);
    });
    dialog.querySelector('[data-candidate-close]').addEventListener('click', () => dialog.close());
    dialog.addEventListener('close', () => { detailRequest?.abort(); setUrl(); if (lastCandidate?.isConnected && !lastCandidate.hidden) lastCandidate.focus({ preventScroll: true }); });
    let saving = false;
    dialog.addEventListener('cancel', event => { if (saving) event.preventDefault(); });
    dialog.addEventListener('submit', async event => {
        const form = event.target.closest('[data-candidate-status-form]'); if (!form) return;
        event.preventDefault(); if (saving) return;
        const body = new URLSearchParams(new FormData(form));
        const target = new URL(form.action); target.searchParams.set('panel', 'true');
        const button = form.querySelector('button[type="submit"]'); const select = form.querySelector('select'); const status = form.querySelector('[data-candidate-form-message]');
        saving = true; button.disabled = select.disabled = true; dialog.querySelector('[data-candidate-close]').disabled = true; status.textContent = 'Guardando…';
        try {
            const response = await fetch(target, { method: 'POST', body, credentials: 'same-origin', headers: { Accept: 'application/json' } });
            const data = await response.json().catch(() => ({}));
            if (!response.ok || response.redirected || !data.estado) throw new Error(data.mensaje || 'No se pudo actualizar el estado. Inténtalo de nuevo.');
            const row = Array.from(pane.querySelectorAll('[data-candidate-open]')).find(item => item.dataset.applicationId === data.id);
            if (row) { row.dataset.state = data.estado; const badge = row.querySelector('.recruit-status'); badge.dataset.state = data.estado; badge.textContent = data.descripcion; }
            status.textContent = data.mensaje; filterCandidates();
        } catch (error) { status.textContent = error.message; }
        finally { saving = false; button.disabled = select.disabled = false; dialog.querySelector('[data-candidate-close]').disabled = false; }
    });
    mobile.addEventListener('change', () => board.classList.remove('is-candidates-open'));
    let saved = {}; try { saved = JSON.parse(sessionStorage.getItem(storageKey) || '{}') || {}; } catch (error) {}
    const url = new URL(window.location.href), initialApplication = url.searchParams.get('postulacion');
    const directLink = url.searchParams.has('vacante');
    search.value = !directLink && typeof saved.search === 'string' ? saved.search : '';
    state.value = !directLink && ['ACTIVA','PENDIENTE','CERRADA'].includes(saved.state) ? saved.state : '';
    const wanted = url.searchParams.get('vacante') || saved.selected || selected;
    const chosen = jobs.find(job => job.dataset.jobId === wanted);
    if (chosen && chosen.dataset.jobId !== selected) selectJob(chosen);
    filterJobs(); mark(); filterCandidates();
    if (mobile.matches && url.searchParams.has('vacante') && selected) board.classList.add('is-candidates-open');
    if (initialApplication) {
        const link = Array.from(pane.querySelectorAll('[data-candidate-open]')).find(item => item.dataset.applicationId === initialApplication);
        if (link) openCandidate(link);
    }
});
