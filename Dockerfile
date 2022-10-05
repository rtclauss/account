FROM maven:3.8.6-jdk-17

RUN mkdir /app

COPY . /app

RUN chown -R 1000 /app

WORKDIR /app

USER 1000


#docker build . -t openjdk17
#docker run -u 1000 --rm -it -v shared_m2_host:/var/maven/.m2 -v project_path:/app -e MAVEN_CONFIG=/var/maven/.m2 openjdk18 bash
#mvn -Duser.home=/var/maven