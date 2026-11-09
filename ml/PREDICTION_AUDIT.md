# Auditoría del porcentaje de aceptación — 7 de noviembre de 2026

La integración funcionaba, pero eso no demostraba que las cifras representaran
la probabilidad de aceptación de un perfil. La auditoría de MongoDB fue de solo
lectura; no se añadieron decisiones, perfiles ni etiquetas artificiales.

## Fallo reproducido

Un perfil sin experiencia y sin las habilidades solicitadas recibía 99,0 % para
una vacante híbrida. Cambiar sus habilidades no alteraba esa predicción.
En el artefacto `2a08c444d4e6`, el coeficiente de habilidades y el de presencia de
educación eran cero. La modalidad tenía una influencia mucho mayor.

La consulta de la base disponible encontró:

- 439 resultados históricos: 216 aceptaciones y 223 rechazos.
- Los 439 ejemplos reconstruidos tenían 100 % de coincidencia de habilidades y
  educación registrada. Esas variables no podían aprender a distinguir casos.
- 0 perfiles capturados antes de la decisión y utilizables para validación.

| Modalidad del histórico | Aceptadas | Rechazadas |
| --- | ---: | ---: |
| Híbrida | 47 | 0 |
| Remota | 41 | 2 |
| Presencial | 128 | 221 |

La modalidad explica el 99 %, pero no demuestra capacidad de predecir la
aceptación de un candidato nuevo. En 512 solicitudes simuladas del diagnóstico
anterior al cambio aparecieron 384 resultados altos, 88 medios y 40 bajos:
«media» existía, aunque el sesgo favorecía las otras modalidades. Esa simulación
comprueba comportamiento, no exactitud con decisiones reales.

## Corrección

No se modifican los umbrales de aceptación (70/45), no se fuerza una distribución
de resultados y no se reentrena usando etiquetas inventadas.

La API distingue:

- `probabilidad`: salida de regresión logística, únicamente con historial de
  capturas trazables, variación en habilidades y experiencia, y perfil dentro
  de los rangos y categorías del entrenamiento.
- `coincidencia_habilidades`: porcentaje de requisitos de habilidades que el
  perfil declara, con coincidencia normalizada de textos. Es una comparación
  explícita, no una probabilidad ni otra salida del modelo entrenado.
- `datos_insuficientes`: no hay respaldo para una probabilidad y tampoco hay
  habilidades requeridas para comparar. No se muestra 0 % ni un reintento inútil.

La API no incluye `probabilidad` en los últimos dos resultados. Java valida el
contrato y el navegador cambia título, etiqueta y explicación según el tipo.
Los fallos de conexión conservan la opción de reintentar. `/health` distingue
`predictionReady` (servicio operativo) de `probabilityReady` (historial elegible).
El control de elegibilidad es una protección mínima, no certifica autenticidad,
representatividad o calibración por sí solo.

Con los datos actuales, la probabilidad de aceptación sigue sin estar validada.
Suprimir el 99 % injustificado corrige el comportamiento; no convierte la
coincidencia de habilidades en el objetivo predictivo solicitado. Para ese
objetivo faltan decisiones reales variadas, sus capturas previas y evaluación
sobre resultados posteriores. Véase DATA_COLLECTION.md.

## Reproducción

```sh
python ml/diagnose.py
python ml/diagnose.py --database
python -m unittest discover -s ml -v
node --test src/test/js/vacante_prediccion.test.cjs
sh mvnw test
```

Las solicitudes de diagnóstico son fixtures y nunca se guardan en MongoDB ni se
usan como entrenamiento. Los informes muestran estadísticas y no identificadores.
