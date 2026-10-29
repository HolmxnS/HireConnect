document.addEventListener('DOMContentLoaded', () => {
    const appearance = window.HireConnectAppearance;
    const status = document.querySelector('[data-preference-status]');
    const radios = document.querySelectorAll('input[name="appearance"]');
    const motion = document.getElementById('reduceMotion');
    function sync() {
        const state = appearance.get();
        radios.forEach(input => { input.checked = input.value === state.theme; });
        motion.checked = state.reduceMotion;
    }
    function saved(success) { status.textContent = success ? 'Preferencias guardadas.' : 'Preferencia aplicada. El navegador no permite guardarla para próximas visitas.'; }
    radios.forEach(input => input.addEventListener('change', () => saved(appearance.setTheme(input.value))));
    motion.addEventListener('change', () => saved(appearance.setReduceMotion(motion.checked)));
    document.addEventListener('hc:appearance', sync);
    sync();
    document.querySelectorAll('[data-show-password]').forEach(button => {
        const field = document.getElementById(button.dataset.showPassword);
        button.setAttribute('aria-label', 'Mostrar ' + document.querySelector('label[for="' + field.id + '"]').textContent.toLowerCase());
        button.addEventListener('click', () => {
            const visible = field.type === 'password';
            field.type = visible ? 'text' : 'password';
            button.textContent = visible ? 'Ocultar' : 'Mostrar';
            button.setAttribute('aria-pressed', String(visible));
            button.setAttribute('aria-label', (visible ? 'Ocultar ' : 'Mostrar ') + document.querySelector('label[for="' + field.id + '"]').textContent.toLowerCase());
        });
    });
    const passwordForm = document.querySelector('[data-password-form]');
    if (passwordForm) {
        const current = document.getElementById('passwordActual');
        const next = document.getElementById('nuevaPassword');
        const confirmation = document.getElementById('confirmarPassword');
        function validate() {
            next.setCustomValidity(next.value && next.value === current.value ? 'Elige una contraseña diferente a la actual.' : '');
            confirmation.setCustomValidity(confirmation.value && confirmation.value !== next.value ? 'Las contraseñas no coinciden.' : '');
        }
        [current, next, confirmation].forEach(field => field.addEventListener('input', validate));
        passwordForm.addEventListener('submit', event => { validate(); if (!passwordForm.reportValidity()) event.preventDefault(); });
    }
    const dialog = document.querySelector('.settings-delete-dialog');
    document.querySelector('[data-delete-open]')?.addEventListener('click', () => { dialog.querySelector('form').reset(); dialog.showModal(); });
    dialog?.querySelector('[data-delete-cancel]').addEventListener('click', () => dialog.close());
});
