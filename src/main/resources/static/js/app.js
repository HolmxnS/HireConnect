(function () {
            var root = document.documentElement;
            var toggle = document.getElementById('theme-toggle');
            if (!toggle) return;
            toggle.addEventListener('click', function () {
                var next = root.getAttribute('data-theme') === 'dark' ? 'light' : 'dark';
                root.setAttribute('data-theme', next);
                try { localStorage.setItem('hc-theme', next); } catch (e) { }
            });
        })();

(function () {
    'use strict';
    var menu = document.querySelector('.menu');
    var toggle = document.querySelector('.menu__toggle');
    if (!menu || !toggle) return;
    var mobile = window.matchMedia('(max-width: 860px)');
    function setOpen(open) {
        menu.classList.toggle('is-open', open);
        toggle.setAttribute('aria-expanded', String(open));
        toggle.setAttribute('aria-label', open ? 'Cerrar menú' : 'Abrir menú');
    }
    menu.classList.add('menu--enhanced');
    toggle.addEventListener('click', function () { setOpen(!menu.classList.contains('is-open')); });
    menu.querySelectorAll('a').forEach(function (link) {
        link.addEventListener('click', function () {
            if (mobile.matches && menu.classList.contains('is-open')) {
                setOpen(false);
                toggle.focus();
            }
        });
    });
    document.addEventListener('keydown', function (event) {
        if (event.key === 'Escape' && menu.classList.contains('is-open')) {
            setOpen(false);
            toggle.focus();
        }
    });
    document.addEventListener('click', function (event) {
        if (!menu.contains(event.target)) setOpen(false);
    });
    mobile.addEventListener('change', function () { setOpen(false); });
})();
