document.addEventListener('DOMContentLoaded', function () {
    const tabs = Array.from(document.querySelectorAll('.profile-tab'));
    const sections = Array.from(document.querySelectorAll('.profile-section'));
    if (!tabs.length) return;
    const storageKey = 'hc-profile-tab';
    function activateTab(name, focus) {
        if (!tabs.some(tab => tab.dataset.tab === name)) name = 'resumen';
        tabs.forEach(tab => {
            const active = tab.dataset.tab === name;
            tab.classList.toggle('profile-tab--active', active);
            tab.setAttribute('aria-selected', String(active));
            tab.tabIndex = active ? 0 : -1;
            if (active && focus) tab.focus();
        });
        sections.forEach(section => {
            const active = section.id === 'section-' + name;
            section.classList.toggle('profile-section--visible', active);
            section.classList.toggle('profile-section--hidden', !active);
        });
        try { sessionStorage.setItem(storageKey, name); } catch (error) {}
    }
    tabs.forEach((tab, index) => {
        const name = tab.dataset.tab;
        tab.id = 'profile-tab-' + name;
        tab.setAttribute('aria-controls', 'section-' + name);
        const section = document.getElementById('section-' + name);
        section.setAttribute('role', 'tabpanel');
        section.setAttribute('aria-labelledby', tab.id);
        tab.addEventListener('click', () => activateTab(name, false));
        tab.addEventListener('keydown', event => {
            let next;
            if (event.key === 'ArrowRight') next = (index + 1) % tabs.length;
            if (event.key === 'ArrowLeft') next = (index - 1 + tabs.length) % tabs.length;
            if (event.key === 'Home') next = 0;
            if (event.key === 'End') next = tabs.length - 1;
            if (next !== undefined) { event.preventDefault(); activateTab(tabs[next].dataset.tab, true); }
        });
    });
    document.querySelectorAll('[data-profile-edit]').forEach(button => {
        button.addEventListener('click', () => activateTab('basica', true));
    });
    // Restaura la sección tras guardar; un valor antiguo no puede dejar todas las pestañas ocultas.
    let saved;
    try { saved = sessionStorage.getItem(storageKey); } catch (error) {}
    activateTab(saved || 'resumen', false);
});
