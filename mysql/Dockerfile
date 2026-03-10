FROM docker.io/library/mysql:8.0

ENV MYSQL_ROOT_PASSWORD=rootpass
ENV MYSQL_USER=payflow_admin
ENV MYSQL_PASSWORD=PayFlow2025Secure!

COPY init.sql /docker-entrypoint-initdb.d/
