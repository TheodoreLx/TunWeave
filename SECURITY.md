# Security policy

## Supported versions

Security fixes are provided for the latest released version and the current
`main` branch. Older releases may not receive patches.

## Reporting a vulnerability

Use GitHub's private vulnerability reporting form for this repository. Do not
open a public issue for a suspected vulnerability.

Include the affected version, Android version and device architecture, impact,
reproduction steps, and a minimal proof of concept when possible. Do not send
proxy passwords, signing material, unredacted logs, or data belonging to other
people. Redact proxy endpoints, local addresses, and application package names.

The maintainer will acknowledge reports on a best-effort basis, validate the
issue, coordinate a fix and disclosure when appropriate, and credit reporters
who request attribution. This project does not currently offer a bug bounty or
a guaranteed response time.

Do not test against proxy servers, networks, devices, or accounts without the
owner's authorization.

## Security boundaries

TunWeave routes traffic to a user-selected SOCKS5 proxy. SOCKS5 is not an
encrypted transport and TunWeave does not make an untrusted proxy or local
network trustworthy. Applications should use end-to-end encryption such as
TLS. Routing bypasses, per-app exclusions, and IPv6 bypass mode intentionally
send some traffic outside the VPN path.

Relevant report areas include credential storage, traffic or DNS leaks,
Android VPN routing, native/JNI memory safety, package or signing integrity,
and build supply-chain issues.

## Release signing

Release keystores and passwords must never be committed, attached to issues,
or included in diagnostic output. The local signing files documented in
`docs/signing.md` are ignored by Git. Before the first public release, keep at
least two encrypted keystore backups and store their passwords separately.
