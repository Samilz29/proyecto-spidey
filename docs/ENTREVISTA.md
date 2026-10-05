# Proyecto Spidey: guía para una charla técnica

Guía de estudio de Samuel. Estado revisado el 5 de octubre de 2026 contra el código y el despliegue. No es un guion para fingir experiencia.

## Lo primero: hablar con honestidad

Una forma de presentarlo, ajustándola a lo que de verdad hayas hecho:

> "Es un diario de entrenamiento para guardar rutinas y sesiones. La implementación y el despliegue se prepararon con ayuda de un asistente de IA. Lo uso como proyecto para aprender. Puedo explicar el recorrido de una petición y las decisiones que he estudiado; lo que todavía no domino, lo digo y lo reviso en el código."

No digas "lo programé todo yo", "domino Spring Security" o "es completamente seguro" si no puedes sostenerlo. Pedir ayuda no es lo mismo que entender lo que recibes. Esta guía explica el código, no certifica tus conocimientos.

Si te preguntan algo que no sabes:

> "Esa parte la preparé con ayuda y todavía no la domino. Sé para qué sirve; para explicar el detalle abriría este archivo y lo comprobaría."

## Explicación de 30 segundos

> "Angular muestra las rutinas, el calendario y el progreso. Spring Boot expone una API REST, valida los datos y consulta SQL con JDBC. Cada cuenta tiene sus propios datos. El navegador conserva una cookie de sesión, no la contraseña. En producción el frontend está en Vercel, el backend en Render y los datos en PostgreSQL de Neon. El OCR de las fotos se hace en el navegador y genera borradores que el usuario revisa. Es una app pequeña, no un servicio preparado para miles de usuarios."

## Recorrido de un entrenamiento

1. Pulsas "Comenzar misión". Angular prepara las series y guarda un borrador en el navegador.
2. Escribes pesos y marcas las series terminadas.
3. Al guardar, el navegador envía JSON a `POST /api/workouts`, con cookie de sesión y token CSRF.
4. Vercel reenvía `/api` a Spring Boot en Render.
5. Spring Security verifica sesión y CSRF. El controlador valida los campos.
6. El servicio guarda sesión y series dentro de una transacción SQL en Neon.
7. Angular vuelve a pedir `/api/state` y muestra el historial actualizado.

"Transacción" significa que un grupo de escrituras se confirma junto; si falla una, se revierte para no dejar una sesión a medias.

## 1. Spring Boot + Angular

**Qué es.** Spring Boot arranca el servidor Java y configura sus componentes. Angular organiza la pantalla, los formularios y las llamadas a la API. REST aquí significa rutas HTTP que intercambian JSON; no es una arquitectura de microservicios.

**Por qué encaja.** Separa presentación de reglas y datos. Permite practicar Java, TypeScript, HTTP y SQL en una app útil. El servidor decide qué se acepta, aunque el navegador también valide para ayudar al usuario.

**Alternativas.** Thymeleaf para renderizar páginas desde Java; React o Vue para la interfaz; otro backend. Servir Angular desde el JAR simplifica el despliegue y ya está previsto en el Dockerfile.

**Límites reales.** Es un monolito de backend, con un componente Angular grande en `main.ts`. No usa NgRx, rutas Angular ni una separación detallada en muchos componentes. Sería mejor dividirlo al crecer. Las versiones están fijadas, pero eso no demuestra que no tengan fallos.

**Pregunta: "¿Por qué no microservicios?"**

> "Dos usuarios y un dominio pequeño no justifican varios servicios, comunicación entre ellos y más despliegues."

**Pregunta: "¿Frontend y backend no son ya microservicios?"**

> "No. Separar la interfaz de una API no divide el dominio en servicios independientes."

**Mira:** `frontend/src/main.ts`, `app.html`, `GymController.java`, `GymService.java`, `pom.xml`, `package.json`.

## 2. JDBC, modelo SQL y transacciones

