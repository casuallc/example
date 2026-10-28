#!/usr/bin/env bash
# Start the demo. An application.yml next to this script overrides the built-in defaults
# (Spring Boot picks up ./application.yml from the working directory automatically).
cd "$(dirname "$0")"

JAR=$(ls rabbitmq-java-springboot-*.jar 2>/dev/null | head -1)
if [ -z "$JAR" ]; then
  echo "ERROR: no rabbitmq-java-springboot-*.jar found, run bin/build.sh first" >&2
  exit 1
fi

exec java ${JAVA_OPTS:--Xmx512m} -jar "$JAR" "$@"
