# Third-party notices

TunWeave is licensed under the Apache License 2.0. This file records the
third-party software used to build or distributed with TunWeave. It does not
change the license of those components.

## Distributed native components

The checked-in `libhev-socks5-tunnel.so` files are built from
`hev-socks5-tunnel` 2.14.4. The release archive and library hashes are recorded
in [`docs/native.md`](docs/native.md). Its Android build links the following
components:

- `hev-socks5-tunnel`, copyright 2022 hev — MIT License
- `hev-socks5-core`, copyright 2022 hev — MIT License
- `hev-task-system`, copyright 2022 hev — MIT License
- `yaml`, copyright 2022 hev — MIT License
- `lwIP`, copyright 2001–2002 Swedish Institute of Computer Science —
  BSD 3-Clause License

Source projects:

- <https://github.com/heiher/hev-socks5-tunnel>
- <https://github.com/heiher/hev-socks5-core>
- <https://github.com/heiher/hev-task-system>
- <https://github.com/heiher/yaml>
- <https://github.com/heiher/lwip>

### MIT License for the hev components

Copyright (c) 2022 hev

Permission is hereby granted, free of charge, to any person obtaining a copy of
this software and associated documentation files (the "Software"), to deal in
the Software without restriction, including without limitation the rights to
use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies
of the Software, and to permit persons to whom the Software is furnished to do
so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.

### BSD 3-Clause License for lwIP

Copyright (c) 2001, 2002 Swedish Institute of Computer Science.
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

1. Redistributions of source code must retain the above copyright notice,
   this list of conditions and the following disclaimer.
2. Redistributions in binary form must reproduce the above copyright notice,
   this list of conditions and the following disclaimer in the documentation
   and/or other materials provided with the distribution.
3. The name of the author may not be used to endorse or promote products
   derived from this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR IMPLIED
WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO
EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR
BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER
IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
POSSIBILITY OF SUCH DAMAGE.

## Android runtime components

TunWeave uses AndroidX, Jetpack Compose, Material Components, the Kotlin
standard library, and Kotlin coroutines. These components and their transitive
runtime dependencies are distributed under the Apache License 2.0. Exact
versions are declared in `gradle/libs.versions.toml`; the Apache License text is
available in this repository's `LICENSE` file.

## Build and test-only components

The Android Gradle Plugin, Kotlin Gradle plugins, AndroidX Test, and their
supporting tools are used to build or test the project and are not shipped as
application runtime code. JUnit 4.13.2 is used for local tests under the
Eclipse Public License 1.0.

When dependencies change, update this file and verify the new component's
license before publishing a release.
