document.addEventListener('DOMContentLoaded', () => {
    const navigation = document.querySelector('[data-company-tabs]');
    if (!navigation) return;
    const tabs = Array.from(navigation.querySelectorAll('[data-company-tab]'));
    navigation.setAttribute('role', 'tablist');
    function activate(name, focus = false) {
        tabs.forEach(tab => {
            const active = tab.dataset.companyTab === name;
            tab.classList.toggle('profile-tab--active', active);
            tab.setAttribute('aria-selected', String(active));
            tab.tabIndex = active ? 0 : -1;
            document.getElementById(tab.dataset.companyTab).hidden = !active;
            if (active && focus) tab.focus();
        });
    }
    tabs.forEach((tab, index) => {
        tab.id = tab.dataset.companyTab + '-tab';
        tab.setAttribute('role', 'tab');
        tab.setAttribute('aria-controls', tab.dataset.companyTab);
        const panel = document.getElementById(tab.dataset.companyTab);
        panel.setAttribute('role', 'tabpanel'); panel.setAttribute('aria-labelledby', tab.id);
        tab.addEventListener('click', () => activate(tab.dataset.companyTab));
        tab.addEventListener('keydown', event => {
            let next;
            if (event.key === 'ArrowRight') next = (index + 1) % tabs.length;
            if (event.key === 'ArrowLeft') next = (index - 1 + tabs.length) % tabs.length;
            if (event.key === 'Home') next = 0;
            if (event.key === 'End') next = tabs.length - 1;
            if (next !== undefined) { event.preventDefault(); activate(tabs[next].dataset.companyTab, true); }
        });
    });
    activate('company-info');
    const editButton = document.querySelector('[data-company-edit]');
    const editForm = document.getElementById('company-edit-form');
    const readView = document.querySelector('[data-company-read]');
    if (editButton && editForm && readView) {
        editButton.addEventListener('click', () => {
            activate('company-info');
            readView.hidden = true; editForm.hidden = false;
            editButton.setAttribute('aria-expanded', 'true');
            editForm.querySelector('input:not([type="hidden"])')?.focus();
        });
        document.querySelector('[data-company-edit-cancel]').addEventListener('click', () => {
            const photo = editForm.querySelector('input[name="foto"]');
            const currentPhoto = photo?.value;
            editForm.querySelector('form').reset();
            if (photo) photo.value = currentPhoto;
            readView.hidden = false; editForm.hidden = true;
            editButton.setAttribute('aria-expanded', 'false'); editButton.focus();
        });
    }
    const jobs = Array.from(document.querySelectorAll('[data-company-job]'));
    const more = document.querySelector('[data-jobs-more]');
    const progress = document.querySelector('[data-jobs-progress]');
    if (!jobs.length || !more) return;
    let visible = 3;
    function render() {
        jobs.forEach((job, index) => { job.hidden = index >= visible; });
        more.hidden = visible >= jobs.length;
        progress.textContent = 'Mostrando ' + Math.min(visible, jobs.length) + ' de ' + jobs.length + ' empleos';
    }
    more.addEventListener('click', () => {
        const firstNew = visible; visible += 3; render();
        jobs[firstNew]?.querySelector('h3 a')?.focus({ preventScroll: true });
    });
    render();
});
