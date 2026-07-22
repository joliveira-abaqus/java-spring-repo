FROM eclipse-temurin:25-jdk AS build
WORKDIR /app
COPY mvnw pom.xml ./
COPY .mvn .mvn
RUN sed -i 's/\r$//' mvnw && chmod +x mvnw && sh mvnw dependency:go-offline -B
COPY src src
RUN sh mvnw package -DskipTests -B

FROM eclipse-temurin:25-jre
WORKDIR /app
# Baixa o dd-java-agent para habilitar APM/tracing via Datadog
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && curl -Lo /app/dd-java-agent.jar https://dtdg.co/latest-java-tracer
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-javaagent:/app/dd-java-agent.jar", "-jar", "app.jar"]
