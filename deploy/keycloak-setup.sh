#!/bin/sh

# Start Keycloak in background
/opt/keycloak/bin/kc.sh start-dev \
  --http-enabled=true \
  --hostname-strict=false \
  --hostname-strict-https=false &
KEYCLOAK_PID=$!

# Wait for Keycloak to become ready
echo "⏳ Waiting for Keycloak to start (checking kcadm connection)..."
until /opt/keycloak/bin/kcadm.sh config credentials \
  --server http://localhost:8080 \
  --realm master \
  --user ${KEYCLOAK_ADMIN:-admin} \
  --password ${KEYCLOAK_ADMIN_PASSWORD:-admin} \
  --config /tmp/kcadm.config >/dev/null 2>&1; do
  sleep 2
done
echo "✅ Keycloak is up and authenticated."

# Disable SSL requirement in master realm
/opt/keycloak/bin/kcadm.sh update realms/master \
  -s sslRequired=NONE \
  --config /tmp/kcadm.config

# ✅ Import the realm file if it exists
if [ -f "/opt/keycloak/data/import/kong-realm.json" ]; then
  echo "📦 Importing realm from kong-realm.json..."
  /opt/keycloak/bin/kcadm.sh create realms \
    -f /opt/keycloak/data/import/kong-realm.json \
    --config /tmp/kcadm.config || echo "⚠️  Realm import may have failed or already exists."
else
  echo "❌ kong-realm.json not found at /opt/keycloak/data/import/"
fi

# Keep Keycloak running
wait $KEYCLOAK_PID
