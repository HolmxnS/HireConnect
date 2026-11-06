# Siguiente etapa: recopilar ejemplos con trazabilidad

Auditoría del 5 de noviembre de 2026: 216 aceptadas y 223 rechazadas, ninguna
con captura del perfil disponible en la base consultada. Los 439 resultados
históricos siguen sirviendo para el modelo experimental. No se reconstruyen
como si fueran observaciones capturadas antes de conocer la decisión.

## Qué registra la aplicación

Al crear una postulación guarda una copia de las variables del perfil y los
requisitos de esa vacante, fecha/hora UTC y origen `FLUJO_APLICACION`.
Cuando la empresa cambia a aceptada o rechazada, guarda fecha/hora del resultado.
La captura inicial no se modifica al editar después el perfil.
Los cambios de estado ya están restringidos por el controlador a la empresa
propietaria de la vacante. El origen del flujo no certifica autenticidad.

## Uso normal para recopilar datos

1. Desplegar esta versión en el servicio que utilizan postulantes y empresas.
2. Los candidatos completan sus perfiles con sus habilidades, estudios y
   experiencia actuales, y se postulan a las vacantes que les interesan.
3. Cada empresa registra su decisión sobre esas postulaciones. Las pendientes,
   en revisión y entrevistas no se cuentan como rechazos. No asignar resultados
   basándose únicamente en el porcentaje del modelo.
4. Revisar el informe de calidad. Para aprender la influencia de habilidades se
   necesitan resultados en distintas coincidencias, no solamente 100%. Para
   educación hacen falta ejemplos distintos; la variable actual sigue siendo
   presencia de estudios, no nivel ni área educativa.

No crear etiquetas falsas, borrar habilidades de perfiles ajenos ni alterar
rechazos/aceptaciones históricos para producir variedad.

## Comandos desde la raíz

```sh
sh ml/run.sh audit
sh ml/run.sh train --captured-only
```

La auditoría no escribe en MongoDB ni cambia el artefacto. Reporta:

- Capturas totales, pendientes y resueltas.
- Ejemplos con origen y fechas consistentes y razones de exclusión.
- Distribuciones de coincidencia de habilidades y presencia de educación.
- Si existen ambas clases y fechas que permitan evaluar sin compartir candidatos.

El mínimo técnico de 50 ejemplos y 5 por clase en cada partición no garantiza
calidad estadística ni elimina la necesidad de representatividad y calibración.
Si todavía no se cumplen las condiciones, el entrenamiento estricto se detiene
antes de escribir y la API conserva el modelo actual. El arranque experimental
con históricos continúa funcionando.

El entrenamiento estricto usa el día de la captura para ubicar la postulación.
Solo utiliza en entrenamiento decisiones que ya se conocían antes de la fecha
de corte; decisiones posteriores no se filtran hacia el pasado.

La presencia de trazabilidad no basta para quitar la etiqueta experimental.
Se requiere comprobar procedencia, variedad y desempeño sobre resultados nuevos.
