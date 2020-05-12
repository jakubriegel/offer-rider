FROM mysql:latest

EXPOSE 3306

ENV MYSQL_ROOT_PASSWORD ptsecret
ENV MYSQL_DATABASE searches

COPY ./create.sql /init.sql
COPY ./run-db.sh /run-db.sh
RUN ["chmod", "+x", "/run-db.sh" ]

ENTRYPOINT [ "/run-db.sh" ]
CMD ["mysqld"]
