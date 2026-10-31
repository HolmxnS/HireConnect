(() => {
    const form = document.querySelector('[data-complete-form]');
    if (!form) return;
    const fields = [...form.querySelectorAll('input:not([type="hidden"]), select, textarea')];
    const alert = form.querySelector('[data-form-error]');
    const submit = form.querySelector('[type="submit"]');
    const submitLabel = submit?.querySelector('span');
    const originalLabel = submitLabel?.textContent;
    form.noValidate = true;

    function validate(field) {
        if (field.disabled) return true;
        const error = document.getElementById(`${field.id}-error`);
        let message = '';
        if (field.required && !field.value.trim()) {
            message = field.tagName === 'SELECT' ? 'Selecciona una opción.' : 'Completa este campo.';
        } else if (field.validity.badInput) {
            message = 'Escribe una fecha válida.';
        } else if (field.validity.rangeOverflow) {
            message = 'La fecha de nacimiento no puede ser futura.';
        } else if (field.validity.patternMismatch || field.validity.tooShort) {
            message = field.dataset.patternMessage || 'Revisa el formato de este dato.';
        } else if (field.validity.typeMismatch) {
            message = 'Escribe una dirección completa, por ejemplo https://tuempresa.com.';
        } else if (field.validity.tooLong) {
            message = `Usa como máximo ${field.maxLength} caracteres.`;
        } else if (!field.validity.valid) {
            message = 'Revisa este campo para continuar.';
        }
        field.classList.toggle('is-invalid', Boolean(message));
        field.setAttribute('aria-invalid', String(Boolean(message)));
        if (error) {
            error.textContent = message;
            error.hidden = !message;
        }
        return !message;
    }

    fields.forEach(field => {
        field.addEventListener('blur', () => {
            field.dataset.touched = 'true';
            validate(field);
        });
        const update = () => {
            if (field.dataset.touched || field.getAttribute('aria-invalid') === 'true') validate(field);
            if (field.hasAttribute('data-counted')) {
                const counter = document.getElementById(`${field.id}-counter`);
                if (counter) counter.textContent = `${field.value.length} / ${field.maxLength}`;
            }
            if (alert && !fields.some(input => input.getAttribute('aria-invalid') === 'true')) alert.hidden = true;
        };
        field.addEventListener('input', update);
        field.addEventListener('change', update);
    });

    form.addEventListener('submit', event => {
        if (!submit || submit.disabled) {
            event.preventDefault();
            return;
        }
        const invalid = fields.filter(field => !validate(field));
        if (invalid.length) {
            event.preventDefault();
            if (alert) alert.hidden = false;
            invalid[0].focus();
            return;
        }
        if (alert) alert.hidden = true;
        submit.disabled = true;
        form.setAttribute('aria-busy', 'true');
        if (submitLabel) submitLabel.textContent = 'Guardando…';
    });

    // El navegador puede restaurar el formulario desde la caché al volver atrás.
    window.addEventListener('pageshow', () => {
        if (submit) submit.disabled = false;
        form.removeAttribute('aria-busy');
        if (submitLabel) submitLabel.textContent = originalLabel;
    });
})();
