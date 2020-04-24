FROM bellsoft/liberica-openjdk-alpine:11

COPY target/scala-2.13/scrapper-search-service-assembly-0.1.jar /app/app.jar

WORKDIR /app

EXPOSE 30001

CMD java -jar app.jar
