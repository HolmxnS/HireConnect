document.addEventListener('DOMContentLoaded', () => {
    const about = document.getElementById('admin-about');
    const count = () => { document.querySelector('[data-about-count]').textContent = about.value.length + ' / 500'; };
    about.addEventListener('input', count); count();
    const dialog = document.querySelector('[data-admin-photo-dialog]');
    const file = document.getElementById('admin-photo-file');
    const preview = dialog.querySelector('img'); let objectUrl;
    document.querySelector('[data-admin-photo-open]').addEventListener('click', () => dialog.showModal());
    dialog.querySelector('[data-admin-photo-cancel]').addEventListener('click', () => dialog.close());
    dialog.addEventListener('close', () => { if (objectUrl) URL.revokeObjectURL(objectUrl); objectUrl = null; file.value = ''; preview.hidden = true; preview.removeAttribute('src'); document.querySelector('[data-admin-photo-open]').focus(); });
    file.addEventListener('change', () => {
        if (objectUrl) URL.revokeObjectURL(objectUrl); preview.hidden = true;
        const selected = file.files[0]; if (!selected) return;
        if (!['image/jpeg', 'image/png'].includes(selected.type) || selected.size > 2 * 1024 * 1024) {
            file.value = ''; window.HireConnectToast.show('Elige una imagen JPG o PNG de hasta 2 MB.', true); return;
        }
        objectUrl = URL.createObjectURL(selected); preview.src = objectUrl; preview.hidden = false;
    });
});
