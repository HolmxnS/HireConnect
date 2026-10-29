(() => {
  const content = document.querySelector('[data-vacancy-content]');
  if (!content) return;

  const cache = new Map();
  const BADGES = {
    Alta:  { cls: 'badge-alta',  icon: 'fa-circle-check',          text: 'Compatibilidad Alta' },
    Media: { cls: 'badge-media', icon: 'fa-triangle-exclamation',  text: 'Compatibilidad Media' },
    Baja:  { cls: 'badge-baja',  icon: 'fa-circle-xmark',          text: 'Compatibilidad Baja' },
  };

  function render(card, data) {
    let p = Number(data.probabilidad);
    if (!Number.isFinite(p) || p <= 0 || !BADGES[data.compatibilidad]) return; // sigue oculta
    if (p <= 1) p *= 100; // por si Python devuelve 0.82 en vez de 82
    card.querySelector('[data-prediction-value]').textContent = `${p.toFixed(1)}%`;

    const b = BADGES[data.compatibilidad];
    const badge = card.querySelector('[data-prediction-badge]');
    badge.className = `badge ${b.cls}`;
    badge.innerHTML = `<i class="fa-solid ${b.icon}" aria-hidden="true"></i> ${b.text}`;
    badge.hidden = false;
    card.hidden = false;
  }

  async function load(card) {
    const url = card.dataset.url;
    try {
      if (!cache.has(url)) {
        cache.set(url, fetch(url, { headers: { Accept: 'application/json' } })
          .then(r => (r.ok ? r.json() : Promise.reject(r.status))));
      }
      const data = await cache.get(url);
      if (card.isConnected) render(card, data);
    } catch (e) {
      cache.delete(url);
    }
  }

  new MutationObserver(() => {
    const card = content.querySelector('[data-prediction]:not([data-loaded])');
    if (!card) return;
    card.dataset.loaded = 'true';
    load(card);
  }).observe(content, { childList: true, subtree: true });
})();