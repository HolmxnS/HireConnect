/*Carrusel de vacantes (landing)*/
(function () {
    'use strict';

    var INTERVALO = 4500;
    var POSICIONES = ['center', 'right', 'left'];

    var carrusel = document.querySelector('[data-carousel]');
    if (!carrusel) return;

    var slides = Array.prototype.slice.call(carrusel.querySelectorAll('.offers__slide'));
    if (slides.length < 2) return;

    // 0 = centro, 1 = derecha, 2 = izquierda
    var orden = slides.slice();
    var timer = null;
    var reduceMotion = window.matchMedia('(prefers-reduced-motion: reduce)');

    function pintar() {
        orden.forEach(function (slide, i) {
            var pos = POSICIONES[i];
            var activa = pos === 'center';

            slide.classList.remove('offers__slide--center', 'offers__slide--right', 'offers__slide--left');
            slide.classList.add('offers__slide--' + pos);
            slide.setAttribute('aria-hidden', activa ? 'false' : 'true');

            Array.prototype.forEach.call(slide.querySelectorAll('a, button'), function (el) {
                if (activa) {
                    el.removeAttribute('tabindex');
                } else {
                    el.setAttribute('tabindex', '-1');
                }
            });
        });
    }

    function siguiente() {
        orden.push(orden.shift());
        pintar();
    }

    function traerAlCentro(slide) {
        var intentos = orden.length;
        while (orden[0] !== slide && intentos-- > 0) {
            orden.push(orden.shift());
        }
        pintar();
    }

    function iniciar() {
        if (timer || reduceMotion.matches || document.hidden) return;
        timer = setInterval(siguiente, INTERVALO);
    }

    function detener() {
        clearInterval(timer);
        timer = null;
    }

    // Pausa
    carrusel.addEventListener('mouseenter', detener);
    carrusel.addEventListener('mouseleave', iniciar);
    carrusel.addEventListener('focusin', detener);
    carrusel.addEventListener('focusout', iniciar);

    document.addEventListener('visibilitychange', function () {
        if (document.hidden) {
            detener();
        } else {
            iniciar();
        }
    });

    //Si se toca la vacante de atras, pasa al frente
    slides.forEach(function (slide) {
        slide.addEventListener('click', function (e) {
            if (!slide.classList.contains('offers__slide--center')) {
                e.preventDefault();
                traerAlCentro(slide);
            }
        });
    });

    pintar();
    iniciar();
})();