## Building libopenmpt with Emscripten (wasm target)

### Step 1: Install Emscripten SDK

```bash
# Clone the emsdk repository
git clone https://github.com/emscripten-core/emsdk.git
cd emsdk

# Install and activate the latest version
./emsdk install latest
./emsdk activate latest

# Set up environment variables (run this in every new terminal, or add to ~/.bashrc)
source ./emsdk_env.sh
```

---

### Step 2: Modify the Emscripten Configuration of original libopenmpt sources
This is needed in order to expose `HEAPU8`, `HEAPF32`, etc. on the `libopenmpt` module object. that
are needed to pass audio data to the libopenmpt WebAssembly module and back.

Before building, you need to add `EXPORTED_RUNTIME_METHODS`. Edit `libopenmpt/src/main/cpp/build/make/config-emscripten.mk`:

__Current line (around line 85):__

```makefile
SO_LDFLAGS += -s EXPORTED_FUNCTIONS="['_malloc','_free']"
```

__Change to:__

```makefile
SO_LDFLAGS += -s EXPORTED_FUNCTIONS="['_malloc','_free']"
SO_LDFLAGS += -s EXPORTED_RUNTIME_METHODS="['HEAPU8','HEAPF32','UTF8ToString','stringToUTF8','lengthBytesUTF8']"
```

---

### Step 3: Build libopenmpt for WebAssembly

From the `libopenmpt/src/main/cpp/` directory:

```bash
cd /home/ddt/Work/OpenMPTDemo/libopenmpt/src/main/cpp/

# Clean any previous build
make clean CONFIG=emscripten

# Build for WASM target (produces .js + .wasm)
make CONFIG=emscripten EMSCRIPTEN_TARGET=wasm SHARED_LIB=1 STATIC_LIB=0 EXAMPLES=0 OPENMPT123=0 TEST=0
```

__Alternative targets you might want:__

| `EMSCRIPTEN_TARGET` | Output | |---------------------|--------| | `wasm` | Native WASM only (recommended for modern browsers) | | `js` | Plain JavaScript only (for older browsers) | | `all` | Both WASM and JS fallback | | `default` | Emscripten's default (currently same as `wasm`) | | `audioworkletprocessor` | ES6 module for AudioWorkletProcessor |

---

### Step 4: Copy the Built Files

After building, the output files will be in `bin/`:

- `libopenmpt.js` - JavaScript glue code
- `libopenmpt.wasm` - WebAssembly binary

Copy them to your project:

```bash
# Copy to the shared module resources
cp bin/libopenmpt.js /home/ddt/Work/OpenMPTDemo/shared/src/wasmJsMain/resources/
cp bin/libopenmpt.wasm /home/ddt/Work/OpenMPTDemo/shared/src/wasmJsMain/resources/

# Also copy to app resources
cp bin/libopenmpt.js /home/ddt/Work/OpenMPTDemo/app/src/wasmJsMain/resources/
cp bin/libopenmpt.wasm /home/ddt/Work/OpenMPTDemo/app/src/wasmJsMain/resources/
```

---

### Step 5: Verify the Build

After copying, you can verify that `HEAPU8` is now exported by checking the JS file:

```bash
grep -o "HEAPU8" bin/libopenmpt.js | head -5
# Should show HEAPU8 being exported on Module
```

---

### Full Build Command (Single Line)

```bash
cd /home/ddt/Work/OpenMPTDemo/libopenmpt/src/main/cpp/ && \
source /path/to/emsdk/emsdk_env.sh && \
make clean CONFIG=emscripten && \
make CONFIG=emscripten EMSCRIPTEN_TARGET=wasm SHARED_LIB=1 STATIC_LIB=0 EXAMPLES=0 OPENMPT123=0 TEST=0 -j$(nproc)
```

---

### Summary of What the Build Does

1. `CONFIG=emscripten` - Uses `build/make/config-emscripten.mk` which sets up emcc/em++ compilers
2. `EMSCRIPTEN_TARGET=wasm` - Builds native WASM with `-s WASM=1`
3. `SHARED_LIB=1` - Builds `libopenmpt.js` (the shared library)
4. The new `EXPORTED_RUNTIME_METHODS` flag will expose `HEAPU8`, `HEAPF32`, etc. on the `libopenmpt` module object

---
