document.addEventListener('DOMContentLoaded', () => {
    const chart = document.querySelector('[data-activity-chart]');
    if (!chart) return;
    const rows = Array.from(document.querySelectorAll('[data-activity-point]'));
    const period = document.getElementById('activity-period');
    const detail = document.querySelector('[data-activity-detail]');
    const ns = 'http://www.w3.org/2000/svg';
    const node = (tag, attrs = {}, text) => {
        const element = document.createElementNS(ns, tag);
        Object.entries(attrs).forEach(([key, value]) => element.setAttribute(key, value));
        if (text !== undefined) element.textContent = text;
        return element;
    };
    const date = value => new Intl.DateTimeFormat('es-CO', { day: 'numeric', month: 'short' }).format(new Date(value + 'T12:00:00'));
    let visible = [];
    function draw() {
        visible = rows.slice(-Number(period.value));
        rows.forEach(row => { row.hidden = !visible.includes(row); });
        const values = visible.map(row => Number(row.dataset.count));
        const total = values.reduce((sum, value) => sum + value, 0);
        document.querySelector('[data-activity-total]').textContent = total;
        document.querySelector('[data-activity-empty]').hidden = total !== 0;
        detail.textContent = '';
        const svg = node('svg', { viewBox: '0 0 640 240', role: 'img', 'aria-label': total + ' postulaciones recibidas en los últimos ' + period.value + ' días. Datos disponibles en la tabla.' });
        const top = 16, bottom = 204, left = 34, right = 620;
        const maximum = Math.max(...values, 1);
        const step = Math.max(1, Math.ceil(maximum / 4));
        const ceiling = step * Math.ceil(maximum / step);
        for (let value = 0; value <= ceiling; value += step) {
            const y = bottom - value / ceiling * (bottom - top);
            svg.append(node('line', { x1: left, x2: right, y1: y, y2: y, class: 'activity-grid' }), node('text', { x: left - 10, y: y + 4, 'text-anchor': 'end' }, value));
        }
        const points = values.map((value, i) => [left + i * (right - left) / Math.max(values.length - 1, 1), bottom - value / ceiling * (bottom - top)]);
        const line = points.map(([x, y], i) => (i ? 'L' : 'M') + x + ',' + y).join(' ');
        const defs = node('defs'), gradient = node('linearGradient', { id: 'activity-fill', x1: '0', y1: '0', x2: '0', y2: '1' });
        gradient.append(node('stop', { offset: '0%', 'stop-color': 'var(--hc-accent)', 'stop-opacity': '.24' }), node('stop', { offset: '100%', 'stop-color': 'var(--hc-accent)', 'stop-opacity': '0' }));
        defs.append(gradient); svg.append(defs, node('path', { d: line + ' L' + right + ',' + bottom + ' L' + left + ',' + bottom + ' Z', fill: 'url(#activity-fill)' }), node('path', { d: line, class: 'activity-line' }));
        points.forEach(([x, y], i) => {
            const label = date(visible[i].dataset.date) + ': ' + values[i] + (values[i] === 1 ? ' postulación' : ' postulaciones');
            const dot = node('circle', { cx: x, cy: y, r: values.length <= 14 ? 3.5 : 2.5, class: 'activity-point' });
            const hit = node('circle', { cx: x, cy: y, r: 10, tabindex: '0', 'aria-label': label, class: 'activity-hit' });
            hit.append(node('title', {}, label));
            hit.addEventListener('pointerenter', () => { detail.textContent = label; });
            hit.addEventListener('focus', () => { detail.textContent = label; });
            hit.addEventListener('click', () => { detail.textContent = label; });
            svg.append(dot, hit);
            if (i === 0 || i === points.length - 1 || i % Math.ceil(points.length / 5) === 0) svg.append(node('text', { x, y: 231, 'text-anchor': i === 0 ? 'start' : i === points.length - 1 ? 'end' : 'middle' }, date(visible[i].dataset.date)));
        });
        chart.replaceChildren(svg);
    }
    period.addEventListener('change', draw);
    document.querySelector('[data-activity-export]').addEventListener('click', () => {
        const csv = '\uFEFFFecha,Postulaciones\r\n' + visible.map(row => row.dataset.date + ',' + Number(row.dataset.count)).join('\r\n');
        const url = URL.createObjectURL(new Blob([csv], { type: 'text/csv;charset=utf-8' }));
        const link = document.createElement('a'); link.href = url; link.download = 'postulaciones-' + period.value + '-dias.csv'; link.click();
        setTimeout(() => URL.revokeObjectURL(url), 1000);
    });
    const colors = ['var(--hc-accent)', '#7e9ee8', 'var(--hc-warn)', 'var(--hc-ok)', 'var(--hc-danger)'];
    const states = Array.from(document.querySelectorAll('.overview-state'));
    const total = states.reduce((sum, item) => sum + Number(item.dataset.count), 0);
    let start = 0;
    if (total) document.querySelector('[data-process-ring]').style.background = 'conic-gradient(' + states.map((item, i) => {
        const end = start + Number(item.dataset.count) / total * 100;
        const segment = colors[i] + ' ' + start + '% ' + end + '%'; start = end; return segment;
    }).join(',') + ')';
    draw();
});
