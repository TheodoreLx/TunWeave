#!/usr/bin/env sh
set -eu

hev_version="2.14.4"
hev_revision="4d6c334"
archive_sha256="90e06a5dc0c139c335d50f5a1645672113a366d30066bc5ff056ab73dc39a24d"
archive_url="https://github.com/heiher/hev-socks5-tunnel/releases/download/${hev_version}/hev-socks5-tunnel-${hev_version}.tar.xz"

script_dir=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
project_dir=$(CDPATH= cd -- "${script_dir}/.." && pwd)
work_dir=$(mktemp -d "${TMPDIR:-/tmp}/tunweave-hev.XXXXXX")
trap 'rm -rf -- "$work_dir"' EXIT HUP INT TERM

if [ -n "${ANDROID_NDK_HOME:-}" ] && [ -x "${ANDROID_NDK_HOME}/ndk-build" ]; then
    ndk_build="${ANDROID_NDK_HOME}/ndk-build"
elif [ -n "${ANDROID_NDK_ROOT:-}" ] && [ -x "${ANDROID_NDK_ROOT}/ndk-build" ]; then
    ndk_build="${ANDROID_NDK_ROOT}/ndk-build"
elif command -v ndk-build >/dev/null 2>&1; then
    ndk_build=$(command -v ndk-build)
else
    echo "Set ANDROID_NDK_HOME (or ANDROID_NDK_ROOT) to an Android NDK directory." >&2
    exit 1
fi

archive_path="${work_dir}/hev-socks5-tunnel.tar.xz"
curl -fL "$archive_url" -o "$archive_path"
printf '%s  %s\n' "$archive_sha256" "$archive_path" | sha256sum -c -
tar --no-same-owner -xJf "$archive_path" -C "$work_dir"

source_dir="${work_dir}/hev-socks5-tunnel-${hev_version}"
output_dir="${work_dir}/libs"
"$ndk_build" -C "$source_dir" \
    NDK_PROJECT_PATH=. \
    APP_BUILD_SCRIPT=Android.mk \
    NDK_APPLICATION_MK=Application.mk \
    'APP_CFLAGS+=-DPKGNAME=com/simpleproxy/tun2socks' \
    'APP_CFLAGS+=-DCLSNAME=Tun2socksJni' \
    NDK_LIBS_OUT="$output_dir"

for abi in arm64-v8a armeabi-v7a x86 x86_64; do
    source_lib="${output_dir}/${abi}/libhev-socks5-tunnel.so"
    target_dir="${project_dir}/app/src/main/jniLibs/${abi}"
    mkdir -p "$target_dir"
    install -m 0644 "$source_lib" "${target_dir}/libhev-socks5-tunnel.so"
done

if strings "${output_dir}/arm64-v8a/libhev-socks5-tunnel.so" | grep -q TProxySetVpnService; then
    echo "Unexpected custom JNI protect bridge found in output." >&2
    exit 1
fi

echo "Built upstream hev-socks5-tunnel ${hev_version} (${hev_revision}) for all Android ABIs."
