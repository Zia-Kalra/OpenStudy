
# Stage 1: Build the Spring Boot WAR
FROM maven:3.9-eclipse-temurin-25 AS build

WORKDIR /app

COPY backend/ .

RUN mvn -B clean package -DskipTests

# Stage 2: Java runtime and R
FROM eclipse-temurin:25-jre

# Install R and the JSON package required by analytics scripts
RUN apt-get update \
    && apt-get install -y --no-install-recommends \
       r-base-core \
       r-cran-jsonlite \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app

# Copy the executable WAR
COPY --from=build /app/target/openstudy-0.0.1-SNAPSHOT.war app.war

# Copy R analytics scripts
COPY R/ /app/R/

# Configure R
ENV RSCRIPT_PATH=Rscript
ENV ANALYTICS_R_SCRIPT_DIR=/app/R

# Default upload directory (use a Render persistent disk for durability)
ENV UPLOAD_DIR=/var/data/uploads/assignments

RUN mkdir -p /var/data/uploads/assignments

EXPOSE 10000

CMD ["sh", "-c", "java -Dserver.port=${PORT:-10000} -Dopenstudy.upload-dir=${UPLOAD_DIR} -jar /app/app.war"]

# Stage 1: Build the Spring Boot WAR
FROM maven:3.9-eclipse-temurin-25 AS build

WORKDIR /app

COPY backend/ .

RUN mvn -B clean package -DskipTests

# Stage 2: Java runtime and R
FROM eclipse-temurin:25-jre

# Install R and the JSON package required by analytics scripts
RUN apt-get update \
    && apt-get install -y --no-install-recommends \
       r-base-core \
       r-cran-jsonlite \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app

# Copy the executable WAR
COPY --from=build /app/target/openstudy-0.0.1-SNAPSHOT.war app.war

# Copy R analytics scripts
COPY R/ /app/R/

# Configure R
ENV RSCRIPT_PATH=Rscript
ENV ANALYTICS_R_SCRIPT_DIR=/app/R

# Default upload directory (use a Render persistent disk for durability)
ENV UPLOAD_DIR=/var/data/uploads/assignments

RUN mkdir -p /var/data/uploads/assignments

EXPOSE 10000

CMD ["sh", "-c", "base64 -d /etc/secrets/aiven-truststore.jks.b64 > /tmp/aiven-truststore.jks && java -Dserver.port=${PORT:-10000} -Dopenstudy.upload-dir=${UPLOAD_DIR} -jar /app/app.war"]
