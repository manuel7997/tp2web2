# ---------- Etapa 1: compilar con Maven ----------
FROM eclipse-temurin:25-jdk AS build
WORKDIR /app
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
COPY src/ src/
# Las pruebas ya corren en el CI (GitHub Actions); acá solo se arma el jar.
RUN chmod +x mvnw && ./mvnw -B -ntp -DskipTests package

# ---------- Etapa 2: imagen final, solo con el JRE ----------
FROM eclipse-temurin:25-jre
WORKDIR /app
RUN useradd --system --uid 10001 appuser
COPY --from=build /app/target/*.jar app.jar
USER appuser
EXPOSE 8080
# La instancia gratuita de Render tiene 512 MB de RAM: se limita el heap y se usan opciones livianas.
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=60", "-XX:+UseSerialGC", "-XX:TieredStopAtLevel=1", "-Xss512k", "-jar", "app.jar"]