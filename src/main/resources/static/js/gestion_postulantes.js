document.addEventListener("DOMContentLoaded", function () {

    const list = document.getElementById('postulantesList');
    const sortSelect = document.getElementById('sortFilterPostulantes');

    function sortPostulantes() {
        if (!list) return;
        const sortValue = sortSelect.value;
        const items = Array.from(list.querySelectorAll('.postulante-item'));

        items.sort((a, b) => {
            const dateA = new Date(a.getAttribute('data-date'));
            const dateB = new Date(b.getAttribute('data-date'));
            return sortValue === 'most_recent' ? dateB - dateA : dateA - dateB;
        });

        list.innerHTML = '';
        items.forEach(item => list.appendChild(item));
    }

    sortSelect.addEventListener('change', sortPostulantes);
    sortPostulantes();
});

function filterPostulantes() {
    const searchInput = document.getElementById('searchInputPostulantes').value.toUpperCase();
    const statusFilter = document.getElementById('statusFilter').value;
    const cvFilter = document.getElementById('cvFilter').value;
    const list = document.getElementById('postulantesList');
    if (!list) return;

    const items = list.querySelectorAll('.postulante-item');

    items.forEach(item => {
        const nombre = item.querySelector('.nombre').textContent.toUpperCase();
        const apellido = item.querySelector('.apellido').textContent.toUpperCase();
        const cv = item.querySelector('.cv').textContent;
        const estado = item.querySelector('.postulacion-estado').textContent;

        const matchName = nombre.includes(searchInput) || apellido.includes(searchInput);
        const matchStatus = statusFilter === 'all' || estado === statusFilter;
        const matchCV = cvFilter === 'all' || (cvFilter === 'complete' && cv === 'Completo') || (cvFilter === 'incomplete' && cv === 'Incompleto');

        item.style.display = (matchName && matchStatus && matchCV) ? '' : 'none';
    });
}
