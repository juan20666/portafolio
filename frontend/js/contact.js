/* =====================================================================
   contact.js — formulario de contacto
   - Con apiBaseUrl configurada: envía JSON a la API Spring Boot.
   - Sin ella: abre la aplicación de correo (mailto) con el mensaje listo.
   - Tolera el "arranque en frío" de los servidores gratuitos: precalienta
     la API al abrir la página y reintenta de forma segura (idempotente).
   ===================================================================== */
(function () {
  "use strict";

  var CONFIG = window.SITE_CONFIG || {};
  var form = document.getElementById("contactForm");
  if (!form) {
    return;
  }

  var statusEl = document.getElementById("formStatus");
  var submitBtn = document.getElementById("submitBtn");
  var API = (CONFIG.apiBaseUrl || "").replace(/\/+$/, "");
  var ENDPOINT = API + "/api/v1/contact-messages";
  var REQUEST_TIMEOUT_MS = 75000;
  var RETRY_STATUS = [502, 503, 504];
  var MAX_ATTEMPTS = 3;

  var submissionId = newId();

  function newId() {
    if (window.crypto && window.crypto.randomUUID) {
      return window.crypto.randomUUID();
    }
    // Respaldo para navegadores antiguos (UUID v4 con Math.random)
    return "xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx".replace(/[xy]/g, function (c) {
      var r = (Math.random() * 16) | 0;
      return (c === "x" ? r : (r & 0x3) | 0x8).toString(16);
    });
  }

  function setStatus(kind, html) {
    statusEl.className = "form-status is-" + kind;
    statusEl.innerHTML = html;
  }

  function sleep(ms) {
    return new Promise(function (resolve) {
      setTimeout(resolve, ms);
    });
  }

  /* ---------- Precalentamiento: despierta el servidor mientras escribes ---------- */
  if (API) {
    try {
      fetch(API + "/actuator/health", { mode: "no-cors", cache: "no-store" }).catch(function () {});
    } catch (e) {
      /* sin conexión: no pasa nada */
    }
  }

  /* ---------- Validación en el navegador (la API vuelve a validar todo) ---------- */

  var RULES = {
    name: function (v) {
      return v.length < 2 || v.length > 120 ? "Escribe tu nombre (entre 2 y 120 caracteres)." : "";
    },
    email: function (v) {
      return /^[^\s@]+@[^\s@]+\.[^\s@]{2,}$/.test(v) && v.length <= 254 ? "" : "Escribe un correo válido.";
    },
    message: function (v) {
      return v.length < 10 || v.length > 2000 ? "El mensaje debe tener entre 10 y 2000 caracteres." : "";
    },
    consent: function (checked) {
      return checked ? "" : "Debes autorizar el tratamiento de tus datos para poder responderte.";
    }
  };

  function showError(field, message) {
    var holder = form.querySelector('[data-field="' + field + '"]');
    var slot = form.querySelector('[data-error-for="' + field + '"]');
    if (holder) {
      holder.classList.toggle("has-error", !!message);
    }
    if (slot) {
      slot.textContent = message || "";
    }
    var input = form.elements[field];
    if (input && input.setAttribute) {
      input.setAttribute("aria-invalid", message ? "true" : "false");
    }
  }

  function clearErrors() {
    ["name", "email", "reason", "message", "consent"].forEach(function (f) {
      showError(f, "");
    });
  }

  function readValues() {
    return {
      name: form.elements.name.value.trim(),
      email: form.elements.email.value.trim(),
      reason: form.elements.reason.value,
      message: form.elements.message.value.trim(),
      consent: form.elements.consent.checked,
      website: form.elements.website.value
    };
  }

  function validate(values) {
    var errors = {};
    ["name", "email", "message", "consent"].forEach(function (field) {
      var msg = RULES[field](values[field]);
      if (msg) {
        errors[field] = msg;
      }
    });
    return errors;
  }

  function focusFirstError(errors) {
    var order = ["name", "email", "reason", "message", "consent"];
    for (var i = 0; i < order.length; i++) {
      if (errors[order[i]]) {
        var el = form.elements[order[i]];
        if (el && el.focus) {
          el.focus();
        }
        return;
      }
    }
  }

  /* ---------- Respaldo por correo ---------- */

  function mailtoHref(values) {
    var subject = "Contacto desde tu portafolio: " + values.name;
    var body = values.message + "\n\n— " + values.name + " (" + values.email + ")";
    return "mailto:" + CONFIG.email + "?subject=" + encodeURIComponent(subject) + "&body=" + encodeURIComponent(body);
  }

  function fallbackLink(values) {
    return '<a href="' + mailtoHref(values) + '">escríbeme directamente por correo</a>';
  }

  /* ---------- Envío a la API ---------- */

  function postOnce(payload) {
    var controller = typeof AbortController !== "undefined" ? new AbortController() : null;
    var timer = controller ? setTimeout(function () { controller.abort(); }, REQUEST_TIMEOUT_MS) : 0;
    return fetch(ENDPOINT, {
      method: "POST",
      headers: { "Content-Type": "application/json", Accept: "application/json, application/problem+json" },
      body: JSON.stringify(payload),
      signal: controller ? controller.signal : undefined
    }).then(
      function (res) {
        clearTimeout(timer);
        return res;
      },
      function (err) {
        clearTimeout(timer);
        throw err;
      }
    );
  }

  async function sendWithRetry(payload) {
    var lastError = null;
    for (var attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
      try {
        var res = await postOnce(payload);
        if (RETRY_STATUS.indexOf(res.status) === -1) {
          return res;
        }
        lastError = new Error("HTTP " + res.status);
      } catch (err) {
        lastError = err;
      }
      if (attempt < MAX_ATTEMPTS) {
        await sleep(2500 * attempt);
      }
    }
    throw lastError;
  }

  async function readProblem(res) {
    try {
      return await res.json();
    } catch (e) {
      return null;
    }
  }

  form.addEventListener("submit", async function (event) {
    event.preventDefault();
    clearErrors();
    var values = readValues();
    var errors = validate(values);
    if (Object.keys(errors).length) {
      Object.keys(errors).forEach(function (f) {
        showError(f, errors[f]);
      });
      setStatus("error", "Revisa los campos marcados.");
      focusFirstError(errors);
      return;
    }

    // Sin API configurada: abre el correo del visitante
    if (!API) {
      setStatus("info", "Abriendo tu aplicación de correo con el mensaje listo para enviar…");
      window.location.href = mailtoHref(values);
      return;
    }

    submitBtn.disabled = true;
    form.setAttribute("aria-busy", "true");
    setStatus("info", "Enviando…");
    var slowNotice = setTimeout(function () {
      setStatus("info", "El servidor gratuito estaba dormido y está despertando. Puede tardar hasta un minuto…");
    }, 4000);

    var payload = {
      submissionId: submissionId,
      name: values.name,
      email: values.email,
      reason: values.reason,
      message: values.message,
      consent: values.consent,
      website: values.website
    };

    try {
      var res = await sendWithRetry(payload);
      if (res.status === 201) {
        setStatus("ok", "¡Mensaje enviado! Gracias por escribirme, te responderé lo antes posible.");
        form.reset();
        clearErrors();
        submissionId = newId();
      } else if (res.status === 422) {
        var problem = await readProblem(res);
        var list = (problem && problem.errors) || [];
        list.forEach(function (e) {
          showError(e.field, e.message);
        });
        setStatus("error", "Hay datos por corregir.");
        var byField = {};
        list.forEach(function (e) {
          byField[e.field] = true;
        });
        focusFirstError(byField);
      } else if (res.status === 429) {
        setStatus("error", "Enviaste varios mensajes seguidos. Espera un minuto e inténtalo de nuevo.");
      } else {
        setStatus("error", "No pude enviar el mensaje en este momento. Puedes " + fallbackLink(values) + ".");
      }
    } catch (err) {
      setStatus("error", "No hubo respuesta del servidor. Puedes " + fallbackLink(values) + ".");
    } finally {
      clearTimeout(slowNotice);
      submitBtn.disabled = false;
      form.removeAttribute("aria-busy");
    }
  });
})();
