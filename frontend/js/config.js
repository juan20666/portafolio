/* =====================================================================
   config.js — ÚNICO archivo que necesitas editar para conectar el sitio
   ===================================================================== */
window.SITE_CONFIG = Object.freeze({
  // URL pública de tu API en Render/Railway, SIN barra final.
  // Ejemplo: "https://portafolio-api.onrender.com"
  // Mientras esté vacía, el formulario de contacto abre tu aplicación de correo.
  apiBaseUrl: "",

  // Opcionales: déjalos vacíos y el sitio oculta el enlace automáticamente.
  githubUrl: "", // Ejemplo: "https://github.com/tu-usuario"
  githubRepoUrl: "", // Ejemplo: "https://github.com/tu-usuario/portafolio"
  cvUrl: "", // Ejemplo: "docs/CV-Juan-Pablo-Correa.pdf" (sube el PDF a frontend/docs/)

  // Datos de contacto (también están escritos en las páginas)
  email: "juanpablo0021929@gmail.com",
  linkedinUrl: "https://www.linkedin.com/in/juan-pablo-c-523a3b2a9/"
});
