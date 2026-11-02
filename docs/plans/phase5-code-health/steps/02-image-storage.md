# 02 ImageStorageUtil write-then-delete

## Problem
`saveImageToInternalStorage` deletes every file in `profile_images/`
before it copies the new one. If the copy fails (bad URI, full disk,
revoked permission), the old photo is gone and the profile points at a
missing file. The input stream is closed by hand, not with `use`, so
an exception leaks it.

## Change
- Copy into a new file first (`inputStream.use { }`), check it's
  non-empty, then delete the other files in the folder. On failure,
  delete the partial new file and keep the old one.
- Swallowed exceptions keep returning null (callers expect that), but
  log once, no `printStackTrace`.
- No caller changes unless a caller depends on the old order.

## Tests
Robolectric unit tests in `utils/ImageStorageUtilTest.kt`:
- success: new file exists, old file gone, path returned;
- copy fails (stream throws mid-copy or null stream): old file kept,
  no partial file left, null returned.

## Out of scope
Avatars from 6.5, image compression, moving the util into data/.

## Done when
`./gradlew testDebugUnitTest` green.
