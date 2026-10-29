document.addEventListener("DOMContentLoaded", function () {
    const list = document.getElementById('vacantesList');
    const select = document.getElementById('sortFilterVacantes');

    if (!list || !select) return;

    function sortVacantes() {
        const sortValue = select.value;
        const items = Array.from(list.querySelectorAll('.vacancy-item'));

        if (items.length === 0) return;

        items.sort((a, b) => {
            const dateA = new Date(a.getAttribute('data-date'));
            const dateB = new Date(b.getAttribute('data-date'));

            return sortValue === 'most_recent' ? dateB - dateA : dateA - dateB;
        });

        list.innerHTML = '';
        items.forEach(item => list.appendChild(item));
    }

    select.addEventListener('change', sortVacantes);
    sortVacantes();
});

document.addEventListener("DOMContentLoaded", function () {
    const list = document.getElementById('postulacionesList');
    const select = document.getElementById('sortFilterPostulaciones');

    if (!list || !select) return;

    function sortPostulaciones() {
        const sortValue = select.value;
        const items = Array.from(list.querySelectorAll('.vacancy-item'));

        if (items.length === 0) return;

        items.sort((a, b) => {
            const dateTextA = a.querySelector('.vacancy-dates span').textContent;
            const dateTextB = b.querySelector('.vacancy-dates span').textContent;
            
            const dateA = new Date(dateTextA);
            const dateB = new Date(dateTextB);

            return sortValue === 'most_recent' ? dateB - dateA : dateA - dateB;
        });

        list.innerHTML = '';
        items.forEach(item => list.appendChild(item));
    }

    select.addEventListener('change', sortPostulaciones);
    sortPostulaciones();
});

function filterVacantes() {
    const input = document.getElementById('searchInputVacantes');
    const filterText = input.value.toUpperCase();
    const list = document.getElementById('vacantesList');
    if (!list) return;
    const items = list.querySelectorAll('.vacancy-item');

    items.forEach(item => {
        const title = item.querySelector('.vacante-titulo').textContent.toUpperCase();
        item.style.display = (title.indexOf(filterText) > -1) ? '' : 'none';
    });
}

function filterPostulantes() {
    const input = document.getElementById('searchInputPostulantes');
    const filterText = input ? input.value.toUpperCase() : '';
    const filterStatus = document.getElementById('statusFilter').value;
    const list = document.getElementById('postulacionesList');

    if (!list) return;
    const items = list.querySelectorAll('.vacancy-item');

    items.forEach(item => {
        const nameCell = item.querySelector('.vacancy-type span');
        const statusCell = item.querySelector('.vacancy-status span');

        let nameMatch = true, statusMatch = true;

        if (nameCell) nameMatch = nameCell.textContent.toUpperCase().indexOf(filterText) > -1;

        if (statusCell) {
            const status = statusCell.textContent.trim();
            statusMatch = filterStatus === 'all' || status === filterStatus;
        }

        item.style.display = (nameMatch && statusMatch) ? '' : 'none';
    });
}