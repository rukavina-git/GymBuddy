# 03 Scheduler never drops a request

## Problem
`SyncScheduler.requestSync()` (data/sync/SyncScheduler.kt): the loop
exits once `rerunRequested` is false, and only then does `finally` set
`running` to false. A request that arrives in that gap fails the
compareAndSet, sets `rerunRequested` and returns. Nothing runs it, so
e.g. a SESSION_COMPLETED sync waits for the next foreground or
periodic trigger.

## Change
After `running.set(false)`, check `rerunRequested` again. If it is set,
and `running` can be claimed again, run another loop. Keep it simple; a
`Channel(CONFLATED)` consumer is an acceptable alternative if it reads
more clearly.

## Tests
- A request made while a cycle is running causes exactly one extra
  cycle.
- Hammering requestSync from many coroutines never leaves a request
  unserved (final state: a cycle ran after the last request).
- Signed out: no cycle runs.

## Done when
Tests pass, SyncTriggersTest unchanged and green.
