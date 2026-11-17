(function () {
    'use strict';
    var application = document.getElementById('confirm-application-dialog');
    var invitation = document.querySelector('[data-profile-invitation]');
    var trigger = null;
    var proceeding = false;

    document.querySelectorAll('.candidate-modal').forEach(function (dialog) {
        dialog.querySelectorAll('[data-modal-close]').forEach(function (button) {
            button.addEventListener('click', function () { if (!proceeding) dialog.close(); });
        });
        dialog.addEventListener('cancel', function (event) { if (proceeding) event.preventDefault(); });
        dialog.addEventListener('click', function (event) {
            if (event.target !== dialog || proceeding) return;
            var bounds = dialog.getBoundingClientRect();
            if (event.clientX < bounds.left || event.clientX > bounds.right || event.clientY < bounds.top || event.clientY > bounds.bottom) dialog.close();
        });
        dialog.addEventListener('close', function () {
            if (dialog === application && trigger && trigger.isConnected) trigger.focus();
        });
    });

    document.addEventListener('click', function (event) {
        var link = event.target.closest('[data-confirm-application="true"]');
        if (!link || !application || typeof application.showModal !== 'function') return;
        event.preventDefault();
        trigger = link;
        proceeding = false;
        application.querySelector('[data-application-name]').textContent = link.dataset.applicationTitle || 'Oferta de empleo';
        application.querySelector('[data-application-employer]').textContent = link.dataset.applicationCompany || 'Empresa';
        var proceed = application.querySelector('[data-application-proceed]');
        proceed.href = link.href;
        proceed.removeAttribute('aria-disabled');
        if (!application.open) application.showModal();
    });
    if (application) {
        application.querySelector('[data-application-proceed]').addEventListener('click', function (event) {
            if (proceeding) { event.preventDefault(); return; }
            proceeding = true;
            this.setAttribute('aria-disabled', 'true');
        });
    }
    window.addEventListener('pageshow', function () {
        proceeding = false;
        if (application && application.open) application.close();
    });
    if (invitation && typeof invitation.showModal === 'function') {
        var preference = invitation.querySelector('[data-profile-preference]');
        var checkbox = preference.querySelector('input[type="checkbox"]');
        preference.addEventListener('submit', function (event) { event.preventDefault(); });
        checkbox.addEventListener('change', async function () {
            var error = preference.querySelector('[data-preference-error]');
            error.hidden = true;
            checkbox.disabled = true;
            var body = new FormData(preference);
            body.set('ocultar', String(checkbox.checked));
            try {
                var response = await fetch(preference.action, { method: 'POST', body: body, credentials: 'same-origin', keepalive: true });
                if (!response.ok || response.redirected) throw new Error('Preference not saved');
            } catch (_) {
                checkbox.checked = !checkbox.checked;
                error.hidden = false;
            } finally {
                checkbox.disabled = false;
            }
        });
        invitation.showModal();
    }
})();
