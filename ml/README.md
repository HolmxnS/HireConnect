# Predicción de aceptación de HireConnect

Regresión logística binaria: `ACEPTADA=1`, `RECHAZADA=0`. Las postulaciones
pendientes, en revisión o en entrevista no se consideran negativas.

## Arranque rápido

Desde la raíz de HireConnect:

```sh
sh ml/run.sh
```

El script crea un entorno de Python si falta, instala las dependencias y arranca
la API en `http://127.0.0.1:5050`. Lee la configuración de MongoDB del entorno o
del `.env` del proyecto sin ejecutar su contenido. Si no existe un modelo,
entrena con las postulaciones resueltas de la base y los perfiles actuales.
El entrenamiento sucede al preparar el servicio, nunca durante una predicción.

La aplicación Java ya usa esa URL por defecto. Reiniciarla desde el IDE después
de aplicar estos cambios. Para actualizar el modelo con resultados nuevos:

```sh
sh ml/run.sh train
```

La API detecta el nuevo archivo y lo recarga automáticamente. Si el reemplazo
está dañado, mantiene el último modelo válido. No se reentrena automáticamente con cada
postulación ni se crean etiquetas artificiales. Para usar otro puerto:
`sh ml/run.sh serve --port 5051` y configurar la misma URL en `PYTHON_ML_URL`.
`ML_VENV` permite usar un entorno de Python existente.

## Entorno local

Usar Python 3.12–3.14. Desde esta carpeta:

```sh
python3 -m venv .venv
. .venv/bin/activate
pip install -r requirements.lock.txt
python -m unittest discover -p 'test_*.py' -v
```

Definir `MONGODB_URI` y opcionalmente `MONGODB_DATABASE` (por defecto
`hireconnect`) mediante el entorno del proceso. No incluir credenciales en
archivos de código ni subir datos o artefactos al repositorio. Para TLS se usan
certificados raíz de certifi; `MONGODB_TLS_CA_FILE` permite indicar una CA propia.

## Entrenamiento y evaluación

```sh
python train.py --audit-only
python train.py
```

El entrenamiento normal usa `prediccionPerfil` con `prediccionOrigen=FLUJO_APLICACION`,
`prediccionCapturadaEn` y `resultadoActualizadoEn`, guardados por Java. El perfil
debe haberse capturado antes de la decisión. No reconstruye silenciosamente el pasado con el
perfil actual. Las postulaciones antiguas no contienen esa captura.

Para una primera exploración, explícitamente:

```sh
python train.py --allow-current-profiles --audit-only
python train.py --allow-current-profiles
```

Este modo usa perfiles actuales: los datos podrían haber cambiado después de
conocerse la decisión. Sus métricas NO prueban validez histórica. También se
debe comprobar la procedencia de los registros: estar en MongoDB no demuestra
que sean decisiones reales y no datos de prueba. No se escriben datos en MongoDB.

La separación usa fechas (aproximadamente último 20% para evaluación), mantiene
las fechas iguales en el mismo conjunto y excluye del entrenamiento los
candidatos del conjunto de evaluación. Puede descartar muchos registros o
rechazar el entrenamiento si faltan ambas clases. El mínimo de 50 ejemplos y
5 por clase en cada conjunto es una barrera técnica, no garantía estadística.

Se guarda `artifacts/model.joblib` y `artifacts/report.json`, sin IDs ni datos
personales. El informe incluye ROC AUC, Brier, log loss, precisión, recall,
comparación Brier con la tasa base y frecuencias observadas por intervalo.
No se autoriza automáticamente un modelo para producción por superar una
métrica. Revisar calibración, representatividad y estabilidad con más datos.

## API e integración

```sh
ML_ENABLE_EXPERIMENTAL=true python app.py
```

Desde Java, configurar `PYTHON_ML_URL=http://127.0.0.1:5050/predict` (valor local
por defecto). Reiniciar Java después de aplicar cambios. Si Java corre en un
contenedor, indicar el hostname accesible del servicio Python, no localhost.

Para un servidor WSGI, desde esta carpeta:

```sh
ML_ENABLE_EXPERIMENTAL=true gunicorn --bind 0.0.0.0:5050 'app:create_app()'
```

`MODEL_PATH` permite indicar el artefacto. Cargar solo artefactos joblib de
origen confiable. La API recarga automáticamente el archivo después de entrenar. Flask no usa debug.
La API no conecta a MongoDB ni reentrena durante solicitudes.
Sin artefacto devuelve 503; no inventa porcentajes. El modo experimental está
habilitado por defecto para este proyecto; `ML_ENABLE_EXPERIMENTAL=false` lo desactiva.
Java tiene tiempos de espera limitados y rechaza el contrato antiguo por reglas.
El frontend muestra explícitamente que la estimación es experimental.

