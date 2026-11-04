# syntax=docker/dockerfile:1
#
# Build multi-stage: el stage "build" compila el codigo fuente ACTUAL con Maven Wrapper y produce
# un JAR nuevo dentro del propio build de Docker. El stage final solo copia ese JAR recien
# generado. Esto evita el riesgo de empaquetar silenciosamente un target/*.jar desactualizado que
# ya no corresponda al codigo fuente (ver docs del backend sobre "JAR desactualizado").
# El starter de MessageUcoLab vive en GitHub Packages (repositorio privado del equipo de mensajeria). Las credenciales
# entran como secreto de BuildKit (docker build --secret id=m2settings,src=<settings.xml>) y no quedan en ninguna capa
# de la imagen. El secreto es opcional (required=false): sin el, el build solo falla si realmente necesita el starter.
FROM eclipse-temurin:25-jdk-alpine AS build
WORKDIR /workspace

COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN --mount=type=secret,id=m2settings,target=/root/.m2/settings.xml,required=false chmod +x mvnw && ./mvnw -B -ntp dependency:go-offline

COPY src/ src/
COPY docs/contracts/openapi/openapi-golden-path.yaml docs/contracts/openapi/openapi-golden-path.yaml
COPY docs/contracts/openapi/openapi-golden-path.sha256 docs/contracts/openapi/openapi-golden-path.sha256
# AccessCatalogConsistencyTest contrasta Spring MVC y SecurityConfig con el catalogo de acceso compartido.
COPY infra/security-baseline/access-catalog.json infra/security-baseline/access-catalog.json
RUN --mount=type=secret,id=m2settings,target=/root/.m2/settings.xml,required=false ./mvnw -B -ntp clean verify

FROM eclipse-temurin:25-jre-alpine
WORKDIR /app
COPY --from=build /workspace/target/AsistenciasUCO-0.0.1-SNAPSHOT.jar app.jar
# SEC-002: Alpine CVE-2026-46675 (libpng), CVE-2026-85091 (zlib).
# Update OS packages in the runtime stage, never merely in the builder stage.
RUN apk upgrade --no-cache libpng zlib
# SEC-002 defense-in-depth: production JVM runs without root privileges.
# JAR is copied as world-readable and Java can use /tmp on Alpine.
USER 10001:10001
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
