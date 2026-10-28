/*Selección de tipo de cuenta (registro)*/
(function () {
    'use strict';

    var opciones = document.querySelectorAll('.roles__input');
    var enlace = document.querySelector('[data-next-link]');
    var formulario = document.querySelector('[data-next-form]');

    if (!opciones.length) return;

    // Ajusta el destino del botón de continuar según la opción marcada
    function actualizar() {
        var elegida = document.querySelector('.roles__input:checked');
        if (!elegida) return;

        if (enlace) {
            enlace.setAttribute('href', elegida.getAttribute('data-href'));
        }
        if (formulario) {
            formulario.setAttribute('action', elegida.getAttribute('data-oauth-href'));
        }
    }

    Array.prototype.forEach.call(opciones, function (opcion) {
        opcion.addEventListener('change', actualizar);
    });

    // Al volver con el botón "atrás" el navegador restaura la opción marcada
    window.addEventListener('pageshow', actualizar);

    actualizar();
})();