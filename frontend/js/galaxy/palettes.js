/* =====================================================================
   palettes.js — paletas de color de la galaxia
   Cada color se define en HSL: [matiz 0-360, saturación 0-100, luminosidad 0-100]
   `ui` son los colores de acento que se aplican a toda la interfaz.
   ===================================================================== */
(function (global) {
  "use strict";

  var PALETTES = [
    {
      id: "nebulosa",
      name: "Nebulosa",
      core: [48, 100, 80],
      mid: [268, 92, 66],
      arm: [193, 96, 60],
      star: [215, 60, 93],
      ui: { accent: "#8f7bff", accent2: "#36e2ff", ink: "#07081a", glow: "rgba(143,123,255,0.35)" }
    },
    {
      id: "aurora",
      name: "Aurora",
      core: [150, 100, 84],
      mid: [165, 90, 54],
      arm: [208, 96, 60],
      star: [170, 55, 93],
      ui: { accent: "#4dffb4", accent2: "#4aa8ff", ink: "#031510", glow: "rgba(77,255,180,0.30)" }
    },
    {
      id: "supernova",
      name: "Supernova",
      core: [46, 100, 82],
      mid: [24, 100, 60],
      arm: [346, 92, 60],
      star: [32, 85, 93],
      ui: { accent: "#ff9a4d", accent2: "#ff4d7a", ink: "#1a0903", glow: "rgba(255,120,80,0.32)" }
    },
    {
      id: "hielo",
      name: "Hielo",
      core: [198, 100, 93],
      mid: [206, 88, 72],
      arm: [250, 84, 68],
      star: [200, 45, 96],
      ui: { accent: "#9ad7ff", accent2: "#c9b8ff", ink: "#06101a", glow: "rgba(154,215,255,0.30)" }
    }
  ];

  /** Ángulo más corto entre dos matices para interpolar sin saltos. */
  function lerpHue(a, b, t) {
    var d = ((b - a + 540) % 360) - 180;
    return (a + d * t + 360) % 360;
  }

  function lerpColor(a, b, t) {
    return [lerpHue(a[0], b[0], t), a[1] + (b[1] - a[1]) * t, a[2] + (b[2] - a[2]) * t];
  }

  /** Color del gradiente radial (0 = núcleo, 1 = borde) sobre una paleta viva. */
  function sample(palette, t) {
    if (t < 0.5) {
      return lerpColor(palette.core, palette.mid, t * 2);
    }
    return lerpColor(palette.mid, palette.arm, (t - 0.5) * 2);
  }

  function clone(p) {
    return {
      core: p.core.slice(),
      mid: p.mid.slice(),
      arm: p.arm.slice(),
      star: p.star.slice()
    };
  }

  /** Acerca `current` a `target` y devuelve la distancia restante (para saber si terminó). */
  function approach(current, target, k) {
    var keys = ["core", "mid", "arm", "star"];
    var remaining = 0;
    for (var i = 0; i < keys.length; i++) {
      var c = current[keys[i]];
      var t = target[keys[i]];
      c[0] = lerpHue(c[0], t[0], k);
      c[1] += (t[1] - c[1]) * k;
      c[2] += (t[2] - c[2]) * k;
      remaining += Math.abs(((t[0] - c[0] + 540) % 360) - 180) + Math.abs(t[1] - c[1]) + Math.abs(t[2] - c[2]);
    }
    return remaining;
  }

  function css(color, alpha, hueShift) {
    var h = (color[0] + (hueShift || 0) + 360) % 360;
    return "hsla(" + h.toFixed(1) + "," + color[1].toFixed(1) + "%," + color[2].toFixed(1) + "%," + alpha + ")";
  }

  global.GalaxyPalettes = {
    list: PALETTES,
    sample: sample,
    clone: clone,
    approach: approach,
    css: css
  };
})(window);
