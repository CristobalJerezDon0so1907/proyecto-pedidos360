# ---- Etapa 1: compilar con Maven (así el servidor no necesita Java ni Maven) ----
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /src
COPY pom.xml .
RUN mvn -B -q dependency:go-offline || true
COPY src ./src
RUN mvn -B -q -DskipTests package

# ---- Etapa 2: imagen liviana solo con el .jar ----
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /src/target/*-SNAPSHOT.jar app.jar
EXPOSE 8080
ENV JAVA_OPTS="-Xms128m -Xmx384m"
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]