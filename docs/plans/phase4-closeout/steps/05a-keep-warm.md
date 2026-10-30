# 05a Keep the free Render instance awake (me)

Manual. No code change.

## Why
Render's free instance sleeps after ~15 min without traffic. Spring
takes ~149s to start there, so the first sync after idle waits ~2.5
min. A tester trying sync would hit that. Pinging every 5 min keeps it
awake. One always-on service uses ~730 of the 750 free hours a month.

UptimeRobot, not a GitHub Action: Actions cron often runs 10–30 min
late (can miss the 15 min window), only runs from main, and is
disabled in public repos after 60 days without activity.

## Steps
1. UptimeRobot free account. New HTTP(s) monitor:
   `https://api.rukavina.app/v1/health`, interval 5 min, alert by
   email.
2. Wait 20+ minutes without using the app, then
   `curl -s -w '\n%{time_total}s\n' https://api.rukavina.app/v1/health`
   returns UP in under 2 s.
3. Render dashboard: no "spun down" event in the logs after step 1.

## Out of scope
Paid Render plan, VPS move, Spring startup tuning.

## Done when
The monitor is green, and the curl after 20+ idle minutes is fast.
Report back: interval used, curl time.
