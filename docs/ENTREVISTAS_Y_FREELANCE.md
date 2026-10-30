# Entrevistas y proyectos freelance

## Entrevistas

- Empresa: Postulaciones → abrir candidato → Programar entrevista. Se indican fecha y hora de Colombia (UTC−5), duración, modalidad, lugar e indicaciones.
- Postulante: Entrevistas → confirmar asistencia, solicitar otro horario con un mensaje o indicar que no asistirá.
- La empresa puede consultar la respuesta, cambiar el horario o cancelar. Al cambiar el horario se descarta la confirmación anterior y se solicita una nueva respuesta.
- Se envían notificaciones dentro de la app al programar y cancelar. Desde el detalle de la postulación se accede a Entrevistas.
- Una invitación corresponde a una postulación. Sus estados se guardan por separado del estado de la candidatura. Las fechas se persisten como instantes UTC y se presentan en `America/Bogota`.
- No se aceptan respuestas a citas pasadas, canceladas o cuya versión cambió. `@Version` evita sobrescribir respuestas o confirmar un horario antiguo.

## Freelance

- Empresa: Proyectos → Publicar proyecto. Define título, entregables, categoría, presupuesto total en COP, plazo de 1 a 365 días y modalidad.
- Postulante: Proyectos → abrir proyecto → enviar mensaje, precio total, plazo y enlace opcional al portafolio. Puede consultar sus envíos con «Mis propuestas».
- Empresa: abrir proyecto → revisar propuestas → seleccionar profesional. El proyecto pasa de Abierto a En curso; las demás propuestas quedan como No seleccionadas.
- La empresa puede finalizar un proyecto en curso o cancelar uno abierto/en curso. No se reabren proyectos terminados.
- Después de la selección se comparte el correo de contacto entre la empresa y la persona elegida. Los postulantes no pueden consultar propuestas ajenas.
- Las propuestas se incrustan en `ProyectoFreelance` para guardar la selección y sus estados en una sola escritura con control de versión. Se admite una propuesta por postulante y un máximo de 200 por proyecto.

## Persistencia y alcance

MongoDB crea las colecciones `entrevistas` y `proyectos_freelance` al guardar sus primeros documentos. Las pantallas usan paginación de 20 elementos; el listado de proyectos excluye el contenido de las propuestas.

Esta versión no procesa pagos ni comisiones, no envía correos de entrevistas y no incluye recordatorios automáticos ni integración con calendarios. Los pagos y entregas se coordinan directamente entre las partes.

## Comprobaciones

- Compilación con `bash mvnw -DskipTests compile`.
- Pruebas de servicio con repositorios simulados: propiedad de las entrevistas, conversión horaria, versiones antiguas, confirmación, reprogramación, cancelación, enlaces válidos, propuestas duplicadas, selección única y transiciones de cierre.
- Pruebas MVC: enlace de formularios, validación antes de guardar, restricciones por rol y mensajes ante conflictos.
- Renderizado Thymeleaf de ambos roles, con y sin datos y con paginación; comprobación de que las propuestas ajenas no se incluyen en el HTML del postulante.
- Revisión en Chrome de los formularios en escritorio y móvil.

Para una prueba manual completa, crea una postulación con una cuenta de postulante y programa su entrevista desde la empresa propietaria. Para freelance, publica un proyecto desde una empresa, envía propuestas desde dos postulantes y selecciona uno; verifica que solo el elegido tenga acceso al correo de contacto de la empresa.
