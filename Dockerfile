FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /app

# Copiar archivos de Maven primero (mejor cache)
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./

# Descargar dependencias (se cachea si pom.xml no cambia)
RUN chmod +x mvnw && ./mvnw dependency:go-offline -B

COPY src ./src

RUN ./mvnw package -DskipTests -B

# Java and Python run in the same image: localhost:5050 exists in Render too.
# Ubuntu 24.04 provides Python 3.12, supported by the ML dependencies.
FROM eclipse-temurin:21-jre-noble

WORKDIR /app

RUN apt-get update \
    && apt-get install -y --no-install-recommends python3 python3-venv \
    && python3 -m venv /opt/venv \
    && rm -rf /var/lib/apt/lists/*

ENV PATH="/opt/venv/bin:${PATH}"
ENV PYTHONUNBUFFERED=1
ENV ML_EMBEDDED=true

COPY ml/requirements.lock.txt /app/ml/requirements.lock.txt
RUN pip install --no-cache-dir -r /app/ml/requirements.lock.txt

COPY ml/ /app/ml/
COPY deploy/ /app/deploy/
COPY --from=builder /app/target/*.jar /app/app.jar
RUN mkdir -p /app/uploads /app/ml/artifacts

EXPOSE 8080

ENTRYPOINT ["python", "/app/deploy/start.py"]
