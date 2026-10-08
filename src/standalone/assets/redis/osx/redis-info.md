# Redis for macOS (Apple Silicon), no-TLS build

`redis-server-7.2-darwin-arm64` is Redis 7.2.3 for Apple Silicon, built without TLS, so it has no
OpenSSL dependency (`otool -L` lists only `libSystem`).

## Why it is here

The Redis binary bundled in the `embedded-redis` library for Apple Silicon links Homebrew's OpenSSL
and will not start on a Mac without it:

    dyld: Library not loaded: /opt/homebrew/opt/openssl@3/lib/libssl.3.dylib

Standalone Firefly (`OP_standaloneEnabled=true`) on Apple Silicon uses this binary instead.
Linux, Intel Macs and Docker are unchanged. Firefly does not use Redis TLS.

## Where it came from

- A third-party build ("HankCP") distributed in the embedded-redis project, which is also the
  library's `ExecutableProvider.REDIS_7_2_MACOSX_14_SONOMA_HANKCP`:
  https://github.com/codemonstur/embedded-redis/tree/master/src/main/binaries
- `./redis-server-7.2-darwin-arm64 --version` reports `v=7.2.3 sha=7f4bae81:0`. That commit is the
  official redis/redis tag 7.2.3, and `:0` means a clean checkout.
- Built on macOS 14 (minimum macOS 14.0), ad-hoc signed, 2,338,096 bytes.
- SHA-256: `a4a0078c060895140d8277877cec64ef414702af1fbc53dc3b3d7f1d6700dced`

## How to get it

The URL below is pinned to a commit, so the content cannot change:

    curl -L -o redis-server-7.2-darwin-arm64 \
      https://github.com/codemonstur/embedded-redis/raw/4a83d18637478bfffef191648d6e9c36a708a347/src/main/binaries/redis-server-7.2-darwin-arm64
    shasum -a 256 redis-server-7.2-darwin-arm64     # must match the SHA-256 above
    chmod +x redis-server-7.2-darwin-arm64
    otool -L redis-server-7.2-darwin-arm64          # only /usr/lib/libSystem.B.dylib
    ./redis-server-7.2-darwin-arm64 --version       # Redis server v=7.2.3 sha=7f4bae81:0

Use curl, not a browser. A browser adds `com.apple.quarantine`, and macOS kills a non-notarized
binary that has it (the process just dies, exit 137). If that happened:

    xattr -d com.apple.quarantine redis-server-7.2-darwin-arm64

## License

Redis 7.2.3 is BSD-3-Clause. The license text must accompany binary distributions:

    Copyright (c) 2006-2020, Salvatore Sanfilippo
    All rights reserved.

    Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:

        * Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
        * Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
        * Neither the name of Redis nor the names of its contributors may be used to endorse or promote products derived from this software without specific prior written permission.

    THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
