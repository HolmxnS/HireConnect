# ============================
# Etapa 1: Build con Maven
# ============================
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /app

# Copiar archivos de Maven primero (mejor cache)
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./

# Descargar dependencias (se cachea si pom.xml no cambia)
RUN ./mvnw dependency:go-offline -B

# Copiar el código fuente
COPY src ./src

# Construir el JAR (sin correr tests)
RUN ./mvnw package -DskipTests -B

# ============================
# Etapa 2: Imagen de producción
# ============================
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Crear directorio para uploads
RUN mkdir -p /app/uploads

# Copiar el JAR desde la etapa de build
COPY --from=builder /app/target/*.jar app.jar

# Puerto que expone Spring Boot
EXPOSE 8080

# Comando de arranque
ENTRYPOINT ["java", "-jar", "app.jar"]
