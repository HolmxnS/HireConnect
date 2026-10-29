(function () {
    'use strict';
    var workspace = document.querySelector('[data-vacancy-workspace]');
    if (!workspace) return;
    var cards = Array.from(workspace.querySelectorAll('.vacancies__card'));
    var reader = workspace.querySelector('[data-vacancy-reader]');
    var content = workspace.querySelector('[data-vacancy-content]');
    var back = workspace.querySelector('[data-vacancy-back]');
    var count = document.querySelector('[data-vacancy-count]');
    var mobile = window.matchMedia('(max-width: 900px)');
    var selected = null;
    var emptyContent = content.cloneNode(true);
    workspace.classList.add('is-enhanced');
    reader.hidden = false;

    function visibleCards() { return cards.filter(function (card) { return card.style.display !== 'none' && !card.hidden; }); }
    function select(card, userAction) {
        selected = card;
        cards.forEach(function (item) {
            var link = item.querySelector('[data-vacancy-open]');
            item.classList.toggle('is-selected', item === card);
            if (item === card) link.setAttribute('aria-current', 'true');
            else link.removeAttribute('aria-current');
        });
        content.replaceChildren(card.querySelector('[data-vacancy-template]').content.cloneNode(true));
        content.scrollTop = 0;
        if (userAction && mobile.matches) {
            workspace.classList.add('is-detail-open');
            back.focus();
        }
    }
    function synchronize() {
        var visible = visibleCards();
        count.textContent = visible.length + (visible.length === 1 ? ' vacante disponible' : ' vacantes disponibles');
        if (visible.includes(selected)) return;
        if (visible.length) select(visible[0], false);
        else {
            selected = null;
            workspace.classList.remove('is-detail-open');
            cards.forEach(function (card) {
                card.classList.remove('is-selected');
                card.querySelector('[data-vacancy-open]').removeAttribute('aria-current');
            });
            content.replaceChildren.apply(content, Array.from(emptyContent.cloneNode(true).childNodes));
        }
    }
    cards.forEach(function (card) {
        card.querySelector('[data-vacancy-open]').addEventListener('click', function (event) {
            if (event.button !== 0 || event.metaKey || event.ctrlKey || event.shiftKey || event.altKey) return;
            event.preventDefault();
            select(card, true);
        });
    });
    back.addEventListener('click', function () {
        workspace.classList.remove('is-detail-open');
        if (selected) selected.querySelector('[data-vacancy-open]').focus({ preventScroll: true });
    });
    mobile.addEventListener('change', function () { workspace.classList.remove('is-detail-open'); });
    document.addEventListener('vacantes:filtradas', synchronize);
    synchronize();
})();
