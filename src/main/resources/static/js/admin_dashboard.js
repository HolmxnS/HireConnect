document.addEventListener('DOMContentLoaded', () => {
    const source = window.adminDashboardData;
    const period = document.getElementById('chartPeriod');
    if (!source || !period) return;
    const months = ['Ene', 'Feb', 'Mar', 'Abr', 'May', 'Jun', 'Jul', 'Ago', 'Sep', 'Oct', 'Nov', 'Dic'];
    const count = value => Math.max(0, Number(value) || 0);
    const states = ['ACTIVA', 'PENDIENTE', 'CERRADA'];
    const stateLabels = ['Activas', 'Pendientes', 'Cerradas'];
    const stateColors = ['var(--hc-accent)', 'var(--hc-faint)', '#735184'];
    const stateCounts = states.map(state => count(source.estados[state]));
    const total = stateCounts.reduce((sum, value) => sum + value, 0);
    const legend = document.getElementById('vacancyLegend');
    stateCounts.forEach((value, index) => {
        const item = document.createElement('li'), label = document.createElement('span'), dot = document.createElement('i');
        dot.style.backgroundColor = stateColors[index]; label.append(dot, stateLabels[index]);
        const number = document.createElement('strong'); number.textContent = value + ' · ' + (total ? (value / total * 100).toFixed(1) : '0') + '%';
        item.append(label, number); legend.append(item);
    });
    document.getElementById('vacancyEmpty').hidden = total > 0;
    document.querySelector('[data-active-percentage]').textContent = (total ? (stateCounts[0] / total * 100).toFixed(0) : '0') + '%';
    if (total) {
        let start = 0;
        document.querySelector('[data-vacancy-ring]').style.background = 'conic-gradient(' + stateCounts.map((value, i) => {
            const end = start + value / total * 100, segment = stateColors[i] + ' ' + start + '% ' + end + '%'; start = end; return segment;
        }).join(',') + ')';
    }
    function rows() {
        const end = Math.min(12, Math.max(1, source.month)), start = Math.max(0, end - Number(period.value));
        return Array.from({ length: end - start }, (_, offset) => {
            const index = start + offset;
            return { month: months[index], date: source.year + '-' + String(index + 1).padStart(2, '0'), empresa: count(source.registros.empresa?.[index]), postulante: count(source.registros.postulante?.[index]) };
        });
    }
    const node = (tag, attrs, text) => {
        const element = document.createElementNS('http://www.w3.org/2000/svg', tag);
        Object.entries(attrs || {}).forEach(([key, value]) => element.setAttribute(key, value));
        if (text !== undefined) element.textContent = text;
        return element;
    };
    function render() {
        const data = rows(), table = document.getElementById('registrationsData'); table.replaceChildren();
        data.forEach(row => { const tr = document.createElement('tr'); [row.month + ' ' + source.year, row.empresa, row.postulante].forEach(value => { const td = document.createElement('td'); td.textContent = value; tr.append(td); }); table.append(tr); });
        document.getElementById('registrationEmpty').hidden = data.some(row => row.empresa || row.postulante);
        const svg = node('svg', { viewBox: '0 0 640 240', role: 'img', 'aria-label': 'Registros mensuales de empresas y postulantes. Valores exactos en la tabla inferior.' });
        const max = Math.max(1, ...data.flatMap(row => [row.empresa, row.postulante])), step = Math.max(1, Math.ceil(max / 4)), ceiling = Math.ceil(max / step) * step;
        const left = 34, top = 12, bottom = 208, width = 590;
        for (let value = 0; value <= ceiling; value += step) {
            const y = bottom - value / ceiling * (bottom - top);
            svg.append(node('line', { x1: left, x2: left + width, y1: y, y2: y, class: 'registration-grid' }), node('text', { x: left - 10, y: y + 4, 'text-anchor': 'end' }, value));
        }
        const group = width / data.length, bar = Math.min(16, group * .28);
        data.forEach((row, i) => {
            const x = left + group * (i + .5);
            ['empresa', 'postulante'].forEach((type, index) => {
                const height = row[type] / ceiling * (bottom - top), label = row.month + ' ' + source.year + ': ' + row[type] + (type === 'empresa' ? ' empresas' : ' postulantes');
                const rect = node('rect', { x: x + (index ? 2 : -bar - 2), y: bottom - height, width: bar, height, rx: 3, class: type === 'empresa' ? 'registration-company' : 'registration-applicant', tabindex: 0, 'aria-label': label });
                rect.append(node('title', {}, label)); svg.append(rect);
            });
            svg.append(node('text', { x, y: 231, 'text-anchor': 'middle' }, row.month));
        });
        document.querySelector('[data-registration-chart]').replaceChildren(svg);
    }
    period.addEventListener('change', render); render();
    document.getElementById('downloadChart').addEventListener('click', () => {
        const csv = ['Mes,Empresas,Postulantes', ...rows().map(row => [row.date, row.empresa, row.postulante].join(','))].join('\r\n');
        const url = URL.createObjectURL(new Blob(['\uFEFF' + csv], { type: 'text/csv;charset=utf-8;' }));
        const link = document.createElement('a'); link.href = url; link.download = 'hireconnect-registros-' + source.year + '.csv'; link.click(); setTimeout(() => URL.revokeObjectURL(url), 1000);
    });
});
