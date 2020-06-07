FROM bellsoft/liberica-openjdk-debian:11 as build-stage

# install sbt
RUN apt update
RUN apt install -y gnupg apt-transport-https ca-certificates
RUN echo "deb https://dl.bintray.com/sbt/debian /" | tee -a /etc/apt/sources.list.d/sbt.list
RUN curl -sL "https://keyserver.ubuntu.com/pks/lookup?op=get&search=0x2EE0EA64E40A89B84B2DF73499E82A75642AC823" | apt-key add
RUN apt update
RUN apt install -y sbt

COPY ./project project
COPY ./build.sbt build.sbt
COPY ./src /src
COPY ./logback.xml logback.xml

WORKDIR /

# build service
RUN sbt assembly
# cleanup
RUN apt purge -y sbt gnupg apt-transport-https ca-certificates
RUN apt autoremove -y
RUN rm -r project
RUN rm -r src

FROM build-stage as develop-stage

EXPOSE 30001

CMD java "-Dlogback.configurationFile=logback.xml" -jar target/scala-2.13/scrapper-search-service-assembly-0.1.jar

FROM build-stage as production-stage

EXPOSE 30001

CMD java "-Dlogback.configurationFile=logback.xml" -jar target/scala-2.13/scrapper-search-service-assembly-0.1.jar
