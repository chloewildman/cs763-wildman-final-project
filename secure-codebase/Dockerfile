FROM maven:3.9-eclipse-temurin-25 AS package

WORKDIR /build

COPY pom.xml .
RUN mvn -B dependency:go-offline

COPY src ./src
RUN mvn -B package && mv target/*.jar target/app.jar

FROM eclipse-temurin:25-jre-jammy AS final

ARG UID=10001
RUN adduser \
    --disabled-password \
    --gecos "" \
    --home "/nonexistent" \
    --shell "/sbin/nologin" \
    --no-create-home \
    --uid "${UID}" \
    appuser
WORKDIR /app
RUN mkdir -p /app/data && chown appuser:appuser /app/data

USER appuser

COPY --from=package /build/target/app.jar app.jar

VOLUME ["/app/data"]

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
