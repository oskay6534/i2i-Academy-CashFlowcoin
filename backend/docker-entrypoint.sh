#!/bin/sh
# Keep this script LF-terminated: it is executed inside the Linux container.
set -eu

# Render can provide a PostgreSQL internal URL either with an explicit
# port or without one. PostgreSQL uses 5432 when the port is omitted.
if [ -n "${DATABASE_URL:-}" ]; then
  DB_ADDRESS="${DATABASE_URL#*@}"
  DB_HOSTPORT="${DB_ADDRESS%%/*}"
  DB_DATABASE="${DB_ADDRESS#*/}"

  case "$DB_HOSTPORT" in
    *:*)
      DB_HOST="${DB_HOSTPORT%%:*}"
      DB_PORT="${DB_HOSTPORT##*:}"
      ;;
    *)
      DB_HOST="$DB_HOSTPORT"
      DB_PORT="${POSTGRES_PORT:-5432}"
      ;;
  esac

  export SPRING_DATASOURCE_URL="jdbc:postgresql://${DB_HOST}:${DB_PORT}/${DB_DATABASE}"
fi

exec java \
  ${JAVA_OPTS:-} \
  -Dserver.address=0.0.0.0 \
  -Dserver.port="${PORT:-10000}" \
  -jar /app/app.jar
