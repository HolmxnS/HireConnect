/* La campana consulta GET /postulante/notificaciones (noLeidas, items).
   Los POST /leer-todas y /{id}/leer, bajo esa ruta, actualizan el estado de lectura. */
(function () {
  "use strict";

  var root = document.documentElement;
  var desktop = window.matchMedia("(min-width: 901px)");

  function guardar(clave, valor) {
    try {
      localStorage.setItem(clave, valor);
    } catch (e) {}
  }

  function leer(clave) {
    try {
      return localStorage.getItem(clave);
    } catch (e) {
      return null;
    }
  }

  var toggle = document.getElementById("sidebarToggle");
  var opener = document.getElementById("sidebarOpener");
  var overlay = document.getElementById("sidebarOverlay");

  function menuAbierto() {
    return desktop.matches
      ? !root.classList.contains("is-sidebar-collapsed")
      : root.classList.contains("is-sidebar-open");
  }

  function actualizarBotonesMenu() {
    var abierto = menuAbierto();
    if (toggle) {
      toggle.setAttribute("aria-expanded", String(abierto));
      toggle.setAttribute(
        "aria-label",
        abierto ? "Ocultar menú" : "Mostrar menú",
      );
    }
    if (opener) {
      opener.setAttribute(
        "aria-expanded",
        String(root.classList.contains("is-sidebar-open")),
      );
    }
  }

  function cerrarPanelMovil() {
    root.classList.remove("is-sidebar-open");
    actualizarBotonesMenu();
  }

  if (toggle) {
    toggle.addEventListener("click", function () {
      if (desktop.matches) {
        var plegado = root.classList.toggle("is-sidebar-collapsed");
        guardar("hc-sidebar", plegado ? "collapsed" : "expanded");
      } else {
        root.classList.toggle("is-sidebar-open");
      }
      actualizarBotonesMenu();
    });
  }

  if (opener) {
    opener.addEventListener("click", function () {
      root.classList.add("is-sidebar-open");
      actualizarBotonesMenu();
    });
  }

  if (overlay) overlay.addEventListener("click", cerrarPanelMovil);

  // Al cambiar de tamano de pantalla se recupera la preferencia guardada
  function alCambiarPantalla() {
    root.classList.remove("is-sidebar-open");
    if (desktop.matches) {
      root.classList.toggle(
        "is-sidebar-collapsed",
        leer("hc-sidebar") === "collapsed",
      );
    }
    actualizarBotonesMenu();
  }

  if (desktop.addEventListener)
    desktop.addEventListener("change", alCambiarPantalla);
  else if (desktop.addListener) desktop.addListener(alCambiarPantalla);

  actualizarBotonesMenu();

  // theme.js gestiona el tema y las etiquetas de [data-theme-toggle].
  // No registrar otro cambio aquí: un segundo listener revierte el mismo clic.

  var caja = document.querySelector("[data-notif]");
  var panel = null;
  var boton = null;

  function iniciarNotificaciones(caja) {
    var url = caja.getAttribute("data-url");
    var csrf = caja.getAttribute("data-csrf") || "";
    boton = caja.querySelector("#notifBtn");
    panel = caja.querySelector("#notifPanel");
    var lista = caja.querySelector("#notifList");
    var vacio = caja.querySelector("#notifEmpty");
    var insignia = caja.querySelector("#notifBadge");
    var leerTodas = caja.querySelector("#notifReadAll");
    var temporizador = null;
    var disponible = true;

    if (!url || !boton || !panel || !lista || !vacio || !insignia || !leerTodas)
      return;

    var relativo =
      window.Intl && Intl.RelativeTimeFormat
        ? new Intl.RelativeTimeFormat("es", { numeric: "auto" })
        : null;

    function hace(texto) {
      var fecha = new Date(texto);
      if (!texto || isNaN(fecha.getTime())) return "";
      var segundos = Math.round((fecha.getTime() - Date.now()) / 1000);
      var abs = Math.abs(segundos);
      if (!relativo) return fecha.toLocaleDateString("es");
      if (abs < 60) return "Hace un momento";
      if (abs < 3600)
        return relativo.format(Math.round(segundos / 60), "minute");
      if (abs < 86400)
        return relativo.format(Math.round(segundos / 3600), "hour");
      if (abs < 2592000)
        return relativo.format(Math.round(segundos / 86400), "day");
      return fecha.toLocaleDateString("es");
    }

    function tono(estado) {
      var e = String(estado || "").toLowerCase();
      if (/acept|contrat|selecc|oferta/.test(e))
        return { clase: "ok", icono: "fa-circle-check" };
      if (/rechaz|descart|cerrad/.test(e))
        return { clase: "bad", icono: "fa-circle-xmark" };
      if (/revis|proceso|pendient|entrevist/.test(e))
        return { clase: "wait", icono: "fa-clock" };
      return { clase: "info", icono: "fa-bell" };
    }

    function enlaceSeguro(valor) {
      // Solo rutas internas, nunca javascript: ni otros sitios
      if (
        typeof valor === "string" &&
        valor.charAt(0) === "/" &&
        valor.charAt(1) !== "/"
      )
        return valor;
      return "/postulante/postulaciones";
    }

    function cabeceras(conCuerpo) {
      var h = { Accept: "application/json" };
      if (csrf) h["X-CSRF-TOKEN"] = csrf;
      if (conCuerpo) h["Content-Type"] = "application/x-www-form-urlencoded";
      return h;
    }

    function pintar(datos) {
      var items = (datos && datos.items) || [];
      var noLeidas =
        typeof (datos && datos.noLeidas) === "number"
          ? datos.noLeidas
          : items.filter(function (n) {
              return !n.leida;
            }).length;

      insignia.hidden = noLeidas <= 0;
      insignia.textContent = noLeidas > 99 ? "99+" : String(noLeidas);
      boton.setAttribute(
        "aria-label",
        noLeidas > 0
          ? "Notificaciones, " + noLeidas + " sin leer"
          : "Notificaciones",
      );
      leerTodas.disabled = noLeidas <= 0;

      lista.textContent = "";
      vacio.hidden = items.length > 0;

      items.forEach(function (n) {
        var t = tono(n.estado);
        var li = document.createElement("li");
        var a = document.createElement("a");
        a.className =
          "notif__item notif__item--" + t.clase + (n.leida ? "" : " is-unread");
        a.href = enlaceSeguro(n.enlace);

        var icono = document.createElement("span");
        icono.className = "notif__icon";
        var i = document.createElement("i");
        i.className = "fa-solid " + t.icono;
        icono.appendChild(i);

        var cuerpo = document.createElement("div");
        cuerpo.className = "notif__body";
        var texto = document.createElement("p");
        texto.className = "notif__text";
        texto.textContent = n.mensaje || "";
        var tiempo = document.createElement("span");
        tiempo.className = "notif__time";
        tiempo.textContent = hace(n.fecha);
        cuerpo.appendChild(texto);
        cuerpo.appendChild(tiempo);

        a.appendChild(icono);
        a.appendChild(cuerpo);
        if (!n.leida) {
          var punto = document.createElement("span");
          punto.className = "notif__dot";
          a.appendChild(punto);
        }

        a.addEventListener("click", function () {
          if (n.leida || !n.id) return;
          try {
            fetch(url + "/" + encodeURIComponent(n.id) + "/leer", {
              method: "POST",
              headers: cabeceras(false),
              credentials: "same-origin",
              keepalive: true,
            });
          } catch (e) {}
        });

        li.appendChild(a);
        lista.appendChild(li);
      });
    }

    function cargar() {
      if (!disponible) return;
      fetch(url, { headers: cabeceras(false), credentials: "same-origin" })
        .then(function (r) {
          // Si el servicio aun no existe o no hay sesion, no se insiste
          if (r.status === 404 || r.status === 401 || r.status === 403) {
            disponible = false;
            detener();
            return null;
          }
          if (!r.ok) throw new Error("HTTP " + r.status);
          return r.json();
        })
        .then(function (datos) {
          if (datos) pintar(datos);
        })
        .catch(function () {
          /* sin red: se reintenta en el siguiente ciclo */
        });
    }

    function iniciarSondeo() {
      detener();
      temporizador = setInterval(function () {
        if (document.visibilityState === "visible") cargar();
      }, 60000);
    }

    function detener() {
      if (temporizador) {
        clearInterval(temporizador);
        temporizador = null;
      }
    }

    function abrir() {
      panel.hidden = false;
      boton.setAttribute("aria-expanded", "true");
      cargar();
    }

    function cerrar() {
      panel.hidden = true;
      boton.setAttribute("aria-expanded", "false");
    }

    boton.addEventListener("click", function (e) {
      e.stopPropagation();
      if (panel.hidden) abrir();
      else cerrar();
    });

    panel.addEventListener("click", function (e) {
      e.stopPropagation();
    });

    leerTodas.addEventListener("click", function () {
      leerTodas.disabled = true;
      fetch(url + "/leer-todas", {
        method: "POST",
        headers: cabeceras(false),
        credentials: "same-origin",
      })
        .then(function () {
          cargar();
        })
        .catch(function () {
          cargar();
        });
    });

    document.addEventListener("click", cerrar);
    document.addEventListener("visibilitychange", function () {
      if (document.visibilityState === "visible") cargar();
    });

    caja.cerrarNotificaciones = cerrar;

    cargar();
    iniciarSondeo();
  }

  if (caja) iniciarNotificaciones(caja);

  document.addEventListener("keydown", function (e) {
    if (e.key === "Escape") {
      if (root.classList.contains("is-sidebar-open")) cerrarPanelMovil();
      if (panel && !panel.hidden && boton) {
        panel.hidden = true;
        boton.setAttribute("aria-expanded", "false");
        boton.focus();
      }
      return;
    }

    // "/" enfoca el buscador, salvo que se este escribiendo en otro campo
    if (e.key === "/" && !e.ctrlKey && !e.metaKey && !e.altKey) {
      var activo = document.activeElement;
      var escribiendo =
        (activo && /^(INPUT|TEXTAREA|SELECT)$/.test(activo.tagName)) ||
        (activo && activo.isContentEditable);
      var buscador = document.getElementById("buscador");
      if (!escribiendo && buscador) {
        e.preventDefault();
        buscador.focus();
        buscador.select();
      }
    }
  });
})();
