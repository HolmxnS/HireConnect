(function () {
    'use strict';

    var INTERVALO = 4500;
    var POSICIONES = ['center', 'right', 'left'];

    var carrusel = document.querySelector('[data-carousel]');
    if (!carrusel) return;

    var slides = Array.prototype.slice.call(carrusel.querySelectorAll('.offers__slide'));
    if (slides.length < 2) return;

    var orden = slides.slice();
    var timer = null;
    var reduceMotion = window.matchMedia('(prefers-reduced-motion: reduce)');
    var mobile = window.matchMedia('(max-width: 860px)');

    function pintar() {
        orden.forEach(function (slide, i) {
            var pos = POSICIONES[i];
            var activa = pos === 'center';

            slide.classList.remove('offers__slide--center', 'offers__slide--right', 'offers__slide--left');
            slide.classList.add('offers__slide--' + pos);
            slide.setAttribute('aria-hidden', mobile.matches || activa ? 'false' : 'true');

            Array.prototype.forEach.call(slide.querySelectorAll('a, button'), function (el) {
                if (mobile.matches || activa) {
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
        if (timer || mobile.matches || reduceMotion.matches || document.hidden || carrusel.contains(document.activeElement)) return;
        timer = setInterval(siguiente, INTERVALO);
    }

    function detener() {
        clearInterval(timer);
        timer = null;
    }

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

    slides.forEach(function (slide) {
        slide.addEventListener('click', function (e) {
            if (!mobile.matches && !slide.classList.contains('offers__slide--center')) {
                e.preventDefault();
                traerAlCentro(slide);
            }
        });
    });

    // En móvil se usa scroll nativo; en escritorio, rotación de tarjetas superpuestas.
    function adaptar() {
        detener();
        pintar();
        if (mobile.matches) {
            carrusel.setAttribute('tabindex', '0');
            carrusel.setAttribute('aria-label', 'Vacantes recientes. Desliza o usa las flechas para explorar.');
        } else {
            carrusel.removeAttribute('tabindex');
            carrusel.setAttribute('aria-label', 'Vacantes recientes');
            carrusel.scrollLeft = 0;
            iniciar();
        }
    }
    carrusel.addEventListener('keydown', function (e) {
        if (!mobile.matches || e.target !== carrusel || (e.key !== 'ArrowRight' && e.key !== 'ArrowLeft')) return;
        e.preventDefault();
        carrusel.scrollBy({ left: (e.key === 'ArrowRight' ? 1 : -1) * (slides[0].getBoundingClientRect().width + 16),
            behavior: reduceMotion.matches ? 'auto' : 'smooth' });
    });
    mobile.addEventListener('change', adaptar);
    reduceMotion.addEventListener('change', adaptar);
    adaptar();
})();