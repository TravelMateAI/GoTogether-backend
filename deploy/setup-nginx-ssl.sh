#!/bin/bash

set -e

echo "🔧 Installing NGINX..."
sudo dnf install -y nginx

echo "🔧 Enabling and starting NGINX..."
sudo systemctl enable nginx
sudo systemctl start nginx

echo "🔐 Creating SSL directory and generating self-signed certificate..."
sudo mkdir -p /etc/nginx/ssl

sudo openssl req -x509 -nodes -days 365 \
  -newkey rsa:2048 \
  -keyout /etc/nginx/ssl/nginx-selfsigned.key \
  -out /etc/nginx/ssl/nginx-selfsigned.crt \
  -subj "/C=LK/ST=Western/L=Colombo/O=University/OU=CS/CN=144.24.107.189"

echo "📝 Creating NGINX reverse proxy config..."
sudo tee /etc/nginx/conf.d/backend.conf > /dev/null <<EOF
server {
    listen 443 ssl;
    server_name 144.24.107.189;

    ssl_certificate /etc/nginx/ssl/nginx-selfsigned.crt;
    ssl_certificate_key /etc/nginx/ssl/nginx-selfsigned.key;

    location /api/ {
        proxy_pass http://localhost:8080/;
        proxy_set_header Host \$host;
        proxy_set_header X-Real-IP \$remote_addr;
        proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto \$scheme;
    }

    location / {
        return 404;
    }
}
EOF

echo "🔓 Allowing HTTPS traffic in firewall..."
sudo firewall-cmd --add-service=https --permanent
sudo firewall-cmd --reload

echo "🔁 Restarting NGINX to apply changes..."
sudo systemctl restart nginx

echo "✅ NGINX with HTTPS reverse proxy is ready on https://144.24.107.189"
