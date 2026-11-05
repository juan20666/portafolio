# ---- Build ---------------------------------------------------------------
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src ./src
RUN mvn -B -q -DskipTests package

# ---- Run -----------------------------------------------------------------
FROM eclipse-temurin:21-jre
RUN useradd --system --uid 10001 --no-create-home app
WORKDIR /app
COPY --from=build /build/target/portfolio-api.jar app.jar
USER app
EXPOSE 8080
# Small-memory settings for free plans (512 MB): serial GC, C1 compiler only, capped heap.
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=70 -XX:+UseSerialGC -XX:TieredStopAtLevel=1 -Xss512k"
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
