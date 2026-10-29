document.addEventListener('DOMContentLoaded', () => {
    const form = document.getElementById('vacancy-compose-form');
    if (!form) return;
    const fields = ['titulo', 'descripcion', 'salario', 'modalidad', 'tipoContrato', 'experienciaRequerida', 'habilidadesRequeridas', 'fechaInicio', 'fechaFin'];
    const input = name => form.elements.namedItem(name);
    const write = (selector, text) => { document.querySelector(selector).textContent = text; };
    const date = value => value ? new Intl.DateTimeFormat('es-CO', { day: 'numeric', month: 'short', year: 'numeric' }).format(new Date(value + 'T12:00:00')) : 'Por definir';
    function update() {
        write('[data-preview-title]', input('titulo').value.trim() || 'El próximo gran puesto');
        write('[data-preview-description]', input('descripcion').value.trim() || 'La descripción de tu vacante aparecerá aquí.');
        write('[data-description-count]', input('descripcion').value.length + ' / 1000');
        ['modalidad', 'tipoContrato', 'experienciaRequerida'].forEach(name => {
            const select = input(name);
            write('[data-preview-' + name + ']', select.value ? select.selectedOptions[0].textContent : { modalidad: 'Modalidad', tipoContrato: 'Contrato', experienciaRequerida: 'Por definir' }[name]);
        });
        const salary = input('salario');
        write('[data-preview-salary]', salary.value && salary.validity.valid ? new Intl.NumberFormat('es-CO', { style: 'currency', currency: 'COP', maximumFractionDigits: 0 }).format(salary.valueAsNumber) : 'Salario por definir');
        const skills = input('habilidadesRequeridas').value.split(',').map(value => value.trim()).filter(Boolean);
        const container = document.querySelector('[data-preview-skills]');
        container.replaceChildren(...[...new Set(skills)].map(skill => { const tag = document.createElement('span'); tag.textContent = skill; return tag; }));
        const start = input('fechaInicio'), end = input('fechaFin');
        end.min = start.value;
        end.setCustomValidity(start.value && end.value && end.value < start.value ? 'El cierre debe ser igual o posterior a la apertura.' : '');
        write('[data-preview-dates]', start.value || end.value ? date(start.value) + ' — ' + date(end.value) : 'Fechas por definir');
        const count = fields.filter(name => input(name).value.trim() && input(name).validity.valid).length;
        document.querySelector('[data-compose-progress]').value = count;
        write('[data-progress-label]', count + ' de 9 campos');
        const today = new Date();
        const localDay = today.getFullYear() + '-' + String(today.getMonth() + 1).padStart(2, '0') + '-' + String(today.getDate()).padStart(2, '0');
        write('[data-publish-note]', form.dataset.editing === 'true' ? 'Los cambios se aplicarán a esta vacante al guardar.' : count < fields.length ? 'Completa los datos para preparar la publicación.' : start.value > localDay ? 'La vacante quedará programada para la fecha de apertura.' : 'Tu vacante se publicará al guardar.');
    }
    form.addEventListener('input', update);
    form.addEventListener('change', update);
    update();
});
