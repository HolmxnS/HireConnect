# Despliegue de HireConnect con el modelo

La imagen Docker incluye Java 21 y Python 3.12. El proceso de entrada prepara el
modelo, inicia Gunicorn en `127.0.0.1:5050` y espera a que esté listo antes de
arrancar Java. Java continúa usando `PORT` para el servidor público de Render.
No se expone públicamente el puerto del predictor.

## Actualizar un servicio existente en Render

1. Actualizar el repositorio y la rama que Render realmente despliega. Si es un
   fork de un compañero, primero sincronizar ese fork con este commit.
2. Usar el Dockerfile de la raíz del repositorio.
3. Mantener `MONGODB_URI` o `SPRING_DATA_MONGODB_URI` en Environment, con la base
   de postulaciones. No subir el `.env` al repositorio.
4. Ejecutar **Manual Deploy → Deploy latest commit**, o esperar el autodespliegue
   si está habilitado para esa rama.
5. Comprobar en los logs `Modelo listo. Java usara la API interna...` antes del
   mensaje de arranque de Spring Boot. Luego abrir una vacante.

No hace falta crear otro servicio Python ni copiar manualmente el archivo del
modelo. El contenedor entrena al arrancar si falta el artefacto. No utiliza
archivos `.joblib` ni entornos de Python de la máquina del desarrollador.

En modo integrado (`ML_EMBEDDED=true`, valor por defecto), se utiliza la API
interna incluso si quedó una variable `PYTHON_ML_URL` de la API antigua.
Para un predictor externo actualizado, establecer `ML_EMBEDDED=false` y definir
`PYTHON_ML_URL` explícitamente.

El arranque requiere que haya suficientes postulaciones resueltas y conectividad
con MongoDB. Si falta la URI o falla la preparación, el despliegue termina con un
error explícito en los logs. No se publican porcentajes inventados.
Para una base local vacía, Docker Compose configura `ML_REQUIRED=false`: permite
iniciar la app y recolectar datos aunque el modelo aún no esté preparado.

Si Java o Python termina inesperadamente, el supervisor detiene el otro proceso
y devuelve un error para que la plataforma reinicie la instancia completa. Al
recibir SIGTERM, cierra ambos procesos y los trabajadores de Gunicorn.

## Comprobaciones

```sh
python -m unittest discover -s deploy -p 'test_*.py' -v
RUN_DEPLOY_SMOKE=1 python -m unittest discover -s deploy -p 'test_*.py' -v
```

La prueba de arranque usa un puerto libre y procesos temporales. La construcción
completa de la imagen requiere Docker:

```sh
docker build -t hireconnect .
```

Referencias: https://render.com/docs/docker y https://render.com/docs/web-services

## Azure Linux con Java SE (HireConnect-expo)

Crear un App Service Linux de tipo Codigo/Java no ejecuta el Dockerfile.
El flujo `main_hireconnect-expo.yml` publica ahora un ZIP que contiene:

- `app.jar` con Java 21.
- `.python/` con un CPython 3.12 portable y las dependencias del modelo.
- `ml/` y los scripts de arranque de ambos procesos.

El arranque configurado por GitHub Actions es:

```sh
bash /home/site/wwwroot/deploy/azure-start.sh
```

No se requiere otro App Service ni cambiar el runtime del servicio ya creado.
El modelo se guarda en `/home/data/hireconnect-ml`, fuera del paquete de codigo,
y usa `MONGODB_URI` o `SPRING_DATA_MONGODB_URI` del App Service.
La URL interna `127.0.0.1:5050/predict` es valida porque ahora ambos procesos
se ejecutan juntos. El servicio Windows anterior conserva su flujo de JAR.

La construccion verifica el ZIP, rechaza `.env` y `.git` y prueba el runtime
tras descomprimirlo en otra carpeta antes de publicarlo. Solo se empaquetan
rutas permitidas; no se sube el directorio completo del repositorio.

En el Log stream del servicio Linux deben aparecer, en este orden:

1. `Iniciando HireConnect con Java + Python (paquete Linux)`.
2. La preparacion del modelo, cuando aun no existe un archivo entrenado.
3. `Modelo listo. Java usara la API interna en el puerto 5050.`.
4. El arranque de Spring Boot.

Si solo aparece el arranque de Java, no se esta usando el ZIP/comando nuevo.
