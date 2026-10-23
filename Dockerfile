FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app
COPY . .
RUN mkdir -p bin && \
    javac -cp "lib/*" -d bin $(find src -name "*.java")

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=builder /app/bin ./bin
COPY --from=builder /app/lib ./lib
COPY --from=builder /app/web ./web

EXPOSE 8080
ENV PORT=8080

CMD ["java", "-cp", "bin:lib/*", "com.velocityx.Main"]