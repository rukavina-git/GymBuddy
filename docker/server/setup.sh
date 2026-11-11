#!/usr/bin/env bash
# Hardens a fresh Ubuntu 24.04 box for GymBuddy (ADR 0016).
# Run as root, once; running it again is safe.
#
#   sudo DEPLOY_PUBKEY="ssh-ed25519 AAAA... you@laptop" bash setup.sh
#
# Env:
#   DEPLOY_PUBKEY  required, public key for the deploy user
#   DEPLOY_USER    default "deploy"
#   SKIP_SYSTEMD   "1" skips ufw, swap and service reloads (container test)
set -euo pipefail

DEPLOY_USER="${DEPLOY_USER:-deploy}"
DEPLOY_PUBKEY="${DEPLOY_PUBKEY:-}"
SKIP_SYSTEMD="${SKIP_SYSTEMD:-0}"

log() { printf '==> %s\n' "$*"; }
die() { printf 'error: %s\n' "$*" >&2; exit 1; }

[[ $EUID -eq 0 ]] || die "run as root"
[[ -n "$DEPLOY_PUBKEY" ]] || die "DEPLOY_PUBKEY is required"
[[ "$DEPLOY_PUBKEY" =~ ^(ssh-ed25519|ssh-rsa|ecdsa-sha2-[a-z0-9]+)\  ]] \
  || die "DEPLOY_PUBKEY does not look like an SSH public key"

# Password login gets turned off below; without a root key that locks root out.
if [[ ! -s /root/.ssh/authorized_keys ]]; then
  [[ "$SKIP_SYSTEMD" == "1" ]] || die "root has no SSH key; run ssh-copy-id root@<host> first"
  log "warning: root has no SSH key (allowed only with SKIP_SYSTEMD=1)"
fi

export DEBIAN_FRONTEND=noninteractive

log "packages"
apt-get update -q
apt-get upgrade -yq
apt-get install -yq --no-install-recommends \
  ufw unattended-upgrades ca-certificates curl gnupg openssh-server

log "docker"
install -m 0755 -d /etc/apt/keyrings
if [[ ! -s /etc/apt/keyrings/docker.asc ]]; then
  curl -fsSL https://download.docker.com/linux/ubuntu/gpg -o /etc/apt/keyrings/docker.asc
  chmod a+r /etc/apt/keyrings/docker.asc
fi
# shellcheck source=/dev/null
codename="$(. /etc/os-release && echo "$VERSION_CODENAME")"
echo "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.asc] https://download.docker.com/linux/ubuntu $codename stable" \
  > /etc/apt/sources.list.d/docker.list
apt-get update -q
apt-get install -yq docker-ce docker-ce-cli containerd.io docker-compose-plugin

install -d -m 0755 /etc/docker
cat > /etc/docker/daemon.json <<'EOF'
{
  "log-driver": "json-file",
  "log-opts": { "max-size": "10m", "max-file": "3" }
}
EOF

log "deploy user: $DEPLOY_USER"
if ! id "$DEPLOY_USER" >/dev/null 2>&1; then
  useradd --create-home --shell /bin/bash "$DEPLOY_USER"
fi
passwd -l "$DEPLOY_USER" >/dev/null
usermod -aG docker "$DEPLOY_USER"
home="$(getent passwd "$DEPLOY_USER" | cut -d: -f6)"
install -d -m 0700 -o "$DEPLOY_USER" -g "$DEPLOY_USER" "$home/.ssh"
keys="$home/.ssh/authorized_keys"
touch "$keys"
grep -qxF "$DEPLOY_PUBKEY" "$keys" || printf '%s\n' "$DEPLOY_PUBKEY" >> "$keys"
chown "$DEPLOY_USER:$DEPLOY_USER" "$keys"
chmod 0600 "$keys"

log "ssh: keys only"
install -d -m 0755 /etc/ssh/sshd_config.d /run/sshd
cat > /etc/ssh/sshd_config.d/10-gymbuddy.conf <<'EOF'
PasswordAuthentication no
KbdInteractiveAuthentication no
PermitRootLogin prohibit-password
EOF
sshd -t || die "sshd config invalid; not reloading"

log "unattended-upgrades: security only, no auto-reboot"
cat > /etc/apt/apt.conf.d/20auto-upgrades <<'EOF'
APT::Periodic::Update-Package-Lists "1";
APT::Periodic::Unattended-Upgrade "1";
EOF
cat > /etc/apt/apt.conf.d/52gymbuddy-unattended <<'EOF'
Unattended-Upgrade::Automatic-Reboot "false";
EOF

if [[ "$SKIP_SYSTEMD" == "1" ]]; then
  log "SKIP_SYSTEMD=1: skipping ufw, swap and service reloads"
  exit 0
fi

log "services"
systemctl reload ssh
systemctl enable --now unattended-upgrades
systemctl enable docker
systemctl restart docker

log "firewall"
ufw default deny incoming
ufw default allow outgoing
ufw allow 22/tcp
ufw allow 80/tcp
ufw allow 443/tcp
ufw --force enable

log "swap"
if ! swapon --show --noheadings | grep -q .; then
  fallocate -l 2G /swapfile
  chmod 0600 /swapfile
  mkswap /swapfile
  swapon /swapfile
fi
grep -q '^/swapfile ' /etc/fstab || echo '/swapfile none swap sw 0 0' >> /etc/fstab
echo 'vm.swappiness=10' > /etc/sysctl.d/99-gymbuddy.conf
sysctl -q --system

log "done. Keep this session open and test: ssh $DEPLOY_USER@<host> docker ps"
