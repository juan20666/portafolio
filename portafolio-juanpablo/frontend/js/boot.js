/* =====================================================================
   boot.js — se ejecuta antes de pintar la página
   Marca que hay JavaScript (para las animaciones de aparición) y aplica
   los colores de la galaxia guardados, para que no haya parpadeo.
   ===================================================================== */
(function () {
  "use strict";
  var root = document.documentElement;
  root.classList.add("js");

  var PALETTE_UI = [
    { accent: "#8f7bff", accent2: "#36e2ff", ink: "#07081a", glow: "rgba(143,123,255,0.35)" },
    { accent: "#4dffb4", accent2: "#4aa8ff", ink: "#031510", glow: "rgba(77,255,180,0.30)" },
    { accent: "#ff9a4d", accent2: "#ff4d7a", ink: "#1a0903", glow: "rgba(255,120,80,0.32)" },
    { accent: "#9ad7ff", accent2: "#c9b8ff", ink: "#06101a", glow: "rgba(154,215,255,0.30)" }
  ];

  try {
    var saved = parseInt(localStorage.getItem("galaxy.palette"), 10);
    if (saved >= 0 && saved < PALETTE_UI.length) {
      var ui = PALETTE_UI[saved];
      root.style.setProperty("--accent", ui.accent);
      root.style.setProperty("--accent-2", ui.accent2);
      root.style.setProperty("--accent-ink", ui.ink);
      root.style.setProperty("--accent-glow", ui.glow);
    }
  } catch (e) {
    /* El almacenamiento puede estar bloqueado: se usa la paleta por defecto. */
  }
})();
