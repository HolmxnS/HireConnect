(function () {
    'use strict';

    var form = document.getElementById('register-form');
    var step1 = document.getElementById('step-1');
    var step2 = document.getElementById('step-2');
    var stepInput = document.getElementById('currentStep');
    var stepper = document.getElementById('stepper');

    if (!form || !step1 || !step2 || !stepInput) return;

    var mostrarPaso2 = window.mostrarPaso2 || false;

    // Actualiza la barra de pasos (el CSS pinta los pasos segun data-step)
    function marcarPaso(numero) {
        if (stepper) {
            stepper.setAttribute('data-step', String(numero));
        }
    }

    // Si el servidor valido el paso 1, se muestra directamente el paso 2
    if (mostrarPaso2) {
        step1.hidden = true;
        step2.hidden = false;
        step2.classList.add('form__step--enter');
        stepInput.value = '2';
        marcarPaso(2);
    } else {
        marcarPaso(1);
    }

    var enviando = false;
    var estado = document.createElement('p');
    estado.setAttribute('role', 'status');
    estado.setAttribute('aria-live', 'polite');
    estado.hidden = true;
    form.appendChild(estado);

    form.addEventListener('submit', function (evento) {
        if (enviando) {
            evento.preventDefault();
            return;
        }
        enviando = true;
        form.setAttribute('aria-busy', 'true');
        // Mantener los campos visibles mientras el servidor procesa el registro.
        estado.hidden = false;
        estado.textContent = stepInput.value === '2'
            ? 'Enviando tu código de verificación. Espera un momento…'
            : 'Validando tus datos. Espera un momento…';
    });

    window.addEventListener('pageshow', function () {
        enviando = false;
        form.removeAttribute('aria-busy');
        estado.hidden = true;
    });

    Array.prototype.forEach.call(document.querySelectorAll('[data-toggle-password]'), function (boton) {
        boton.addEventListener('click', function () {
            var input = document.getElementById(boton.getAttribute('data-toggle-password'));
            if (!input) return;

            var mostrar = input.type === 'password';
            input.type = mostrar ? 'text' : 'password';
            boton.setAttribute('aria-pressed', mostrar ? 'true' : 'false');
            boton.setAttribute('aria-label', mostrar ? 'Ocultar contraseña' : 'Mostrar contraseña');
        });
    });

    var password = document.getElementById('password');
    var confirmar = document.getElementById('confirmPassword');
    var aviso = document.querySelector('[data-match-error]');

    if (password && confirmar && aviso) {
        confirmar.addEventListener('input', function () {
            var distintas = confirmar.value && password.value !== confirmar.value;
            aviso.textContent = distintas ? 'Las contraseñas no coinciden' : '';
        });
    }
})();