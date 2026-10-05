FROM node:22-alpine AS frontend
WORKDIR /app/frontend
COPY frontend/package*.json ./
RUN npm ci
COPY frontend/ ./
RUN npm run build
FROM maven:3.9.11-eclipse-temurin-21 AS backend
WORKDIR /app/backend
COPY backend/ ./
COPY --from=frontend /app/frontend/dist/browser/ src/main/resources/static/
RUN mvn -B clean verify
FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S gym && adduser -S gym -G gym
WORKDIR /app
COPY --from=backend /app/backend/target/gymlog-1.0.0.jar app.jar
RUN mkdir data && chown -R gym:gym /app
USER gym
ENV BIND_ADDRESS=0.0.0.0
EXPOSE 8080
ENTRYPOINT ["java","-jar","app.jar"]
