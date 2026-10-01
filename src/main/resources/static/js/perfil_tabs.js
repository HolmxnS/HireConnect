document.addEventListener('DOMContentLoaded', function () {
    const tabs = document.querySelectorAll('.profile-tab');
    const sections = document.querySelectorAll('.profile-section');

    function activateTab(tabName) {
        // Desactiva todos los tabs
        tabs.forEach(t => {
            t.classList.remove('profile-tab--active');
        });

        // Oculta todas las secciones
        sections.forEach(s => {
            s.classList.remove('profile-section--visible');
            s.classList.add('profile-section--hidden');
        });

        // Activa el tab clickeado
        const activeTab = document.querySelector(`.profile-tab[data-tab="${tabName}"]`);
        if (activeTab) activeTab.classList.add('profile-tab--active');

        // Muestra la sección correspondiente
        const activeSection = document.getElementById(`section-${tabName}`);
        if (activeSection) {
            activeSection.classList.remove('profile-section--hidden');
            activeSection.classList.add('profile-section--visible');
        }

        // Guarda el tab activo en sessionStorage
        sessionStorage.setItem('activeProfileTab', tabName);
    }

    // Click en tabs
    tabs.forEach(tab => {
        tab.addEventListener('click', function () {
            activateTab(this.dataset.tab);
        });
    });

    // Restaura el tab activo al recargar (útil tras submit de formulario)
    const savedTab = sessionStorage.getItem('activeProfileTab') || 'basica';
    activateTab(savedTab);
});