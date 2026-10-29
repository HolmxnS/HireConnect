# Contrato de datos para regenerar registros

Revisado el 29 de octubre de 2026 contra los modelos Java, el flujo de registro y el BSON que produce `MappingMongoConverter`. Este documento describe el contrato utilizado para generar los registros; no ejecuta inserciones.

## Estado actual

Se generaron y verificaron 400 empresas, 596 postulantes y 1.000 vacantes. Junto con los 4 administradores conservados, suman 2.000 registros principales. No se generaron postulaciones.

Las 996 cuentas nuevas tienen correos únicos terminados en `@gmail.com` y hashes BCrypt independientes, comprobados con `matches`. Sus fechas de registro varían entre el 1 de enero y el 29 de octubre de 2026.

Las imágenes suministradas se almacenaron en GridFS con una copia independiente para cada perfil, incluidos los cuatro administradores por solicitud posterior. Las credenciales y demás campos de los administradores permanecieron intactos durante esta asignación. Los archivos de imagen y sus chunks son datos auxiliares y no forman parte del total de 2.000 registros principales.

Los datos están en MongoDB: un clone o un pull del repositorio no los importa a otra base. Los informes privados y el listado de cuentas permanecen excluidos de Git.

## Estado anterior a la generación

La base configurada por la app es `hireconnect`. Después de la limpieza quedan únicamente los 4 documentos de `admins`. Se conservaron sus identificadores, nombres, apellidos, correos, roles y hashes de contraseña. Se completó `about` donde faltaba y se dejó `foto: null` en todos.

Las demás colecciones se vaciaron con `deleteMany`, sin eliminar colecciones ni índices. El respaldo de MongoDB, el recibo de verificación y los archivos locales antiguos están en `.local-backups/`, una carpeta privada excluida de Git. No incorporar esos archivos al repositorio.

## Contraseñas y autenticación

- `BeanConfig.passwordEncoder()` usa `BCryptPasswordEncoder` con configuración predeterminada. Se verificó el prefijo `$2a$10$` con una contraseña de prueba exclusivamente en memoria: BCrypt, costo 10 y sal aleatoria.
- `password` contiene el hash BCrypt, nunca la contraseña en texto ni una contraseña cifrada reversible. La misma contraseña puede producir distintos hashes por la sal.
- Aplicar `passwordEncoder.encode(...)` exactamente una vez. Comprobar con `passwordEncoder.matches(texto, hash)`; no comparar hashes generados por separado ni volver a codificar un hash existente.
- El registro con verificación de correo codifica en `LoginController` y después llama a `guardar`. `registrarEmpresa` y `registrarPostulante` también codifican: no encadenar ambos recorridos sobre la misma contraseña.
- Para futuros usuarios locales, usar contraseñas de al menos 8 caracteres y de máximo 72 bytes UTF-8. El modelo de Empresa admite un mínimo de 6; usar 8 o más mantiene compatibilidad con ambos roles.
- Roles persistidos: `ROLE_PERSONA`, `ROLE_EMPRESA`, `ROLE_ADMIN_PLATAFORMA`. Mantener el prefijo `ROLE_`.
- En nuevos registros locales: `authProvider = "LOCAL"`. No simular usuarios de Google sin configurar ese flujo.
- Los correos deben ser únicos; evitar también reutilizarlos entre las tres colecciones de cuentas para que la autenticación identifique el rol correcto.
- No cambiar ni regenerar las contraseñas de los administradores conservados.

## BSON y referencias

| Dato | Representación persistida |
| --- | --- |
| Identificadores de cuentas, vacantes y postulaciones | BSON `ObjectId`; en Java se manejan como `String` de 24 caracteres hexadecimales |
| Correos, contraseñas, identificación, RUT/NIT y teléfono | `String`; documentos y teléfonos no deben convertirse en números ni perder ceros iniciales |
| Fechas Java `LocalDate`, `LocalDateTime` o `Date` | BSON Date mediante el conversor de Spring; no cadenas de fecha |
| Salario | Número BSON compatible con `Double` |
| Roles y estados enum | Cadenas con los nombres de las constantes |
| Banderas como `perfilCompleto` o `leida` | Boolean BSON, no `"true"` / `"false"` |
| Habilidades, educación, experiencia y referencias múltiples | Arrays BSON; no JSON serializado dentro de una cadena |
| `@DBRef` | Referencia MongoDB con `$ref` y `$id: ObjectId` |
| `_class` | Discriminador que añade el conversor de Spring cuando corresponde; no inventar nombres de clase |

Crear preferiblemente las entidades Java y persistirlas con el mismo conversor de la aplicación. Así se respetan `@Field`, `@DBRef`, conversiones de identificadores y fechas. La zona horaria del proceso influye en la conversión de fechas: usar la configuración de la aplicación y verificar el día resultante al leer.

### Diferencias detectadas en datos antiguos

Algunos documentos antiguos tenían `fechaNacimiento`, `fechaInicio` y `fechaFin` en lugar de los nombres actuales con guion bajo. También existían postulaciones con identificadores sueltos donde el modelo actual usa DBRef. No copiar esos documentos antiguos como plantilla para el nuevo conjunto.

## Campos por entidad

### Administradores (`admins`)

`_id`, `nombre`, `apellido`, `email`, `password`, `rol`, `foto`, `about`.

- Los cuatro administradores existentes son la excepción de la regeneración.
- `about`: texto de hasta 500 caracteres; nombre y apellido admiten hasta 80 en el editor.
- Sus fotos se asignaron desde las imágenes de Admin suministradas. No sustituir sus credenciales.

### Empresas (`empresas`)

