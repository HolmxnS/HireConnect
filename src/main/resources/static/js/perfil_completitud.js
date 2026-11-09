document.addEventListener('DOMContentLoaded', () => {
  const card = document.querySelector('[data-profile-completeness]');
  if (!card) return;
  // La foto se guarda sin recargar la página; el resto de formularios vuelve al perfil.
  document.addEventListener('profile-photo-saved', event => {
    const photo = card.querySelector('[data-profile-part="foto"]');
    const complete = Boolean(event.detail.foto);
    photo.dataset.complete = String(complete);
    photo.querySelector('.profile-completeness__check').textContent = complete ? '✓' : '○';
    photo.querySelector('.profile-completeness__state').textContent = complete ? 'Completo' : 'Pendiente';
    const parts = [...card.querySelectorAll('[data-profile-part]')];
    const count = parts.filter(part => part.dataset.complete === 'true').length;
    const percentage = Math.round(count / parts.length * 100);
    const ring = card.querySelector('[role="progressbar"]');
    ring.style.setProperty('--profile-progress', `${percentage}%`);
    ring.setAttribute('aria-valuenow', percentage);
    card.querySelector('[data-profile-percentage]').textContent = `${percentage}%`;
    card.querySelector('[data-profile-completed-count]').textContent = `${count} de ${parts.length} apartados completos`;
  });
});
