FROM hypriot/rpi-mysql:latest

EXPOSE 3306

ENV MYSQL_ROOT_PASSWORD ptsecret
ENV MYSQL_DATABASE searches

RUN apt update
RUN apt install dos2unix

COPY ./create.sql /init.sql
COPY ./run-db.sh /run-db.sh

RUN dos2unix /init.sql -o /init.sql
RUN dos2unix /run-db.sh -o /run-db.sh
RUN chmod +x /run-db.sh

ENTRYPOINT [ "/run-db.sh" ]
CMD ["mysqld"]