Campos de texto: `nombreLegal`, `rut`, `email`, `password`, `telefono`, `website`, `ubicacion`, `descripcion`, `sector` y enlaces sociales opcionales (`threads`, `instagram`, `facebook`, `x`).

- `rol = "ROLE_EMPRESA"`; `estado` es `APROBADA` o `SUSPENDIDA`.
- `fechaRegistro`: BSON Date.
- `nombreLegal`: 3–100 caracteres; descripción: máximo 300.
- RUT/NIT: 7–12 dígitos, con posible guion y dígito final según el patrón del modelo. Teléfono: cadena de 7–10 dígitos.
- `foto` y `portada`: `null` mientras no se soliciten imágenes. No inventar rutas de archivos inexistentes.
- `perfilCompleto` es booleano. Marcarlo en función de los datos realmente completados.
- `vacantes`: array de DBRef cuando se utilice; no incrustar vacantes completas en este campo.

### Postulantes (`postulantes`)

Campos de texto: `nombre`, `apellido`, `email`, `password`, `telefono`, `identificacion`, `titulacion`, `nacionalidad`, `foto` y `cv`.

- `rol = "ROLE_PERSONA"`.
- `tipoDocumento`: `CEDULA`, `PASAPORTE` u `OTRO`.
- `genero`: `MASCULINO` o `FEMENINO`.
- Fecha de nacimiento persistida: **`fecha_nacimiento`**. Debe producir una edad de al menos 18 años y ser anterior a hoy.
- `fechaRegistro`: BSON Date.
- Nombre y apellido: 2–50 caracteres; identificación: cadena de 5–15 dígitos; teléfono: cadena de 7–10 dígitos.
- `titulacion`: máximo 100 caracteres; `nacionalidad`: máximo 50.
- `habilidades`: array de cadenas sin duplicados (`Set<String>` en Java).
- `educacion`: array de documentos con `institucion`, `titulo`, **`fecha_inicio`**, **`fecha_fin`**.
- `experiencia`: array de documentos con `cargo`, `empresa`, `descripcion`, **`fecha_inicio`**, **`fecha_fin`**.
- Las fechas de educación y experiencia deben ser coherentes y respetar la validación de fechas pasadas cuando estén presentes.
- `postulaciones`: array de DBRef si se mantiene esa relación inversa.
- `foto` y `cv`: `null` si no se generan archivos reales. No marcar un CV como adjunto sin que exista su archivo.

### Vacantes (`vacantes`)

- `titulo`: máximo 100 caracteres; `descripcion`: máximo 1000.
- `salario`: `Double` no negativo.
- `habilidadesRequeridas`: **una cadena separada por comas**, a diferencia del array de habilidades de Postulante.
- `fechaPublicacion`: BSON Date; apertura y cierre se guardan como **`fecha_inicio`** y **`fecha_fin`**.
- La apertura no debe ser posterior al cierre. Apertura futura corresponde a `PENDIENTE`; convocatoria vigente a `ACTIVA`; finalizada a `CERRADA`.
- `empresa`: **documento embebido** `EmpresaResumen`, no DBRef. El conversor genera `{ _id: ObjectId, nombre: String, foto: String|null }`. El identificador debe pertenecer a una empresa existente.
- Modalidad, contrato y experiencia tienen campos String con getters/setters enum. Usar los setters enum, que guardan sus descripciones:

| Campo | Valores que guardan los setters |
| --- | --- |
| `modalidad` | `Presencial`, `Remoto`, `Hibrido` |
| `tipoContrato` | `Tiempo completo`, `Tiempo parcial`, `Indefinido` |
| `experienciaRequerida` | `Sin experiencia`, `1-2 años`, `2-3 años`, `+3 años` |

### Postulaciones (`postulaciones`)

- `postulanteId`: DBRef hacia `postulantes` con `$id` ObjectId.
- `vacanteId`: DBRef hacia `vacantes` con `$id` ObjectId.
- `fechaPostulacion`: BSON Date; `mensaje`: cadena opcional.
- `estado`: `PENDIENTE`, `EN_REVISION`, `ENTREVISTA_PENDIENTE`, `ACEPTADA` o `RECHAZADA`.
- Evitar duplicados del par postulante–vacante y referencias a registros inexistentes.

### Datos auxiliares

- `notificaciones_postulante`: identificadores `postulanteId` y `postulacionId` como **String**, textos del aviso, `fecha` BSON Date y `leida` booleano. Su índice TTL conserva la política de expiración.
- `historial_busquedas`: `email`, `texto` y `fecha` BSON Date.
- `password_reset_tokens`: no generar tokens activos como datos de prueba.
- `imagenes.files` y `imagenes.chunks`: colecciones GridFS. No insertar imágenes ficticias ni archivos sueltos sin sus metadatos.
- Los avisos de Admin de `NotificacionService` están en memoria, no en MongoDB. Reiniciar la instancia elimina esos avisos antiguos si todavía se ven en una sesión abierta.

## Orden para futuras generaciones

1. Esperar la instrucción con cantidades, sectores y contraseñas de prueba deseadas.
2. Conservar administradores; crear empresas y postulantes válidos, sin imágenes por defecto.
3. Crear vacantes con resúmenes de empresas que usen sus identificadores reales.
4. Crear postulaciones con DBRef a las entidades anteriores únicamente si se solicitan; este conjunto no incluye postulaciones.
5. Verificar unicidad de correos, hashes BCrypt, tipos BSON, fechas, estados y lectura desde la app.

No persistir `confirmPassword`, `edad` ni `totalPostulaciones`: son campos transitorios. Tampoco activar un generador automático al iniciar el proyecto.
