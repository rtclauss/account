FROM maven:3.8.4-openjdk-17

RUN mkdir /app

COPY . /app

WORKDIR /app

RUN mvn clean package spring-boot:repackage -DskipTests=true

# normal run
CMD ["java", "-jar", "target/account-graphql-0.0.1.jar"]

# Debug
#CMD ["java", "-Xdebug", "-Xrunjdwp:server=y,transport=dt_socket,address=7777,suspend=n", "-jar", "target/account-graphql-0.0.1.jar"]
