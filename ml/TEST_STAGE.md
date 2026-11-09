# Modelo activo en la etapa de pruebas

Por solicitud del usuario del 7 de noviembre de 2026, el modo predeterminado
actual es `demo` (configurado en `runtime_mode.py`). Se conserva la presentación
«Estimación de aceptación» y los niveles alta, media y baja. Por solicitud del usuario, el bloque no muestra una nota adicional; la procedencia
se conserva en la API y en esta documentación. Los resultados no validan precisión real.

Se crearon en MongoDB, en la base separada `<MONGODB_DATABASE>_ml_demo`:

- 1.200 postulantes, sin cuentas utilizables, con correos `example.invalid`.
- 120 vacantes de tecnología, finanzas, salud, construcción y administración.
- 3.600 postulaciones con estados aceptada/rechazada simulados, identificadas
  con `synthetic=true` y `prediccionOrigen=SIMULACION`.

No se insertaron decisiones inventadas en las colecciones reales. Cada par
candidato-vacante es único. Volver a ejecutar la carga actualiza los mismos IDs
del conjunto de pruebas, sin duplicarlos ni modificar registros ajenos.

La generación incluye coincidencias de habilidades nulas, parciales y completas;
perfiles con/sin estudios y con distintos años de experiencia; todas las
modalidades y contratos. La etiqueta depende de una función declarada y de
muestreo aleatorio con semilla fija. No favorece la modalidad híbrida. La regresión
logística se entrena sobre esas etiquetas y se evalúa en un corte posterior con
candidatos separados. Sus métricas describen ese escenario generado solamente.

## Operación

```sh
# Generar/actualizar los registros en la base de pruebas y entrenar leyendo sus capturas.
python ml/demo.py --write-db

# Arrancar con el modelo ya entrenado.
sh ml/run.sh serve

# Reproducir el entrenamiento de pruebas localmente sin escribir en MongoDB.
sh ml/run.sh train
```

El artefacto se guarda en `ml/artifacts/demo/model.joblib`. Azure utiliza
`/home/data/hireconnect-ml/demo/model.joblib` y lo genera al primer arranque si
falta. No requiere añadir variables para usar este modo de pruebas. No realiza
cargas ni cambios en MongoDB durante el arranque.

Para volver a datos reales se configura `ML_MODEL_MODE=real`, usando su artefacto
separado. Los controles de trazabilidad/cobertura del modo real se conservan.
El modelo de pruebas nunca se habilita como una probabilidad real; su API marca
`synthetic=true`, `tipoResultado=simulacion` y `probabilidadDisponible=false`.
