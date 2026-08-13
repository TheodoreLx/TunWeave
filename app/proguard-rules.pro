# Keep tun2socks native methods
-keepclasseswithmembernames class * {
    native <methods>;
}

# Keep data classes used in DataStore serialization
-keep class io.github.theodorelx.tunweave.data.** { *; }

# Keep VPN service
-keep class io.github.theodorelx.tunweave.service.ProxyVpnService { *; }
