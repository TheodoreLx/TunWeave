# Native library provenance

TunWeave uses the unmodified Android JNI implementation from
`hev-socks5-tunnel` 2.17.1 (`9a06bc6`). The release archive is pinned to:

```text
a7b86050091c5a268d81de70b95d3bb0871ba4136160662b6496596761c2f9a7
```

The JNI class is compiled for
`com/simpleproxy/tun2socks/Tun2socksJni`. This stable internal JNI ABI is
intentionally independent from TunWeave's public application ID. The upstream
bridge exposes only:

- `TProxyStartService`
- `TProxyStopService`
- `TProxyIsRunning`
- `TProxyGetStats`

The upstream JNI signatures are `TProxyStartService(String, int): boolean`,
`TProxyStopService(): boolean`, `TProxyIsRunning(): boolean`, and
`TProxyGetStats(): long[]`. Keep the Kotlin declarations exactly aligned: ART
aborts the process during `RegisterNatives` when a return type differs.

The runtime-state API is the source of truth for tunnel liveness. The Android
service polls it from a serialized background dispatcher so native shutdown or
statistics collection cannot block the application main thread.

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
arm64-v8a    f1746ec99eb91d68443753f21ff9fe77419603365e911781d0e832cc7e490b1f
armeabi-v7a  f9caa0545bd46f317f696d056b28dce8cdbaf0357d7d7724e7593f4e962b21c8
x86          cc4839adf7e9eec57e6e98d646749ec2401c1de017f507912097196b3806eddf
x86_64       f7b675c94011abc07272308faf18cda48228dc7ad9d6837e83ff5b25a037d723
```
