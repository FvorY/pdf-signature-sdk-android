# Contributing

## Setup

- JDK 17
- Android SDK with platform 36 (Android Studio, or the command-line tools)

```bash
git clone git@github.com:FvorY/pdf-signature-sdk-android.git
cd pdf-signature-sdk-android
echo "sdk.dir=/path/to/android/sdk" > local.properties   # not needed when opened in Android Studio
```

## Checks before opening a PR

```bash
./gradlew ktlintCheck :psdk-core:testDebugUnitTest :sample:assembleDebug
```

Fix formatting automatically with `./gradlew ktlintFormat`.

## Dependency versions

All versions live in `gradle/libs.versions.toml`. AndroidX libraries are kept on releases
that still compile against **API 36**. A library passes its minimum `compileSdk` on to every
app that uses it, so raising it forces all users to upgrade. Raise it only on purpose and
note it in `CHANGELOG.md`.

## Public API

The library modules use Kotlin explicit API mode: every public declaration needs an
explicit `public` modifier and KDoc. API change tracking is added in milestone M1.

## Commits and branches

- Branch from `main` (`feat/...`, `fix/...`) and open a pull request.
- Use [Conventional Commits](https://www.conventionalcommits.org/) (`feat:`, `fix:`, `docs:`, `test:`, `chore:`).
- Add a line to `CHANGELOG.md` under **Unreleased**.

## Releasing (maintainer)

1. Set `VERSION_NAME` in `gradle.properties` and move the **Unreleased** notes in
   `CHANGELOG.md` under the new version.
2. Commit, then tag without a `v` prefix and push the tag:
   ```bash
   git tag 1.0.0 && git push origin 1.0.0
   ```
   The **Release** workflow checks the version, creates the GitHub Release and triggers
   the JitPack build. Check the log at https://jitpack.io/#FvorY/pdf-signature-sdk-android.

Never move or delete a published tag. Release a new patch version instead.
