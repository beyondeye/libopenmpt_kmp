# Build configuration for libopenmpt
# Supports both Debug and Release builds based on NDK_DEBUG variable
# Debug builds: -O0 -g (no optimization, with debug symbols)
# Release builds: -O3 -DNDEBUG (maximum optimization, no assertions)

# Determine optimization flags based on build type
ifeq ($(NDK_DEBUG),1)
# Debug build configuration
# Use -Og for better debugging experience with some optimization
OPT_CFLAGS := -Og -g -ffunction-sections -fdata-sections
OPT_CPPFLAGS := -Og -g -ffunction-sections -fdata-sections
OPT_LDFLAGS := -Wl,--gc-sections
else
# Release build configuration
OPT_CFLAGS := -O3 -DNDEBUG -ffunction-sections -fdata-sections
OPT_CPPFLAGS := -O3 -DNDEBUG -ffunction-sections -fdata-sections
OPT_LDFLAGS := -Wl,--gc-sections,--strip-all
endif

ifeq ($(NDK_MAJOR),)
APP_CFLAGS   := -std=c18 $(OPT_CFLAGS)
APP_CPPFLAGS := -std=c++17 -fexceptions -frtti $(OPT_CPPFLAGS)
else
ifeq ($(NDK_MAJOR),21)
# clang 9
APP_CFLAGS   := -std=c18 $(OPT_CFLAGS)
APP_CPPFLAGS := -std=c++17 -fexceptions -frtti $(OPT_CPPFLAGS)
else ifeq ($(NDK_MAJOR),22)
# clang 11
APP_CFLAGS   := -std=c18 $(OPT_CFLAGS)
APP_CPPFLAGS := -std=c++20 -fexceptions -frtti $(OPT_CPPFLAGS)
else ifeq ($(NDK_MAJOR),23)
# clang 12
APP_CFLAGS   := -std=c18 $(OPT_CFLAGS)
APP_CPPFLAGS := -std=c++20 -fexceptions -frtti $(OPT_CPPFLAGS)
else ifeq ($(NDK_MAJOR),24)
# clang 14
APP_CFLAGS   := -std=c18 $(OPT_CFLAGS)
APP_CPPFLAGS := -std=c++20 -fexceptions -frtti $(OPT_CPPFLAGS)
else ifeq ($(NDK_MAJOR),25)
# clang 14
APP_CFLAGS   := -std=c18 $(OPT_CFLAGS)
APP_CPPFLAGS := -std=c++20 -fexceptions -frtti $(OPT_CPPFLAGS)
else ifeq ($(NDK_MAJOR),26)
# clang 17
APP_CFLAGS   := -std=c18 $(OPT_CFLAGS)
APP_CPPFLAGS := -std=c++20 -fexceptions -frtti $(OPT_CPPFLAGS)
else ifeq ($(NDK_MAJOR),27)
# clang 18
APP_CFLAGS   := -std=c23 $(OPT_CFLAGS)
APP_CPPFLAGS := -std=c++23 -fexceptions -frtti $(OPT_CPPFLAGS)
else
APP_CFLAGS   := -std=c23 $(OPT_CFLAGS)
APP_CPPFLAGS := -std=c++23 -fexceptions -frtti $(OPT_CPPFLAGS)
endif
endif

# Set linker flags based on build type
APP_LDFLAGS  := $(OPT_LDFLAGS)
APP_STL      := c++_shared

APP_SUPPORT_FLEXIBLE_PAGE_SIZES := true

# Build only for ARM architectures
APP_ABI := armeabi-v7a arm64-v8a