**Qué es.** `JdbcTemplate` ejecuta SQL con parámetros. Hay tablas para usuarios, rutinas, ejercicios, sesiones, series y planes. Las claves foráneas unen los registros y evitan referencias sin padre.

**Por qué encaja.** El modelo es pequeño; SQL explícito facilita ver qué se lee y quién es propietario. El peso se guarda con `DECIMAL(7,2)` y se maneja con `BigDecimal`, no con un número binario aproximado en Java.

**Alternativas.** JPA/Hibernate para mapear objetos; jOOQ para consultas tipadas. Flyway o Liquibase para llevar cambios de esquema versionados.

**Límites reales.** `schema.sql` crea tablas con `IF NOT EXISTS`; no es un sistema de migraciones. Al añadir una columna no basta con cambiar ese archivo. `/state` carga todo el historial y hace consultas por rutina/sesión: con muchos datos harían falta paginación y menos consultas.

**Pregunta: "¿Cómo evitas inyección SQL?"**

> "Los valores van como parámetros `?`, no pegados al texto de la consulta. Eso separa datos de instrucciones SQL."

**Pregunta: "¿Por qué transacciones?"**

> "Una sesión incluye varias series. No quiero guardar la sesión y perder la mitad de las series si algo falla."

**Mira:** `GymService.java`, `schema.sql`, `Models.java`.

## 3. Contraseñas y BCrypt

**Qué es.** Se guarda un hash BCrypt, no la contraseña original. Al entrar, se compara la contraseña con ese hash. El factor de trabajo es 12; BCrypt incorpora una sal distinta para cada hash.

**Por qué encaja.** Usa el codificador de Spring Security y hace más costoso probar muchas contraseñas tras un robo de la base. No se inventó un algoritmo propio.

**Alternativas.** Argon2, otros codificadores mantenidos o un proveedor de identidad. La elección debe considerar recursos del servidor y requisitos del producto.

**Límites reales.** No hay recuperación de contraseña, email verificado ni MFA. Se exigen al menos 12 caracteres y un máximo de 72 bytes UTF-8; caracteres como tildes pueden ocupar varios bytes. El límite de 20 intentos por IP en 15 minutos está en memoria: se pierde al reiniciar y no es un sistema antiabuso completo. Con un proxy hay que revisar qué IP recibe realmente el servidor.

**Pregunta: "¿Está cifrada la contraseña?"**

> "No es cifrado reversible. Es un hash de contraseña: se verifica, no se descifra."

**Pregunta: "¿Por qué coste 12?"**

> "Es el valor configurado para añadir trabajo al cálculo. Lo mediría en el servidor antes de afirmar que es el mejor equilibrio."

**Mira:** `AuthController.java`, `SecurityConfig.java`, `Models.java`.

## 4. Sesiones y cookies

**Qué es.** El servidor guarda la identidad autenticada en una sesión. El navegador lleva su identificador en una cookie. El login cambia el ID de sesión para evitar conservar el mismo ID antes y después de entrar.

**Por qué encaja.** Para una web pequeña del mismo origen no hace falta gestionar JWT y su revocación. Cerrar sesión invalida la sesión del servidor.

**Alternativas.** JWT para ciertos clientes o APIs; sesiones compartidas con Spring Session y Redis si hay varias instancias. JWT no hace una app más segura por sí solo.

**Límites reales.** Las sesiones viven en memoria y caducan tras 30 minutos de inactividad. Reiniciar o reemplazar Render puede cerrar la sesión. Los datos SQL no se pierden por eso. Varias instancias no compartirían sesiones con esta configuración.

La cookie de sesión es `HttpOnly` (JavaScript no puede leerla), `SameSite=Strict` (limita envío entre sitios) y `Secure` en producción (solo HTTPS). Ninguna de ellas evita todos los ataques por separado.

**Pregunta: "¿Por qué no guardas el token en localStorage?"**

> "Uso la cookie HttpOnly para la sesión. El borrador sí está en localStorage, pero no contiene la contraseña ni sustituye la autenticación."

