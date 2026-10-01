function fechaRelativa(fechaStr) {
    if (!fechaStr) return '';
    
    const fecha = fechaStr.includes('T') ? new Date(fechaStr) : new Date(fechaStr + 'T00:00:00');
    
    const ahora = new Date();
    const diffMs = ahora - fecha;
    const diffMin = Math.floor(diffMs / 60000);
    const diffH = Math.floor(diffMs / 3600000);
    const diffD = Math.floor(diffMs / 86400000);
    const diffSem = Math.floor(diffD / 7);
    const diffMes = Math.floor(diffD / 30);
    const diffAnios = Math.floor(diffMes / 12);

    if (diffMin < 60) return diffMin <= 1 ? 'Hace un momento' : `Hace ${diffMin} minutos`;
    if (diffH < 24) return diffH === 1 ? 'Hace 1 hora' : `Hace ${diffH} horas`;
    if (diffD < 7) return diffD === 1 ? 'Hace 1 día' : `Hace ${diffD} días`;
    if (diffSem < 4) return diffSem === 1 ? 'Hace 1 semana' : `Hace ${diffSem} semanas`;
    if (diffMes < 12) return diffMes === 1 ? 'Hace 1 mes' : `Hace ${diffMes} meses`;
    return diffAnios === 1 ? 'Hace 1 año' : `Hace ${diffAnios} años`;
}

document.querySelectorAll('.vacancies__date').forEach(el => {
    const card = el.closest('.vacancies__card');
    const fecha = card ? card.dataset.fecha : el.dataset.fecha;
    el.textContent = fechaRelativa(fecha);
});