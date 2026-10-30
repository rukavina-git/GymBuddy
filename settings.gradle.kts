// IDE-only composite build: lets IntelliJ open the repo root as one
// project. backend/ and android/ stay independent builds; CI and the
// commands in CLAUDE.md still run inside each folder.
rootProject.name = "gymbuddy"

includeBuild("backend")
includeBuild("android")
