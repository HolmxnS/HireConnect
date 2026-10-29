document.addEventListener('DOMContentLoaded', () => {
    const dialog = document.querySelector('[data-admin-dialog]');
    let trigger;
    document.addEventListener('click', event => {
        const link = event.target.closest('[data-admin-confirm]');
        if (!link || !dialog) return;
        event.preventDefault(); trigger = link;
        dialog.querySelector('[data-admin-confirm-message]').textContent = link.dataset.adminConfirm;
        dialog.querySelector('[data-admin-proceed]').href = link.href;
        dialog.showModal();
    });
    dialog?.querySelector('[data-admin-cancel]').addEventListener('click', () => dialog.close());
    dialog?.addEventListener('close', () => trigger?.focus());
    const toggle = document.getElementById('notifToggle'), panel = document.getElementById('notifPanel');
    const close = () => { panel?.classList.add('hidden'); toggle?.setAttribute('aria-expanded', 'false'); };
    toggle?.addEventListener('click', () => { panel.classList.toggle('hidden'); toggle.setAttribute('aria-expanded', String(!panel.classList.contains('hidden'))); });
    document.addEventListener('click', event => { if (panel && !panel.contains(event.target) && !toggle.contains(event.target)) close(); });
    document.addEventListener('keydown', event => { if (event.key === 'Escape' && panel && !panel.classList.contains('hidden')) { close(); toggle.focus(); } });
    async function updateNotification(event, path, action) {
        event.preventDefault(); const button = event.currentTarget;
        if (button.dataset.busy) return; button.dataset.busy = 'true';
        try {
            const response = await fetch(path, { method: 'POST', headers: { Accept: 'application/json' } });
            if (!response.ok || response.redirected) throw Error();
            const data = await response.json(); if (!data.success) throw Error();
            action();
            let badge = toggle.querySelector('.notification__badge');
            if (!badge && data.unreadCount) { badge = document.createElement('span'); badge.className = 'notification__badge'; toggle.append(badge); }
            if (badge) { badge.textContent = data.unreadCount; badge.hidden = !data.unreadCount; }
            const empty = panel.querySelector('.notification__empty');
            if (empty) empty.hidden = Boolean(panel.querySelector('.notification__item'));
        } catch (error) { window.HireConnectToast?.show('No se pudo actualizar la notificación. Inténtalo de nuevo.', true); }
        finally { delete button.dataset.busy; }
    }
    panel?.querySelectorAll('.notification__item').forEach(item => {
        item.querySelector('.mark-one')?.addEventListener('click', event => updateNotification(event, '/admin/notificaciones/leida/' + encodeURIComponent(item.dataset.id), () => { item.classList.remove('unread'); item.classList.add('read'); }));
        item.querySelector('.delete-one')?.addEventListener('click', event => updateNotification(event, '/admin/notificaciones/eliminar/' + encodeURIComponent(item.dataset.id), () => item.remove()));
    });
    document.getElementById('markAllRead')?.addEventListener('click', event => updateNotification(event, '/admin/notificaciones/leida/todas', () => panel.querySelectorAll('.notification__item').forEach(item => { item.classList.remove('unread'); item.classList.add('read'); })));
    document.getElementById('deleteAll')?.addEventListener('click', event => updateNotification(event, '/admin/notificaciones/eliminar/todas', () => panel.querySelectorAll('.notification__item').forEach(item => item.remove())));
});
