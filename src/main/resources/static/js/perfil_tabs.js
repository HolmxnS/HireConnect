document.addEventListener('DOMContentLoaded', function () {
    const tabs = document.querySelectorAll('.profile-tab');
    const sections = document.querySelectorAll('.profile-section');

    function activateTab(tabName) {
        tabs.forEach(t => {
            t.classList.remove('profile-tab--active');
        });

        sections.forEach(s => {
            s.classList.remove('profile-section--visible');
            s.classList.add('profile-section--hidden');
        });

        const activeTab = document.querySelector(`.profile-tab[data-tab="${tabName}"]`);
        if (activeTab) activeTab.classList.add('profile-tab--active');

        const activeSection = document.getElementById(`section-${tabName}`);
        if (activeSection) {
            activeSection.classList.remove('profile-section--hidden');
            activeSection.classList.add('profile-section--visible');
        }

        sessionStorage.setItem('activeProfileTab', tabName);
    }

    tabs.forEach(tab => {
        tab.addEventListener('click', function () {
            activateTab(this.dataset.tab);
        });
    });

    // Restaura el tab activo al recargar (útil tras submit de formulario)
    const savedTab = sessionStorage.getItem('activeProfileTab') || 'basica';
    activateTab(savedTab);
});