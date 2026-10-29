document.addEventListener('DOMContentLoaded', () => {
    const filters = document.querySelector('.applicant-quick-filters');
    if (!filters) return;
    const search = document.getElementById('buscador');
    const publication = document.getElementById('quickPublicationFilter');
    const sector = document.getElementById('quickSectorFilter');
    const modality = document.getElementById('quickModalityFilter');
    const contract = document.getElementById('quickContractFilter');
    const minimum = document.getElementById('quickSalaryMin');
    const maximum = document.getElementById('quickSalaryMax');
    const salary = filters.querySelector('details:not(.applicant-sector-filter)');
    const sectorMenu = filters.querySelector('.applicant-sector-filter');
    const sectorSearch = document.getElementById('quickSectorSearch');
    const sectorOptions = document.getElementById('quickSectorOptions');
    const candidates = document.getElementById('quickCandidatesFilter');
    const clear = document.getElementById('quickClearFilters');
    const cards = Array.from(document.querySelectorAll('.vacancies__card'));
    const normalize = text => (text || '').normalize('NFD').replace(/[\u0300-\u036f]/g, '').trim().toLowerCase();
    const sectors = new Map();
    cards.forEach(card => { if (card.dataset.sector) sectors.set(normalize(card.dataset.sector), card.dataset.sector); });
    const sectorList = Array.from(sectors).sort((a, b) => a[1].localeCompare(b[1], 'es'));
    function chooseSector(value) {
        sector.value = value; sectorMenu.open = false; filter(); sectorMenu.querySelector('summary').focus();
    }
    function renderSectors() {
        const matches = sectorList.filter(([id]) => id.includes(normalize(sectorSearch.value)));
        sectorOptions.replaceChildren();
        matches.slice(0, 5).forEach(([id, name]) => {
            const item = document.createElement('li'); const button = document.createElement('button');
            button.type = 'button'; button.textContent = name; button.setAttribute('aria-pressed', String(sector.value === id));
            button.addEventListener('click', () => chooseSector(id)); item.append(button); sectorOptions.append(item);
        });
        filters.querySelector('[data-sector-empty]').hidden = matches.length !== 0;
        filters.querySelector('[data-sector-count]').textContent = matches.length > 5 ? 'Mostrando 5 de ' + matches.length + '. Escribe para buscar otro.' : '';
    }
    sectorSearch.addEventListener('input', renderSectors);
    sectorMenu.addEventListener('toggle', () => {
        if (sectorMenu.open) { salary.open = false; sectorSearch.value = ''; renderSectors(); sectorSearch.focus(); }
    });
    filters.querySelector('[data-sector-all]').addEventListener('click', () => chooseSector(''));
    sectorMenu.addEventListener('keydown', event => { if (event.key === 'Escape') { sectorMenu.open = false; sectorMenu.querySelector('summary').focus(); } });
    document.addEventListener('click', event => { if (!sectorMenu.contains(event.target)) sectorMenu.open = false; });
    renderSectors();
    const params = new URL(window.location.href).searchParams;
    publication.value = params.get('fechaFiltro') || '';
    modality.value = params.get('modalidad') || '';
    contract.value = params.get('tipoContrato') || '';
    minimum.value = params.get('salarioMin') || ''; maximum.value = params.get('salarioMax') || '';
    let fewer = false;
    function filter() {
        const today = new Date(); today.setHours(0, 0, 0, 0);
        const cutoff = new Date(today);
        const days = { hoy: 1, '3dias': 3, semana: 7, mes: 30 }[publication.value];
        if (days) cutoff.setDate(cutoff.getDate() - days + 1);
        const min = minimum.value === '' ? null : minimum.valueAsNumber;
        const max = maximum.value === '' ? null : maximum.valueAsNumber;
        const invalidSalary = !minimum.validity.valid || !maximum.validity.valid || (min !== null && max !== null && min > max);
        document.getElementById('quickSalaryError').textContent = invalidSalary ? 'Revisa el rango: el máximo debe ser igual o mayor al mínimo.' : '';
        let count = 0;
        cards.forEach(card => {
            const data = card.dataset;
            const date = data.fecha ? new Date(data.fecha + 'T00:00:00') : null;
            const salaryValue = Number(data.salario);
            const visible = (!search?.value.trim() || normalize(data.titulo + ' ' + data.empresa).includes(normalize(search.value)))
                && (!sector.value || normalize(data.sector) === sector.value)
                && (!modality.value || normalize(data.modalidad) === normalize(modality.value))
                && (!contract.value || normalize(data.tipoContrato) === normalize(contract.value))
                && (!days || (date && date >= cutoff && date <= today))
                && (!fewer || Number(data.candidatos) < 10)
                && !invalidSalary
                && (min === null || (data.salario && salaryValue >= min))
                && (max === null || (data.salario && salaryValue <= max));
            card.style.display = visible ? 'block' : 'none';
            if (visible) count++;
        });
        [publication, sector, modality, contract].forEach(control => control.classList.toggle('is-active', Boolean(control.value)));
        sectorMenu.classList.toggle('is-active', Boolean(sector.value));
        document.getElementById('quickSectorSummary').textContent = sectors.get(sector.value) || 'Sector';
        candidates.setAttribute('aria-pressed', String(fewer));
        salary.classList.toggle('is-active', min !== null || max !== null);
        document.getElementById('quickSalarySummary').textContent = min !== null || max !== null ? 'Salario · filtrado' : 'Salario';
        clear.hidden = !publication.value && !sector.value && !modality.value && !contract.value && min === null && max === null && !fewer;
        document.getElementById('no-vacantes-message').style.display = count ? 'none' : 'block';
        document.dispatchEvent(new CustomEvent('vacantes:filtradas'));
    }
    [publication, sector, modality, contract].forEach(control => control.addEventListener('change', filter));
    [minimum, maximum].forEach(control => control.addEventListener('input', filter));
    candidates.addEventListener('click', () => { fewer = !fewer; filter(); });
    clear.addEventListener('click', () => {
        [publication, sector, modality, contract, minimum, maximum].forEach(control => { control.value = ''; });
        fewer = false; salary.open = false; sectorMenu.open = false; sectorSearch.value = ''; renderSectors(); filter(); publication.focus();
    });
    filters.querySelector('[data-salary-done]').addEventListener('click', () => { salary.open = false; salary.querySelector('summary').focus(); });
    document.addEventListener('click', event => { if (!salary.contains(event.target)) salary.open = false; });
    salary.addEventListener('toggle', () => { if (salary.open) sectorMenu.open = false; });
    salary.addEventListener('keydown', event => { if (event.key === 'Escape') { salary.open = false; salary.querySelector('summary').focus(); } });
    const history = document.getElementById('historial');
    const historyList = document.getElementById('lista-historial');
    let request, debounce;
    function suggestions(items) {
        if (!history || !historyList) return;
        historyList.replaceChildren();
        (Array.isArray(items) ? items : []).slice(0, 8).forEach(item => {
            const li = document.createElement('li'); const button = document.createElement('button');
            button.type = 'button'; button.textContent = item;
            button.addEventListener('click', () => { search.value = item; history.style.display = 'none'; filter(); });
            li.append(button); historyList.append(li);
        });
        history.style.display = historyList.children.length ? 'block' : 'none';
    }
    async function loadSuggestions() {
        request?.abort(); request = new AbortController(); const signal = request.signal;
        try {
            const text = search.value.trim();
            const response = await fetch(text.length >= 2 ? '/postulante/vacantes/sugerencias?texto=' + encodeURIComponent(text) : '/postulante/historial', { signal });
            if (response.ok) suggestions(await response.json());
        } catch (error) { if (error.name !== 'AbortError') suggestions([]); }
    }
    search?.addEventListener('input', () => { filter(); clearTimeout(debounce); debounce = setTimeout(loadSuggestions, 200); });
    search?.addEventListener('focus', loadSuggestions);
    document.getElementById('searchForm')?.addEventListener('submit', event => {
        event.preventDefault(); filter(); if (history) history.style.display = 'none';
        const text = search?.value.trim();
        if (text) fetch('/postulante/historial/guardar', { method: 'POST', headers: { 'Content-Type': 'application/x-www-form-urlencoded' }, body: new URLSearchParams({ texto: text }) }).catch(() => {});
    });
    document.getElementById('limpiar-historial')?.addEventListener('click', async () => {
        const response = await fetch('/postulante/historial/limpiar', { method: 'POST' }).catch(() => null);
        if (response?.ok) suggestions([]);
    });
    document.addEventListener('click', event => { if (!event.target.closest('.search-main') && history) { history.style.display = 'none'; request?.abort(); } });
    filter();
});
