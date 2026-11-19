#!/usr/bin/env bash
# Hardens a fresh Debian 13 or Ubuntu 24.04 box for GymBuddy (ADR 0016).
# Run as root, once; running it again is safe.
#
#   sudo DEPLOY_PUBKEY="ssh-ed25519 AAAA... you@laptop" bash setup.sh
#
# Env:
#   DEPLOY_PUBKEY  required, public key for the deploy user
#   DEPLOY_USER    default "deploy"
#   SKIP_SYSTEMD   "1" skips ufw, swap and service reloads (container test)
#   NTFY_TOPIC     optional, secret ntfy.sh topic for SSH login pushes
#                  (root logins only). Kept from an earlier run if unset.
set -euo pipefail

DEPLOY_USER="${DEPLOY_USER:-deploy}"
DEPLOY_PUBKEY="${DEPLOY_PUBKEY:-}"
SKIP_SYSTEMD="${SKIP_SYSTEMD:-0}"
NTFY_TOPIC="${NTFY_TOPIC:-}"

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

# Docker's apt repo is per distro; only these two are tested.
# shellcheck source=/dev/null
os_id="$(. /etc/os-release && echo "$ID")"
case "$os_id" in
  debian|ubuntu) ;;
  *) die "unsupported OS '$os_id' (need debian or ubuntu)" ;;
esac

export DEBIAN_FRONTEND=noninteractive

log "packages"
apt-get update -q
# Keep existing conffiles so a changed default can't stop the run with a prompt.
APT_OPTS=(-yq -o Dpkg::Options::=--force-confdef -o Dpkg::Options::=--force-confold)
apt-get upgrade "${APT_OPTS[@]}"
apt-get install "${APT_OPTS[@]}" --no-install-recommends \
  ufw unattended-upgrades ca-certificates curl gnupg openssh-server \
  fail2ban python3-systemd

log "docker"
install -m 0755 -d /etc/apt/keyrings
if [[ ! -s /etc/apt/keyrings/docker.asc ]]; then
  curl -fsSL "https://download.docker.com/linux/$os_id/gpg" -o /etc/apt/keyrings/docker.asc
  chmod a+r /etc/apt/keyrings/docker.asc
fi
# shellcheck source=/dev/null
codename="$(. /etc/os-release && echo "$VERSION_CODENAME")"
echo "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.asc] https://download.docker.com/linux/$os_id $codename stable" \
  > /etc/apt/sources.list.d/docker.list
apt-get update -q
apt-get install "${APT_OPTS[@]}" docker-ce docker-ce-cli containerd.io docker-compose-plugin

install -d -m 0755 /etc/docker
daemon_json='{
  "log-driver": "json-file",
  "log-opts": { "max-size": "10m", "max-file": "3" }
}'
# Restarting Docker restarts the whole stack, so only do it on a change.
docker_changed=0
if [[ "$(cat /etc/docker/daemon.json 2>/dev/null)" != "$daemon_json" ]]; then
  printf '%s\n' "$daemon_json" > /etc/docker/daemon.json
  docker_changed=1
fi

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

log "fail2ban: ban an IP for 1 h after 5 failed SSH logins in 10 min"
# Debian 13 has no auth.log; read sshd from the journal.
cat > /etc/fail2ban/jail.d/10-gymbuddy.conf <<'EOF'
[sshd]
enabled = true
backend = systemd
maxretry = 5
findtime = 10m
bantime = 1h
EOF

log "ssh login alert: ntfy push on root login"
install -d -m 0700 /etc/gymbuddy
topic_file=/etc/gymbuddy/ntfy-topic
if [[ -n "$NTFY_TOPIC" ]]; then
  [[ "$NTFY_TOPIC" =~ ^[A-Za-z0-9_-]{16,64}$ ]] \
    || die "NTFY_TOPIC must be 16-64 letters, digits, _ or - (it is the secret)"
  printf '%s\n' "$NTFY_TOPIC" > "$topic_file"
  chmod 0600 "$topic_file"
fi
[[ -s "$topic_file" ]] || log "warning: no NTFY_TOPIC, login alerts stay off"
cat > /usr/local/sbin/gymbuddy-login-alert <<'EOF'
#!/bin/sh
# Called by pam_exec from /etc/pam.d/sshd. Never blocks or fails a login.
[ "$PAM_TYPE" = "open_session" ] || exit 0
[ "$PAM_USER" = "root" ] || exit 0
topic="$(cat /etc/gymbuddy/ntfy-topic 2>/dev/null)" || exit 0
[ -n "$topic" ] || exit 0
( curl -fsS -m 10 -H "Title: SSH login on $(hostname)" -H "Tags: warning" \
    -d "$PAM_USER from ${PAM_RHOST:-unknown} at $(date -u '+%Y-%m-%d %H:%M UTC')" \
    "https://ntfy.sh/$topic" >/dev/null 2>&1 & )
exit 0
EOF
chmod 0755 /usr/local/sbin/gymbuddy-login-alert
pam_line='session optional pam_exec.so quiet /usr/local/sbin/gymbuddy-login-alert'
grep -qxF "$pam_line" /etc/pam.d/sshd || printf '%s\n' "$pam_line" >> /etc/pam.d/sshd

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
# The unit is ssh on both; Ubuntu 24.04 starts it via ssh.socket.
# reload-or-restart covers both.
systemctl reload-or-restart ssh
systemctl enable --now unattended-upgrades
systemctl enable fail2ban
systemctl restart fail2ban
systemctl enable docker
if [[ "$docker_changed" == "1" ]]; then systemctl restart docker; fi

log "firewall"
ufw default deny incoming
ufw default allow outgoing
ufw allow 22/tcp
ufw allow 80/tcp
ufw allow 443/tcp
ufw --force enable

log "swap"
# Only touch fstab for a swapfile this script made (now or on an earlier run).
if [[ -f /swapfile ]] || ! swapon --show --noheadings | grep -q .; then
  if [[ ! -f /swapfile ]]; then
    fallocate -l 2G /swapfile
    chmod 0600 /swapfile
    mkswap /swapfile
  fi
  swapon --show=NAME --noheadings | grep -qx /swapfile || swapon /swapfile
  grep -q '^/swapfile ' /etc/fstab || echo '/swapfile none swap sw 0 0' >> /etc/fstab
fi
echo 'vm.swappiness=10' > /etc/sysctl.d/99-gymbuddy.conf
sysctl -q --system

log "done. Keep this session open and test: ssh $DEPLOY_USER@<host> docker ps"
