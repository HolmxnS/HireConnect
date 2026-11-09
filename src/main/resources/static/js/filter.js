document.addEventListener('DOMContentLoaded', () => {
    const filters = document.querySelector('.applicant-quick-filters');
    if (!filters) return;
    const guest = document.body.classList.contains('vacancy-browser--guest');

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
    const sectorCatalog = document.getElementById('vacancySectorCatalog');
    const candidates = document.getElementById('quickCandidatesFilter');
    const clear = document.getElementById('quickClearFilters');
    const salaryError = document.getElementById('quickSalaryError');
    const normalize = text => (text || '').normalize('NFD').replace(/[\u0300-\u036f]/g, '').trim().toLowerCase();

    const sectors = new Map();
    if (sectorCatalog) {
        Array.from(sectorCatalog.options).forEach(option => {
            const value = option.value.trim();
            if (value) sectors.set(normalize(value), value);
        });
    } else {
        document.querySelectorAll('.vacancies__card[data-sector]').forEach(card => {
            const value = card.dataset.sector.trim();
            if (value) sectors.set(normalize(value), value);
        });
    }
    const sectorList = Array.from(sectors.entries()).sort((a, b) => a[1].localeCompare(b[1], 'es'));
    const params = new URL(window.location.href).searchParams;

    if (search && !search.value) search.value = params.get('filtro') || params.get('query') || '';
    publication.value = params.get('fechaFiltro') || '';
    sector.value = params.get('sector') || '';
    modality.value = params.get('modalidad') || '';
    contract.value = params.get('tipoContrato') || '';
    minimum.value = params.get('salarioMin') || '';
    maximum.value = params.get('salarioMax') || '';
    let fewer = params.get('pocosCandidatos') === 'true';
    let salaryDebounce;

    function queryParams() {
        const query = new URLSearchParams();
        const values = {
            filtro: search?.value.trim() || '',
            sector: sector.value.trim(),
            modalidad: modality.value,
            tipoContrato: contract.value,
            fechaFiltro: publication.value,
            salarioMin: minimum.value,
            salarioMax: maximum.value
        };
        Object.entries(values).forEach(([name, value]) => {
            if (value !== '') query.set(name, value);
        });
        if (fewer) query.set('pocosCandidatos', 'true');
        return query;
    }

    function updateControls() {
        const min = minimum.value === '' ? null : minimum.valueAsNumber;
        const max = maximum.value === '' ? null : maximum.valueAsNumber;
        const invalidSalary = !minimum.validity.valid || !maximum.validity.valid
            || (min !== null && max !== null && min > max);
        salaryError.textContent = invalidSalary
            ? 'Revisa el rango: el máximo debe ser igual o mayor al mínimo.'
            : '';

        [publication, modality, contract].forEach(control => {
            control.classList.toggle('is-active', Boolean(control.value));
        });
        sectorMenu.classList.toggle('is-active', Boolean(sector.value));
        document.getElementById('quickSectorSummary').textContent = sector.value || 'Sector';
        candidates.setAttribute('aria-pressed', String(fewer));
        salary.classList.toggle('is-active', min !== null || max !== null);
        document.getElementById('quickSalarySummary').textContent = min !== null || max !== null
            ? 'Salario · filtrado'
            : 'Salario';
        clear.hidden = !publication.value && !sector.value && !modality.value && !contract.value
            && min === null && max === null && !fewer;
        return !invalidSalary;
    }

    function searchVacancies() {
        if (!updateControls()) return;
        document.dispatchEvent(new CustomEvent('vacantes:buscar', { detail: queryParams() }));
    }

    function chooseSector(value) {
        sector.value = value;
        sectorMenu.open = false;
        searchVacancies();
        sectorMenu.querySelector('summary').focus();
    }

    function renderSectors() {
        const matches = sectorList.filter(([normalized]) => normalized.includes(normalize(sectorSearch.value)));
        sectorOptions.replaceChildren();
        matches.slice(0, 5).forEach(([, name]) => {
            const item = document.createElement('li');
            const button = document.createElement('button');
            button.type = 'button';
            button.textContent = name;
            button.setAttribute('aria-pressed', String(sector.value === name));
            button.addEventListener('click', () => chooseSector(name));
            item.append(button);
            sectorOptions.append(item);
        });
        filters.querySelector('[data-sector-empty]').hidden = matches.length !== 0;
        filters.querySelector('[data-sector-count]').textContent = matches.length > 5
            ? `Mostrando 5 de ${matches.length}. Escribe para buscar otro.`
            : '';
    }

    sectorSearch.addEventListener('input', renderSectors);
    sectorMenu.addEventListener('toggle', () => {
        if (!sectorMenu.open) return;
        salary.open = false;
        sectorSearch.value = '';
        renderSectors();
        sectorSearch.focus();
    });
    filters.querySelector('[data-sector-all]').addEventListener('click', () => chooseSector(''));
    sectorMenu.addEventListener('keydown', event => {
        if (event.key !== 'Escape') return;
        sectorMenu.open = false;
        sectorMenu.querySelector('summary').focus();
    });
    document.addEventListener('click', event => {
        if (!sectorMenu.contains(event.target)) sectorMenu.open = false;
    });
    renderSectors();

    [publication, modality, contract].forEach(control => control.addEventListener('change', searchVacancies));
    [minimum, maximum].forEach(control => control.addEventListener('input', () => {
        clearTimeout(salaryDebounce);
        if (updateControls()) salaryDebounce = setTimeout(searchVacancies, 300);
    }));
    candidates.addEventListener('click', () => {
        fewer = !fewer;
        searchVacancies();
    });
    clear.addEventListener('click', () => {
        clearTimeout(salaryDebounce);
        [publication, sector, modality, contract, minimum, maximum].forEach(control => { control.value = ''; });
        fewer = false;
        salary.open = false;
        sectorMenu.open = false;
        sectorSearch.value = '';
        renderSectors();
        searchVacancies();
        publication.focus();
    });
    filters.querySelector('[data-salary-done]').addEventListener('click', () => {
        clearTimeout(salaryDebounce);
        searchVacancies();
        salary.open = false;
        salary.querySelector('summary').focus();
    });
    document.addEventListener('click', event => {
        if (!salary.contains(event.target)) salary.open = false;
    });
    salary.addEventListener('toggle', () => {
        if (salary.open) sectorMenu.open = false;
    });
    salary.addEventListener('keydown', event => {
        if (event.key !== 'Escape') return;
        salary.open = false;
        salary.querySelector('summary').focus();
    });

    const history = document.getElementById('historial');
    const historyList = document.getElementById('lista-historial');
    let suggestionRequest;
    let suggestionDebounce;
    let searchDebounce;

    function suggestions(items) {
        if (!history || !historyList) return;
        historyList.replaceChildren();
        (Array.isArray(items) ? items : []).slice(0, 8).forEach(item => {
            const listItem = document.createElement('li');
            const button = document.createElement('button');
            button.type = 'button';
            button.textContent = item;
            button.addEventListener('click', () => {
                clearTimeout(searchDebounce);
                search.value = item;
                history.style.display = 'none';
                searchVacancies();
            });
            listItem.append(button);
            historyList.append(listItem);
        });
        history.style.display = historyList.children.length ? 'block' : 'none';
    }

    async function loadSuggestions() {
        if (guest) return;
        suggestionRequest?.abort();
        suggestionRequest = new AbortController();
        const signal = suggestionRequest.signal;
        try {
            const text = search.value.trim();
            const endpoint = text.length >= 2
                ? `/postulante/vacantes/sugerencias?texto=${encodeURIComponent(text)}`
                : '/postulante/historial';
            const response = await fetch(endpoint, { signal, credentials: 'same-origin' });
            if (response.redirected && /\/(login|logout)(?:\/|$)/.test(new URL(response.url).pathname)) {
                window.location.assign(response.url);
                return;
            }
            if (response.ok) suggestions(await response.json());
        } catch (error) {
            if (error.name !== 'AbortError') suggestions([]);
        }
    }

    search?.addEventListener('input', () => {
        clearTimeout(searchDebounce);
        searchDebounce = setTimeout(searchVacancies, 300);
        clearTimeout(suggestionDebounce);
        suggestionDebounce = setTimeout(loadSuggestions, 200);
    });
    search?.addEventListener('focus', loadSuggestions);
    document.getElementById('searchForm')?.addEventListener('submit', event => {
        event.preventDefault();
        clearTimeout(searchDebounce);
        searchVacancies();
        if (history) history.style.display = 'none';
        const text = search?.value.trim();
        if (text && !guest) {
            fetch('/postulante/historial/guardar', {
                method: 'POST',
                credentials: 'same-origin',
                headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
                body: new URLSearchParams({ texto: text })
            }).catch(() => {});
        }
    });
    document.getElementById('limpiar-historial')?.addEventListener('click', async () => {
        const response = await fetch('/postulante/historial/limpiar', {
            method: 'POST',
            credentials: 'same-origin'
        }).catch(() => null);
        if (response?.ok) suggestions([]);
    });
    document.addEventListener('click', event => {
        if (event.target.closest('.search-main') || !history) return;
        history.style.display = 'none';
        suggestionRequest?.abort();
    });

    updateControls();
    if (!new URLSearchParams(window.location.search).has('oferta')) searchVacancies();
});
