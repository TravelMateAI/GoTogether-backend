#!/bin/bash

set -e

echo "📁 Creating .env for api-service..."
cat <<EOT > ../api-service/.env
GOOGLE_MAPS_API_KEY=AIzaSyBqQhDKu98x-nZO85f-JVoUGgNUw2W_SWE
GOOGLE_GEMINI_API_KEY=AIzaSyAb08virZswAtMDNWXGGfBxM8ECwmFQS1w
PORT=8000
EOT
echo "✅ Created ../api-service/.env"

echo "📁 Creating .env for social-media-service..."
cat <<EOT > ../social-media-service/.env
CLIENT_ID=785994007278-lr2k84dv19513frlhkid2dvnf396mpa0.apps.googleusercontent.com
CLIENT_SECRET=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
baseUrl=http://localhost:8080
registrationId=keycloak
EOT
echo "✅ Created ../social-media-service/.env"
