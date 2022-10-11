FROM maven:3.8.4-openjdk-17

RUN mkdir /app

COPY . /app

WORKDIR /app

RUN mvn clean package spring-boot:repackage

CMD ["java", "-jar", "target/account-graphql-0.0.1.jar"]