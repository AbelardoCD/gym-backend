# Imagen del backend: compila con Maven y corre solo con el JRE.
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -q -B dependency:go-offline
COPY src src
RUN mvn -q -B package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/gym-backend-*.jar app.jar
# Archivos de rutinas y fotos de perfil; en producción va en un volumen.
ENV STORAGE_PATH=/data/uploads
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
