(function () {
    let container;
    function show(text, error = false) {
        if (!text) return;
        if (!container) {
            container = document.createElement('div'); container.className = 'hc-toasts';
            document.body.append(container);
        }
        while (container.children.length >= 3) container.firstElementChild.remove();
        const toast = document.createElement('div'); toast.className = 'hc-toast' + (error ? ' hc-toast--error' : '');
        toast.setAttribute('role', error ? 'alert' : 'status');
        const icon = document.createElement('span'); icon.className = 'hc-toast__icon'; icon.textContent = error ? '!' : '✓'; icon.setAttribute('aria-hidden', 'true');
        const message = document.createElement('p'); message.textContent = text;
        const close = document.createElement('button'); close.type = 'button'; close.textContent = '×'; close.setAttribute('aria-label', 'Cerrar mensaje');
        const timer = setTimeout(() => toast.remove(), 5000);
        close.addEventListener('click', () => { clearTimeout(timer); toast.remove(); });
        toast.append(icon, message, close); container.append(toast);
    }
    window.HireConnectToast = { show };
    document.addEventListener('DOMContentLoaded', () => {
        document.querySelectorAll('[data-toast-message]').forEach(element => {
            const text = element.textContent.trim(); element.hidden = true;
            show(text, element.hasAttribute('data-toast-error') || element.classList.contains('error') || element.classList.contains('settings-notice--error'));
        });
    });
})();
