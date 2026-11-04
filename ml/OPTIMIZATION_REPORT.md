# Ajuste del modelo — 1 de noviembre de 2026

Datos: 345 ejemplos de entrenamiento y 94 de evaluación.
Se mantienen las etiquetas existentes de MongoDB. No se crean perfiles,
postulaciones ni resultados artificiales. Las limitaciones de procedencia se
describen en INITIAL_EVALUATION.md.

Se compararon ocho configuraciones dentro del entrenamiento, con dos
particiones temporales y candidatos separados. Selección: variables
`base`, C=10.0, Brier interno=0.174162.
Las variables de experiencia faltante/excedente se probaron y no fueron elegidas.

| Métrica de evaluación | Configuración anterior | Seleccionada |
| --- | --- | --- |
| Brier, menor es mejor | 0.180729 | 0.180976 |
| Log loss, menor es mejor | 0.524267 | 0.519173 |

El log loss mejoró ligeramente y Brier empeoró ligeramente. No hay una mejora
general demostrada. La configuración se eligió por validación interna, sin
usar esta comparación final para volver a seleccionar. Las evaluaciones
repetidas sobre esta muestra no sustituyen una nueva evaluación prospectiva.

Las habilidades y la presencia de educación siguen sin variar en entrenamiento.
No se ha eliminado esa limitación. Las capturas de nuevas postulaciones
permitirán incorporar ejemplos distintos cuando tengan resultados reales;
no se considera un rechazo una postulación todavía pendiente.

La API recarga el artefacto actualizado sin reiniciar y conserva el último
válido si falla el reemplazo. Perfiles fuera de rangos o categorías observadas
siguen recibiendo una estimación con cobertura limitada.

Validación: 10 pruebas Python y 4 de JavaScript pasaron. Se vuelve a comprobar
la integración Java con la API local después del ajuste.
