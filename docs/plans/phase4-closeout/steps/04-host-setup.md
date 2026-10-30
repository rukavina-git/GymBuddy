# 04 Accounts, first release, DNS, restore test (me)

Manual. The guard hook blocks release commands, so I do these.

1. Neon: create a project in an EU region. Copy the direct and pooled
   connection strings.
2. Render: new Blueprint from `render.yaml` on develop. Set
   `DATABASE_URL`, `DB_PASSWORD`, `FIREBASE_CREDENTIALS_B64`.
3. Wait for the first start; Flyway migrates the empty Neon DB.
   `curl https://<service>.onrender.com/v1/health` returns UP.
4. DNS at the rukavina.app registrar: CNAME `api` → the Render
   hostname. Add `api.rukavina.app` as a custom domain in Render and
   wait for its TLS certificate.
5. `curl https://api.rukavina.app/v1/health` returns UP.
6. GitHub secrets: `BACKUP_DATABASE_URL`, `BACKUP_PASSPHRASE`. Run the
   backup workflow by hand, download the artifact, run `restore.sh`
   into local compose Postgres, check the tables exist.
7. Note the cold-start time after 15+ minutes idle (for testers).

Report back: health URL works, backup and restore worked, cold-start
time. Any fixes go into a new step, not into this one.
