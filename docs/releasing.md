# Releasing TunWeave

This checklist is for project maintainers. Release signing remains a local,
manual trust boundary: private keys and passwords must not be stored in Git,
GitHub Actions, release attachments, issues, or logs.

## 1. Prepare the release commit

1. Start from a clean, up-to-date `main` branch.
2. Update `versionCode` and `versionName` in `app/build.gradle.kts`.
3. Move the relevant entries from `Unreleased` into a dated version section in
   `CHANGELOG.md`.
4. Run the standard verification suite:

   ```sh
   ./gradlew testDebugUnitTest lintDebug assembleDebug
   ```

5. Merge the release-preparation pull request only after CI succeeds.

Version codes must increase for every Android update. Published version names
and Git tags must never be reused for different content.

## 2. Build signed assets

Confirm that the signing key has current encrypted backups and that either the
ignored `keystore.properties` file or all four documented signing environment
variables are configured. Then run:

```sh
./scripts/package-release.sh 1.0.0
```

The script:

- checks that the requested version matches the Gradle configuration;
- builds the Release APK and Android App Bundle;
- rejects an unsigned APK or an unexpected signing certificate;
- verifies the AAB signature;
- writes consistently named assets and `SHA256SUMS` below
  `app/build/outputs/release-packages/`.

The APK is the sideloading artifact for GitHub users. The AAB is intended for
an application store and cannot be installed directly with `adb`.

## 3. Validate on a device

Install the packaged APK as an update so Android verifies signing continuity:

```sh
adb install -r \
  app/build/outputs/release-packages/v1.0.0/TunWeave-v1.0.0.apk
```

On at least one supported physical device:

1. Cold-start the app and confirm that it remains alive.
2. Connect through a representative SOCKS5 server.
3. Verify an HTTPS request, DNS resolution, IPv4, and IPv6 behavior applicable
   to the selected mode.
4. Compare TunWeave traffic counters with a controlled transfer.
5. Disconnect and reconnect, including from the Quick Settings tile when
   available.
6. Review crash records and logs for secrets before retaining or sharing them.

## 4. Tag and draft the GitHub release

Create an annotated tag from the exact verified release commit, push the tag,
and create a draft GitHub Release. Attach only:

- `TunWeave-vX.Y.Z.apk`
- `TunWeave-vX.Y.Z.aab`
- `SHA256SUMS`

Release notes must summarize the changelog, identify the minimum Android
version, explain that the AAB is for stores, and publish the release
certificate SHA-256 digest recorded in `docs/signing.md`.

Before publishing, download the draft assets into a new temporary directory,
verify `SHA256SUMS`, and run `apksigner verify --print-certs` on the downloaded
APK. A draft must not be published while device validation or CI is failing.

## 5. After publishing

1. Confirm that the tag, release title, assets, checksums, and source archives
   are publicly accessible.
2. Install the downloaded APK on a device that already has the previous
   release when one exists.
3. Keep the signing key offline except while producing a release.
4. Open a new `Unreleased` changelog section and increment `versionCode` before
   the next Android release.
