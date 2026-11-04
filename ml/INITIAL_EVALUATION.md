# Primera evaluación experimental — 1 de noviembre de 2026

Se consultó MongoDB sin modificar sus datos. Se encontraron 216 postulaciones
ACEPTADA y 223 RECHAZADA. Se excluyeron los otros estados. No existen capturas
históricas del perfil: para esta exploración se usaron perfiles actuales.
La auditoría posterior encontró evidencias de cargas de demostración. Los
resultados de esta evaluación no deben presentarse como validación con procesos
reales de selección.

La regresión logística aprendió con 345 ejemplos y se evaluó con 94. Fecha de
corte: 2026-10-28. Los candidatos de evaluación se excluyeron del entrenamiento.

| Medida | Resultado |
| --- | --- |
| ROC AUC | 0.7953 |
| Brier (menor es mejor) | 0.1807 |
| Brier de usar siempre la tasa de aceptación de entrenamiento | 0.2523 |
| Log loss | 0.5243 |
| Precisión con umbral 0.5 | 0.8378 |
| Recall con umbral 0.5 | 0.6078 |

ROC AUC no significa porcentaje de aciertos ni valida por sí sola los porcentajes
mostrados. La muestra de evaluación es pequeña y usa perfiles actuales; hace
falta validar prospectivamente y comprobar calibración con más ejemplos.

Limitación encontrada: toda la muestra de entrenamiento tiene coincidencia de
habilidades de 100%, requisitos de habilidades presentes y educación registrada.
No puede aprender el efecto de variaciones en esas variables. Las variables que
sí varían son experiencia, experiencia requerida, modalidad y contrato.
La API marca como cobertura limitada los perfiles fuera de los rangos
numéricos aprendidos. Por ejemplo, una coincidencia de 50% no se observó al
entrenar: se devuelve la estimación del modelo con una nota en pantalla,
sin inventar ejemplos ni modificar manualmente el porcentaje.

Las nuevas postulaciones guardarán una captura del perfil y la vacante al
postularse. Los registros antiguos no se rellenan como si fueran capturas reales.
El siguiente entrenamiento normal utilizará estas capturas; el modo de perfiles
actuales requiere una opción explícita y sigue siendo experimental.

El modelo generado y el informe completo se guardan en `artifacts/`, excluido
de Git. No se ha publicado la API ni sustituido el servicio remoto de Render.

## Auditoría de procedencia

Consulta de solo lectura realizada el 1 de noviembre de 2026:

- `operaciones_carga` contiene `oportunidades-demo-20261029`.
- Contiene `postulaciones-tres-por-candidato-20261029`, con 1.764 nuevas
  postulaciones y fecha 2026-10-30 01:05:10 UTC.
- Las 439 postulaciones resueltas usadas en esta evaluación tienen ObjectId
  generado el 2026-10-30 a las 01:05 UTC, el mismo minuto que ese lote.
- Las fechas declaradas de postulación de las 439 son anteriores al día de
  generación de sus ObjectId. Son fechas retrospectivas, no una secuencia de
  registros observados a medida que ocurrieron las postulaciones.
- Las 439, no solamente las 345 de entrenamiento, tienen 100% de coincidencia
  de habilidades y educación registrada.

La base incluye datos de demostración. La coincidencia temporal indica que los
439 ejemplos pertenecen probablemente al lote de carga; no hay una marca de
procedencia por registro que permita demostrar individualmente su origen.
No se ha encontrado un script de generación en el repositorio ni evidencia
independiente de que las etiquetas sean decisiones reales de empleadores.
Un ObjectId permite observar su fecha de generación, no certificar la fecha de
inserción ni la autenticidad de un proceso de selección.

Decisión: conservar el artefacto como demostración experimental del algoritmo.
Su AUC no acredita capacidad para predecir aceptaciones reales. No activar como
probabilidad validada para usuarios. No se han borrado, modificado ni completado
artificialmente registros de la base.

Para una validación real hacen falta resultados de empleadores identificados
como tales y capturas del perfil previas a la decisión, además de ejemplos con
diferentes coincidencias de habilidades. La captura nueva ya está implementada;
no debe confundirse con una certificación de procedencia ni con una corrección
de las etiquetas antiguas.

## Estado funcional

La API local quedó integrada con Java y habilitada en modo experimental.
Se verificaron nueve combinaciones de perfiles y vacantes de MongoDB mediante
HTTP: todas respondieron 200 y sus porcentajes variaron entre 41,6% y 96,6%.
También pasó la prueba del controlador Java contra Python. Esto demuestra el
funcionamiento técnico, no valida los porcentajes con resultados reales.
