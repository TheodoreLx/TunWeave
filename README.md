# TunWeave

[![Android CI](https://github.com/TheodoreLx/TunWeave/actions/workflows/android-ci.yml/badge.svg)](https://github.com/TheodoreLx/TunWeave/actions/workflows/android-ci.yml)

TunWeave turns a SOCKS5 proxy reachable from an Android device into a local
Android VPN. Applications send traffic to Android's TUN interface, and
TunWeave forwards it through the configured SOCKS5 server with
[hev-socks5-tunnel](https://github.com/heiher/hev-socks5-tunnel).

The project is intended for private, trusted SOCKS5 services on a local network
or another network reachable by the device. It does not operate a proxy server
or cloud service of its own.

## Features

- SOCKS5 TCP and UDP forwarding through Android `VpnService`
- IPv4 and IPv6 proxying by default
- Optional IPv6 blocking or explicit bypass mode
- Global, allowlist, and blocklist per-app routing
- Custom DNS, LAN bypass routes, MTU, and automatic reconnect settings
- Quick Settings tile for connecting and disconnecting
- Live upload, download, connection-time, and Android PSS memory statistics
- Android Keystore-backed encryption for the stored proxy password
- Detailed diagnostic logs with automatic credential and key redaction

## Requirements and compatibility

- Android 8.0 (API 26) or newer
- A reachable SOCKS5 server with UDP support if proxied UDP traffic is required
- Android VPN consent

The checked-in native libraries support `arm64-v8a`, `armeabi-v7a`, `x86`, and
`x86_64`. Phones and tablets are the currently supported form factors. Android
TV has not yet been adapted or validated for launcher, D-pad, and text-input
requirements.

## Usage

1. Enter the SOCKS5 server address and port. Add credentials if the server
   requires username/password authentication.
2. Review DNS, IPv6, bypass, and per-app routing options in **Settings**.
3. Tap the connection button and approve Android's VPN consent dialog.
4. Confirm the connected state and traffic counters on the home screen.

The SOCKS5 server must be reachable outside the VPN path. TunWeave excludes its
own process from VPN routing so the native proxy connection does not loop back
into the TUN interface.

## Security model

TunWeave creates a device-local VPN path to a user-selected SOCKS5 server. It
does not add encryption to SOCKS5 itself. Use a proxy and local network you
trust, and rely on application-layer encryption such as TLS for sensitive
traffic.

IPv6 is proxied by default. Selecting IPv6 bypass intentionally lets IPv6
traffic use the underlying network and can disclose traffic outside the proxy.
Likewise, configured LAN routes and per-app exclusions intentionally bypass the
VPN.

Diagnostic logs retain proxy and DNS endpoints, test URLs, route addresses,
selected package names, file paths, raw exception messages, and stack traces so
network and device-specific failures can be investigated. Before application
logs are written or displayed, TunWeave redacts configured passwords and
recognized password, token, authorization, secret-key, and private-key values.

Native HEV logs go directly to Android logcat and cannot pass through that
redactor. They therefore use `info` rather than `debug`: the upstream client
debug stream contains a credential-bearing line, while the audited client info
stream retains connection and handshake diagnostics without printing the
credential values. Logs are never uploaded automatically. Because endpoints,
package names, paths, and other environment details can still be sensitive,
review logs before sharing them.

Read [PRIVACY.md](PRIVACY.md) for on-device data handling and [SECURITY.md](SECURITY.md)
for vulnerability reporting and security boundaries.

## Build from source

Requirements:

- JDK 17
- Android SDK 34 or newer
- Android NDK 26.1.10909125 only when rebuilding native libraries

Build, test, and lint the debug application:

```sh
./gradlew testDebugUnitTest lintDebug assembleDebug
```

The same verification suite runs in GitHub Actions for every pull request and
push to `main`. CI uses only the checked-in debug configuration; it does not
have access to release signing keys or proxy credentials.

The debug APK is written to:

```text
app/build/outputs/apk/debug/app-debug.apk
```

Install it on a connected device with:

```sh
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Release signing is optional and never reads credentials from tracked files.
See [docs/signing.md](docs/signing.md) for local key generation, environment
variables, signature verification, and backup requirements.

Maintainers can use [scripts/package-release.sh](scripts/package-release.sh)
to build, verify, and checksum consistently named release assets. The complete
release checklist is documented in [docs/releasing.md](docs/releasing.md).

## Device tests

Run the JNI loading smoke test on a connected device:

```sh
./gradlew connectedDebugAndroidTest
```

The VPN startup smoke test is opt-in because it requires prior VPN consent and
a reachable SOCKS5 configuration on the device:

```sh
./gradlew connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.runVpnSmoke=true
```

It starts the VPN, verifies that the native engine remains alive, and then
disconnects. The test is skipped unless the argument is supplied.

## Native libraries

Checked-in native libraries are reproducibly built from upstream
`hev-socks5-tunnel` 2.14.4, revision `4d6c334`. See
[docs/native.md](docs/native.md) for provenance, checksums, JNI compatibility,
and rebuild instructions.

The stable internal JNI class remains
`com.simpleproxy.tun2socks.Tun2socksJni`; it is intentionally independent from
the public application ID `io.github.theodorelx.tunweave`.

## Project status

TunWeave is a young project. Configuration formats, behavior, and UI may evolve
between releases. Review the changelog and open issues before relying on it for
unattended or critical connectivity.

## Contributing

Bug reports and focused pull requests are welcome. Read
[CONTRIBUTING.md](CONTRIBUTING.md) before contributing and use the private
reporting process in [SECURITY.md](SECURITY.md) for suspected vulnerabilities.

## License

Copyright 2026 TheodoreLx.

Licensed under the [Apache License 2.0](LICENSE). Third-party attribution and
native-library notices are listed in [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md)
and [NOTICE](NOTICE).
