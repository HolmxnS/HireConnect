(function () {
    'use strict';
    var tools = document.querySelector('[data-application-tools]');
    if (!tools) return;
    var search = document.getElementById('application-search');
    var state = document.getElementById('application-state');
    var cards = Array.from(document.querySelectorAll('[data-application-card]'));
    var count = document.querySelector('[data-application-count]');
    var empty = document.querySelector('[data-application-no-results]');
    function normalize(value) {
        return String(value || '').normalize('NFD').replace(/[\u0300-\u036f]/g, '').toLowerCase();
    }
    function filter() {
        var query = normalize(search.value).trim();
        var visible = 0;
        cards.forEach(function (card) {
            var matches = normalize(card.dataset.title + ' ' + card.dataset.company).includes(query)
                && (!state.value || card.dataset.state === state.value);
            card.hidden = !matches;
            if (matches) visible++;
        });
        count.textContent = visible + ' de ' + cards.length + ' postulaciones · Más recientes primero';
        empty.hidden = visible !== 0;
    }
    tools.hidden = false;
    search.addEventListener('input', filter);
    state.addEventListener('change', filter);
    document.querySelector('[data-application-reset]').addEventListener('click', function () {
        search.value = ''; state.value = ''; filter(); search.focus();
    });
    filter();
})();
