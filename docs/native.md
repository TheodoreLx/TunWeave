# Native library provenance

TunWeave uses the unmodified Android JNI implementation from
`hev-socks5-tunnel` 2.14.4 (`4d6c334`). The release archive is pinned to:

```text
90e06a5dc0c139c335d50f5a1645672113a366d30066bc5ff056ab73dc39a24d
```

The JNI class is compiled for
`com/simpleproxy/tun2socks/Tun2socksJni`. This stable internal JNI ABI is
intentionally independent from TunWeave's public application ID. The upstream
bridge exposes only:

- `TProxyStartService`
- `TProxyStopService`
- `TProxyGetStats`

The upstream JNI signatures are `TProxyStartService(String, int): void`,
`TProxyStopService(): void`, and `TProxyGetStats(): long[]`. Keep the Kotlin
declarations exactly aligned: ART aborts the process during `RegisterNatives`
when a return type differs.

`TProxyGetStats()` returns `[txPackets, txBytes, rxPackets, rxBytes]`. HEV reads
TX bytes from the TUN interface, so TunWeave reports them as upload; it
writes RX bytes back to TUN, so they are reported as download. The native
counters are cumulative and the app derives session totals and rates from
successive samples.

It intentionally does not include the former custom `TProxySetVpnService` or
`hev_jni_protect_socket` callbacks. Android routing excludes the TunWeave
package itself, preventing proxy sockets from being captured recursively.

To rebuild all four ABIs:

```sh
export ANDROID_NDK_HOME=/path/to/android-ndk
./scripts/build-native.sh
```

The script downloads the official release archive, verifies its SHA-256,
builds it with `ndk-build`, and replaces files under `app/src/main/jniLibs`.

Checked-in library SHA-256 values:

```text
arm64-v8a    7f6b2ea5f1e344d306669aa7f36606bf73a16889d69fb94229d34c3fbd163ee4
armeabi-v7a  e724cd04e7a7ae4910bf9bb507f0952c9cdec5fceccb6d0c12f293da464f9721
x86          2ab697371dd3b78b51bcdff908212e024a520f4654806cd349e9bd442fece6ae
x86_64       c40edf23a5eb200d803dd779e78d6f79c9c6c02741fc472418d791b60d542649
```