El contrato v2 usa datos del usuario autenticado y de la vacante: habilidades,
fechas de experiencia, existencia de educación registrada, experiencia requerida,
modalidad y contrato. No transmite nombre, correo, documento, foto ni CV.
La edad no se utiliza. Si las variables numéricas quedan fuera del rango
observado al entrenar, la API entrega la predicción del modelo y marca
`coberturaLimitada=true`. La pantalla explica que hay pocos ejemplos similares;
no se bloquea el perfil ni se ajusta el porcentaje con penalizaciones manuales. El porcentaje se expresa siempre entre 0 y 100.

La coincidencia de habilidades es exacta tras normalizar mayúsculas, espacios
exteriores y tildes. No interpreta sinónimos ni similitud semántica. Las fechas
de trabajos superpuestos se unen para no duplicar experiencia. Educación
registrada no significa que se cumpla un requisito educativo: las vacantes
actuales no tienen un campo estructurado para ese requisito. Modalidad y
contrato son categorías, sin bonificaciones manuales.

Alta/Media/Baja son umbrales de presentación provisionales (70/45), no otra
variable objetivo entrenada. El evento predicho es ACEPTADA en HireConnect,
no contratación confirmada ni una decisión automatizada sobre el candidato.

No se ha modificado ni desplegado la API antigua alojada en Render.

## Verificación

```sh
python -m unittest discover -p 'test_*.py' -v
```

Desde la raíz, con la API iniciada:

```sh
sh mvnw -q test
sh mvnw -q -Dtest=PrediccionLiveIntegrationTest -Dml.integration=true test
node --test src/test/js/vacante_prediccion.test.cjs
```

La prueba de integración conecta el controlador Java con la API local usando
perfiles de prueba en memoria, sin crear cuentas ni escribir en MongoDB.
La prueba de JavaScript verifica carga de la primera vacante, porcentajes bajos,
reintentos y reutilización de resultados válidos. Los fallos de conexión no se
guardan en caché y se ofrece reintentar; cada solicitud tiene tiempo límite.

## Selección del modelo

Se comparan ocho configuraciones de regresión logística: cuatro valores de
regularización (`C=0.01, 0.1, 1, 10`) con las variables originales o incorporando
experiencia faltante/excedente frente a los requisitos. Estas últimas son
variables de entrada calculadas; no bonificaciones ni penalizaciones manuales
sobre el porcentaje.

Se elige el menor Brier medio en dos particiones temporales internas del
entrenamiento, excluyendo candidatos compartidos. La evaluación final se
mantiene separada de esa selección. Si faltan ejemplos para validación interna,
se conserva la configuración base sin elegir usando los resultados de prueba.
El informe contiene los ocho resultados, variables constantes y categorías
observadas. Las categorías nuevas también activan la nota de cobertura limitada.

La selección automática no garantiza que todas las métricas mejoren. Revisar el
informe antes de interpretar cambios como una mejora; los datos actuales siguen
siendo de demostración y con variables sin variación.

## Despliegue Docker / Render

El Dockerfile de la raíz incluye ahora el predictor. El supervisor de
`deploy/start.py` prepara el artefacto al arrancar si falta y espera al modelo
antes de iniciar Java. Ver `deploy/README.md`. En Render no es necesario ejecutar
`run.sh` aparte ni mantener otro servicio para Python.

## Recolección y auditoría de nuevos ejemplos

```sh
sh ml/run.sh audit
sh ml/run.sh train --captured-only
```

La auditoría es de solo lectura y no reemplaza el modelo. Informa capturas,
resultados utilizables, distribución de habilidades, variación de educación y
posibilidad de separar entrenamiento y evaluación. Ver `DATA_COLLECTION.md`.
El segundo comando excluye los históricos sin trazabilidad. Si faltan ejemplos,
termina con una explicación antes de escribir un nuevo artefacto; la API mantiene
el modelo existente. El entrenamiento experimental de arranque sigue disponible
para que la app pueda funcionar con la base actual.

Cuando una postulación vuelve a revisión se borra la fecha del resultado vigente;
al decidir de nuevo se registra una fecha nueva. Repetir el mismo estado no
reescribe fechas ni fabrica una decisión para un registro histórico. Las
particiones temporales excluyen de entrenamiento las decisiones posteriores a
su fecha de corte. El origen describe el flujo de la app y no certifica que una
postulación corresponda a un proceso real.
