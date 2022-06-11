FROM gradle:jdk17 AS builder

COPY . /home/gradle/src
WORKDIR /home/gradle/src

RUN gradle shadowJar

FROM openjdk:17-slim-buster

COPY --from=builder /home/gradle/src/build/libs /usr/src/app
WORKDIR /usr/src/app
CMD ["java", "-jar", "pantograph.jar", "--config", "config.yaml", "data.db"]

LABEL org.opencontainers.image.source https://github.com/dellisd/pantograph
