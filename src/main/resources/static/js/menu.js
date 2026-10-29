document.addEventListener('DOMContentLoaded', () => {
    const menu = document.querySelector('.menu');
    const toggle = document.querySelector('.menu__toggle');
    const panel = document.getElementById('menu-panel');
    if (!menu || !toggle || !panel) return;

    const setOpen = (open) => {
        menu.classList.toggle('menu--open', open);
        toggle.setAttribute('aria-expanded', String(open));
        toggle.setAttribute('aria-label', open ? 'Cerrar menú' : 'Abrir menú');
    };

    toggle.addEventListener('click', () => {
        setOpen(!menu.classList.contains('menu--open'));
    });

    panel.querySelectorAll('a').forEach((a) => {
        a.addEventListener('click', () => setOpen(false));
    });
    document.addEventListener('keydown', (e) => {
        if (e.key === 'Escape') setOpen(false);
    });
    window.matchMedia('(min-width: 901px)').addEventListener('change', (e) => {
        if (e.matches) setOpen(false);
    });

    const onScroll = () => menu.classList.toggle('menu--scrolled', window.scrollY > 8);
    onScroll();
    window.addEventListener('scroll', onScroll, { passive: true });
});