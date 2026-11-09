# Modelo de aceptación de HireConnect

Modo actual: pruebas con datos generados y artefacto separado. La configuración
predeterminada y su operación se describen en TEST_STAGE.md. Las limitaciones
del histórico real y su protección se mantienen como se detalla abajo.

Actualización del 7 de noviembre: la auditoría reprodujo un 99 % injustificado
por el sesgo de modalidad del histórico. El servicio ahora se abstiene de mostrar
probabilidades sin capturas trazables y variación de perfiles. Puede mostrar una
coincidencia de habilidades identificada como comparación, nunca como aceptación.
La validación predictiva sigue pendiente. Detalles en PREDICTION_AUDIT.md.

Estado al 5 de noviembre de 2026: versión operativa y comprobada técnicamente;
validación predictiva con resultados reales pendiente. La etiqueta experimental
se conserva por esa limitación, no por fallos de arranque o integración.

## Objetivo y datos

Estimar ACEPTADA frente a RECHAZADA para una combinación de candidato y vacante.
No equivale a contratación confirmada. El entrenamiento actual usa 439 resultados
históricos de la base, con evidencia de una carga de demostración. La auditoría
actual no encuentra ejemplos nuevos con perfil capturado antes de la decisión.

No se usaron como etiquetas rechazos inventados ni postulaciones pendientes.
Los perfiles simulados de las pruebas no se añadieron a MongoDB ni al entrenamiento.

## Entrenamiento reproducible

Se comparan 16 configuraciones: dos grupos de variables, cuatro regularizaciones
y con/sin calibración sigmoide. La calibración usa ejemplos posteriores separados
del ajuste del clasificador. La selección compara Brier en particiones internas
por fecha y candidato; el conjunto final no decide qué configuración se publica.

Seleccionada en esta evaluación: base, C=10.0,
sin_calibracion_adicional. El calibrador adicional no mejoró la validación
interna y por eso no se forzó su uso.

| Medida sobre los datos históricos | Resultado |
| --- | --- |
| Entrenamiento, incluida calibración cuando se usa | 345 |
| Evaluación separada | 94 |
| ROC AUC | 0.7939 |
| Brier | 0.1810 |
| Brier de la tasa base | 0.2523 |
| Log loss | 0.5192 |

Estas cifras describen la muestra disponible. No son evidencia de exactitud en
procesos reales ni ROC AUC significa porcentaje de aciertos. Esta muestra se ha
examinado repetidamente durante desarrollo; una validación futura debe usar
resultados posteriores no utilizados para tomar decisiones de implementación.

## Funcionamiento comprobado

512 solicitudes simuladas válidas, 7 entradas
inválidas y 0 fallos. Incluye habilidades parciales, perfiles sin
estudios, categorías nuevas, experiencia superpuesta y perfiles sin experiencia.
Es una prueba de robustez de la API; no mide precisión predictiva.

Cada artefacto identifica versión, huella del conjunto de datos y entorno.
El manifiesto incluye SHA-256. Las actualizaciones se escriben de forma atómica;
la API conserva la versión anterior si no puede cargar un reemplazo.

## Límites que impiden dar por terminada la validación

- Habilidades y presencia de educación no varían en el entrenamiento actual.
  No se puede estimar su influencia a partir de esa muestra.
- Educación representa presencia de estudios, no nivel ni área.
- Los perfiles actuales usados para registros antiguos pueden diferir de los
  disponibles antes de la decisión.
- Faltan resultados nuevos de procedencia comprobada y evaluación prospectiva
  con variación de perfiles, calibración y comparación con una referencia.

## Operación

```sh
sh ml/run.sh audit
sh ml/run.sh train
sh ml/run.sh verify
```

Para entrenar únicamente con capturas y decisiones fechadas:

```sh
sh ml/run.sh train --captured-only
```

La recolección necesaria para cerrar la validación está en DATA_COLLECTION.md.
Los datos simulados permiten terminar pruebas de software; no se convertirán en
prueba de precisión real cambiándoles la etiqueta o el nombre del estado.
