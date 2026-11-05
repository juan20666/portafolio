/* =====================================================================
   shapes.js — formas hacia las que se reorganizan las partículas
   Cada forma calcula la posición "de reposo" de una partícula en el
   espacio normalizado [-1, 1]. El motor mueve las partículas hacia allí
   con resortes, así el cambio de forma es una transformación fluida.

   Semillas por partícula (constantes): a, b, c, d en [0,1); g1, g2 gaussianas.
   ===================================================================== */
(function (global) {
  "use strict";

  var TAU = Math.PI * 2;

  /** Espiral de 3 brazos con núcleo brillante y rotación diferencial. */
  function spiral(p, t, out) {
    if (p.b < 0.2) {
      // Bulbo central: nube gaussiana
      var br = Math.abs(p.g1) * 0.11;
      var ba = p.c * TAU + t * 0.25;
      out.x = br * Math.cos(ba);
      out.y = br * Math.sin(ba) * 0.62;
      out.z = 0.85;
      return;
    }
    var r = Math.pow(p.a, 1.3);
    var spin = t * 0.07 * (1.35 - r);
    var theta = p.arm * (TAU / 3) + r * 5.4 + p.g1 * (0.1 + 0.34 * r) + spin;
    out.x = r * Math.cos(theta);
    out.y = r * Math.sin(theta) * 0.58;
    out.z = 0.35 + 0.5 * (1 - r) + p.g2 * 0.04;
  }

  /** Planeta con anillos inclinados (bandas concéntricas). */
  function ring(p, t, out) {
    var tilt = -0.38;
    var x;
    var y;
    if (p.b < 0.3) {
      var rr = 0.3 * Math.sqrt(p.a);
      var an = p.c * TAU;
      x = rr * Math.cos(an);
      y = rr * Math.sin(an);
      out.z = 0.9 - rr;
    } else {
      var band = Math.floor(p.d * 5);
      var rho = 0.5 + band * 0.1 + p.g1 * 0.012;
      var th = p.c * TAU + t * (0.28 / rho);
      x = rho * Math.cos(th);
      y = rho * Math.sin(th) * 0.27;
      out.z = 0.45 + 0.4 * Math.sin(th);
    }
    out.x = x * Math.cos(tilt) - y * Math.sin(tilt);
    out.y = x * Math.sin(tilt) + y * Math.cos(tilt);
  }

  /** Doble hélice (como el ADN) con peldaños. */
  function helix(p, t, out) {
    var amp = 0.3;
    var turns = 7;
    var speed = 0.9;
    var tilt = -0.28;
    var x;
    var y;
    if (p.c < 0.14) {
      var k = Math.round((p.a * 2 - 1) * 14) / 14;
      var ph = k * turns + t * speed;
      x = k;
      y = Math.sin(ph) * amp * (p.d * 2 - 1);
      out.z = 0.4;
    } else {
      var u = p.a * 2 - 1;
      var strand = p.b < 0.5 ? 0 : Math.PI;
      var phase = u * turns + t * speed + strand;
      x = u;
      y = Math.sin(phase) * amp + p.g1 * 0.012;
      out.z = 0.5 + 0.5 * Math.cos(phase);
    }
    out.x = x * 0.98 * Math.cos(tilt) - y * Math.sin(tilt);
    out.y = x * 0.98 * Math.sin(tilt) + y * Math.cos(tilt);
  }

  // ---- Texto "JAVA": se muestrea desde un lienzo auxiliar ----
  var textPoints = null; // Float32Array [x0, y0, x1, y1, ...]

  function buildTextPoints(word) {
    var w = 900;
    var h = 340;
    var cv = document.createElement("canvas");
    cv.width = w;
    cv.height = h;
    var cx = cv.getContext("2d");
    cx.fillStyle = "#fff";
    cx.textAlign = "center";
    cx.textBaseline = "middle";
    cx.font = '600 300px "Inter Display", "Inter", Arial, sans-serif';
    cx.fillText(word, w / 2, h / 2 + 12);
    var data = cx.getImageData(0, 0, w, h).data;
    var pts = [];
    var step = 4;
    for (var y = 0; y < h; y += step) {
      for (var x = 0; x < w; x += step) {
        if (data[(y * w + x) * 4 + 3] > 140) {
          pts.push((x - w / 2) / (w / 2), (y - h / 2) / (w / 2));
        }
      }
    }
    return new Float32Array(pts);
  }

  function text(p, t, out) {
    if (!textPoints || textPoints.length === 0) {
      // Mientras la tipografía carga, usa la espiral como respaldo.
      spiral(p, t, out);
      return;
    }
    var count = textPoints.length / 2;
    var idx = Math.floor(p.a * count) % count;
    var wobble = 0.006;
    out.x = textPoints[idx * 2] * 0.96 + Math.sin(t * 1.3 + p.c * TAU) * wobble;
    out.y = textPoints[idx * 2 + 1] * 0.96 + Math.cos(t * 1.1 + p.d * TAU) * wobble;
    out.z = 0.55 + 0.35 * Math.sin(t * 0.8 + p.c * 20);
  }

  /** Prepara los puntos del texto cuando la tipografía ya está disponible. */
  function prepareText(word) {
    function build() {
      textPoints = buildTextPoints(word);
    }
    if (document.fonts && document.fonts.load) {
      document.fonts.load('600 120px "Inter Display"').then(build, build);
    } else {
      build();
    }
  }

  global.GalaxyShapes = {
    list: [
      { id: "espiral", name: "Espiral", fit: 1, home: spiral },
      { id: "anillo", name: "Anillo", fit: 0.95, home: ring },
      { id: "helice", name: "Hélice", fit: 0.8, home: helix },
      { id: "java", name: "JAVA", fit: 0.74, home: text }
    ],
    prepareText: prepareText
  };
})(window);
