(function () {
    var root = document.documentElement;
    var KEY = 'hc-theme';

    function saved() {
        try { return localStorage.getItem(KEY); } catch (e) { return null; }
    }

    var theme = saved();
    if (theme !== 'dark' && theme !== 'light') {
        theme = (window.matchMedia && window.matchMedia('(prefers-color-scheme: dark)').matches) ? 'dark' : 'light';
    }
    root.setAttribute('data-theme', theme);

    function sync() {
        var dark = root.getAttribute('data-theme') === 'dark';
        var buttons = document.querySelectorAll('[data-theme-toggle]');
        for (var i = 0; i < buttons.length; i++) {
            buttons[i].setAttribute('aria-pressed', dark ? 'true' : 'false');
            buttons[i].setAttribute('aria-label', dark ? 'Cambiar a modo claro' : 'Cambiar a modo oscuro');
            buttons[i].setAttribute('title', dark ? 'Cambiar a modo claro' : 'Cambiar a modo oscuro');
        }
    }

    document.addEventListener('click', function (e) {
        var btn = e.target.closest ? e.target.closest('[data-theme-toggle]') : null;
        if (!btn) return;
        var next = root.getAttribute('data-theme') === 'dark' ? 'light' : 'dark';
        root.setAttribute('data-theme', next);
        try { localStorage.setItem(KEY, next); } catch (err) { }
        sync();
    });

    window.addEventListener('storage', function (e) {
        if (e.key === KEY && (e.newValue === 'dark' || e.newValue === 'light')) {
            root.setAttribute('data-theme', e.newValue);
            sync();
        }
    });

    document.addEventListener('DOMContentLoaded', sync);
})();