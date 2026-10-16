# Compatibility check

A minimal app that uses the SDK from Maven Local with older build tools than the SDK itself.
CI publishes the SDK and builds this app, so a dependency or Kotlin bump that would break older
apps fails the pull request. It is not part of the main Gradle build.

```bash
./gradlew :psdk-core:publishToMavenLocal :psdk-pdfium:publishToMavenLocal
gradle -p compat :app:assembleRelease -Pagp=8.9.1 -Pkotlin=2.1.0 -PcompileSdk=36
gradle -p compat :app:assembleRelease -Pagp=8.9.1 -Pkotlin=2.2.21 -PcompileSdk=36 -Ppdfium=true
```

To check a JitPack build instead of Maven Local, add
`-PsdkGroup=com.github.FvorY.pdf-signature-sdk-android -PsdkVersion=<tag or commit>`.

Use Gradle 8.11.1 (the oldest version AGP 8.9 supports).