**Mira:** `AuthController.java`, `application.properties`, `application-prod.properties`.

## 5. CSRF, mismo origen y CORS

**Qué es.** CSRF es que otra web intente provocar una acción con la sesión de la víctima. Spring exige un token adicional para escrituras. Angular pide `/api/auth/csrf` y manda el token en una cabecera.

**Por qué encaja.** Las cookies pueden viajar automáticamente; el token añade una comprobación que una página de otro origen no debería poder obtener. El token CSRF sí es legible para el cliente; no es la cookie HttpOnly de sesión.

**Alternativas.** Cambiar el modelo de autenticación o usar las integraciones estándar del framework. No se debe desactivar CSRF solo para evitar un error 403.

**Límites reales.** CSRF no protege frente a un script malicioso que ya se ejecuta dentro de la aplicación. SameSite es una capa adicional, no una excusa para quitar el token.

**Pregunta: "¿Por qué no tienes CORS abierto?"**

> "El navegador llama a `/api` en el mismo dominio de Vercel. El proxy habla con Render por detrás. No necesito permitir cualquier origen. En desarrollo también hay un proxy."

**Pregunta: "¿Un 401 y un 403 son lo mismo?"**

> "No. En este proyecto 401 suele indicar falta de sesión; 403 puede indicar que falla CSRF. Primero miro la petición y la respuesta."

**Mira:** `SecurityConfig.java`, `main.ts` (`token` y `request`), `vercel.json`.

## 6. CSP y otras cabeceras

**Qué es.** Content Security Policy indica al navegador qué recursos y ejecución permite. Aquí limita scripts, conexiones e imágenes al mismo origen, permite Google Fonts y bloquea que otras páginas encajen la app en un marco.

**Por qué encaja.** Reduce algunas formas de cargar o ejecutar contenido no esperado. La política permite WebAssembly para Tesseract (`wasm-unsafe-eval`), no un permiso general para evaluar cualquier JavaScript.

**Alternativas.** Una política más estricta con nonces o hashes, fuentes locales y eliminación de estilos inline. Eso requiere adaptar la app y comprobar el OCR.

**Límites reales.** Se permite `unsafe-inline` en estilos. CSP no reemplaza validación, escape de contenido ni permisos. Spring envía su política cuando sirve la app; como Vercel sirve el HTML público, también necesita las cabeceras en `vercel.json`. Comprobar solo el backend no demuestra la protección del frontend.

**Pregunta: "¿Cómo la compruebas?"**

> "Miro la cabecera de la respuesta HTML en Network, pruebo las funciones y reviso errores de CSP en consola. Que no haya errores no demuestra que la política bloquee todos los ataques."

**Mira:** `SecurityConfig.java`, `vercel.json`, `frontend/tests/e2e.mjs`.

## 7. Aislamiento por usuario

**Qué es.** Las consultas filtran por el ID del usuario autenticado. No se acepta un `user_id` enviado por el cliente para decidir el propietario. Editar, borrar o planificar comprueba pertenencia.

**Por qué encaja.** Saber el ID de una rutina ajena no debe dar acceso a ella. Los UUID ayudan a identificar, pero no son un control de permisos.

**Alternativas.** Políticas Row Level Security en PostgreSQL como capa adicional, si se diseña bien la conexión y el contexto de usuario.

**Límites reales.** La autorización se aplica en el servicio, no con RLS en la base. Una ruta nueva debe repetir el patrón y probarse. El registro está abierto: cualquiera que conozca la URL puede crear su propia cuenta. No hay invitaciones limitadas a dos personas ni panel de administración. Los recursos estáticos, incluido el avatar, no requieren login.

**Pregunta: "¿Qué pasa si cambio el ID de una petición?"**

> "La operación se filtra también por mi usuario y devuelve 404 si no me pertenece. Hay una prueba E2E que intenta borrar una rutina de otra cuenta."

**Mira:** `GymController.java`, `GymService.java`, prueba Alice/Bob en `e2e.mjs`.

