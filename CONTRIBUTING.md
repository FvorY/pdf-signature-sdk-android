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

All versions live in `gradle/libs.versions.toml`. A library passes its dependencies' minimum
`compileSdk`, Android Gradle Plugin version and Kotlin metadata version on to every app that uses
it, so raising a dependency can force all users to upgrade. The supported minimums are listed in
the README and checked by the `compat` CI job (see `compat/README.md`). Before raising a
dependency, check its AAR metadata and run the compat builds; note any new minimum in
`CHANGELOG.md`.

## Public API

The library modules use Kotlin explicit API mode: every public declaration needs an
explicit `public` modifier and KDoc. Public API changes are not checked by a tool yet; list them
in `CHANGELOG.md`.

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
