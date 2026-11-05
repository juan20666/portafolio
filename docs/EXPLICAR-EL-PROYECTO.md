# Cómo explicar este proyecto en una entrevista

Un portafolio solo te sirve si puedes **explicar y defender** lo que muestra. Léelo, ejecútalo y cambia cosas hasta que
cada decisión sea tuya.

## Tu pitch de 60 segundos

> "Este portafolio es mi propia prueba técnica. El sitio es estático y está en GitHub Pages; el formulario de contacto
> llama a una API que hice en Java con Spring Boot, con arquitectura hexagonal: el dominio no conoce Spring ni la base
> de datos, y una prueba automática lo vigila. La API valida todo el formulario, guarda en PostgreSQL con migraciones y
> me avisa por correo. Pensé en los problemas reales de un servidor gratuito que se duerme: el envío es idempotente,
> reintenta y tiene respaldo por correo. En mi práctica apliqué la misma arquitectura en un módulo de trazabilidad de
> activos con 112 endpoints."

## Preguntas que te pueden hacer

**¿Por qué arquitectura hexagonal?**
Para que las reglas de negocio no dependan de frameworks ni de la base de datos. Puedo cambiar PostgreSQL por otra cosa o
Resend por otro proveedor tocando solo un adaptador, y puedo probar el dominio sin levantar Spring.

**¿Qué es un puerto y qué es un adaptador?**
Un puerto es una interfaz que define lo que la aplicación ofrece (entrada) o necesita (salida), por ejemplo
`ContactMessageRepository`. Un adaptador es la implementación concreta con una tecnología, por ejemplo
`JpaContactMessageRepository`.

**¿Qué es DDD aquí?**
`ContactMessage` es un agregado con identidad (`MessageId`); `SenderName`, `EmailAddress` y `MessageBody` son objetos de
valor que se validan solos; `ContactMessageReceived` es un evento de dominio.

**¿Por qué el `submissionId`?**
Si la red falla y el navegador reintenta, el mismo id hace que el servidor devuelva el recibo original en vez de crear un
duplicado. Es idempotencia.

**¿Cómo evitas el spam?**
Campo señuelo (honeypot) que los bots llenan, límite de peticiones por visitante y global, validación estricta y
consentimiento obligatorio.

**¿Qué pasa si el correo falla?**
El mensaje ya está guardado; la notificación corre en segundo plano, reintenta 3 veces y, si falla, solo deja una línea en
el log sin datos personales.

**¿Cómo pruebas?**
Pruebas unitarias del dominio y la aplicación con fakes hechos a mano, pruebas del contrato HTTP con MockMvc, un servidor
HTTP real para el notificador y ArchUnit para la arquitectura.

**¿Qué mejorarías?**
Un panel para leer mensajes, una cola (outbox) para los correos, límite de peticiones compartido con Redis y observabilidad
(métricas y trazas).

## Lo que debes entender antes de publicarlo

Abre estos archivos y explícalos con tus palabras (en este orden):

1. `domain/model/ContactMessage.java` — la regla central.
2. `application/service/ReceiveContactMessageService.java` — el orden de los pasos.
3. `infrastructure/adapter/in/web/ContactMessageController.java` y `ApiExceptionHandler.java` — HTTP y errores.
4. `infrastructure/adapter/out/persistence/*` — cómo se guarda.
5. `infrastructure/config/ApplicationConfig.java` — dónde se conecta todo.
6. `frontend/js/contact.js` — validación, reintentos y respaldo.

## Sé honesto sobre qué hiciste tú

Si usaste ayuda de IA para construirlo, dilo con naturalidad y explica lo que **entiendes y modificaste**. Los equipos
valoran más a quien entiende y puede explicar que a quien aparenta haberlo escrito todo.
