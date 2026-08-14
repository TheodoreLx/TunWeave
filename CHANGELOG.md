# Changelog

All notable changes to TunWeave will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and the project intends to follow [Semantic Versioning](https://semver.org/).

## [Unreleased]

## [1.0.0] - 2026-08-14

### Added

- Android `VpnService` integration for SOCKS5 TCP and UDP traffic.
- Global and per-app routing modes.
- IPv4 and IPv6 proxying, blocking, and bypass controls.
- Live traffic and Android PSS memory statistics.
- Android Keystore-backed proxy password storage.
- Native-library provenance, checksums, and device smoke tests.
- GitHub Actions verification for unit tests, Android Lint, and Debug APK builds.
- Optional release signing from ignored local properties or environment variables.

### Changed

- Renamed the project from SimpleProxy to TunWeave.
- Changed the Android application ID to `io.github.theodorelx.tunweave`.
- Added centralized redaction for passwords, tokens, authorization values,
  secrets, and private keys while retaining diagnostic network context and
  stack traces. Native HEV logging uses the credential-safe `info` level.

[Unreleased]: https://github.com/TheodoreLx/TunWeave/compare/v1.0.0...HEAD
[1.0.0]: https://github.com/TheodoreLx/TunWeave/releases/tag/v1.0.0
