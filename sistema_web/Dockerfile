# ETAPA 1: Construcción (JDK completo)
FROM eclipse-temurin:21-jdk-jammy AS build
WORKDIR /root

# Copiar archivos de configuración de Maven
COPY ./pom.xml ./
COPY ./.mvn ./.mvn
COPY ./mvnw ./

# Descargar dependencias (Caché de Docker)
RUN ./mvnw dependency:go-offline

# Copiar código fuente y construir
COPY ./src ./src
RUN ./mvnw clean install -DskipTests

# ETAPA 2: Imagen de Ejecución (JRE más ligero)
# Usamos el tag ubi9-minimal que viste antes por seguridad y ligereza
FROM eclipse-temurin:21-jre-ubi9-minimal
WORKDIR /app

# EXPOSE debe ir en la etapa de ejecución para que sea útil
EXPOSE 8080

# IMPORTANTE: Copiamos el .war generado pero lo renombramos a app.jar
# para que el comando de ejecución sea siempre el mismo
COPY --from=build /root/target/sistema_web-0.0.1-SNAPSHOT.war app.jar

# Ejecutar la aplicación
ENTRYPOINT ["java", "-jar", "app.jar"]