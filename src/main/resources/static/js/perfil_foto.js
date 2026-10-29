document.addEventListener('DOMContentLoaded', () => {
  const avatar = document.getElementById('profileAvatarClickArea');
  const input = document.getElementById('fileInput');
  const form = document.getElementById('fotoUploadForm');
  const dialog = document.querySelector('.profile-photo-dialog');
  if (!avatar || !input || !form || !dialog) return;
  const canvas = dialog.querySelector('canvas');
  const context = canvas.getContext('2d');
  const zoom = document.getElementById('photoZoom');
  const save = dialog.querySelector('[data-photo-save]');
  const cancel = dialog.querySelector('[data-photo-cancel]');
  const remove = document.querySelector('[data-photo-remove]');
  const feedback = document.querySelector('[data-photo-feedback]');
  const message = dialog.querySelector('[data-photo-editor-message]');
  let image, objectUrl, x = .5, y = .5, busy = false, drag;
  function notify(text, error = false) {
    if (window.HireConnectToast) { window.HireConnectToast.show(text, error); return; }
    feedback.textContent = text; feedback.hidden = false;
    feedback.classList.toggle('is-error', error);
  }
  function draw() {
    const side = Math.min(image.width, image.height) / Number(zoom.value);
    context.fillStyle = '#fff'; context.fillRect(0, 0, 512, 512);
    context.drawImage(image, (image.width - side) * x, (image.height - side) * y, side, side, 0, 0, 512, 512);
  }
  function updatePhoto(url) {
    const header = document.querySelector('.profile-box > a');
    for (const [container, placeholderClass] of [[avatar, 'photo-placeholder'], [header, 'profile-box__placeholder']]) {
      if (!container) continue;
      container.querySelector('img')?.remove();
      container.querySelector('.' + placeholderClass)?.remove();
      const element = document.createElement(url ? 'img' : 'span');
      if (url) { element.src = url; element.alt = 'Foto de perfil'; element.addEventListener('error', () => { const fallback = document.createElement('span'); fallback.className = placeholderClass; fallback.innerHTML = '<i class="fa-solid fa-user" aria-hidden="true"></i>'; element.replaceWith(fallback); }, { once: true }); }
      else { element.className = placeholderClass; element.innerHTML = '<i class="fa-solid fa-user" aria-hidden="true"></i>'; }
      container.prepend(element);
    }
    document.querySelectorAll('input[name="foto"]').forEach(field => { field.value = url || ''; });
    remove.hidden = !url;
  }
  async function request(options) {
    const headers = {};
    const csrf = document.querySelector('meta[name="_csrf"]');
    const csrfHeader = document.querySelector('meta[name="_csrf_header"]');
    if (csrf && csrfHeader) headers[csrfHeader.content] = csrf.content;
    const response = await fetch(form.dataset.photoUrl, { ...options, headers, credentials: 'same-origin' });
    const data = await response.json().catch(() => ({}));
    if (!response.ok || response.redirected || !data.mensaje) throw new Error(data.mensaje || 'No se pudo actualizar la foto. Revisa tu sesión e inténtalo de nuevo.');
    return data;
  }
  avatar.addEventListener('click', () => { if (!busy) input.click(); });
  input.addEventListener('change', async () => {
    const file = input.files[0]; input.value = '';
    if (!file) return;
    if (!['image/jpeg', 'image/png'].includes(file.type) || file.size > 2 * 1024 * 1024) {
      notify('Selecciona una imagen JPG o PNG de hasta 2 MB.', true); return;
    }
    objectUrl = URL.createObjectURL(file);
    image = new Image(); image.src = objectUrl;
    try {
      await image.decode();
      if (image.width * image.height > 24000000) throw new Error('La imagen debe tener como máximo 24 megapíxeles.');
      x = y = .5; zoom.value = '1'; message.textContent = ''; draw(); dialog.showModal();
    } catch (error) { URL.revokeObjectURL(objectUrl); notify(error.message.startsWith('La imagen') ? error.message : 'No se pudo abrir la imagen. Prueba con otro archivo.', true); }
  });
  zoom.addEventListener('input', draw);
  canvas.tabIndex = 0;
  canvas.addEventListener('keydown', event => {
    const directions = { ArrowLeft: [-.03, 0], ArrowRight: [.03, 0], ArrowUp: [0, -.03], ArrowDown: [0, .03] };
    if (!directions[event.key] || busy) return;
    event.preventDefault();
    x = Math.max(0, Math.min(1, x + directions[event.key][0]));
    y = Math.max(0, Math.min(1, y + directions[event.key][1])); draw();
  });
  canvas.addEventListener('pointerdown', event => {
    drag = { x: event.clientX, y: event.clientY }; canvas.setPointerCapture(event.pointerId);
  });
  canvas.addEventListener('pointermove', event => {
    if (!drag || busy) return;
    // Convierte el arrastre visible en desplazamiento dentro de la imagen original.
    const side = Math.min(image.width, image.height) / Number(zoom.value);
    const scale = side / canvas.getBoundingClientRect().width;
    const clamp = value => Math.max(0, Math.min(1, value));
    if (image.width > side) x = clamp(x - (event.clientX - drag.x) * scale / (image.width - side));
    if (image.height > side) y = clamp(y - (event.clientY - drag.y) * scale / (image.height - side));
    drag = { x: event.clientX, y: event.clientY }; draw();
  });
  canvas.addEventListener('pointerup', () => { drag = null; });
  canvas.addEventListener('pointercancel', () => { drag = null; });
  cancel.addEventListener('click', () => dialog.close());
  dialog.addEventListener('cancel', event => { if (busy) event.preventDefault(); });
  dialog.addEventListener('close', () => { URL.revokeObjectURL(objectUrl); image = null; drag = null; avatar.focus(); });
  function setBusy(value) {
    busy = value; save.disabled = cancel.disabled = zoom.disabled = remove.disabled = value;
    save.textContent = value ? 'Guardando…' : 'Guardar foto';
  }
  save.addEventListener('click', async () => {
    setBusy(true); message.textContent = 'Guardando tu foto…';
    try {
      const blob = await new Promise(resolve => canvas.toBlob(resolve, 'image/jpeg', .9));
      if (!blob) throw new Error('No se pudo preparar la foto. Inténtalo de nuevo.');
      const body = new FormData(); body.append('file', blob, 'perfil.jpg');
      const data = await request({ method: 'POST', body });
      updatePhoto(data.foto); dialog.close(); notify(data.mensaje);
    } catch (error) { message.textContent = error.message; }
    finally { setBusy(false); }
  });
  remove.addEventListener('click', async () => {
    if (busy || !window.confirm('¿Quieres quitar tu foto de perfil?')) return;
    setBusy(true);
    try { const data = await request({ method: 'DELETE' }); updatePhoto(null); notify(data.mensaje); }
    catch (error) { notify(error.message, true); }
    finally { setBusy(false); }
  });
});
