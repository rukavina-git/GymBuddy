# 04 Backend JSON logs, sync metrics, client version headers

## Problem
On Render the backend logs plain text with no request context: a
`traceId` is generated only for error bodies (`ErrorResponseWriter`),
never logged with the request, and nothing says which user or client
version sent it. There are no counters for sync volume, conflicts or
410s. Architecture plan §8 (versioning) says clients send
`X-Client-Version` and `X-Client-Platform`; Android doesn't, and the
spec doesn't mention them.

## Change
Backend:
- A request filter, first in the chain, that puts `traceId` (new
  UUID per request), `clientVersion`, `clientPlatform` into MDC, and
  `userId` (Firebase uid) once `FirebaseAuthenticationFilter`
  authenticates. Cleared after the request.
- `ErrorResponseWriter` uses the MDC `traceId` instead of a new one,
  so the error body and the log line match.
- JSON logs via Spring Boot's structured logging
  (`logging.structured.format.console`, ECS or logstash), on when a
  profile or env var says so (Render), plain text locally.
- Micrometer counters (actuator is already on the classpath): pushed
  changes, pulled changes, conflicts, 410 resyncs, tagged by entity
  type where cheap. Exposed only if actuator exposure already allows
  it; no new public endpoint.
- No log line contains the ID token, email or the DB URL.

Android:
- An OkHttp interceptor on the API client adds
  `X-Client-Version: <versionName>` and `X-Client-Platform: android`.

**Spec change (docs only):** describe both headers in the wire rules
(`info.description` and api/README.md). No per-operation header
parameters, so the generated client's signatures don't change.

## Tests
- Backend: a request with both headers logs them in MDC (log capture);
  an error response's `traceId` equals the logged one; counters
  increase after a push with a conflict and after a 410.
- `OpenApiSpecDriftTest` still passes.
- Android: interceptor unit test (MockWebServer) sees both headers.
- `npm run lint` in api/.

## Out of scope
Log shipping, dashboards, alerts, force-upgrade logic, rate limiting
by version.

## Done when
`./gradlew test` (backend) and `./gradlew testDebugUnitTest` (android)
green, spec lint green.

Manual (me): after a Render deploy, one sync from the phone shows a
JSON log line with traceId, userId and client version.
