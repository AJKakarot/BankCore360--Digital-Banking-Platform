# Multi-stage Dockerfile for BankCore360 Spring Boot Backend

# Stage 1: Build JAR
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app

# Copy Maven wrapper and POM
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw

# Download dependencies
RUN ./mvnw dependency:go-offline -B || true

# Copy source code and build
COPY src ./src
RUN ./mvnw clean package -DskipTests -B

# Stage 2: Runtime
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Add unprivileged user and pre-create storage directories with proper ownership
RUN addgroup -S appgroup && adduser -S appuser -G appgroup \
    && mkdir -p /app/storage/private/loan-documents /app/storage/private/kyc-documents \
    && chown -R appuser:appgroup /app

USER appuser

# Copy built artifact
COPY --chown=appuser:appgroup --from=builder /app/target/*.jar app.jar

ENV PORT=8080
ENV JAVA_OPTS="-Xmx384m -Xms128m -Xss512k -XX:+ExitOnOutOfMemoryError"
EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -Djava.security.egd=file:/dev/./urandom -Dserver.port=${PORT:-8080} -jar app.jar"]

