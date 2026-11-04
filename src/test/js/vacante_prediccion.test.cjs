const { test } = require('node:test');
const assert = require('node:assert/strict');
const { readFileSync } = require('node:fs');
const vm = require('node:vm');
const source = readFileSync('src/main/resources/static/js/vacante_prediccion.js', 'utf8');

function card(url = '/postulante/vacante/1/prediccion') {
  const children = [];
  const value = { textContent: 'Calculando…' };
  const badge = { hidden: true };
  const result = {
    dataset: { url }, isConnected: true, hidden: true, value, badge, children,
    append(element) { children.push(element); element.parent = result; },
    querySelector(selector) {
      if (selector === '[data-prediction-value]') return value;
      if (selector === '[data-prediction-badge]') return badge;
      if (selector === '[data-prediction-note]') return children.find(e => e.dataset.predictionNote);
      if (selector === '[data-prediction-retry]') return children.find(e => e.dataset.predictionRetry);
    }
  };
  return result;
}

function page(fetch) {
  let cards = [card()];
  let observer;
  const events = {};
  const content = {
    querySelectorAll() { return cards.filter(c => !c.dataset.loaded); },
    addEventListener(event, handler) { events[event] = handler; }
  };
  const document = {
    querySelector() { return content; },
    createElement() {
      return { dataset: {}, remove() { this.parent.children.splice(this.parent.children.indexOf(this), 1); },
        closest(selector) { return selector === '[data-prediction-retry]' ? this : this.parent; } };
    }
  };
  vm.runInNewContext(source, {
    document, fetch, AbortController, setTimeout, clearTimeout,
    MutationObserver: class { constructor(callback) { observer = callback; } observe() {} }
  });
  return { get card() { return cards[0]; },
    select(next) { cards = [next]; observer(); },
    retry() { events.click({ target: cards[0].querySelector('[data-prediction-retry]') }); }
  };
}

const settled = () => new Promise(resolve => setImmediate(resolve));
const response = data => ({ ok: true, json: async () => data });

test('carga la vacante ya seleccionada y muestra 0% correctamente', async () => {
  let calls = 0;
  const p = page(async () => { calls++; return response({ probabilidad: 0, compatibilidad: 'Baja' }); });
  await settled();
  assert.equal(calls, 1);
  assert.equal(p.card.value.textContent, '0.0%');
  assert.equal(p.card.hidden, false);
  assert.equal(p.card.badge.hidden, false);
});

test('un 0.5% no se convierte incorrectamente en 50%', async () => {
  const p = page(async () => response({ probabilidad: 0.5, compatibilidad: 'Baja', coberturaLimitada: true }));
  await settled();
  assert.equal(p.card.value.textContent, '0.5%');
  assert.match(p.card.querySelector('[data-prediction-note]').textContent, /pocos ejemplos/);
});

test('permite reintentar después de un fallo sin dejarlo guardado en cache', async () => {
  let calls = 0;
  const p = page(async () => ++calls === 1 ? { ok: false, status: 503 } : response({ probabilidad: 42.5, compatibilidad: 'Media' }));
  await settled();
  assert.equal(p.card.value.textContent, 'No disponible por ahora');
  assert.ok(p.card.querySelector('[data-prediction-retry]'));
  p.retry();
  await settled();
  assert.equal(calls, 2);
  assert.equal(p.card.value.textContent, '42.5%');
  assert.equal(p.card.querySelector('[data-prediction-retry]'), undefined);
});

test('reutiliza una predicción válida al volver a seleccionar la misma vacante', async () => {
  let calls = 0;
  const p = page(async () => { calls++; return response({ probabilidad: 84, compatibilidad: 'Alta' }); });
  await settled();
  p.select(card());
  await settled();
  assert.equal(calls, 1);
  assert.equal(p.card.value.textContent, '84.0%');
});
