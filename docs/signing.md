# Release signing

Android requires every installable APK to be signed. The same signing
certificate must be retained for future updates distributed outside Google
Play. With Play App Signing, the local key can instead be used as an upload key
and reset through Play Console if necessary.

## Local signing setup

Generate the initial TunWeave release keystore once:

```sh
./scripts/generate-release-keystore.sh
```

The script creates:

- `.signing/tunweave-release.jks`: private signing key and certificate
- `.signing/tunweave-release-cert.pem`: exportable public certificate
- `keystore.properties`: local Gradle signing configuration and passwords

All three paths are excluded from Git. The script refuses to overwrite an
existing keystore or properties file. It uses RSA-4096, SHA-256, two random
256-bit passwords, restrictive file permissions, and a certificate validity of
25,000 days.

Build signed release artifacts with:

```sh
./gradlew assembleRelease bundleRelease
```

When `keystore.properties` is absent, release builds remain unsigned. A partial
signing configuration fails during Gradle configuration instead of silently
using the wrong key.

## Environment-variable setup

Release automation can provide all four values without creating
`keystore.properties`:

```text
TUNWEAVE_KEYSTORE_FILE
TUNWEAVE_KEYSTORE_PASSWORD
TUNWEAVE_KEY_ALIAS
TUNWEAVE_KEY_PASSWORD
```

The keystore file must already exist in the runner. Store its encoded contents
and passwords in the release platform's encrypted secret store, reconstruct it
in a temporary location, and delete the temporary file after signing. Never
print secret values or enable shell tracing in a signing job.

## Verify an APK

Use the Android SDK Build Tools version installed on the machine:

```sh
apksigner verify --verbose --print-certs \
  app/build/outputs/apk/release/app-release.apk
```

Record the SHA-256 certificate digest in release notes so users can check that
future packages use the same identity.

## TunWeave release certificate

The certificate generated for the initial GitHub release has the following
public identity:

```text
Subject: CN=TheodoreLx, OU=TunWeave, O=TheodoreLx
SHA-256: 17:CF:44:5F:B8:AF:9A:04:D6:34:32:FD:B5:7C:07:7E:A2:ED:E6:F7:D9:09:AB:E4:B2:8D:C3:88:82:42:2A:CD
Valid: 2026-08-12 through 2095-01-22
```

Treat a different certificate digest on a purported GitHub release as a
package-integrity failure unless an official key rotation has been announced.

## Backup and recovery

Before the first public release:

1. Back up the JKS file in at least two encrypted locations.
2. Store the passwords in a password manager separately from the JKS backups.
3. Record the alias and SHA-256 certificate digest.
4. Test restoring the backup and producing a verifiable signed APK.

Loss of a self-managed app signing key prevents publishing compatible updates.
If Google Play distribution is planned, enable Play App Signing and use a
separate upload key where practical.
