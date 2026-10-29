document.addEventListener('DOMContentLoaded', () => {

    const buscador = document.getElementById("buscador");
    const historialDiv = document.getElementById("historial");
    const form = document.getElementById("filtroForm");

    const filtroTipoContrato = document.getElementById("filtroTipoContrato");
    const filtroFecha = document.getElementById("filtroFecha");
    const salarioMin = document.getElementById("salarioMin");
    const salarioMax = document.getElementById("salarioMax");
    const minDisplay = document.getElementById('salarioMinDisplay');
    const maxDisplay = document.getElementById('salarioMaxDisplay');


    if (form) {
        form.addEventListener("submit", async (e) => {
            e.preventDefault();
            const texto = buscador?.value.trim();
            if (texto) {
                await fetch("/postulante/historial/guardar", {
                    method: "POST",
                    headers: { "Content-Type": "application/x-www-form-urlencoded" },
                    body: `texto=${encodeURIComponent(texto)}`
                });
            }
            form.submit();
        });
    }


    // Combina los filtros sobre las tarjetas cargadas, sin consultar de nuevo al servidor.
    function filtrarVacantes() {
        const texto = buscador?.value.toLowerCase().trim() || "";
        const modalidad = document.querySelector('.filter-card input[type="radio"]:checked')?.value.toLowerCase() || "";
        const tipoContrato = filtroTipoContrato?.value.toLowerCase() || "";
        const fechaFiltro = filtroFecha?.value || "";
        const salarioMinVal = salarioMin?.value ? parseFloat(salarioMin.value) : null;
        const salarioMaxVal = salarioMax?.value ? parseFloat(salarioMax.value) : null;

        const vacantes = document.querySelectorAll(".vacancies__card");
        const noVacantesMessage = document.getElementById("no-vacantes-message");
        const ahora = new Date();
        let encontradas = 0;

        vacantes.forEach(card => {
            const titulo = (card.dataset.titulo || '').toLowerCase();
            const mod = (card.dataset.modalidad || '').toLowerCase();
            const empresa = (card.dataset.empresa || '').toLowerCase();
            const contrato = (card.dataset.tipoContrato || '').toLowerCase();
            const salario = parseFloat(card.dataset.salario || '0');
            const fechaPublicacion = card.dataset.fecha ? new Date(card.dataset.fecha) : null;

            const coincideTexto =
                !texto ||
                titulo.includes(texto) ||
                empresa.includes(texto);

            const coincideModalidad = modalidad === "" || mod === modalidad;

            const coincideContrato = tipoContrato === "" || contrato === tipoContrato;

            let coincideSalario = true;
            if (salarioMinVal !== null && !isNaN(salario)) {
                coincideSalario = salario >= salarioMinVal;
            }
            if (salarioMaxVal !== null && !isNaN(salario)) {
                coincideSalario = coincideSalario && salario <= salarioMaxVal;
            }

            let coincideFecha = true;
            if (fechaFiltro && fechaPublicacion) {
                const diffDias = (ahora - fechaPublicacion) / (1000 * 60 * 60 * 24);
                if (fechaFiltro === "hoy") coincideFecha = diffDias < 1;
                else if (fechaFiltro === "3dias") coincideFecha = diffDias <= 3;
                else if (fechaFiltro === "semana") coincideFecha = diffDias <= 7;
                else if (fechaFiltro === "mes") coincideFecha = diffDias <= 30;
            }

            const visible = coincideTexto && coincideModalidad && coincideContrato && coincideSalario && coincideFecha;
            card.style.display = visible ? "block" : "none";
            if (visible) encontradas++;
        });

        if (noVacantesMessage) {
            noVacantesMessage.style.display = encontradas === 0 ? 'block' : 'none';
        }
    }


    document.querySelectorAll(".filter-chip").forEach(chip => {
        chip.addEventListener("click", () => {
            const field = chip.dataset.field;
            document.querySelectorAll(`.filter-chip[data-field="${field}"]`).forEach(c => {
                c.classList.remove("filter-chip--active");
            });
            chip.classList.add("filter-chip--active");

            const fieldMap = {
                tipoContrato: filtroTipoContrato,
                fechaFiltro: filtroFecha
            };

            if (fieldMap[field]) fieldMap[field].value = chip.dataset.value;
            filtrarVacantes();
        });
    });


    document.querySelectorAll('.filter-card input[type="radio"]').forEach(radio => {
        radio.addEventListener("change", () => {
            document.querySelectorAll(".filter-card").forEach(c => c.classList.remove("filter-card--active"));
            radio.closest(".filter-card").classList.add("filter-card--active");
            filtrarVacantes();
        });
    });


    function formatCOP(val) {
        return '$' + parseInt(val).toLocaleString('es-CO');
    }

    function syncSliders() {
        if (!salarioMin || !salarioMax) return;
        const gap = 500000;
        if (parseInt(salarioMin.value) >= parseInt(salarioMax.value) - gap) {
            salarioMin.value = parseInt(salarioMax.value) - gap;
        }
        if (minDisplay) minDisplay.textContent = formatCOP(salarioMin.value);
        if (maxDisplay) maxDisplay.textContent = formatCOP(salarioMax.value);
    }

    salarioMin?.addEventListener('input', () => { syncSliders(); filtrarVacantes(); });
    salarioMax?.addEventListener('input', () => { syncSliders(); filtrarVacantes(); });


    const canvas = document.getElementById('histogramCanvas');
    if (canvas) {
        const ctx = canvas.getContext('2d');
        const bars = [2, 4, 7, 12, 18, 24, 30, 28, 22, 16, 11, 8, 6, 4, 3, 2, 2, 1, 1, 1];
        const maxBar = Math.max(...bars);

        function drawHistogram() {
            const W = canvas.width;
            const H = canvas.height;
            const barW = W / bars.length;
            const minVal = parseInt(salarioMin?.value || 0);
            const maxVal = parseInt(salarioMax?.value || 20000000);
            const range = 20000000;
            ctx.clearRect(0, 0, W, H);
            bars.forEach((val, i) => {
                const barStart = (i / bars.length) * range;
                const barEnd = ((i + 1) / bars.length) * range;
                ctx.fillStyle = (barEnd > minVal && barStart < maxVal) ? '#9400D3' : '#ddd';
                const bh = (val / maxBar) * H;
                ctx.beginPath();
                ctx.roundRect(i * barW + 1, H - bh, barW - 2, bh, 2);
                ctx.fill();
            });
        }

        drawHistogram();
        salarioMin?.addEventListener('input', drawHistogram);
        salarioMax?.addEventListener('input', drawHistogram);
    }


    document.getElementById('btnLimpiarFiltros')?.addEventListener('click', () => {
        document.querySelectorAll('.filter-chip').forEach(c => c.classList.remove('filter-chip--active'));
        document.querySelectorAll('.filter-chip[data-value=""]').forEach(c => c.classList.add('filter-chip--active'));

        document.querySelectorAll('.filter-card').forEach(c => c.classList.remove('filter-card--active'));
        const primeraCard = document.querySelector('.filter-card input[value=""]');
        if (primeraCard) {
            primeraCard.checked = true;
            primeraCard.closest('.filter-card').classList.add('filter-card--active');
        }

        if (filtroTipoContrato) filtroTipoContrato.value = '';
        if (filtroFecha) filtroFecha.value = '';
        if (salarioMin) salarioMin.value = 0;
        if (salarioMax) salarioMax.value = 20000000;

        syncSliders();
        salarioMin?.dispatchEvent(new Event('input'));
        filtrarVacantes();
    });


    const panel = document.getElementById('filterPanel');
    const overlay = document.getElementById('filterOverlay');
    const openBtn = document.getElementById('openFilterPanel');
    const closeBtn = document.getElementById('closeFilterPanel');

    const closePanel = () => {
        panel?.classList.remove('filter-panel--open');
        overlay?.classList.remove('filter-overlay--visible');
        document.body.style.overflow = '';
    };

    openBtn?.addEventListener('click', () => {
        panel?.classList.add('filter-panel--open');
        overlay?.classList.add('filter-overlay--visible');
        document.body.style.overflow = 'hidden';
    });

    closeBtn?.addEventListener('click', closePanel);
    overlay?.addEventListener('click', closePanel);


    buscador?.addEventListener("input", async () => {
        const texto = buscador.value.trim();
        filtrarVacantes();

        if (texto.length < 2) {
            mostrarHistorial();
            return;
        }

        try {
            const [sugerenciasRes, historialRes] = await Promise.all([
                fetch(`/postulante/vacantes/sugerencias?texto=${texto}`),
                fetch(`/postulante/historial`)
            ]);
            const sugerencias = await sugerenciasRes.json();
            const historial = await historialRes.json();
            const historialFiltrado = historial.filter(h => h.toLowerCase().includes(texto.toLowerCase()));
            const combinado = [...new Set([...historialFiltrado, ...sugerencias])];
            mostrarSugerencias(combinado);
        } catch (error) {
            console.error(error);
        }
    });

    function mostrarSugerencias(lista) {
        const listaEl = document.getElementById("lista-historial");
        const dropdown = document.getElementById("historial");
        listaEl.innerHTML = "";

        if (!lista || lista.length === 0) {
            dropdown.style.display = "none";
            return;
        }

        dropdown.style.display = "block";
        lista.forEach(item => {
            const li = document.createElement("li");
            li.textContent = item;
            li.addEventListener("click", () => {
                buscador.value = item;
                dropdown.style.display = "none";
                filtrarVacantes();
            });
            listaEl.appendChild(li);
        });
    }

    async function mostrarHistorial() {
        try {
            const res = await fetch(`/postulante/historial`);
            const data = await res.json();
            mostrarSugerencias(data);
        } catch (error) {
            console.error("Error historial:", error);
        }
    }

    buscador?.addEventListener("focus", () => mostrarHistorial());

    document.getElementById("limpiar-historial")?.addEventListener("click", async () => {
        await fetch("/postulante/historial/limpiar", { method: "POST" });
        document.getElementById("lista-historial").innerHTML = "";
        document.getElementById("historial").style.display = "none";
    });

    document.addEventListener("click", (e) => {
        if (!e.target.closest(".search-main")) {
            if (historialDiv) historialDiv.style.display = "none";
        }
    });

    syncSliders();
    filtrarVacantes();
});