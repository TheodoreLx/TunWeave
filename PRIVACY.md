# Privacy

Last updated: 2026-08-11

TunWeave is a local-first Android VPN client. It does not include advertising,
analytics, crash-reporting SDKs, account systems, or developer-operated cloud
services. The project developer does not collect or receive personal data from
the app.

## Data processed on the device

TunWeave stores the configuration needed to operate the VPN, including the
proxy address, port, username, DNS settings, routing preferences, and selected
application package names. The proxy password is encrypted with an
Android Keystore-backed AES-GCM key before it is stored in Android DataStore.
Other configuration fields are stored in the app's private DataStore.

The app reads the list of launchable applications so the user can configure
per-app routing. Selected package names remain on the device. Android backup is
disabled for TunWeave.

## Network traffic

When the VPN is enabled, traffic selected by the routing configuration is sent
to the SOCKS5 server chosen by the user. The SOCKS5 operator and network
providers may observe connection metadata and any application traffic that is
not independently encrypted. SOCKS5 does not itself provide transport
encryption; use only a proxy and local network you trust.

DNS requests use the DNS servers selected in TunWeave. The defaults are
`223.5.5.5` and `114.114.114.114`. A user-initiated latency test opens a SOCKS5
connection to the configured test destination, which defaults to
`www.gstatic.com:443`; it does not send an HTTP request body.

IPv6 traffic is proxied by default. If the user selects IPv6 bypass mode, IPv6
traffic may leave the device outside the VPN path.

## Logs

TunWeave keeps at most 500 application diagnostic entries in memory and also
writes them to Android logcat. To support investigation of network and
device-specific failures, logs can contain proxy and DNS endpoints,
latency-test URLs, route addresses, selected application package names, file
paths, raw exception messages, and stack traces.

Before application logs are stored or written, TunWeave redacts the configured
proxy password and recognized password, token, authorization, secret-key, and
private-key fields. Native HEV logs bypass the application logger, so they use
the upstream `info` level rather than `debug`; the upstream client debug stream
contains a credential-bearing entry. The audited client info stream does not
print credential values. Logs are not uploaded automatically.

The user can copy application logs to the system clipboard. Other software
with clipboard access may then be able to read them. Review logs before sharing
because endpoints, package names, local paths, and other environment details
can still be personal or sensitive even when credentials have been redacted.

## Data deletion

Clearing TunWeave's storage or uninstalling the app removes its locally stored
configuration. Android Keystore manages deletion of the app's encryption key.

## Changes

Material changes to this document will be recorded in the repository history
and release notes.
