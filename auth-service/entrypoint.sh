#!/bin/sh
echo "Waiting for Keycloak to be ready..."
# Check internal Keycloak endpoint
until curl -sSf http://keycloak:8080/realms/kong/.well-known/openid-configuration; do
  echo "Waiting for Keycloak service (kong realm) at http://keycloak:8080...";
  sleep 10;
done
# Optionally, also check external Keycloak endpoint if services depend on its public availability during startup
# until curl -sSf -k https://gotogetheruom.duckdns.org:8446/realms/kong/.well-known/openid-configuration; do
#   echo "Waiting for Keycloak public endpoint (kong realm)...";
#   sleep 10;
# done
echo "Keycloak realm is ready. Starting app..."
exec java -jar /app.jar
