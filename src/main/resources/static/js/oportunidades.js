document.addEventListener('DOMContentLoaded', function () {
    var dialog = document.getElementById('nuevo-proyecto');
    if (!(dialog instanceof HTMLDialogElement)) return;

    var openers = document.querySelectorAll('[data-open-project-dialog]');
    var closers = dialog.querySelectorAll('[data-close-project-dialog]');
    var lastTrigger = null;

    function openDialog(trigger) {
        lastTrigger = trigger;
        if (!dialog.open) dialog.showModal();
        var firstField = dialog.querySelector('input, textarea, select');
        if (firstField) firstField.focus();
    }

    function closeDialog() {
        if (dialog.open) dialog.close();
    }

    openers.forEach(function (trigger) {
        trigger.addEventListener('click', function () { openDialog(trigger); });
    });

    closers.forEach(function (trigger) {
        trigger.addEventListener('click', closeDialog);
    });

    dialog.addEventListener('click', function (event) {
        var bounds = dialog.getBoundingClientRect();
        var outside = event.clientX < bounds.left || event.clientX > bounds.right ||
            event.clientY < bounds.top || event.clientY > bounds.bottom;
        if (outside) closeDialog();
    });

    dialog.addEventListener('close', function () {
        if (lastTrigger) lastTrigger.focus();
    });
});
