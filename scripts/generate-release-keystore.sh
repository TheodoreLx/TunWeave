#!/usr/bin/env bash
set -euo pipefail

script_dir="$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)"
project_dir="$(CDPATH= cd -- "${script_dir}/.." && pwd)"
signing_dir="${project_dir}/.signing"
keystore_file="${signing_dir}/tunweave-release.jks"
certificate_file="${signing_dir}/tunweave-release-cert.pem"
properties_file="${project_dir}/keystore.properties"
keystore_tmp="${signing_dir}/.tunweave-release.jks.tmp"
certificate_tmp="${signing_dir}/.tunweave-release-cert.pem.tmp"
properties_tmp="${signing_dir}/.keystore.properties.tmp"
key_alias="tunweave-release"

cleanup() {
    unset store_password key_password 2>/dev/null || true
    unset TUNWEAVE_GENERATED_STORE_PASSWORD TUNWEAVE_GENERATED_KEY_PASSWORD 2>/dev/null || true
    rm -f -- "$keystore_tmp" "$certificate_tmp" "$properties_tmp"
}

trap cleanup EXIT HUP INT TERM

for command_name in keytool openssl; do
    if ! command -v "$command_name" >/dev/null 2>&1; then
        echo "Required command not found: $command_name" >&2
        exit 1
    fi
done

if [[ -e "$keystore_file" || -e "$properties_file" ]]; then
    echo "Release signing files already exist; refusing to overwrite them." >&2
    echo "Keystore: $keystore_file" >&2
    echo "Properties: $properties_file" >&2
    exit 1
fi

umask 077
mkdir -p "$signing_dir"

store_password="$(openssl rand -hex 32)"
key_password="$(openssl rand -hex 32)"
export TUNWEAVE_GENERATED_STORE_PASSWORD="$store_password"
export TUNWEAVE_GENERATED_KEY_PASSWORD="$key_password"

keytool -genkeypair \
    -keystore "$keystore_tmp" \
    -storetype JKS \
    -storepass:env TUNWEAVE_GENERATED_STORE_PASSWORD \
    -keypass:env TUNWEAVE_GENERATED_KEY_PASSWORD \
    -alias "$key_alias" \
    -keyalg RSA \
    -keysize 4096 \
    -sigalg SHA256withRSA \
    -validity 25000 \
    -dname "CN=TheodoreLx, OU=TunWeave, O=TheodoreLx" \
    -noprompt

keytool -exportcert -rfc \
    -keystore "$keystore_tmp" \
    -storepass:env TUNWEAVE_GENERATED_STORE_PASSWORD \
    -alias "$key_alias" \
    -file "$certificate_tmp"

{
    printf 'storeFile=.signing/tunweave-release.jks\n'
    printf 'storePassword=%s\n' "$store_password"
    printf 'keyAlias=%s\n' "$key_alias"
    printf 'keyPassword=%s\n' "$key_password"
} > "$properties_tmp"

chmod 600 "$keystore_tmp" "$certificate_tmp" "$properties_tmp"
mv -- "$keystore_tmp" "$keystore_file"
mv -- "$certificate_tmp" "$certificate_file"
mv -- "$properties_tmp" "$properties_file"

echo "Created TunWeave release signing material."
echo "Private keystore: $keystore_file"
echo "Public certificate: $certificate_file"
echo "Local Gradle configuration: $properties_file"
echo "Back up the keystore and passwords separately before publishing a release."
