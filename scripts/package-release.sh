#!/usr/bin/env bash
set -euo pipefail

script_dir="$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)"
project_dir="$(CDPATH= cd -- "${script_dir}/.." && pwd)"
requested_version="${1:-}"
expected_certificate_sha256="17cf445fb8af9a04d63432fdb57c077ea2ede6f7d909abe4b28dc38882422acd"

if [[ ! "$requested_version" =~ ^[0-9]+\.[0-9]+\.[0-9]+([.-][0-9A-Za-z.-]+)?$ ]]; then
    echo "Usage: $0 <version-name>" >&2
    echo "Example: $0 1.0.0" >&2
    exit 2
fi

for command_name in git jarsigner sha256sum; do
    if ! command -v "$command_name" >/dev/null 2>&1; then
        echo "Required command not found: $command_name" >&2
        exit 1
    fi
done

if [[ -n "$(git -C "$project_dir" status --porcelain --untracked-files=normal)" ]]; then
    echo "Refusing to package a release from a dirty Git worktree." >&2
    exit 1
fi

declared_version="$({
    sed -n 's/^[[:space:]]*versionName = "\([^"]*\)"/\1/p' \
        "${project_dir}/app/build.gradle.kts"
} | head -n 1)"

if [[ "$declared_version" != "$requested_version" ]]; then
    echo "Requested version $requested_version does not match Gradle version $declared_version." >&2
    exit 1
fi

apksigner_path="${APKSIGNER:-}"
if [[ -z "$apksigner_path" ]] && command -v apksigner >/dev/null 2>&1; then
    apksigner_path="$(command -v apksigner)"
fi

if [[ -z "$apksigner_path" ]]; then
    for sdk_root in "${ANDROID_SDK_ROOT:-}" "${ANDROID_HOME:-}" /opt/android-sdk; do
        if [[ ! -d "${sdk_root}/build-tools" ]]; then
            continue
        fi
        while IFS= read -r candidate; do
            apksigner_path="$candidate"
        done < <(find "${sdk_root}/build-tools" -mindepth 2 -maxdepth 2 \
            -type f -name apksigner | sort -V)
    done
fi

if [[ -z "$apksigner_path" || ! -x "$apksigner_path" ]]; then
    echo "apksigner was not found. Set APKSIGNER or ANDROID_SDK_ROOT." >&2
    exit 1
fi

apk_source="${project_dir}/app/build/outputs/apk/release/app-release.apk"
unsigned_apk="${project_dir}/app/build/outputs/apk/release/app-release-unsigned.apk"
aab_source="${project_dir}/app/build/outputs/bundle/release/app-release.aab"
release_dir="${project_dir}/app/build/outputs/release-packages/v${requested_version}"
apk_name="TunWeave-v${requested_version}.apk"
aab_name="TunWeave-v${requested_version}.aab"

rm -f -- "$apk_source" "$unsigned_apk" "$aab_source"

(
    cd "$project_dir"
    ./gradlew --no-daemon :app:assembleRelease :app:bundleRelease
)

if [[ ! -f "$apk_source" ]]; then
    echo "Signed APK was not produced. Check the release signing configuration." >&2
    exit 1
fi

if [[ ! -f "$aab_source" ]]; then
    echo "Signed Android App Bundle was not produced." >&2
    exit 1
fi

certificate_sha256="$({
    "$apksigner_path" verify --print-certs "$apk_source"
} | sed -n 's/^Signer #1 certificate SHA-256 digest: //p' | head -n 1)"

if [[ "$certificate_sha256" != "$expected_certificate_sha256" ]]; then
    echo "Unexpected APK signing certificate: $certificate_sha256" >&2
    exit 1
fi

"$apksigner_path" verify --verbose "$apk_source"
jarsigner -verify "$aab_source"

mkdir -p "$release_dir"
install -m 0644 "$apk_source" "${release_dir}/${apk_name}"
install -m 0644 "$aab_source" "${release_dir}/${aab_name}"

(
    cd "$release_dir"
    sha256sum "$apk_name" "$aab_name" > SHA256SUMS
)

echo "Release assets created in: $release_dir"
echo "APK certificate SHA-256: $certificate_sha256"
echo "Assets:"
echo "  $apk_name"
echo "  $aab_name"
echo "  SHA256SUMS"
