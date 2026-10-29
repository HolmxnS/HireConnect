(function () {
    const root = document.documentElement;
    const KEY = 'hc-theme';
    const MOTION = 'hc-reduce-motion';
    const system = window.matchMedia('(prefers-color-scheme: dark)');
    let preference, reduceMotion;
    try { preference = localStorage.getItem(KEY); reduceMotion = localStorage.getItem(MOTION) === 'true'; } catch (error) {}
    if (!['dark', 'light', 'system'].includes(preference)) preference = 'system';
    function sync() {
        const dark = root.getAttribute('data-theme') === 'dark';
        document.querySelectorAll('[data-theme-toggle]').forEach(button => {
            button.setAttribute('aria-pressed', String(dark));
            button.setAttribute('aria-label', dark ? 'Cambiar a modo claro' : 'Cambiar a modo oscuro');
            button.title = button.getAttribute('aria-label');
        });
    }
    function apply() {
        root.setAttribute('data-theme', preference === 'system' ? (system.matches ? 'dark' : 'light') : preference);
        root.toggleAttribute('data-reduce-motion', Boolean(reduceMotion));
        sync();
        document.dispatchEvent(new CustomEvent('hc:appearance', { detail: { theme: preference, reduceMotion } }));
    }
    function persist(key, value) {
        try { localStorage.setItem(key, value); return true; } catch (error) { return false; }
    }
    window.HireConnectAppearance = {
        get: () => ({ theme: preference, reduceMotion: Boolean(reduceMotion) }),
        setTheme(value) {
            if (!['dark', 'light', 'system'].includes(value)) return false;
            preference = value; const saved = persist(KEY, value); apply(); return saved;
        },
        setReduceMotion(value) {
            reduceMotion = Boolean(value); const saved = persist(MOTION, String(reduceMotion)); apply(); return saved;
        }
    };
    apply();
    system.addEventListener('change', () => { if (preference === 'system') apply(); });
    document.addEventListener('click', event => {
        if (event.target.closest?.('[data-theme-toggle]')) window.HireConnectAppearance.setTheme(root.getAttribute('data-theme') === 'dark' ? 'light' : 'dark');
    });
    window.addEventListener('storage', event => {
        if (event.key === KEY || event.key === MOTION || event.key === null) {
            try { preference = localStorage.getItem(KEY); reduceMotion = localStorage.getItem(MOTION) === 'true'; } catch (error) {}
            if (!['dark', 'light', 'system'].includes(preference)) preference = 'system';
            apply();
        }
    });
    document.addEventListener('DOMContentLoaded', sync);
})();
