document.addEventListener("DOMContentLoaded", function () {
    const list = document.getElementById('empresasList'); 
    const select = document.getElementById('sortFilterEmpresas');

    if (!list || !select) return;

    function sortEmpresas() {
        const sortValue = select.value;
        const items = Array.from(list.querySelectorAll('.empresa-item'));

        items.sort((a, b) => {
            const dateA = new Date(a.getAttribute('data-date'));
            const dateB = new Date(b.getAttribute('data-date'));

            return sortValue === 'most_recent' ? dateB - dateA : dateA - dateB;
        });

        list.innerHTML = '';
        items.forEach(item => list.appendChild(item));
    }

    select.addEventListener('change', sortEmpresas);

    sortEmpresas();
});


function filterEmpresas() {
    const input = document.getElementById('searchInputEmpresas');
    const filterText = input.value.toUpperCase();
    const list = document.getElementById('empresasList');
    if (!list) return;
    const items = list.querySelectorAll('.empresa-item');

    items.forEach(item => {
        const title = item.querySelector('.empresa-nombre').textContent.toUpperCase();
        item.style.display = (title.indexOf(filterText) > -1) ? '' : 'none';
    });
}