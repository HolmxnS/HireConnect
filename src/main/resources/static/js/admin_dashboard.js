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

    const source = window.adminDashboardData || { registros: {}, estados: {}, year: new Date().getFullYear(), month: 1 };
    const months = ['Ene', 'Feb', 'Mar', 'Abr', 'May', 'Jun', 'Jul', 'Ago', 'Sep', 'Oct', 'Nov', 'Dic'];
    const period = document.getElementById('chartPeriod');
    const table = document.getElementById('registrationsData');
    const registrationEmpty = document.getElementById('registrationEmpty');
    const vacancyEmpty = document.getElementById('vacancyEmpty');
    if (!period || !table) return;
    const reducedMotion = () => document.documentElement.hasAttribute('data-reduce-motion') || window.matchMedia('(prefers-reduced-motion: reduce)').matches;
    const colors = () => {
        const style = getComputedStyle(document.documentElement);
        return { text: style.getPropertyValue('--gris-o').trim() || '#51465a', grid: style.getPropertyValue('--pc-line').trim() || '#ece4f1', purple: style.getPropertyValue('--morado-c').trim() || '#8e22bb' };
    };
    const count = value => Math.max(0, Number(value) || 0);
    const states = ['ACTIVA', 'PENDIENTE', 'CERRADA'];
    const stateLabels = ['Activas', 'Pendientes', 'Cerradas'];
    const stateColors = ['#24b88b', '#e9a23b', '#a98aba'];
    const stateCounts = states.map(state => count(source.estados[state]));
    const total = stateCounts.reduce((sum, value) => sum + value, 0);
    const legend = document.getElementById('vacancyLegend');
    stateCounts.forEach((value, index) => {
        const item = document.createElement('li');
        const label = document.createElement('span');
        const dot = document.createElement('i'); dot.style.backgroundColor = stateColors[index];
        label.append(dot, stateLabels[index]);
        const number = document.createElement('strong'); number.textContent = value + ' · ' + (total ? (value / total * 100).toFixed(1) : '0') + '%';
        item.append(label, number); legend.append(item);
    });
    vacancyEmpty.hidden = total > 0;
    document.getElementById('vacancyChart').parentElement.hidden = !total;
    let line, donut;
    if (typeof Chart !== 'undefined') {
        const palette = colors();
        const options = { responsive: true, maintainAspectRatio: false, animation: reducedMotion() ? false : { duration: 450 }, plugins: { legend: { display: false } } };
        line = new Chart(document.getElementById('lineChart'), {
            type: 'line',
            data: { labels: [], datasets: [
                { label: 'Empresas', data: [], borderColor: palette.purple, backgroundColor: palette.purple + '16', fill: true, tension: .25, pointRadius: 2, pointHoverRadius: 5, borderWidth: 2.5 },
                { label: 'Postulantes', data: [], borderColor: '#329ad6', backgroundColor: '#329ad610', fill: true, tension: .25, pointRadius: 2, pointHoverRadius: 5, borderWidth: 2.5 }
            ] },
            options: { ...options, interaction: { mode: 'index', intersect: false }, scales: {
                x: { grid: { display: false }, ticks: { color: palette.text } },
                y: { beginAtZero: true, ticks: { precision: 0, color: palette.text }, grid: { color: palette.grid } }
            } }
        });
        donut = new Chart(document.getElementById('vacancyChart'), {
            type: 'doughnut', data: { labels: stateLabels, datasets: [{ data: stateCounts, backgroundColor: stateColors, borderWidth: 0, hoverOffset: 5, spacing: 4, borderRadius: 7 }] },
            options: { ...options, cutout: '76%', plugins: { legend: { display: false }, tooltip: { callbacks: { label: context => context.label + ': ' + context.raw + ' (' + (total ? (context.raw / total * 100).toFixed(1) : 0) + '%)' } } } }
        });
    } else {
        document.querySelector('[data-registration-chart]').hidden = true;
        document.getElementById('vacancyChart').parentElement.hidden = true;
        document.querySelector('.overview-data').open = true;
    }
    function rows() {
        const end = Math.min(12, Math.max(1, source.month));
        const start = Math.max(0, end - Number(period.value));
        return Array.from({ length: end - start }, (_, offset) => {
            const index = start + offset;
            return { month: months[index], date: source.year + '-' + String(index + 1).padStart(2, '0'), empresa: count(source.registros.empresa?.[index]), postulante: count(source.registros.postulante?.[index]) };
        });
    }
    function render() {
        const data = rows(); table.replaceChildren();
        data.forEach(row => {
            const tr = document.createElement('tr');
            [row.month + ' ' + source.year, row.empresa, row.postulante].forEach(value => { const td = document.createElement('td'); td.textContent = value; tr.append(td); });
            table.append(tr);
        });
        registrationEmpty.hidden = data.some(row => row.empresa || row.postulante);
        if (line) {
            line.data.labels = data.map(row => row.month);
            line.data.datasets[0].data = data.map(row => row.empresa);
            line.data.datasets[1].data = data.map(row => row.postulante);
            line.update();
        }
    }
    period.addEventListener('change', render); render();
    document.getElementById('downloadChart').addEventListener('click', () => {
        const csv = ['Mes,Empresas,Postulantes', ...rows().map(row => [row.date, row.empresa, row.postulante].join(','))].join('\r\n');
        const url = URL.createObjectURL(new Blob(['\uFEFF' + csv], { type: 'text/csv;charset=utf-8;' }));
        const link = document.createElement('a'); link.href = url; link.download = 'hireconnect-registros-' + source.year + '.csv';
        document.body.append(link); link.click(); link.remove(); setTimeout(() => URL.revokeObjectURL(url), 1000);
    });
    new MutationObserver(() => {
        if (!line) return;
        const palette = colors();
        line.data.datasets[0].borderColor = palette.purple;
        line.data.datasets[0].backgroundColor = palette.purple + '16';
        line.options.scales.x.ticks.color = line.options.scales.y.ticks.color = palette.text;
        line.options.scales.y.grid.color = palette.grid;
        line.options.animation = donut.options.animation = reducedMotion() ? false : { duration: 450 };
        line.update('none'); donut.update('none');
    }).observe(document.documentElement, { attributes: true, attributeFilter: ['data-theme', 'data-reduce-motion'] });
});
