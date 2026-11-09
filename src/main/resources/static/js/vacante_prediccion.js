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
    card.dataset.state = 'unavailable';
    card.querySelector('[data-prediction-meter]').hidden = true;
    card.querySelector('[data-prediction-note]').hidden = true;
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

  function renderOverlap(card, data) {
    const hasScore = data.tipoResultado === 'coincidencia_habilidades';
    const score = Number(data.coincidenciaHabilidades);
    if (hasScore && (!Number.isFinite(score) || score < 0 || score > 100)) {
      showUnavailable(card); return;
    }
    card.dataset.state = hasScore ? 'ready' : 'insufficient';
    card.querySelector('.prediction-header h3').textContent = hasScore ? 'Coincidencia de habilidades' : 'Probabilidad de aceptación';
    card.querySelector('[data-prediction-value]').textContent = hasScore ? `${score.toFixed(1)}%` : 'Faltan datos para estimar';
    card.querySelector('.percentage > span').textContent = hasScore
      ? `${data.habilidadesCoincidentes} de ${data.habilidadesRequeridasTotal} habilidades requeridas coinciden` : '';
    const meter = card.querySelector('[data-prediction-meter]');
    meter.hidden = !hasScore;
    if (hasScore) meter.style.setProperty('--prediction-progress', `${score}%`);
    const badge = card.querySelector('[data-prediction-badge]');
    badge.hidden = !hasScore;
    if (hasScore) {
      const level = score >= 70 ? 'Alta' : score >= 45 ? 'Media' : 'Baja';
      badge.className = `badge ${BADGES[level].cls}`;
      badge.textContent = `Coincidencia ${level.toLowerCase()}`;
    }
    const note = card.querySelector('[data-prediction-note]');
    note.hidden = false;
    note.textContent = hasScore
      ? 'Compara las habilidades guardadas con las pedidas en esta vacante. No es una probabilidad de aceptación: el historial disponible no permite estimarla para este caso.'
      : 'El historial no permite estimar la aceptación y esta vacante no especifica habilidades para comparar.';
    card.querySelector('[data-prediction-retry]')?.remove();
    card.hidden = false;
  }

  function render(card, data) {
    if (['coincidencia_habilidades', 'datos_insuficientes'].includes(data.tipoResultado)) {
      renderOverlap(card, data); return;
    }
    const simulated = data.synthetic === true && data.tipoResultado === 'simulacion';
    card.querySelector('.prediction-header h3').textContent = simulated ? 'Estimación de aceptación' : 'Probabilidad de aceptación';
    card.querySelector('.percentage > span').textContent = 'según tu perfil y esta vacante';
    const level = LEVELS[String(data.compatibilidad || '').trim().toLowerCase()];
    let p = parseFloat(String(data.probabilidad).replace('%', '').replace(',', '.'));

    if (!Number.isFinite(p) || p < 0 || p > 100 || !level) {
      showUnavailable(card);
      return;
    }

    // Contrato v2: Python siempre devuelve porcentajes entre 0 y 100.

    card.querySelector('[data-prediction-value]').textContent = `${p.toFixed(1)}%`;
    card.dataset.state = 'ready';
    const meter = card.querySelector('[data-prediction-meter]');
    meter.style.setProperty('--prediction-progress', `${p}%`);
    meter.hidden = false;

    const b = BADGES[level];
    const badge = card.querySelector('[data-prediction-badge]');
    badge.className = `badge ${b.cls}`;
    badge.innerHTML = `<i class="fa-solid ${b.icon}" aria-hidden="true"></i> ${b.text}`;
    badge.hidden = false;
    let note = card.querySelector('[data-prediction-note]');
    if (!note) {
      note = document.createElement('p');
      note.className = 'prediction-note';
      note.dataset.predictionNote = 'true';
      card.append(note);
    }
    note.textContent = simulated
      ? ''
      : data.coberturaLimitada
      ? 'Hay pocos casos similares a tu perfil. Toma este resultado como orientación; la decisión es de la empresa.'
      : 'Basada en postulaciones anteriores. La decisión final es de la empresa.';
    note.hidden = simulated;
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
              if (!['coincidencia_habilidades', 'datos_insuficientes'].includes(data.tipoResultado)
                  && (data.disponible === false || !Number.isFinite(data.probabilidad))) {
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
    card.dataset.state = 'loading';
    card.querySelector('[data-prediction-value]').textContent = 'Calculando…';
    cache.delete(card.dataset.url);
    load(card);
  });

  new MutationObserver(scan).observe(content, { childList: true, subtree: true });
  // La primera vacante puede estar seleccionada antes de cargar este script.
  scan();
})();
