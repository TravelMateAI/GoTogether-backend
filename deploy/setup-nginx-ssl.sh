#!/bin/bash

set -e

DOMAIN="gotogetheruom.duckdns.org"
NGINX_CONF="/etc/nginx/conf.d/backend.conf"

echo "🔧 Installing NGINX..."
sudo dnf install -y nginx

echo "🔧 Enabling and starting NGINX..."
sudo systemctl enable nginx
sudo systemctl start nginx

echo "📝 Creating NGINX reverse proxy config..."
sudo tee $NGINX_CONF > /dev/null <<'EOF' 
# Redirect HTTP to HTTPS
server {
    listen 80;
    server_name gotogetheruom.duckdns.org;

    location / {
        return 301 https://$host$request_uri;
    }
}

# HTTPS reverse proxy
server {
    listen 443 ssl;
    server_name gotogetheruom.duckdns.org;

    ssl_certificate /etc/letsencrypt/live/gotogetheruom.duckdns.org/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/gotogetheruom.duckdns.org/privkey.pem;

    location /api/ {
        proxy_pass http://127.0.0.1:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    location /auth/ {
        proxy_pass http://127.0.0.1:8084/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;

        sub_filter '/resources/' '/auth/resources/';
        sub_filter '/js/' '/auth/js/';
        sub_filter '/realms/' '/auth/realms/';
        sub_filter '/admin/' '/auth/admin/';
        sub_filter '/auth/' '/auth/auth/';
        sub_filter '/protocol/' '/auth/protocol/';
        sub_filter '/favicon.ico' '/auth/favicon.ico';
        sub_filter '/welcome-content/' '/auth/welcome-content/';
        sub_filter '/login-actions/' '/auth/login-actions/';
        sub_filter_once off;

        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
    }

    location / {
        return 404;
    }
}
EOF

echo "🔓 Configuring firewall rules..."
sudo firewall-cmd --add-service=http --permanent
sudo firewall-cmd --add-service=https --permanent
sudo firewall-cmd --reload

echo "🔁 Testing and restarting NGINX..."
sudo nginx -t && sudo systemctl restart nginx

echo "🎉 HTTPS reverse proxy is live at https://$DOMAIN"

# sudo dnf install epel-release -y  # Just in case
# sudo dnf install snapd -y
# sudo systemctl enable --now snapd.socket
# sudo ln -s /var/lib/snapd/snap /snap

# # Restart shell to pick up `snap`
# exec "$SHELL"

# # Install Certbot
# sudo snap install core
# sudo snap refresh core
# sudo snap install --classic certbot

# # Link certbot
# sudo ln -s /snap/bin/certbot /usr/bin/certbot

# # Now install nginx plugin (optional if you're using certbot directly with nginx)
# sudo snap set certbot trust-plugin-with-root=ok
# sudo snap install certbot-dns-cloudflare  # optional, only if using Cloudflare

# sudo dnf install python3-pip -y
# pip3 install certbot certbot-nginx --break-system-packages


# sudo certbot --nginx
