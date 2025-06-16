# #!/bin/bash
# set -e

# echo "🟢 Updating system..."
# sudo dnf update -y

# echo "📦 Installing Git..."
# sudo dnf install -y git

# echo "☕ Installing Java 17 (OpenJDK)..."
# sudo dnf install -y java-17-openjdk-devel

# echo "✅ Setting JAVA_HOME..."
# JAVA_PATH=$(dirname $(dirname $(readlink -f $(which java))))
# echo "export JAVA_HOME=$JAVA_PATH" | sudo tee -a /etc/profile.d/java.sh
# echo "export PATH=\$JAVA_HOME/bin:\$PATH" | sudo tee -a /etc/profile.d/java.sh
# source /etc/profile.d/java.sh

# echo "🧪 Verifying Java..."
# java -version
# echo "JAVA_HOME=$JAVA_HOME"

# echo "🐳 Installing Docker..."
# sudo dnf config-manager --add-repo https://download.docker.com/linux/centos/docker-ce.repo
# sudo dnf install -y docker-ce docker-ce-cli containerd.io
# sudo systemctl enable --now docker

# echo "🐳 Installing Docker Compose..."
# sudo curl -L "https://github.com/docker/compose/releases/download/1.29.2/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
# sudo chmod +x /usr/local/bin/docker-compose

# echo "✅ All setup complete!"
sudo ln -s /usr/libexec/docker/cli-plugins/docker-compose /usr/local/bin/docker-compose
docker-compose version
