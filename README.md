# Portafolio de Juan Pablo Correa

Portafolio web con una **galaxia interactiva** (canvas, sin librerías) y un **backend en Java 21 + Spring Boot 4**
con arquitectura hexagonal, DDD y Clean Architecture. El formulario de contacto del sitio llama a esa API,
que valida, guarda en PostgreSQL y avisa por correo.

```
GitHub Pages (HTML + CSS + JS)  ──fetch──▶  API Spring Boot (Render)  ──▶  PostgreSQL
                                                        └──────────────▶  Resend (correo)
```

## Qué hay en el repositorio

| Carpeta | Contenido |
|---|---|
| `frontend/` | Sitio estático de 5 páginas. Se publica tal cual en GitHub Pages. Solo editas `js/config.js`. |
| `backend/` | API REST (`POST /api/v1/contact-messages`), Swagger en `/swagger-ui.html`, migraciones Flyway, pruebas. |
| `docs/` | [Arquitectura](docs/ARQUITECTURA.md), [guía de despliegue](docs/DESPLIEGUE.md) y [cómo explicar el proyecto](docs/EXPLICAR-EL-PROYECTO.md). |
| `docker-compose.yml` | Levanta todo en tu computador: sitio, API y PostgreSQL. |
| `render.yaml` | Plano para desplegar la API en Render. |
| `.github/workflows/` | CI del backend y publicación del sitio en GitHub Pages. |

## Probarlo en tu computador

**Opción A: todo con Docker** (necesitas Docker Desktop)

```bash
docker compose up --build
```

- Sitio: <http://localhost:8000> · API: <http://localhost:8080> · Swagger: <http://localhost:8080/swagger-ui.html>

**Opción B: solo el sitio** (el formulario abrirá tu correo porque no hay API configurada)

```bash
cd frontend
python -m http.server 8000      # y abre http://localhost:8000
```

**Opción C: la API sin Docker** (necesitas Java 21, Maven y un PostgreSQL local)

```bash
cd backend
mvn verify                      # compila y corre todas las pruebas
mvn spring-boot:run             # variables DB_HOST, DB_NAME, DB_USER, DB_PASSWORD (ver application.yml)
```

## Publicarlo

Sigue [docs/DESPLIEGUE.md](docs/DESPLIEGUE.md): base de datos gratuita, API en Render, correo con Resend y el sitio en
GitHub Pages. Al final pegas la URL de tu API en `frontend/js/config.js` y listo.

## Pruebas

`mvn verify` (en `backend/`) ejecuta:

- **Dominio y aplicación:** reglas del mensaje, idempotencia, anti-bots (sin Spring ni base de datos).
- **Adaptadores:** limitador de peticiones, dirección real del cliente, notificador contra un servidor HTTP real.
- **Contrato HTTP:** 201, 422, 400, 405, 429 y CORS, con MockMvc y sin base de datos.
- **Arquitectura (ArchUnit):** si alguien importa Spring dentro del dominio, el build falla.

## Decisiones importantes

- **Idempotencia:** el navegador genera un `submissionId` por formulario; si la red falla y reintenta, no se duplica el mensaje.
- **Servidores gratuitos que se duermen:** el sitio precalienta la API al abrir el formulario y reintenta con espera.
- **Correo por HTTPS (Resend), no SMTP:** Render gratis bloquea los puertos SMTP.
- **Base de datos:** la de Render gratis expira a los 30 días; usa una externa que no expire (Neon, Supabase…).
- **Privacidad:** consentimiento obligatorio, sin cookies ni rastreadores, datos mínimos, logs sin datos personales.
- **Accesibilidad:** navegación con teclado, `prefers-reduced-motion`, contraste revisado, funciona sin JavaScript (se ve todo; el formulario ofrece el correo).

## Controles de la galaxia

Mouse: mueve para atraer partículas · clic: color nuevo · doble clic: forma nueva · mantener presionado: singularidad.
Teclado: `C` color · `F` forma · `P` pausa.

## Licencias

Código: tuyo. Tipografías Inter y Lora (subconjunto latino) bajo SIL Open Font License, ver `frontend/fonts/LICENSE-FONTS.txt`.
