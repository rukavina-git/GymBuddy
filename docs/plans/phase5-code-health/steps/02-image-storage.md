# 02 Remove custom profile photos

## Problem
v1 has no custom photos (owner, 2026-11-02; council you-settings):
the profile gets a preset avatar id like `cat` in 6.5, local for
guests and synced for accounts. The old picker is still in the code:
`ProfileScreen` copies a picked photo via `ImageStorageUtil` (which
deletes the old file before writing the new one), and
`ProfileViewModel` saves the phone's file path as `profileImageUrl`,
which syncs to the server as if it were a URL.

## Change
- Delete `utils/ImageStorageUtil.kt`, the picker launcher and its
  tap target in `ProfileScreen`, and `ProfileViewModel.onImageSelected`.
- Drop `profileImageUri` from `ProfileUiState`, `SettingsUiState`, and
  the Home/Settings/Profile avatar composables; they show the existing
  initial/placeholder fallback.
- Saving the profile writes `profileImageUrl = null`, so junk paths
  already on the server are cleared on the next save.
- Account deletion still removes the old `profile_images/` folder
  (inline `File(filesDir, "profile_images").deleteRecursively()` in
  `SyncModule`), so devices that picked a photo don't keep it.

## Tests
- ProfileViewModelTest: saving sends `profileImageUrl = null`.
- Existing AccountDeletionCoordinator tests still pass.

## Out of scope
Avatar picker and avatar id (6.5, spec change). Removing the
`profileImageUrl` column or spec field (6.5 replaces it). No Room
schema or `api/openapi.yaml` change here.

## Done when
`./gradlew testDebugUnitTest` green, `grep -rn "ImageStorageUtil\|PickVisualMedia" app/src`
finds nothing.
