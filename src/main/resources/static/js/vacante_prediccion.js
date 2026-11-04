(() => {
  const content = document.querySelector('[data-vacancy-content]');
  if (!content) return;

  const cache = new Map();

  const BADGES = {
    Alta:  { cls: 'badge-alta',  icon: 'fa-circle-check',         text: 'Probabilidad alta' },
    Media: { cls: 'badge-media', icon: 'fa-triangle-exclamation', text: 'Probabilidad media' },
    Baja:  { cls: 'badge-baja',  icon: 'fa-circle-xmark',         text: 'Probabilidad baja' },
  };

  const LEVELS = {
    alta: 'Alta', alto: 'Alta',
    media: 'Media', medio: 'Media',
    baja: 'Baja', bajo: 'Baja',
  };

  function showUnavailable(card) {
    const valueEl = card.querySelector('[data-prediction-value]');
    if (valueEl) valueEl.textContent = 'No disponible por ahora';
    const badge = card.querySelector('[data-prediction-badge]');
    if (badge) badge.hidden = true;
    if (!card.querySelector('[data-prediction-retry]')) {
      const button = document.createElement('button');
      button.type = 'button';
      button.dataset.predictionRetry = 'true';
      button.textContent = 'Reintentar';
      card.append(button);
    }
    card.hidden = false;
  }

  function render(card, data) {
    const level = LEVELS[String(data.compatibilidad || '').trim().toLowerCase()];
    let p = parseFloat(String(data.probabilidad).replace('%', '').replace(',', '.'));

    if (!Number.isFinite(p) || p < 0 || p > 100 || !level) {
      showUnavailable(card);
      return;
    }

    // Contrato v2: Python siempre devuelve porcentajes entre 0 y 100.

    card.querySelector('[data-prediction-value]').textContent = `${p.toFixed(1)}%`;

    const b = BADGES[level];
    const badge = card.querySelector('[data-prediction-badge]');
    badge.className = `badge ${b.cls}`;
    badge.innerHTML = `<i class="fa-solid ${b.icon}" aria-hidden="true"></i> ${b.text}`;
    badge.hidden = false;
    let note = card.querySelector('[data-prediction-note]');
    if (!note) {
      note = document.createElement('small');
      note.dataset.predictionNote = 'true';
      card.append(note);
    }
    note.textContent = data.coberturaLimitada
      ? 'Estimación experimental: hay pocos ejemplos similares a tu perfil.'
      : 'Estimación experimental basada en las postulaciones registradas.';
    card.querySelector('[data-prediction-retry]')?.remove();
    card.hidden = false;
  }

  async function load(card) {
    const url = card.dataset.url;
    try {
      if (!cache.has(url)) {
        const controller = new AbortController();
        const timeout = setTimeout(() => controller.abort(), 8000);
        cache.set(
          url,
          fetch(url, { headers: { Accept: 'application/json' }, signal: controller.signal })
            .then(r => (r.ok ? r.json() : Promise.reject(r.status)))
            .then(data => {
              if (data.disponible === false || !Number.isFinite(data.probabilidad)) {
                throw new Error('Predicción no disponible');
              }
              return data;
            })
            .finally(() => clearTimeout(timeout))
        );
      }
      const data = await cache.get(url);
      if (card.isConnected) render(card, data);
    } catch (e) {
      cache.delete(url);
      if (card.isConnected) showUnavailable(card);
    }
  }

  function scan() {
    content.querySelectorAll('[data-prediction]:not([data-loaded])').forEach(card => {
      card.dataset.loaded = 'true';
      load(card);
    });
  }

  content.addEventListener('click', event => {
    const retry = event.target.closest('[data-prediction-retry]');
    if (!retry) return;
    const card = retry.closest('[data-prediction]');
    retry.remove();
    card.querySelector('[data-prediction-value]').textContent = 'Calculando…';
    cache.delete(card.dataset.url);
    load(card);
  });

  new MutationObserver(scan).observe(content, { childList: true, subtree: true });
  // La primera vacante puede estar seleccionada antes de cargar este script.
  scan();
})();
