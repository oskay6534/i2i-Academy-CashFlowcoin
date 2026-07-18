FROM eclipse-temurin:17-jdk-jammy AS builder

WORKDIR /workspace

COPY .mvn .mvn
COPY mvnw pom.xml ./

RUN chmod +x mvnw \
    && ./mvnw -DskipTests dependency:go-offline

COPY src src

RUN ./mvnw -DskipTests clean package

FROM eclipse-temurin:17-jre-jammy

WORKDIR /app

RUN groupadd --system cashflowcoin \
    && useradd --system --gid cashflowcoin cashflowcoin

COPY --from=builder /workspace/target/*.jar /app/app.jar
COPY docker-entrypoint.sh /app/docker-entrypoint.sh

RUN chmod +x /app/docker-entrypoint.sh \
    && chown -R cashflowcoin:cashflowcoin /app

USER cashflowcoin

EXPOSE 10000

ENTRYPOINT ["/app/docker-entrypoint.sh"]