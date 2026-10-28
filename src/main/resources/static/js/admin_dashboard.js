document.addEventListener("DOMContentLoaded", () => {

    const notifToggle = document.getElementById("notifToggle");
    const notifPanel  = document.getElementById("notifPanel");

    if (notifToggle && notifPanel) {

        notifToggle.addEventListener("click", () => {
            notifPanel.classList.toggle("hidden");
        });

        document.addEventListener("click", (e) => {
            if (!notifPanel.contains(e.target) && !notifToggle.contains(e.target)) {
                notifPanel.classList.add("hidden");
            }
        });

        document.querySelectorAll(".mark-one").forEach(btn => {
            btn.addEventListener("click", async () => {
                const item = btn.closest(".notification__item");
                const id   = item.dataset.id;

                await fetch(`/admin/notificaciones/leida/${id}`, { method: "POST" });

                item.classList.remove("unread");
                item.classList.add("read");
                actualizarBadge();
            });
        });

        document.querySelectorAll(".delete-one").forEach(btn => {
            btn.addEventListener("click", async () => {
                const item = btn.closest(".notification__item");
                const id   = item.dataset.id;

                await fetch(`/admin/notificaciones/eliminar/${id}`, { method: "POST" });

                item.style.opacity = "0";
                item.style.transition = "opacity .3s";
                setTimeout(() => { item.remove(); actualizarBadge(); }, 300);
            });
        });

        const markAll = document.getElementById("markAllRead");
        if (markAll) {
            markAll.addEventListener("click", async (e) => {
                e.preventDefault();
                await fetch("/admin/notificaciones/leida/todas", { method: "POST" });

                document.querySelectorAll(".notification__item").forEach(item => {
                    item.classList.remove("unread");
                    item.classList.add("read");
                });
                actualizarBadge();
            });
        }

        const delAll = document.getElementById("deleteAll");
        if (delAll) {
            delAll.addEventListener("click", async (e) => {
                e.preventDefault();
                await fetch("/admin/notificaciones/eliminar/todas", { method: "POST" });

                document.querySelectorAll(".notification__item").forEach(item => {
                    item.style.opacity = "0";
                    item.style.transition = "opacity .3s";
                    setTimeout(() => item.remove(), 300);
                });
                setTimeout(actualizarBadge, 320);
            });
        }
    }

    function actualizarBadge() {
        const badge = document.querySelector(".notification__badge");
        if (!badge) return;

        const unreadCount = document.querySelectorAll(".notification__item.unread").length;

        if (unreadCount > 0) {
            badge.textContent = unreadCount;
            badge.style.display = "flex";
        } else {
            badge.style.display = "none";
        }
    }

    /* ── Colores de las gráficas tomados del tema (claro / oscuro) ── */
    function cssVar(name, fallback) {
        const v = getComputedStyle(document.documentElement).getPropertyValue(name).trim();
        return v || fallback;
    }

    function hexToRgba(hex, alpha) {
        const h = hex.replace("#", "");
        if (h.length !== 6) return hex;
        const n = parseInt(h, 16);
        return `rgba(${(n >> 16) & 255}, ${(n >> 8) & 255}, ${n & 255}, ${alpha})`;
    }

    function palette() {
        return {
            primary: cssVar("--morado-c", "#8e22bb"),
            ok:      cssVar("--pc-ok", "#1f8f62"),
            text:    cssVar("--gris-o", "#51465a"),
            grid:    cssVar("--pc-line", "#ece4f1")
        };
    }

    /* ── Datos ── */
    const meses        = ["Ene","Feb","Mar","Abr","May","Jun","Jul","Ago","Sep","Oct","Nov","Dic"];
    const currentMonth = new Date().getMonth();
    const labels       = meses.slice(0, currentMonth + 1);
    const rawData      = window.registrosPorMesData || {};

    function buildDataset(tipo) {
        if (rawData[tipo] && Array.isArray(rawData[tipo])) {
            return rawData[tipo].slice(0, currentMonth + 1);
        }
        return new Array(currentMonth + 1).fill(0);
    }

    const dsEmpresas    = buildDataset("empresa");
    const dsPostulantes = buildDataset("postulante");
    const dsTodos       = dsEmpresas.map((v, i) => v + (dsPostulantes[i] || 0));

    let lineChart = null;
    let barChart  = null;

    /* ── Gráfica de línea ── */
    const lineCanvas = document.getElementById("lineChart");

    if (lineCanvas && typeof Chart !== "undefined") {
        const c = palette();

        lineChart = new Chart(lineCanvas.getContext("2d"), {
            type: "line",
            data: {
                labels,
                datasets: [{
                    label: "Usuarios",
                    data: dsTodos,
                    borderColor: c.primary,
                    backgroundColor: hexToRgba(c.primary, 0.12),
                    borderWidth: 2.5,
                    pointBackgroundColor: c.primary,
                    pointRadius: 4,
                    pointHoverRadius: 6,
                    tension: 0.4,
                    fill: true
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                plugins: { legend: { display: false } },
                scales: {
                    x: {
                        grid: { display: false },
                        ticks: { font: { size: 12 }, color: c.text }
                    },
                    y: {
                        grid: { color: c.grid },
                        ticks: { font: { size: 12 }, color: c.text, stepSize: 1 },
                        beginAtZero: true
                    }
                }
            }
        });

        /* Tabs de filtro de la gráfica de línea */
        document.querySelectorAll(".chart-tabs__btn").forEach(btn => {
            btn.addEventListener("click", () => {
                document.querySelectorAll(".chart-tabs__btn").forEach(b => b.classList.remove("active"));
                btn.classList.add("active");

                const dataMap = {
                    empresa:    dsEmpresas,
                    postulante: dsPostulantes,
                    todos:      dsTodos
                };

                lineChart.data.datasets[0].data = dataMap[btn.dataset.filter] || dsTodos;
                lineChart.update();
            });
        });
    }

    /* ── Gráfica de barras ── */
    const barCanvas = document.getElementById("barChart");

    if (barCanvas && typeof Chart !== "undefined") {
        const c = palette();

        barChart = new Chart(barCanvas.getContext("2d"), {
            type: "bar",
            data: {
                labels,
                datasets: [
                    {
                        label: "Empresas",
                        data: dsEmpresas,
                        backgroundColor: hexToRgba(c.primary, 0.85),
                        borderRadius: 8,
                        barPercentage: 0.6
                    },
                    {
                        label: "Postulantes",
                        data: dsPostulantes,
                        backgroundColor: hexToRgba(c.ok, 0.8),
                        borderRadius: 8,
                        barPercentage: 0.6
                    }
                ]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                plugins: {
                    legend: {
                        display: true,
                        position: "top",
                        labels: {
                            font: { size: 12 },
                            color: c.text,
                            boxWidth: 10,
                            boxHeight: 10,
                            usePointStyle: true
                        }
                    }
                },
                scales: {
                    x: {
                        grid: { display: false },
                        ticks: { font: { size: 11 }, color: c.text }
                    },
                    y: {
                        grid: { color: c.grid },
                        ticks: { font: { size: 11 }, color: c.text, stepSize: 1 },
                        beginAtZero: true
                    }
                }
            }
        });
    }

    /* ── Al cambiar entre modo claro / oscuro, repintar las gráficas ── */
    function repintarGraficas() {
        const c = palette();

        if (lineChart) {
            const ds = lineChart.data.datasets[0];
            ds.borderColor = c.primary;
            ds.pointBackgroundColor = c.primary;
            ds.backgroundColor = hexToRgba(c.primary, 0.12);
            lineChart.options.scales.x.ticks.color = c.text;
            lineChart.options.scales.y.ticks.color = c.text;
            lineChart.options.scales.y.grid.color  = c.grid;
            lineChart.update();
        }

        if (barChart) {
            barChart.data.datasets[0].backgroundColor = hexToRgba(c.primary, 0.85);
            barChart.data.datasets[1].backgroundColor = hexToRgba(c.ok, 0.8);
            barChart.options.plugins.legend.labels.color = c.text;
            barChart.options.scales.x.ticks.color = c.text;
            barChart.options.scales.y.ticks.color = c.text;
            barChart.options.scales.y.grid.color  = c.grid;
            barChart.update();
        }
    }

    document.addEventListener("click", (e) => {
        if (e.target.closest && e.target.closest("[data-theme-toggle]")) {
            // theme.js cambia el atributo en el mismo evento; esperamos un instante
            setTimeout(repintarGraficas, 50);
        }
    });

    /* ── Tabs de filtro de la lista de registros ── */
    document.querySelectorAll(".filter-tabs__btn").forEach(btn => {
        btn.addEventListener("click", () => {
            document.querySelectorAll(".filter-tabs__btn").forEach(b => b.classList.remove("active"));
            btn.classList.add("active");

            const tab = btn.dataset.tab;

            document.querySelectorAll("#registrosMesList .user-item").forEach(item => {
                const tipo    = (item.dataset.tipo || "").toLowerCase();
                const visible = tab === "todos" || tipo.includes(tab);
                item.style.display = visible ? "flex" : "none";
            });
        });
    });
});