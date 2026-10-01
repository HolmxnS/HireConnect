document.addEventListener("DOMContentLoaded", () => {
    const notifToggle = document.getElementById("notifToggle");
    const notifPanel  = document.getElementById("notifPanel");
    const badge       = document.querySelector(".notification__badge");

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
            setTimeout(() => item.remove(), 300);
            actualizarBadge();
        });
    });

    document.getElementById("markAllRead").addEventListener("click", async () => {
        await fetch("/admin/notificaciones/leida/todas", { method: "POST" });

        document.querySelectorAll(".notification__item").forEach(item => {
            item.classList.remove("unread");
            item.classList.add("read");
        });
        actualizarBadge();
    });

    document.getElementById("deleteAll").addEventListener("click", async () => {
        await fetch("/admin/notificaciones/eliminar/todas", { method: "POST" });

        document.querySelectorAll(".notification__item").forEach(item => {
            item.style.opacity = "0";
            item.style.transition = "opacity .3s";
            setTimeout(() => item.remove(), 300);
        });
        actualizarBadge();
    });

    function actualizarBadge() {
        const unreadCount = document.querySelectorAll(".notification__item.unread").length;
        if (!badge) return;

        if (unreadCount > 0) {
            badge.textContent = unreadCount;
            badge.style.display = "flex";
        } else {
            badge.style.display = "none";
        }
    }

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

    const lineCanvas = document.getElementById("lineChart");
    let lineChart = null;

    if (lineCanvas) {
        lineChart = new Chart(lineCanvas.getContext("2d"), {
            type: "line",
            data: {
                labels,
                datasets: [{
                    label: "Usuarios",
                    data: dsTodos,
                    borderColor: "#9400D3",
                    backgroundColor: "rgba(148,0,211,0.08)",
                    borderWidth: 2.5,
                    pointBackgroundColor: "#9400D3",
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
                        ticks: { font: { size: 11 }, color: "#6b7280" }
                    },
                    y: {
                        grid: { color: "rgba(0,0,0,0.05)" },
                        ticks: { font: { size: 11 }, color: "#6b7280", stepSize: 1 },
                        beginAtZero: true
                    }
                }
            }
        });

        /* ── Tabs de filtro de la gráfica de línea ── */
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

    const barCanvas = document.getElementById("barChart");

    if (barCanvas) {
        new Chart(barCanvas.getContext("2d"), {
            type: "bar",
            data: {
                labels,
                datasets: [
                    {
                        label: "Empresas",
                        data: dsEmpresas,
                        backgroundColor: "rgba(148,0,211,0.75)",
                        borderRadius: 6,
                        barPercentage: 0.5
                    },
                    {
                        label: "Postulantes",
                        data: dsPostulantes,
                        backgroundColor: "rgba(5,150,105,0.65)",
                        borderRadius: 6,
                        barPercentage: 0.5
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
                            font: { size: 11 },
                            color: "#6b7280",
                            boxWidth: 10,
                            boxHeight: 10
                        }
                    }
                },
                scales: {
                    x: {
                        grid: { display: false },
                        ticks: { font: { size: 10 }, color: "#6b7280" }
                    },
                    y: {
                        grid: { color: "rgba(0,0,0,0.05)" },
                        ticks: { font: { size: 10 }, color: "#6b7280", stepSize: 1 },
                        beginAtZero: true
                    }
                }
            }
        });
    }

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

            scrollListIfNeeded();
        });
    });

    /* ── Scroll automático si hay más de 10 registros ── */
    function scrollListIfNeeded() {
        const list = document.getElementById("registrosMesList");
        if (!list) return;

        const visibleItems = [...list.querySelectorAll(".user-item")]
            .filter(item => item.style.display !== "none");

        if (visibleItems.length > 10) {
            requestAnimationFrame(() => {
                list.scrollTo({ top: list.scrollHeight, behavior: "smooth" });
            });
        }
    }

    // Ejecutar al cargar la página
    scrollListIfNeeded();

});