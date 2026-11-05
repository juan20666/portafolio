# Guía de despliegue (gratis)

Orden recomendado: **0 → 1 → 2 → 3 → 4 → 5**. Cada paso se puede comprobar antes de seguir.

## 0. Comprueba que todo compila en tu computador

```bash
cd backend && mvn verify
```

Debe terminar en `BUILD SUCCESS`. Si algo falla, copia el mensaje completo y pídele ayuda a Claude con ese texto.
Luego prueba todo junto con `docker compose up --build` (ver README).

## 1. Base de datos PostgreSQL que no expire

La base gratuita de Render **expira a los 30 días**, así que usa una externa gratuita (por ejemplo Neon o Supabase;
revisa sus límites vigentes). Crea un proyecto y anota: **host, puerto, nombre de la base, usuario y contraseña**.
Flyway crea la tabla solo, no tienes que ejecutar SQL.

## 2. Publica la API en Render

1. Sube este repositorio a GitHub.
2. En Render: **New + → Blueprint** y elige tu repositorio (usa `render.yaml`).
3. Completa las variables que quedan en blanco:

| Variable | Valor |
|---|---|
| `DB_HOST`, `DB_NAME`, `DB_USER`, `DB_PASSWORD` | los de tu base de datos |
| `DB_PORT` | `5432` (ya viene) |
| `DB_PARAMS` | `?sslmode=require` (ya viene; cámbialo si tu proveedor no lo pide) |
| `CORS_ALLOWED_ORIGINS` | tu sitio: `https://TU-USUARIO.github.io` (sin barra final y sin ruta) |
| `RESEND_API_KEY`, `NOTIFY_TO_EMAIL` | opcionales, ver paso 3 |

4. Espera el primer despliegue (varios minutos). Comprueba:
   - `https://TU-API.onrender.com/actuator/health` → `{"status":"UP"}`
   - `https://TU-API.onrender.com/swagger-ui.html` → la documentación

> El plan gratuito **duerme la API tras 15 minutos sin visitas** y tarda cerca de un minuto en despertar.
> El sitio ya lo maneja: precalienta la API cuando abres Contacto, avisa "el servidor está despertando" y reintenta.

## 3. Avisos por correo con Resend (opcional pero recomendado)

Render gratis **bloquea el correo SMTP** (puertos 25, 465 y 587), por eso se usa la API HTTPS de Resend.

1. Crea una cuenta en <https://resend.com> y una API key.
2. En Render: `RESEND_API_KEY` = tu key y `NOTIFY_TO_EMAIL` = el correo con el que creaste la cuenta de Resend.
   (Con el remitente de pruebas `onboarding@resend.dev`, Resend normalmente solo permite enviar a ese correo;
   verifica lo vigente en su documentación. Para otro remitente tendrías que verificar un dominio.)
3. Cuando alguien escriba, te llega un correo con **Reply-To** del visitante: responder el correo responde a la persona.

Sin estas variables la API igual funciona: guarda el mensaje y deja una línea en el log.

## 4. Publica el sitio en GitHub Pages

1. El repositorio debe ser **público** (Pages gratis lo exige).
2. **Settings → Pages → Build and deployment → Source: GitHub Actions**.
3. Edita `frontend/js/config.js`:

```js
apiBaseUrl: "https://TU-API.onrender.com",   // sin barra final
githubUrl: "https://github.com/TU-USUARIO",   // opcional
githubRepoUrl: "https://github.com/TU-USUARIO/TU-REPO", // opcional
cvUrl: "docs/CV-Juan-Pablo-Correa.pdf",       // opcional: sube el PDF a frontend/docs/
```

4. Haz *commit* y *push* a `main`: la acción **Deploy portfolio to GitHub Pages** publica el sitio y rellena los
   enlaces canónicos y la imagen para compartir en redes.

## 5. Pruébalo de punta a punta

1. Abre tu sitio → Contacto → envía un mensaje de prueba.
2. Debe aparecer "¡Mensaje enviado!" (la primera vez puede tardar hasta un minuto).
3. Revisa tu correo y, si quieres, la tabla: `select * from contact_messages order by received_at desc;`

## Si algo falla

| Síntoma | Causa probable |
|---|---|
| El formulario abre tu correo en vez de enviar | `apiBaseUrl` está vacío en `config.js` |
| Error de CORS en la consola del navegador | `CORS_ALLOWED_ORIGINS` no coincide exactamente con el origen del sitio (usa `https://usuario.github.io`, sin ruta) |
| "Enviaste varios mensajes seguidos" con un solo envío | El límite está contando a todos como una sola dirección: ajusta `TRUSTED_PROXY_HOPS` (prueba `2`) |
| 500 al enviar | Mira los logs en Render; casi siempre son credenciales de la base de datos |
| No llega el correo | `RESEND_API_KEY`/`NOTIFY_TO_EMAIL` mal puestos; revisa el log por "Could not notify the owner" |
| La API tarda mucho la primera vez | Es normal en el plan gratuito (arranque en frío) |

## Datos personales

Los mensajes contienen nombre y correo de otras personas. El sitio pide autorización expresa y la tabla guarda el
momento de recepción. Si alguien pide que borres su mensaje: `delete from contact_messages where sender_email = '...';`
