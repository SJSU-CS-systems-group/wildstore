# Stage 1: Build
FROM eclipse-temurin:17-jdk AS build
WORKDIR /src
COPY pom.xml .
COPY wildstore-common/pom.xml wildstore-common/pom.xml
COPY wildstore-crawl/pom.xml wildstore-crawl/pom.xml
COPY wildstore-meta/pom.xml wildstore-meta/pom.xml
COPY wildstore-fileserve/pom.xml wildstore-fileserve/pom.xml
COPY wildstore-cli/pom.xml wildstore-cli/pom.xml
COPY wildstore-testdata/pom.xml wildstore-testdata/pom.xml
RUN apt-get update && apt-get install -y maven && rm -rf /var/lib/apt/lists/*
RUN mvn dependency:go-offline -B -ntp || true
COPY . .
RUN mvn package -DskipTests -B -ntp

# Stage 2a: Metadata server
FROM eclipse-temurin:17-jre-jammy AS meta
RUN apt-get update && apt-get install -y --no-install-recommends curl && rm -rf /var/lib/apt/lists/*
RUN groupadd -r wildstore && useradd -r -g wildstore wildstore
WORKDIR /app
COPY --from=build /src/wildstore-meta/target/wildstore-meta-server.jar app.jar
USER wildstore
EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=5s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1
ENTRYPOINT ["java", "-jar", "app.jar"]

# Stage 2b: File server
FROM eclipse-temurin:17-jre-jammy AS fileserve
RUN apt-get update && apt-get install -y --no-install-recommends curl && rm -rf /var/lib/apt/lists/*
RUN groupadd -r wildstore && useradd -r -g wildstore wildstore
WORKDIR /app
COPY --from=build /src/wildstore-fileserve/target/wildstore-fileserve.jar app.jar
USER wildstore
EXPOSE 27778
HEALTHCHECK --interval=30s --timeout=5s --retries=3 \
  CMD curl -f http://localhost:27778/actuator/health || exit 1
ENTRYPOINT ["java", "-jar", "app.jar"]
