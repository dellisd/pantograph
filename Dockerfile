FROM gradle:jdk17 AS builder

COPY . /home/gradle/src
WORKDIR /home/gradle/src

RUN gradle installDist

FROM openjdk:17-slim-buster

COPY --from=builder /home/gradle/src/build/install/pantograph /usr/src/app
WORKDIR /usr/src/app
CMD ["./bin/pantograph", "--config", "config.yaml", "store.db"]

LABEL org.opencontainers.image.source https://github.com/dellisd/pantograph
