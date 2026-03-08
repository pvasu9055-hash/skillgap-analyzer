FROM openjdk:17

WORKDIR /app

COPY . .

RUN chmod +x gradlew
RUN ./gradlew build

CMD ["java","-jar","build/libs/*.jar"]