function filterVacantes() {
    const input = document.getElementById("searchInputVacantes");
    const filter = input.value.toLowerCase();
    const vacantes = document.querySelectorAll(".lista-vacantes .card-vacante");

    vacantes.forEach(vacante => {
        const titulo = vacante.querySelector("h3").textContent.toLowerCase();
        if (titulo.includes(filter)) {
            vacante.style.display = "";
        } else {
            vacante.style.display = "none";
        }
    });
}

function sortVacantes() {
    const select = document.getElementById("sortFilterVacantes");
    const vacantesContainer = document.querySelector(".lista-vacantes");
    const vacantes = Array.from(vacantesContainer.querySelectorAll(".card-vacante"));

    vacantes.sort((a, b) => {
        const fechaA = a.querySelector(".card-header span").textContent.trim();
        const fechaB = b.querySelector(".card-header span").textContent.trim();

        const parseDate = str => {
            const [day, month, year] = str.split("/");
            return new Date(year, month - 1, day);
        };

        const dateA = parseDate(fechaA);
        const dateB = parseDate(fechaB);

        return select.value === "most_recent" ? dateB - dateA : dateA - dateB;
    });

    vacantes.forEach(vacante => vacantesContainer.appendChild(vacante));
}

document.addEventListener("DOMContentLoaded", () => {
    const searchInput = document.getElementById("searchInputVacantes");
    searchInput.addEventListener("keyup", filterVacantes);

    const sortSelect = document.getElementById("sortFilterVacantes");
    sortSelect.addEventListener("change", sortVacantes);
});
