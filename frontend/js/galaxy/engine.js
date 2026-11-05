/* =====================================================================
   engine.js — motor de la galaxia (canvas 2D, sin dependencias)

   Qué hace:
   - Miles de partículas con resortes hacia la forma activa (espiral,
     anillo, hélice, JAVA) y rotación diferencial.
   - El puntero es un pozo gravitatorio con remolino; mantener presionado
     colapsa la galaxia y al soltar explota; un clic/toque lanza una onda
     de choque y cambia la paleta.
   - La velocidad del puntero desplaza el matiz; el scroll acelera el giro.
   - Se adapta solo: baja la cantidad de partículas si los cuadros por
     segundo caen, se pausa con la pestaña oculta y respeta
     prefers-reduced-motion (dibuja un cuadro estático).
   ===================================================================== */
(function (global) {
  "use strict";

  var P = global.GalaxyPalettes;
  var S = global.GalaxyShapes;
  var TAU = Math.PI * 2;
  var BUCKETS = 6;
  var MAX_PARTICLES = 3600;

  function gauss() {
    // Box-Muller
    var u = 1 - Math.random();
    var v = Math.random();
    return Math.sqrt(-2 * Math.log(u)) * Math.cos(TAU * v);
  }

  function clamp(v, lo, hi) {
    return v < lo ? lo : v > hi ? hi : v;
  }

  var INTERACTIVE =
    "a,button,input,select,textarea,label,summary,nav,header,footer,form,table,.panel,.dock,.hint,.table-wrap,.btn";

  function Engine(canvas, options) {
    options = options || {};
    this.canvas = canvas;
    this.ctx = canvas.getContext("2d");
    this.reduced = !!options.reducedMotion;
    this.onPalette = options.onPalette || function () {};
    this.onShape = options.onShape || function () {};

    this.paletteIndex = options.paletteIndex || 0;
    this.shapeIndex = options.shapeIndex || 0;
    this.current = P.clone(P.list[this.paletteIndex]);
    this.target = P.clone(P.list[this.paletteIndex]);
    this.paletteDirty = false;
    this.hueShift = 0;

    this.t = 0;
    this.timeScale = 1;
    this.scrollSpeed = 0;
    this.lastScroll = global.scrollY || 0;
    this.glow = 1;

    this.pointer = { x: -9999, y: -9999, tx: -9999, ty: -9999, active: false, pressed: false, speed: 0, trail: [] };
    this.waves = [];
    this.stars = [];
    this.shooting = null;
    this.nextShoot = 4 + Math.random() * 5;
    this.bucketColors = [];

    this.running = false;
    this.userPaused = false;
    this.frameTimes = 0;
    this.frameCount = 0;
    this.downgrades = 0;
    this._tmp = { x: 0, y: 0, z: 0.5 };

    this.initialized = false;
    this._seed();
    this._makeSprite();
    this._resize();
    this._bind();
    this.initialized = true;
  }

  /* ---------------- Inicialización ---------------- */

  Engine.prototype._seed = function () {
    this.seeds = [];
    this.x = new Float32Array(MAX_PARTICLES);
    this.y = new Float32Array(MAX_PARTICLES);
    this.vx = new Float32Array(MAX_PARTICLES);
    this.vy = new Float32Array(MAX_PARTICLES);
    for (var i = 0; i < MAX_PARTICLES; i++) {
      var a = Math.random();
      this.seeds.push({
        a: a,
        b: Math.random(),
        c: Math.random(),
        d: Math.random(),
        g1: gauss(),
        g2: gauss(),
        arm: Math.floor(Math.random() * 3),
        size: 0.7 + Math.pow(Math.random(), 2.2) * 1.9,
        k: 9 + Math.random() * 15,
        tw: 0.6 + Math.random() * 2.4,
        ph: Math.random() * TAU,
        bucket: Math.min(BUCKETS - 1, Math.floor(a * BUCKETS))
      });
    }
    this.count = 0;
  };

  Engine.prototype._targetCount = function () {
    var area = this.w * this.h;
    var base = clamp(Math.round(area / 420), 1100, 3400);
    if (this.w < 700) {
      base = Math.round(base * 0.62);
    }
    var factor = Math.pow(0.75, this.downgrades);
    return clamp(Math.round(base * factor), 500, MAX_PARTICLES);
  };

  Engine.prototype._rebuildGroups = function () {
    var n = this._targetCount();
    var previous = this.count;
    this.count = n;
    this.groups = [];
    this.groupCount = [];
    for (var b = 0; b < BUCKETS; b++) {
      this.groups.push([]);
    }
    for (var i = 0; i < n; i++) {
      this.groups[this.seeds[i].bucket].push(i);
    }
    for (b = 0; b < BUCKETS; b++) {
      this.groupCount[b] = this.groups[b].length;
    }
    // Partículas recién activadas: nacen en el centro ("big bang")
    for (i = previous; i < n; i++) {
      this.x[i] = this.cx + gauss() * 6;
      this.y[i] = this.cy + gauss() * 6;
      var ang = Math.random() * TAU;
      var sp = 80 + Math.random() * 520;
      this.vx[i] = Math.cos(ang) * sp;
      this.vy[i] = Math.sin(ang) * sp;
    }
    // Estrellas fijas de fondo
    var starCount = clamp(Math.round(this.w * this.h / 9000), 70, 260);
    this.stars = [];
    for (i = 0; i < starCount; i++) {
      this.stars.push({
        x: Math.random() * this.w,
        y: Math.random() * this.h,
        z: Math.random(),
        s: 0.5 + Math.random() * 1.3,
        ph: Math.random() * TAU,
        sp: 0.4 + Math.random() * 1.8
      });
    }
    // Estrellas brillantes con halo (una de cada 55 partículas)
    this.bright = [];
    for (i = 0; i < n; i += 55) {
      this.bright.push(i);
    }
  };

  Engine.prototype._resize = function () {
    var dpr = Math.min(global.devicePixelRatio || 1, this.w && this.w < 700 ? 1.5 : 2);
    var w = global.innerWidth;
    var h = global.innerHeight;
    this.w = w;
    this.h = h;
    this.dpr = dpr;
    this.canvas.width = Math.round(w * dpr);
    this.canvas.height = Math.round(h * dpr);
    this.ctx.setTransform(dpr, 0, 0, dpr, 0, 0);
    this._layout(true);
    this._rebuildGroups();
    if (this.reduced || (this.initialized && !this.running)) {
      this.renderStatic();
    }
  };

  /** Posición y tamaño de la galaxia según el tamaño de pantalla y la página. */
  Engine.prototype._layout = function (instant) {
    var w = this.w;
    var h = this.h;
    var wide = w >= 900;
    var home = document.documentElement.getAttribute("data-page") === "home";
    var bx = wide ? (home ? 0.68 : 0.62) : 0.5;
    var by = wide ? 0.5 : home ? 0.4 : 0.46;
    this.targetCx = w * bx;
    this.targetCy = h * by;
    this.unit = Math.min(w * (wide ? 0.3 : 0.46), h * 0.52) * (wide ? (home ? 1.32 : 1.15) : 1);
    if (instant || this.cx === undefined) {
      this.cx = this.targetCx;
      this.cy = this.targetCy;
    }
  };

  Engine.prototype._makeSprite = function () {
    var size = 64;
    var cv = document.createElement("canvas");
    cv.width = size;
    cv.height = size;
    var c = cv.getContext("2d");
    var g = c.createRadialGradient(size / 2, size / 2, 0, size / 2, size / 2, size / 2);
    g.addColorStop(0, "rgba(255,255,255,1)");
    g.addColorStop(0.18, "rgba(255,255,255,0.55)");
    g.addColorStop(0.5, "rgba(255,255,255,0.12)");
    g.addColorStop(1, "rgba(255,255,255,0)");
    c.fillStyle = g;
    c.fillRect(0, 0, size, size);
    this.sprite = cv;
    this.spriteTint = document.createElement("canvas");
    this.spriteTint.width = size;
    this.spriteTint.height = size;
    this._tintSprite();
  };

  Engine.prototype._tintSprite = function () {
    var size = this.sprite.width;
    var c = this.spriteTint.getContext("2d");
    c.clearRect(0, 0, size, size);
    c.globalCompositeOperation = "source-over";
    c.drawImage(this.sprite, 0, 0);
    c.globalCompositeOperation = "source-in";
    c.fillStyle = P.css(this.current.star, 1, this.hueShift);
    c.fillRect(0, 0, size, size);
    c.globalCompositeOperation = "source-over";
  };

  /* ---------------- Eventos ---------------- */

  Engine.prototype._bind = function () {
    var self = this;
    var resizeTimer = 0;
    var downInfo = null;
    var lastTap = 0;

    this._onResize = function () {
      clearTimeout(resizeTimer);
      resizeTimer = setTimeout(function () {
        self._resize();
      }, 120);
    };
    global.addEventListener("resize", this._onResize);

    this._onMove = function (e) {
      var p = self.pointer;
      p.tx = e.clientX;
      p.ty = e.clientY;
      if (!p.active) {
        p.x = e.clientX;
        p.y = e.clientY;
      }
      p.active = true;
    };
    global.addEventListener("pointermove", this._onMove, { passive: true });

    this._onDown = function (e) {
      if (e.target.closest && e.target.closest(INTERACTIVE)) {
        return;
      }
      var p = self.pointer;
      p.tx = p.x = e.clientX;
      p.ty = p.y = e.clientY;
      p.active = true;
      downInfo = { x: e.clientX, y: e.clientY, time: performance.now(), moved: false };
      clearTimeout(self._holdTimer);
      self._holdTimer = setTimeout(function () {
        if (downInfo) {
          p.pressed = true;
        }
      }, 320);
    };
    global.addEventListener("pointerdown", this._onDown, { passive: true });

    this._onUp = function (e) {
      clearTimeout(self._holdTimer);
      var p = self.pointer;
      if (!downInfo) {
        if (e.pointerType === "touch") {
          p.active = false;
        }
        return;
      }
      var info = downInfo;
      downInfo = null;
      if (p.pressed) {
        p.pressed = false;
        self.burst(p.x, p.y, 1.8);
        self.nextPalette();
      } else if (Math.hypot(e.clientX - info.x, e.clientY - info.y) < 10) {
        var now = performance.now();
        self.burst(e.clientX, e.clientY, 1);
        if (now - lastTap < 340) {
          self.nextShape();
        } else {
          self.nextPalette();
        }
        lastTap = now;
      }
      if (e.pointerType === "touch") {
        p.active = false;
      }
    };
    global.addEventListener("pointerup", this._onUp, { passive: true });

    this._onCancel = function () {
      clearTimeout(self._holdTimer);
      downInfo = null;
      self.pointer.pressed = false;
      self.pointer.active = false;
    };
    global.addEventListener("pointercancel", this._onCancel, { passive: true });
    document.documentElement.addEventListener("mouseleave", this._onCancel);

    this._onVisibility = function () {
      if (document.hidden) {
        self._stopLoop();
      } else if (!self.userPaused && !self.reduced) {
        self._startLoop();
      }
    };
    document.addEventListener("visibilitychange", this._onVisibility);
  };

  /* ---------------- API pública ---------------- */

  Engine.prototype.start = function () {
    if (this.reduced) {
      this.renderStatic();
      return;
    }
    this._startLoop();
  };

  Engine.prototype._startLoop = function () {
    if (this.running) {
      return;
    }
    this.running = true;
    this.last = performance.now();
    var self = this;
    this._frameBound = function (now) {
      self._frame(now);
    };
    this._raf = global.requestAnimationFrame(this._frameBound);
  };

  Engine.prototype._stopLoop = function () {
    this.running = false;
    global.cancelAnimationFrame(this._raf);
  };

  /** Pausa o reanuda la animación (control de accesibilidad). Devuelve true si quedó en pausa. */
  Engine.prototype.togglePause = function () {
    this.userPaused = !this.userPaused;
    if (this.userPaused) {
      this._stopLoop();
    } else if (!this.reduced) {
      this._startLoop();
    }
    return this.userPaused;
  };

  Engine.prototype.setPalette = function (index) {
    this.paletteIndex = ((index % P.list.length) + P.list.length) % P.list.length;
    var palette = P.list[this.paletteIndex];
    this.target = P.clone(palette);
    this.paletteDirty = true;
    this.onPalette(palette, this.paletteIndex);
    if (this.reduced || !this.running) {
      // Sin animación en curso: cambia el color al instante y redibuja
      this.current = P.clone(palette);
      this.paletteDirty = false;
      this._tintSprite();
      if (this.reduced) {
        this.renderStatic();
      } else {
        this._paint(0.0001, false);
      }
    }
  };

  Engine.prototype.nextPalette = function () {
    this.setPalette(this.paletteIndex + 1);
  };

  Engine.prototype.setShape = function (index) {
    var len = S.list.length;
    var next = ((index % len) + len) % len;
    if (next === this.shapeIndex) {
      return;
    }
    this.shapeIndex = next;
    this.onShape(S.list[next], next);
    if (this.reduced || !this.running) {
      this.renderStatic();
      return;
    }
    // Impulso de "warp": las partículas salen disparadas y se reagrupan
    for (var i = 0; i < this.count; i++) {
      var dx = this.x[i] - this.cx;
      var dy = this.y[i] - this.cy;
      var d = Math.hypot(dx, dy) + 1;
      var kick = 90 + Math.random() * 260;
      this.vx[i] += (dx / d) * kick;
      this.vy[i] += (dy / d) * kick;
    }
  };

  Engine.prototype.nextShape = function () {
    this.setShape(this.shapeIndex + 1);
  };

  /** Onda de choque desde (x, y). `power` ~ 1 es un clic normal. */
  Engine.prototype.burst = function (x, y, power) {
    if (this.reduced) {
      return;
    }
    this.waves.push({ x: x, y: y, r: 0, age: 0, power: (power || 1) * 4200 });
    if (this.waves.length > 5) {
      this.waves.shift();
    }
  };

  Engine.prototype.getPalette = function () {
    return P.list[this.paletteIndex];
  };

  Engine.prototype.getShape = function () {
    return S.list[this.shapeIndex];
  };

  /* ---------------- Bucle ---------------- */

  Engine.prototype._frame = function (now) {
    if (!this.running) {
      return;
    }
    this._raf = global.requestAnimationFrame(this._frameBound);
    var dt = Math.min(0.05, (now - this.last) / 1000);
    this.last = now;
    if (dt <= 0) {
      return;
    }
    this._monitor(dt);
    this._advance(dt);
    this._paint(dt, false);
  };

  /** Si el equipo va lento, reduce partículas (máximo 3 veces). */
  Engine.prototype._monitor = function (dt) {
    this.frameTimes += dt;
    this.frameCount += 1;
    if (this.frameCount >= 90) {
      var avg = this.frameTimes / this.frameCount;
      if (avg > 0.027 && this.downgrades < 3) {
        this.downgrades += 1;
        this._rebuildGroups();
      }
      this.frameTimes = 0;
      this.frameCount = 0;
    }
  };

  Engine.prototype._advance = function (dt) {
    var p = this.pointer;
    var sy = global.scrollY || 0;
    var scrollV = Math.abs(sy - this.lastScroll) / Math.max(dt, 0.001);
    this.lastScroll = sy;
    this.scrollSpeed += (Math.min(scrollV, 3200) - this.scrollSpeed) * 0.12;
    this.timeScale = 1 + Math.min(this.scrollSpeed / 700, 3.2);
    this.t += dt * this.timeScale;

    this._layout(false);
    var follow = 1 - Math.exp(-dt * 3);
    var parX = (p.active ? (p.x / this.w - 0.5) * -26 : 0);
    var parY = (p.active ? (p.y / this.h - 0.5) * -18 : 0);
    var scrollLift = -Math.min(sy * 0.12, this.h * 0.22);
    this.cx += (this.targetCx + parX - this.cx) * follow;
    this.cy += (this.targetCy + parY + scrollLift - this.cy) * follow;

    // Puntero suavizado y su velocidad (mueve el matiz)
    var lx = p.x;
    var ly = p.y;
    p.x += (p.tx - p.x) * (1 - Math.exp(-dt * 22));
    p.y += (p.ty - p.y) * (1 - Math.exp(-dt * 22));
    var spd = Math.hypot(p.x - lx, p.y - ly) / dt;
    p.speed += (spd - p.speed) * 0.2;
    if (p.active) {
      this.hueShift += Math.min(p.speed, 2500) * 0.035 * dt;
      p.trail.push({ x: p.x, y: p.y, t: this.t });
    }
    this.hueShift -= this.hueShift * 0.5 * dt;
    while (p.trail.length && this.t - p.trail[0].t > 0.5) {
      p.trail.shift();
    }

    if (this.paletteDirty) {
      var remaining = P.approach(this.current, this.target, 1 - Math.exp(-dt * 3.2));
      if (remaining < 0.6) {
        this.paletteDirty = false;
      }
    }
    if (this.paletteDirty || Math.abs(this.hueShift) > 0.5) {
      this.spriteTick = (this.spriteTick || 0) + 1;
      if (this.spriteTick % 6 === 0) {
        this._tintSprite();
      }
    }

    // Ondas de choque
    for (var w = this.waves.length - 1; w >= 0; w--) {
      var wave = this.waves[w];
      wave.age += dt;
      wave.r += 980 * dt;
      if (wave.age > 1.7) {
        this.waves.splice(w, 1);
      }
    }

    // Brillo del núcleo según la forma
    var glowTarget = this.shapeIndex <= 1 ? 1 : 0.4;
    this.glow += (glowTarget - this.glow) * (1 - Math.exp(-dt * 2));

    // Estrella fugaz
    this.nextShoot -= dt;
    if (this.nextShoot <= 0 && !this.shooting) {
      var fromRight = Math.random() < 0.5;
      this.shooting = {
        x: fromRight ? this.w * (0.55 + Math.random() * 0.45) : this.w * Math.random() * 0.45,
        y: this.h * Math.random() * 0.35,
        vx: (fromRight ? -1 : 1) * (700 + Math.random() * 400),
        vy: 380 + Math.random() * 220,
        life: 0
      };
      this.nextShoot = 7 + Math.random() * 7;
    }
    if (this.shooting) {
      this.shooting.x += this.shooting.vx * dt;
      this.shooting.y += this.shooting.vy * dt;
      this.shooting.life += dt;
      if (this.shooting.life > 1.1) {
        this.shooting = null;
      }
    }
  };

  Engine.prototype._updateBucketColors = function () {
    for (var b = 0; b < BUCKETS; b++) {
      this.bucketColors[b] = P.css(P.sample(this.current, (b + 0.5) / BUCKETS), 1, this.hueShift);
    }
  };

  /** Dibuja un solo cuadro, sin animación (modo movimiento reducido o en pausa). */
  Engine.prototype.renderStatic = function () {
    this.t = this.t || 0;
    this._paint(0.016, true);
  };

  Engine.prototype._paint = function (dt, snap) {
    var ctx = this.ctx;
    var w = this.w;
    var h = this.h;
    var p = this.pointer;
    var t = this.t;
    ctx.setTransform(this.dpr, 0, 0, this.dpr, 0, 0);
    ctx.clearRect(0, 0, w, h);
    this._updateBucketColors();

    // --- Estrellas de fondo (paralaje con el puntero) ---
    var px = p.active ? (p.x / w - 0.5) : 0;
    var py = p.active ? (p.y / h - 0.5) : 0;
    ctx.globalCompositeOperation = "source-over";
    ctx.fillStyle = P.css(this.current.star, 1, this.hueShift * 0.3);
    for (var s = 0; s < this.stars.length; s++) {
      var st = this.stars[s];
      var sx = st.x - px * (6 + st.z * 26);
      var sy = st.y - py * (6 + st.z * 18);
      ctx.globalAlpha = (0.25 + 0.55 * st.z) * (0.7 + 0.3 * Math.sin(t * st.sp + st.ph));
      ctx.fillRect(sx, sy, st.s, st.s);
    }

    // --- Resplandor del núcleo ---
    var cx = this.cx;
    var cy = this.cy;
    ctx.globalCompositeOperation = "lighter";
    var gr = this.unit * 0.62;
    var grad = ctx.createRadialGradient(cx, cy, 0, cx, cy, gr);
    grad.addColorStop(0, P.css(this.current.core, 0.34 * this.glow, this.hueShift));
    grad.addColorStop(0.45, P.css(this.current.mid, 0.1 * this.glow, this.hueShift));
    grad.addColorStop(1, P.css(this.current.mid, 0, this.hueShift));
    ctx.globalAlpha = 1;
    ctx.fillStyle = grad;
    ctx.fillRect(cx - gr, cy - gr, gr * 2, gr * 2);

    // --- Partículas ---
    var shape = S.list[this.shapeIndex];
    var home = shape.home;
    var tmp = this._tmp;
    var unit = this.unit * (shape.fit || 1);
    var damp = Math.exp(-dt * 7);
    var X = this.x;
    var Y = this.y;
    var VX = this.vx;
    var VY = this.vy;
    var seeds = this.seeds;
    var pressed = p.pressed;
    var pa = p.active && !snap;
    var R = pressed ? 290 : 150;
    var R2 = R * R;
    var attr = pressed ? 9000 : 1100;
    var swirl = pressed ? 2600 : 3200;
    var waves = this.waves;
    var nw = snap ? 0 : waves.length;
    var pointerX = p.x;
    var pointerY = p.y;

    for (var b = 0; b < BUCKETS; b++) {
      ctx.fillStyle = this.bucketColors[b];
      var group = this.groups[b];
      var cnt = this.groupCount[b];
      for (var k = 0; k < cnt; k++) {
        var i = group[k];
        var sd = seeds[i];
        home(sd, t, tmp);
        var hx = cx + tmp.x * unit;
        var hy = cy + tmp.y * unit;
        if (snap) {
          X[i] = hx;
          Y[i] = hy;
          VX[i] = 0;
          VY[i] = 0;
        } else {
          var xi = X[i];
          var yi = Y[i];
          var ax = (hx - xi) * sd.k;
          var ay = (hy - yi) * sd.k;
          if (pa) {
            var dx = pointerX - xi;
            var dy = pointerY - yi;
            var d2 = dx * dx + dy * dy;
            if (d2 < R2) {
              var d = Math.sqrt(d2) + 0.5;
              var q = 1 - d / R;
              q *= q;
              ax += (dx / d) * attr * q - (dy / d) * swirl * q;
              ay += (dy / d) * attr * q + (dx / d) * swirl * q;
            }
          }
          for (var wi = 0; wi < nw; wi++) {
            var wv = waves[wi];
            var wx = xi - wv.x;
            var wy = yi - wv.y;
            var wd = Math.sqrt(wx * wx + wy * wy) + 0.5;
            var off = Math.abs(wd - wv.r);
            if (off < 90) {
              var fall = (1 - off / 90) * (1 - wv.age / 1.7) * wv.power;
              ax += (wx / wd) * fall;
              ay += (wy / wd) * fall;
            }
          }
          VX[i] = (VX[i] + ax * dt) * damp;
          VY[i] = (VY[i] + ay * dt) * damp;
          X[i] = xi + VX[i] * dt;
          Y[i] = yi + VY[i] * dt;
        }
        var z = tmp.z;
        var size = sd.size * (0.55 + 0.9 * z);
        ctx.globalAlpha = (0.28 + 0.72 * z) * (0.78 + 0.22 * Math.sin(t * sd.tw + sd.ph));
        ctx.fillRect(X[i] - size * 0.5, Y[i] - size * 0.5, size, size);
      }
    }

    // --- Estrellas brillantes con halo ---
    ctx.globalAlpha = 0.9;
    for (var bi = 0; bi < this.bright.length; bi++) {
      var idx = this.bright[bi];
      var bs = 7 + seeds[idx].size * 5 + 2 * Math.sin(t * seeds[idx].tw + seeds[idx].ph);
      ctx.drawImage(this.spriteTint, X[idx] - bs, Y[idx] - bs, bs * 2, bs * 2);
    }

    // --- Ondas de choque visibles ---
    ctx.globalCompositeOperation = "lighter";
    ctx.lineWidth = 2;
    for (var wj = 0; wj < nw; wj++) {
      var wave = waves[wj];
      ctx.globalAlpha = Math.max(0, 0.5 * (1 - wave.age / 1.7));
      ctx.strokeStyle = P.css(this.current.arm, 1, this.hueShift);
      ctx.beginPath();
      ctx.arc(wave.x, wave.y, wave.r, 0, TAU);
      ctx.stroke();
    }

    // --- Estela y brillo del puntero ---
    if (!snap && p.active) {
      var trail = p.trail;
      ctx.lineCap = "round";
      for (var ti = 1; ti < trail.length; ti++) {
        var age = (t - trail[ti].t) / 0.5;
        ctx.globalAlpha = Math.max(0, 0.55 * (1 - age));
        ctx.lineWidth = 1 + 4 * (1 - age);
        ctx.strokeStyle = P.css(this.current.arm, 1, this.hueShift);
        ctx.beginPath();
        ctx.moveTo(trail[ti - 1].x, trail[ti - 1].y);
        ctx.lineTo(trail[ti].x, trail[ti].y);
        ctx.stroke();
      }
      var pr = pressed ? 170 : 96;
      var pg = ctx.createRadialGradient(p.x, p.y, 0, p.x, p.y, pr);
      pg.addColorStop(0, P.css(this.current.core, pressed ? 0.42 : 0.24, this.hueShift));
      pg.addColorStop(1, P.css(this.current.mid, 0, this.hueShift));
      ctx.globalAlpha = 1;
      ctx.fillStyle = pg;
      ctx.fillRect(p.x - pr, p.y - pr, pr * 2, pr * 2);
    }

    // --- Estrella fugaz ---
    var sh = this.shooting;
    if (sh && !snap) {
      var fade = Math.sin(Math.min(1, sh.life / 1.1) * Math.PI);
      var tail = 0.16;
      var lg = ctx.createLinearGradient(sh.x, sh.y, sh.x - sh.vx * tail, sh.y - sh.vy * tail);
      lg.addColorStop(0, "rgba(255,255,255," + (0.9 * fade).toFixed(3) + ")");
      lg.addColorStop(1, "rgba(255,255,255,0)");
      ctx.globalAlpha = 1;
      ctx.strokeStyle = lg;
      ctx.lineWidth = 1.6;
      ctx.beginPath();
      ctx.moveTo(sh.x, sh.y);
      ctx.lineTo(sh.x - sh.vx * tail, sh.y - sh.vy * tail);
      ctx.stroke();
    }

    ctx.globalAlpha = 1;
    ctx.globalCompositeOperation = "source-over";
  };

  global.PortfolioGalaxy = {
    create: function (canvas, options) {
      return new Engine(canvas, options);
    }
  };
})(window);
