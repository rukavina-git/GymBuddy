# 12. Token verification uses the Firebase Admin SDK

Date: 2026-08-15
Status: Accepted

## Context

The backend must verify Firebase ID tokens on every request: fetch and
cache Google's JWKS, verify the RS256 signature, and check issuer,
audience and expiry.

This is roughly 100 lines with `nimbus-jose-jwt`, and doing it by hand
avoids a heavy dependency and the credential management it brings.

## Decision

Use the Firebase Admin SDK's `verifyIdToken`.

The service account credential is supplied as base64-encoded JSON in the
`FIREBASE_CREDENTIALS_B64` environment variable — never a file on disk.
Locally it comes from the macOS Keychain; in production from the host
environment. The application fails at startup if it is absent or
malformed, rather than on the first request.

## Consequences

- Account deletion requires the Admin SDK regardless, so hand-rolling
  verification would mean two Firebase integrations that could disagree.
- Key rotation, JWKS caching and clock-skew tolerance are handled by the
  SDK rather than by us.
- All providers — email, Google, Apple — produce the same token format
  and the same verification path. Adding a provider needs no backend
  change.
- Provisioning must tolerate missing claims: email/password sign-ins
  have no display name or photo, and Apple private-relay sign-ins supply
  a forwarding address and omit the name after first use.
- The credential grants full administrative access to the Firebase
  project. It exists in no file in or near the repository, and a
  pre-commit hook blocks staged content containing private key material.
