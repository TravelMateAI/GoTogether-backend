#!/bin/sh
echo "Waiting for Keycloak to be ready..."
until curl -sSf http://auth-service:8080/realms/kong/.well-known/openid-configuration; do
  echo "Waiting for realm kong...";
  sleep 10;
done
until curl -sSf https://gotogetheruom.duckdns.org:8443/realms/kong/.well-known/openid-configuration; do
  echo "Waiting for realm kong...";
  sleep 10;
done
echo "Keycloak realm is ready. Starting app..."
exec java -jar /app.jar