## 8. OCR local

**Qué es.** Tesseract.js reconoce texto de una imagen dentro del navegador usando un worker y WebAssembly. El modelo español y sus recursos se sirven desde la propia app. Después, un parser separa días y ejercicios.

**Por qué encaja.** No envía la foto a una API de OCR ni necesita una clave o coste por llamada. El usuario convierte el texto en borradores y guarda tras revisar.

**Alternativas.** Pegar texto, importar JSON o usar un OCR remoto. Un servicio remoto puede mejorar ciertos casos, pero añade envío de imágenes, coste y otra dependencia.

**Límites reales.** No es Gemini ni un modelo generativo. Puede equivocarse con columnas, fotos borrosas y números. Un móvil lento tarda más. Se aceptan imágenes de hasta 12 MB. La privacidad del OCR no significa que toda la app sea offline: las rutinas guardadas se envían al backend y las fuentes se descargan de Google Fonts.

**Pregunta: "¿Cómo sabes que no sale la foto?"**

> "El archivo se pasa al worker local. Revisaría también Network al hacer una lectura; no debería aparecer una subida de la foto. Solo se guarda la rutina que confirmo."

**Mira:** `main.ts` (`photo`), `photo-parser.mjs`, `frontend/public/ocr/`.

## 9. H2, PostgreSQL y Neon

**Qué es.** H2 es la base local por defecto; las pruebas usan H2 en memoria. El despliegue usa PostgreSQL administrado en Neon, separado del contenedor.

**Por qué encaja.** H2 simplifica el arranque local. Neon conserva datos cuando Render reinicia o reemplaza el contenedor. El disco efímero de un host no es el lugar correcto para una base que debe durar.

**Alternativas.** PostgreSQL local con Docker; otro proveedor administrado; base propia en un servidor con disco persistente y mantenimiento.

**Límites reales.** Compatibilidad con H2 no demuestra por sí sola compatibilidad con PostgreSQL. Se probó también PostgreSQL real durante el despliegue, pero la CI por defecto sigue usando H2. No hay migraciones versionadas ni una restauración de backups ensayada en este flujo. Exportar JSON no es un backup completo de toda la base y no tiene importación de historial.

**Pregunta: "¿Se pierden datos si Render se duerme?"**

> "No por ese motivo: los datos están en Neon. Puede caducar la sesión o tardar la primera petición. Eso es distinto de perder la base."

**Mira:** `pom.xml`, propiedades, `schema.sql`, `Dockerfile`.

## 10. Render, Vercel y Docker

**Qué es.** Vercel sirve archivos Angular y hace proxy de `/api`. Render ejecuta Java desde una imagen Docker. Neon aloja PostgreSQL. El Dockerfile tiene etapas de Node, Maven y Java; la imagen final ejecuta el JAR con usuario sin privilegios de root.

**Por qué encaja.** Cumple el objetivo de despliegue pequeño sin tarjeta, con datos fuera del disco temporal. El Dockerfile también incluye Angular en el JAR, así que la app puede servirse desde Render directamente.

**Alternativas.** Un único host para el JAR y una base externa; VPS con más mantenimiento; planes de pago para evitar suspensión y mejorar recursos.

**Límites reales.** Render Free puede dormir tras inactividad y la primera carga puede tardar 50 segundos o más. Los planes gratuitos tienen cuotas que pueden cambiar. Tres proveedores añaden configuración y puntos de fallo. Los despliegues automáticos no están condicionados aquí a que la CI termine en verde. Las credenciales se guardan como variables secretas, no en Git.

**Pregunta: "¿Es serverless el backend?"**

> "No. Es un proceso Spring Boot dentro de un contenedor en Render. Vercel sirve la parte estática y reenvía la API."

**Pregunta: "¿Qué problema tuvisteis al desplegar?"**

> "La raíz de Render debía ser la del repo porque allí está el Dockerfile. Además se añadió un proxy de API para mantener el mismo origen y las cookies. El despliegue se preparó con asistencia de IA."

