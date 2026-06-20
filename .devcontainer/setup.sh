#!/usr/bin/env bash
# Provisions SDKMAN (Java 21 + Gradle) and nvm (Node LTS) for the gs-swe-challenge dev container.
# Runs as postCreateCommand. Safe to re-run (idempotent-ish).
set -euo pipefail

# Bump these to change versions later. Pinned for reproducible builds.
JAVA_VERSION="21.0.11-tem"     # Temurin 21 LTS
GRADLE_VERSION="8.10.2"
NVM_VERSION="v0.40.3"

echo "==> Installing OS packages (make, build tools, git, archive utils)"
export DEBIAN_FRONTEND=noninteractive
sudo apt-get update -y
sudo apt-get install -y --no-install-recommends \
  build-essential \
  make \
  git \
  curl \
  wget \
  zip \
  unzip \
  ca-certificates
sudo rm -rf /var/lib/apt/lists/*

echo "==> Installing SDKMAN + Java ${JAVA_VERSION} + Gradle ${GRADLE_VERSION}"
export SDKMAN_DIR="$HOME/.sdkman"
if [ ! -d "$SDKMAN_DIR" ]; then
  curl -s "https://get.sdkman.io?rcupdate=true" | bash
fi

# SDKMAN init script references unset vars; relax nounset while sourcing.
set +u
source "$SDKMAN_DIR/bin/sdkman-init.sh"
sdk install java "$JAVA_VERSION"   || true
sdk install gradle "$GRADLE_VERSION" || true
sdk default java "$JAVA_VERSION"
sdk default gradle "$GRADLE_VERSION"
set -u

echo "==> Installing nvm ${NVM_VERSION} + Node LTS"
export NVM_DIR="$HOME/.nvm"
if [ ! -d "$NVM_DIR" ]; then
  curl -o- "https://raw.githubusercontent.com/nvm-sh/nvm/${NVM_VERSION}/install.sh" | bash
fi

set +u
source "$NVM_DIR/nvm.sh"
nvm install --lts
nvm alias default 'lts/*'
set -u

# Expose node/npm/npx globally so VS Code (ESLint/Prettier) and tasks see them,
# regardless of which interactive shell sourced nvm.
NODE_BIN_DIR="$(dirname "$(nvm which default)")"
sudo ln -sf "$NODE_BIN_DIR/node" /usr/local/bin/node
sudo ln -sf "$NODE_BIN_DIR/npm"  /usr/local/bin/npm
sudo ln -sf "$NODE_BIN_DIR/npx"  /usr/local/bin/npx

echo "==> Configuring git hooks + commit identity"
# Point git at the tracked hooks dir so the single-contributor guards
# (.githooks/pre-commit, commit-msg) apply in every clone of this repo.
# Run from the repo root if we're inside it; harmless otherwise.
if git -C "$PWD" rev-parse --is-inside-work-tree >/dev/null 2>&1; then
  git -C "$PWD" config core.hooksPath .githooks
  chmod +x "$PWD/.githooks/"* 2>/dev/null || true
  # Repo-local identity: personal address, independent of the host global config.
  git -C "$PWD" config user.name  "Ezekiel Akoji"
  git -C "$PWD" config user.email "akojimsg@gmail.com"
fi

echo "==> Versions"
java -version    || true
gradle -version  || true
node --version   || true
npm --version    || true
gh --version     || true

echo "==> Dev container provisioning complete."
