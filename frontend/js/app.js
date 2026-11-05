/* =====================================================================
   app.js — interfaz: galaxia, controles, navegación, apariciones,
   enlaces opcionales y filtro de proyectos
   ===================================================================== */
(function () {
  "use strict";

  var CONFIG = window.SITE_CONFIG || {};
  var root = document.documentElement;

  /* ---------- Utilidades ---------- */

  function $(selector, scope) {
    return (scope || document).querySelector(selector);
  }

  function $all(selector, scope) {
    return Array.prototype.slice.call((scope || document).querySelectorAll(selector));
  }

  function store(key, value) {
    try {
      if (value === undefined) {
        return localStorage.getItem(key);
      }
      localStorage.setItem(key, String(value));
    } catch (e) {
      /* almacenamiento bloqueado: se ignora */
    }
    return null;
  }

  function isTypingTarget(el) {
    return !!el && /^(INPUT|TEXTAREA|SELECT)$/.test(el.tagName);
  }

  /* ---------- Galaxia y controles ---------- */

  function initGalaxy() {
    var canvas = $("#galaxy");
    if (!canvas || !window.PortfolioGalaxy || !canvas.getContext) {
      return;
    }
    var reduced = window.matchMedia && window.matchMedia("(prefers-reduced-motion: reduce)").matches;
    var Palettes = window.GalaxyPalettes;
    var Shapes = window.GalaxyShapes;
    Shapes.prepareText("JAVA");

    var savedPalette = parseInt(store("galaxy.palette"), 10);
    var savedShape = store("galaxy.shape"); // "auto" o un índice
    var auto = savedShape === null || savedShape === "auto";

    var dockColor = $("#dockColor");
    var dockShape = $("#dockShape");
    var dockMotion = $("#dockMotion");
    var status = $("#galaxyStatus");

    function announce(text) {
      if (status) {
        status.textContent = text;
      }
    }

    function applyUi(palette) {
      var ui = palette.ui;
      root.style.setProperty("--accent", ui.accent);
      root.style.setProperty("--accent-2", ui.accent2);
      root.style.setProperty("--accent-ink", ui.ink);
      root.style.setProperty("--accent-glow", ui.glow);
      var meta = $('meta[name="theme-color"]');
      if (meta) {
        meta.setAttribute("content", "#05060f");
      }
    }

    function shapeLabel() {
      return auto ? "Auto" : engine.getShape().name;
    }

    function refreshDock() {
      if (dockColor) {
        $(".dock__value", dockColor).textContent = engine.getPalette().name;
      }
      if (dockShape) {
        $(".dock__value", dockShape).textContent = shapeLabel();
      }
    }

    var engine = window.PortfolioGalaxy.create(canvas, {
      reducedMotion: reduced,
      paletteIndex: savedPalette >= 0 && savedPalette < Palettes.list.length ? savedPalette : 0,
      shapeIndex: auto ? 0 : parseInt(savedShape, 10) || 0,
      onPalette: function (palette, index) {
        applyUi(palette);
        store("galaxy.palette", index);
        if (engine) {
          refreshDock();
          announce("Paleta de la galaxia: " + palette.name);
        }
      },
      onShape: function (shape) {
        if (engine) {
          refreshDock();
          announce("Forma de la galaxia: " + shape.name);
        }
      }
    });
    applyUi(engine.getPalette());
    refreshDock();
    engine.start();
    window.__galaxy = engine; // útil para depurar en la consola

    if (dockColor) {
      dockColor.addEventListener("click", function () {
        engine.nextPalette();
      });
    }

    if (dockShape) {
      dockShape.addEventListener("click", function () {
        // Ciclo: Auto → Espiral → Anillo → Hélice → JAVA → Auto
        if (auto) {
          auto = false;
          engine.setShape(0);
          store("galaxy.shape", 0);
          if (engine.shapeIndex === 0) {
            refreshDock();
            announce("Forma de la galaxia: " + engine.getShape().name);
          }
        } else if (engine.shapeIndex === Shapes.list.length - 1) {
          auto = true;
          store("galaxy.shape", "auto");
          refreshDock();
          announce("Forma de la galaxia: automática según la sección");
        } else {
          engine.nextShape();
          store("galaxy.shape", engine.shapeIndex);
        }
      });
    }

    if (dockMotion) {
      if (reduced) {
        dockMotion.hidden = true;
      } else {
        dockMotion.addEventListener("click", function () {
          var paused = engine.togglePause();
          dockMotion.setAttribute("aria-pressed", String(paused));
          dockMotion.setAttribute("aria-label", paused ? "Reanudar animación de la galaxia" : "Pausar animación de la galaxia");
          announce(paused ? "Animación en pausa" : "Animación reanudada");
        });
      }
    }

    // Atajos de teclado: C = color, F = forma, P = pausa
    document.addEventListener("keydown", function (e) {
      if (e.ctrlKey || e.metaKey || e.altKey || isTypingTarget(e.target)) {
        return;
      }
      var key = e.key.toLowerCase();
      if (key === "c" && dockColor) {
        dockColor.click();
      } else if (key === "f" && dockShape) {
        dockShape.click();
      } else if (key === "p" && dockMotion && !dockMotion.hidden) {
        dockMotion.click();
      }
    });

    // La forma cambia sola según la sección que está en pantalla
    var sections = $all("[data-shape]");
    if ("IntersectionObserver" in window && sections.length) {
      var timer = 0;
      var io = new IntersectionObserver(
        function (entries) {
          entries.forEach(function (entry) {
            if (!entry.isIntersecting || !auto) {
              return;
            }
            var id = entry.target.getAttribute("data-shape");
            clearTimeout(timer);
            timer = setTimeout(function () {
              if (!auto) {
                return;
              }
              for (var i = 0; i < Shapes.list.length; i++) {
                if (Shapes.list[i].id === id) {
                  engine.setShape(i);
                }
              }
            }, 450);
          });
        },
        { rootMargin: "-45% 0px -45% 0px", threshold: 0 }
      );
      sections.forEach(function (s) {
        io.observe(s);
      });
    }

    showHint();
  }

  function showHint() {
    var hint = $("#hint");
    if (!hint || store("galaxy.hintSeen") === "1") {
      return;
    }
    var touch = window.matchMedia && window.matchMedia("(pointer: coarse)").matches;
    hint.textContent = touch
      ? "Toca el fondo para cambiar el color. Mantén presionado para crear una singularidad."
      : "Mueve el mouse. Clic en el fondo = color nuevo, doble clic = forma nueva, mantén presionado = singularidad.";
    setTimeout(function () {
      hint.classList.add("is-visible");
    }, 1400);
    setTimeout(function () {
      hint.classList.remove("is-visible");
      store("galaxy.hintSeen", "1");
    }, 9500);
    window.addEventListener(
      "pointerdown",
      function () {
        hint.classList.remove("is-visible");
        store("galaxy.hintSeen", "1");
      },
      { once: true, passive: true }
    );
  }

  /* ---------- Navegación móvil ---------- */

  function initNav() {
    var toggle = $("#navToggle");
    var nav = $("#nav");
    if (!toggle || !nav) {
      return;
    }
    function close() {
      nav.classList.remove("is-open");
      toggle.setAttribute("aria-expanded", "false");
    }
    toggle.addEventListener("click", function () {
      var open = nav.classList.toggle("is-open");
      toggle.setAttribute("aria-expanded", String(open));
    });
    nav.addEventListener("click", function (e) {
      if (e.target.closest("a")) {
        close();
      }
    });
    document.addEventListener("keydown", function (e) {
      if (e.key === "Escape") {
        close();
      }
    });
  }

  /* ---------- Aparición al hacer scroll ---------- */

  function initReveal() {
    var items = $all(".reveal");
    if (!items.length) {
      return;
    }
    if (!("IntersectionObserver" in window)) {
      items.forEach(function (el) {
        el.classList.add("is-visible");
      });
      return;
    }
    var io = new IntersectionObserver(
      function (entries) {
        entries.forEach(function (entry) {
          if (entry.isIntersecting) {
            entry.target.classList.add("is-visible");
            io.unobserve(entry.target);
          }
        });
      },
      { threshold: 0.12 }
    );
    items.forEach(function (el) {
      io.observe(el);
    });
  }

  /* ---------- Enlaces opcionales según config.js ---------- */

  function initConfigLinks() {
    $all("[data-config-href]").forEach(function (a) {
      var value = CONFIG[a.getAttribute("data-config-href")];
      if (value) {
        a.setAttribute("href", value + (a.getAttribute("data-config-suffix") || ""));
        a.classList.remove("is-hidden");
        var wrapper = a.closest("[data-config-show]");
        if (wrapper) {
          wrapper.classList.remove("is-hidden");
        }
      }
    });
  }

  /* ---------- Filtro de proyectos (método GET: ?q=palabra) ---------- */

  function initProjectFilter() {
    var input = $("#q");
    var cards = $all("[data-project]");
    if (!input || !cards.length) {
      return;
    }
    var empty = $("#noResults");

    function apply(term) {
      var needle = term.trim().toLowerCase();
      var visible = 0;
      cards.forEach(function (card) {
        var haystack = (card.textContent + " " + (card.getAttribute("data-tags") || "")).toLowerCase();
        var match = !needle || haystack.indexOf(needle) !== -1;
        card.classList.toggle("is-hidden", !match);
        if (match) {
          visible += 1;
        }
      });
      if (empty) {
        empty.classList.toggle("is-hidden", visible !== 0);
      }
    }

    var initial = new URLSearchParams(window.location.search).get("q") || "";
    input.value = initial;
    apply(initial);
    input.addEventListener("input", function () {
      apply(input.value);
    });
  }

  /* ---------- Botones de copiar ---------- */

  function initCopy() {
    $all("[data-copy]").forEach(function (btn) {
      btn.addEventListener("click", function () {
        var value = btn.getAttribute("data-copy");
        var original = btn.textContent;
        function done(ok) {
          btn.textContent = ok ? "Copiado" : "No se pudo copiar";
          setTimeout(function () {
            btn.textContent = original;
          }, 1700);
        }
        if (navigator.clipboard && navigator.clipboard.writeText) {
          navigator.clipboard.writeText(value).then(
            function () {
              done(true);
            },
            function () {
              done(false);
            }
          );
        } else {
          done(false);
        }
      });
    });
  }

  document.addEventListener("DOMContentLoaded", function () {
    var year = $("#year");
    if (year) {
      year.textContent = String(new Date().getFullYear());
    }
    initNav();
    initReveal();
    initConfigLinks();
    initProjectFilter();
    initCopy();
    initGalaxy();
  });
})();
