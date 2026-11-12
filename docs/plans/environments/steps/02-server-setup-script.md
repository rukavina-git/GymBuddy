# 02 Server setup script

## Problem
ADR 0016 says a fresh Ubuntu LTS box is hardened by a script in the
repo, so a rebuild or a host move is one command, not a memory test.
No such script exists yet. The server is bought only at step 05, so
this step writes and checks the script without running it on a VPS.

## Change
- `docker/server/setup.sh` (bash, `set -euo pipefail`, run as root
  once on a fresh Ubuntu 24.04 box, safe to run again):
  - Args/env: `DEPLOY_USER` (default `deploy`), `DEPLOY_PUBKEY`
    (required, the public key text). No secrets, no IP, no hostname.
  - apt update/upgrade; install `ufw`, `unattended-upgrades`,
    `ca-certificates`, `curl`.
  - Docker Engine + Compose plugin from Docker's apt repo (signed key,
    `docker-ce docker-ce-cli containerd.io docker-compose-plugin`).
  - Deploy user: created if missing, key in `authorized_keys`
    (0600/0700), in group `docker`, no password, no sudo.
  - SSH: drop-in `/etc/ssh/sshd_config.d/10-gymbuddy.conf` with
    `PasswordAuthentication no`, `KbdInteractiveAuthentication no`,
    `PermitRootLogin prohibit-password`. `sshd -t` before reload; abort
    if it fails. Root keeps key login as the way back in.
  - ufw: default deny incoming, allow 22, 80, 443; enable.
  - unattended-upgrades: security updates on, auto-reboot off.
  - Swap: 2 GB `/swapfile` if none exists, in fstab, `vm.swappiness=10`.
  - Docker log rotation in `/etc/docker/daemon.json` (json-file,
    10m × 3) so logs can't fill the 60 GB disk.
- `docker/README.md`: new "Server setup" section: how to run the
  script (scp + `sudo DEPLOY_PUBKEY=... bash setup.sh`), what it
  changes, and that you keep the root session open until a deploy-user
  login works.
- ADR 0016 licence table: add Docker Compose (Apache 2.0), ufw
  (GPL-3) and unattended-upgrades (GPL-2), noting they only run on the
  server and are never shipped (progress Gotcha from 01).

## Tests
- `shellcheck docker/server/setup.sh` clean (run via the
  `koalaman/shellcheck` image; dev tool only, not shipped).
- Smoke run in an `ubuntu:24.04` container with `SKIP_SYSTEMD=1`
  covering the parts a container can do (packages, user, key, sshd
  drop-in + `sshd -t`, daemon.json), run twice to prove it is
  idempotent. ufw, swap and service reloads are skipped there and
  first run for real at step 05.

## Out of scope
Compose, nginx, certbot (03); CI, GHCR, deploy key in GitHub (04);
buying the VPS or running the script on it (05); fail2ban, monitoring.

## Done when
shellcheck clean, container smoke run passes twice, README and ADR
updated, progress and roadmap ticked.