**Mira:** `Dockerfile`, `frontend/vercel.json`, `application-prod.properties`.

## 11. Pruebas

**Qué es.** Hay pruebas Java de servicio, validación y seguridad; pruebas Node de credenciales, métricas y parser; Playwright recorre la UI contra un backend real de prueba.

**Por qué encaja.** Cada nivel detecta fallos distintos. Las funciones puras de volumen son rápidas de comprobar. Un E2E encuentra fallos de conexión, cookies y formularios que una función aislada no ve.

**Alternativas.** Testcontainers con PostgreSQL en CI; más pruebas de API; pruebas de carga y revisión de seguridad externa.

**Límites reales.** No hay un porcentaje de cobertura demostrado ni prueba de carga. El E2E normal usa H2 y servidor de desarrollo Angular. La opción `PRODUCTION=1` prueba el JAR; OCR se activa con `OCR_IMAGE`. Tener esas opciones no significa que todas se ejecuten en cada CI. La comprobación responsive detecta desbordamiento en varios anchos, pero no sustituye revisar capturas ni accesibilidad con personas.

**Pregunta: "¿Qué caso de seguridad probáis?"**

> "Sin sesión no se puede leer `/api/state`; las escrituras requieren CSRF; una segunda cuenta empieza vacía y no puede borrar una rutina de la primera."

**Pregunta: "¿Qué añadirías primero?"**

> "PostgreSQL en CI, más casos de permisos por ruta y una prueba de recuperación de datos."

**Mira:** `backend/src/test/`, `frontend/tests/`.

## 12. CI y despliegue automático

**Qué es.** GitHub Actions ejecuta el workflow al recibir un push o pull request. Instala Java 21 y Node 22, construye el JAR, pasa pruebas Java y Node, instala Chromium y ejecuta Playwright.

**Por qué encaja.** Da una comprobación repetible fuera del ordenador de quien desarrolla.

**Alternativas.** Otro sistema de CI; checks obligatorios antes de fusionar; despliegue que espere a esos checks.

**Límites reales.** Un archivo de workflow no prueba que haya pasado. Hay que abrir su ejecución en Actions. Al preparar esta guía, la ejecución del cambio de avatar `11ff39e` había terminado bien; revisa siempre la del último commit. No está configurada una puerta que impida a los hosts desplegar si falla CI.

**Pregunta: "¿CI y CD son lo mismo?"**

> "CI comprueba e integra cambios. CD automatiza su entrega o despliegue. Aquí Actions comprueba y los hosts despliegan por separado al recibir un push."

**Mira:** `.github/workflows/ci.yml`, `scripts/build.sh`, pestaña Actions.

## Antes de una entrevista: prueba sin memorizar

- Dibuja navegador -> Vercel -> Render -> Neon y explica qué guarda cada uno.
- Abre `AuthController`: localiza hash, cambio de sesión y logout.
- Abre una consulta con `WHERE user_id=?` y explica por qué importa.
- Registra una sesión y localiza su POST en Network: JSON, cookie, token y respuesta.
- Explica cómo borrar una rutina conserva su historial, porque la sesión guarda el nombre y sus propias series.
- Calcula un volumen: 40 kg × 10 reps + 45 kg × 8 reps = 760 kg·reps. No es una estimación de fuerza ni de 1RM.
- Explica tres mejoras sin venderlas como hechas: recuperación de cuentas, migraciones versionadas y paginación.
- Si no puedes explicar una sección sin leer, di que estás aprendiendo esa parte.

## Cosas que no debes prometer

No está auditada como invulnerable. No está preparada para miles de usuarios. No tiene IA conversacional. No garantiza OCR correcto. No tiene backups completos restaurados ni recuperación de contraseña. No es completamente offline. El aviso de fans no libera los derechos de Marvel/Disney ni los de la imagen personalizada. La visibilidad del repo sigue siendo privada hasta revisión y autorización.
