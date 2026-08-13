# Contributing to TunWeave

Thank you for helping improve TunWeave.

## Before starting

- Search existing issues before opening a duplicate.
- Use a public issue for bugs and feature proposals, but follow `SECURITY.md`
  for vulnerabilities.
- Keep changes focused. Discuss large UI, protocol, routing, or native-library
  changes before investing substantial work.
- Never include real proxy credentials, signing keys, private network details,
  or unredacted device logs.

## Development environment

TunWeave requires JDK 17 and Android SDK 34 or newer. The Android NDK version
declared in `app/build.gradle.kts` is needed only when rebuilding the checked-in
native libraries.

Run the standard verification suite with:

```sh
./gradlew testDebugUnitTest lintDebug assembleDebug
```

The VPN startup test is opt-in because it requires Android VPN consent and a
reachable SOCKS5 configuration:

```sh
./gradlew connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.runVpnSmoke=true
```

## Native code

Read `docs/native.md` before changing JNI declarations or native binaries.
The class name `com/simpleproxy/tun2socks/Tun2socksJni` is a stable internal
native ABI and is intentionally independent from TunWeave's application ID.
Changing it requires rebuilding and validating all four ABI libraries on a
device; a mismatched JNI signature can terminate the Android process.

Do not replace a checked-in `.so` without recording its source version,
archive checksum, build environment, and resulting hashes.

## Pull requests

- Add or update tests for behavioral changes.
- Run the standard verification suite before submitting.
- Update user documentation, privacy disclosures, third-party notices, and the
  changelog when applicable.
- Use clear commit and pull-request descriptions that explain intent and risk.

Unless explicitly stated otherwise, contributions intentionally submitted for
inclusion are licensed under the Apache License 2.0, as described in `LICENSE`.
