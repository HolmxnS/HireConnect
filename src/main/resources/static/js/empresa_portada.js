document.addEventListener('DOMContentLoaded', () => {
    const cover = document.querySelector('[data-company-cover]');
    if (!cover) return;
    const input = cover.querySelector('[data-cover-input]');
    const choose = cover.querySelector('[data-cover-choose]');
    const remove = cover.querySelector('[data-cover-remove]');
    const feedback = document.querySelector('[data-cover-feedback]');
    const dialog = document.querySelector('.company-cover-dialog');
    const canvas = dialog.querySelector('canvas');
    const zoom = document.getElementById('coverZoom');
    const position = document.getElementById('coverPosition');
    const message = dialog.querySelector('[data-cover-message]');
    const save = dialog.querySelector('[data-cover-save]');
    const cancel = dialog.querySelector('[data-cover-cancel]');
    let image, url, busy = false;
    function notify(text, error = false) { if (window.HireConnectToast) { window.HireConnectToast.show(text, error); return; } feedback.textContent = text; feedback.hidden = false; feedback.classList.toggle('is-error', error); }
    function draw() {
        const width = Math.min(image.width, image.height * 4) / Number(zoom.value);
        const height = width / 4;
        const context = canvas.getContext('2d');
        context.fillStyle = '#fff'; context.fillRect(0, 0, 1600, 400);
        context.drawImage(image, (image.width - width) / 2, (image.height - height) * Number(position.value), width, height, 0, 0, 1600, 400);
    }
    function setBusy(value) {
        busy = value; [save, cancel, choose, remove, zoom, position].forEach(element => { element.disabled = value; });
        save.textContent = value ? 'Guardando…' : 'Guardar portada';
    }
    async function request(options) {
        const headers = {};
        const csrf = document.querySelector('meta[name="_csrf"]'), csrfHeader = document.querySelector('meta[name="_csrf_header"]');
        if (csrf && csrfHeader) headers[csrfHeader.content] = csrf.content;
        const response = await fetch(cover.dataset.coverUrl, { ...options, headers, credentials: 'same-origin' });
        const data = await response.json().catch(() => ({}));
        if (!response.ok || response.redirected || !data.mensaje) throw new Error(data.mensaje || 'No pudimos actualizar la portada. Revisa tu sesión e inténtalo de nuevo.');
        return data;
    }
    function update(source) {
        cover.querySelector('[data-cover-image]')?.remove();
        if (source) { const element = document.createElement('img'); element.src = source; element.alt = 'Portada de la empresa'; element.dataset.coverImage = ''; cover.prepend(element); }
        remove.hidden = !source;
        cover.querySelector('[data-cover-label]').textContent = source ? 'Cambiar portada' : 'Subir portada';
    }
    choose.addEventListener('click', () => { if (!busy) input.click(); });
    input.addEventListener('change', async () => {
        const file = input.files[0]; input.value = '';
        if (!file) return;
        if (!['image/jpeg', 'image/png'].includes(file.type) || file.size > 5 * 1024 * 1024) { notify('Selecciona una imagen JPG o PNG de hasta 5 MB.', true); return; }
        setBusy(true);
        url = URL.createObjectURL(file); image = new Image(); image.src = url;
        try {
            await image.decode();
            if (image.width * image.height > 24000000) throw new Error('La imagen debe tener como máximo 24 megapíxeles.');
            zoom.value = '1'; position.value = '0.5'; message.textContent = ''; draw(); dialog.showModal();
        } catch (error) { URL.revokeObjectURL(url); notify(error.message.startsWith('La imagen') ? error.message : 'No se pudo abrir la imagen.', true); }
        finally { setBusy(false); }
    });
    zoom.addEventListener('input', draw); position.addEventListener('input', draw);
    cancel.addEventListener('click', () => dialog.close());
    dialog.addEventListener('cancel', event => { if (busy) event.preventDefault(); });
    dialog.addEventListener('close', () => { URL.revokeObjectURL(url); image = null; choose.focus(); });
    save.addEventListener('click', async () => {
        setBusy(true); message.textContent = 'Guardando tu portada…';
        try {
            const blob = await new Promise(resolve => canvas.toBlob(resolve, 'image/jpeg', .9));
            if (!blob) throw new Error('No se pudo preparar la imagen. Inténtalo de nuevo.');
            const body = new FormData(); body.append('file', blob, 'portada.jpg');
            const data = await request({ method: 'POST', body });
            update(data.portada); dialog.close(); notify(data.mensaje);
        } catch (error) { message.textContent = error.message; }
        finally { setBusy(false); }
    });
    remove.addEventListener('click', async () => {
        if (busy || !window.confirm('¿Quieres quitar la portada de tu empresa?')) return;
        setBusy(true);
        try { const data = await request({ method: 'DELETE' }); update(null); notify(data.mensaje); }
        catch (error) { notify(error.message, true); }
        finally { setBusy(false); }
    });
});
