document.addEventListener('DOMContentLoaded', () => {
    const container = document.querySelector('.people-dashboard'); if (!container) return;
    const source = window.adminPeopleData || { total: 0, conCv: 0, registros: [], recientes: [] };
    const total = Number(source.total) || 0, withCv = Number(source.conCv) || 0;
    document.querySelector('[data-people-total]').textContent = total;
    document.querySelector('[data-people-with-cv]').textContent = withCv;
    document.querySelector('[data-people-without-cv]').textContent = total - withCv;
    const percent = total ? Math.round(withCv / total * 100) : 0;
    document.querySelector('[data-people-cv-percent]').textContent = percent + '%';
    document.querySelector('[data-people-cv-summary]').textContent = withCv + ' de ' + total + ' postulantes tienen un CV disponible.';
    document.querySelector('[data-people-cv-ring]').style.background = 'conic-gradient(var(--hc-accent) ' + percent + '%, var(--hc-soft) 0)';
    const recent = document.querySelector('[data-people-recent]');
    (source.recientes || []).forEach(person => {
        const button = document.createElement('button'); button.type = 'button'; button.className = 'people-recent';
        const name = (person.nombre || '') + ' ' + (person.apellido || '');
        const avatar = document.createElement('span'); avatar.className = 'people-avatar';
        if (person.foto) { const img = document.createElement('img'); img.src = person.foto; img.alt = ''; img.loading = 'lazy'; avatar.append(img); }
        else avatar.textContent = (person.nombre || '?').slice(0, 1);
        const identity = document.createElement('span'), title = document.createElement('strong'), date = document.createElement('small'); title.textContent = name;
        date.textContent = person.fecha ? new Intl.DateTimeFormat('es-CO', { day: 'numeric', month: 'short' }).format(new Date(person.fecha + 'T12:00:00')) : 'Sin fecha de registro';
        identity.append(title, date); button.append(avatar, identity);
        button.addEventListener('click', () => {
            const search = document.getElementById('searchInputPostulantes');
            document.getElementById('statusFilter').value = 'all'; document.getElementById('cvFilter').value = 'all';
            search.value = name; search.dispatchEvent(new Event('input')); document.getElementById('peopleSearchForm').requestSubmit(); search.focus({ preventScroll: true }); document.getElementById('people-directory').scrollIntoView({ block: 'start' });
        }); recent.append(button);
    });
    if (!(source.recientes || []).length) { const empty = document.createElement('p'); empty.className = 'people-quiet'; empty.textContent = 'Los nuevos registros aparecerán aquí.'; recent.append(empty); }
    const now = new Date(), year = Number(container.dataset.year) || now.getFullYear(), end = Number(container.dataset.month) || now.getMonth() + 1;
    document.querySelector('[data-people-year]').textContent = year;
    const months = ['Ene','Feb','Mar','Abr','May','Jun','Jul','Ago','Sep','Oct','Nov','Dic'];
    const period = document.querySelector('[data-people-period]');
    const node = (tag, attrs = {}, text) => { const e = document.createElementNS('http://www.w3.org/2000/svg', tag); Object.entries(attrs).forEach(([k,v]) => e.setAttribute(k,v)); if (text !== undefined) e.textContent = text; return e; };
    function render() {
        const start = Math.max(0, end - Number(period.value));
        const values = Array.from({ length: end - start }, (_, i) => Number(source.registros?.[start + i]) || 0);
        const maximum = Math.max(1,...values), step = Math.max(1,Math.ceil(maximum/4)), ceiling = Math.ceil(maximum/step)*step;
        const svg = node('svg', { viewBox:'0 0 640 180',role:'img','aria-label':'Postulantes registrados cada mes. Datos disponibles en la tabla inferior.' });
        const left=32,right=624,top=12,bottom=146;
        for (let v=0;v<=ceiling;v+=step) { const y=bottom-v/ceiling*(bottom-top);svg.append(node('line',{x1:left,x2:right,y1:y,y2:y,class:'people-chart-grid'}),node('text',{x:left-10,y:y+4,'text-anchor':'end'},v)); }
        const points=values.map((v,i)=>[left+i*(right-left)/Math.max(values.length-1,1),bottom-v/ceiling*(bottom-top)]);
        svg.append(node('path',{d:points.map(([x,y],i)=>(i?'L':'M')+x+','+y).join(' '),class:'people-chart-line'}));
        const table=document.querySelector('[data-people-chart-table]');table.replaceChildren();
        points.forEach(([x,y],i)=>{
            const label=months[start+i]+' '+year+': '+values[i]+' registros';const dot=node('circle',{cx:x,cy:y,r:4,tabindex:0,class:'people-chart-dot','aria-label':label});dot.append(node('title',{},label));svg.append(dot,node('text',{x,y:172,'text-anchor':i===0?'start':i===points.length-1?'end':'middle'},months[start+i]));
            const tr=document.createElement('tr');[months[start+i]+' '+year,values[i]].forEach(value=>{const td=document.createElement('td');td.textContent=value;tr.append(td)});table.append(tr);
        });
        document.querySelector('[data-people-chart]').replaceChildren(svg);
        document.querySelector('[data-people-chart-empty]').hidden=values.some(Boolean);
    }
    period.addEventListener('change',render);render();
});
