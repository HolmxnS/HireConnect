(function () {
    'use strict';
    var container = document.querySelector('[data-company-jobs]');
    if (!container) return;
    var jobs = Array.from(container.children);
    var more = document.querySelector('[data-company-more]');
    var progress = document.querySelector('[data-company-progress]');
    var visible = 5;
    function render() {
        jobs.forEach(function (job, index) { job.hidden = index >= visible; });
        more.hidden = visible >= jobs.length;
        progress.textContent = jobs.length > 5 ? 'Mostrando ' + Math.min(visible, jobs.length) + ' de ' + jobs.length + ' empleos' : '';
    }
    more.addEventListener('click', function () {
        var next = jobs[visible];
        visible += 5;
        render();
        if (next) next.focus();
    });
    render();
})();
