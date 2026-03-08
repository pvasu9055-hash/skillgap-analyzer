FROM eclipse-temurin:17-jdk

WORKDIR /app

COPY . .

RUN chmod +x gradlew

RUN ./gradlew bootJar --no-daemon -x test

CMD ["java","-jar","build/libs/skillgap-analyzer-0.0.1-SNAPSHOT.jar"]