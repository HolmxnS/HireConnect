# Asistente HireConnect

El asistente está disponible para postulantes, incluidos los apartados de entrevistas y proyectos. Usa los campos actuales del perfil y consultas acotadas de vacantes; no modifica cuentas, postulaciones ni invitaciones.

## Funciones

- Tarjetas de ofertas abiertas con enlace, empresa, modalidad, salario y coincidencias exactas de habilidades. Se pueden pedir ofertas remotas o sin experiencia.
- Comparación de requisitos con la vacante seleccionada en la lista o abierta en su página de detalle.
- Revisión de campos del perfil y borrador de presentación profesional. No se lee ni se analiza el archivo del CV.
- Orientación sobre entrevistas, método STAR, proyectos freelance y navegación de postulaciones.
- Respuestas generadas por Gemini para orientación abierta cuando el proveedor está disponible; orientación local cuando no está configurado, falla o no devuelve texto.

## Interfaz

Panel ampliable en escritorio, adaptación móvil y a los temas claro/oscuro, sugerencias, texto multilínea, copiar respuesta, detener espera y reintentar. Nueva conversación cancela la espera e ignora respuestas atrasadas. Escape cierra el panel y devuelve el foco al botón.

El historial conserva hasta 24 mensajes en `sessionStorage`, separado por identificador del postulante. No se guarda en MongoDB. No se importa el historial del antiguo almacenamiento compartido entre cuentas.

## API y configuración

`POST /postulante/chat` recibe `mensaje`, `historial` y el `vacanteId` opcional. Devuelve `respuesta`, `vacantes`, `acciones`, `sugerencias` y `modo` (`orientacion` o `ia`).

- Mensajes de hasta 1.000 caracteres; historial de hasta 12 entradas válidas, acotadas a 3.000 caracteres cada una.
- Hasta 12 solicitudes por minuto y sesión. Los errores HTTP se muestran con opción de reintento.
- Las recomendaciones consultan hasta 80 candidatos en MongoDB y presentan hasta 4 ofertas; no representan un listado exhaustivo ni un porcentaje predictivo de compatibilidad.
- El texto y los enlaces se construyen con elementos DOM. Solo se admiten destinos internos conocidos; no se ejecuta HTML recibido del modelo o del historial.
- El contexto profesional enviado al proveedor excluye nombre, correo, teléfono, identificación, fecha de nacimiento y archivos del CV. Los mensajes escritos en la conversación sí se envían cuando se utiliza Gemini.
- Se mantienen `GEMINI_API_KEY`, `GEMINI_MODEL` y `GEMINI_MODEL_FALLBACK`. Los fallos del proveedor registran tipo o código HTTP, sin volcar la clave, el perfil o la respuesta completa.

La configuración de modelos puede consultarse en la [documentación oficial de Gemini](https://ai.google.dev/gemini-api/docs/models).

## Verificación realizada

Compilación Maven; pruebas con repositorios y proveedor simulados para vigencia de ofertas, coincidencias exactas, filtros, contexto, respuestas locales, contenido del proveedor, ausencia de datos personales en el contexto, autenticación, historial mal formado y límite de solicitudes. Pruebas en Chrome de escritorio y móvil para renderizado seguro, aislamiento del historial, reintentos, nueva conversación durante una solicitud y navegación por teclado.

No se enviaron datos reales de usuarios a Gemini durante estas pruebas.
