#!/bin/bash

if [[ -n ${CREATE_DB} ]]; then
  until mysql -u root -p${MYSQL_ROOT_PASSWORD} < /init.sql; do
    sleep 8
    echo "Waiting for db..."
  done && echo "Provisioning successful" &
fi

exec /entrypoint.sh "$@"
