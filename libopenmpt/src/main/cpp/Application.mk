
# Optimized build configuration for prebuilt libopenmpt
# -O3: Maximum optimization
# -DNDEBUG: Disable assertions
# -ffunction-sections -fdata-sections: Enable dead code elimination
# -flto: Link-time optimization (optional, may increase build time)

ifeq ($(NDK_MAJOR),)
APP_CFLAGS   := -std=c18 -O3 -DNDEBUG -ffunction-sections -fdata-sections
APP_CPPFLAGS := -std=c++17 -fexceptions -frtti -O3 -DNDEBUG -ffunction-sections -fdata-sections
else
ifeq ($(NDK_MAJOR),21)
# clang 9
APP_CFLAGS   := -std=c18 -O3 -DNDEBUG -ffunction-sections -fdata-sections
APP_CPPFLAGS := -std=c++17 -fexceptions -frtti -O3 -DNDEBUG -ffunction-sections -fdata-sections
else ifeq ($(NDK_MAJOR),22)
# clang 11
APP_CFLAGS   := -std=c18 -O3 -DNDEBUG -ffunction-sections -fdata-sections
APP_CPPFLAGS := -std=c++20 -fexceptions -frtti -O3 -DNDEBUG -ffunction-sections -fdata-sections
else ifeq ($(NDK_MAJOR),23)
# clang 12
APP_CFLAGS   := -std=c18 -O3 -DNDEBUG -ffunction-sections -fdata-sections
APP_CPPFLAGS := -std=c++20 -fexceptions -frtti -O3 -DNDEBUG -ffunction-sections -fdata-sections
else ifeq ($(NDK_MAJOR),24)
# clang 14
APP_CFLAGS   := -std=c18 -O3 -DNDEBUG -ffunction-sections -fdata-sections
APP_CPPFLAGS := -std=c++20 -fexceptions -frtti -O3 -DNDEBUG -ffunction-sections -fdata-sections
else ifeq ($(NDK_MAJOR),25)
# clang 14
APP_CFLAGS   := -std=c18 -O3 -DNDEBUG -ffunction-sections -fdata-sections
APP_CPPFLAGS := -std=c++20 -fexceptions -frtti -O3 -DNDEBUG -ffunction-sections -fdata-sections
else ifeq ($(NDK_MAJOR),26)
# clang 17
APP_CFLAGS   := -std=c18 -O3 -DNDEBUG -ffunction-sections -fdata-sections
APP_CPPFLAGS := -std=c++20 -fexceptions -frtti -O3 -DNDEBUG -ffunction-sections -fdata-sections
else ifeq ($(NDK_MAJOR),27)
# clang 18
APP_CFLAGS   := -std=c23 -O3 -DNDEBUG -ffunction-sections -fdata-sections
APP_CPPFLAGS := -std=c++23 -fexceptions -frtti -O3 -DNDEBUG -ffunction-sections -fdata-sections
else
APP_CFLAGS   := -std=c23 -O3 -DNDEBUG -ffunction-sections -fdata-sections
APP_CPPFLAGS := -std=c++23 -fexceptions -frtti -O3 -DNDEBUG -ffunction-sections -fdata-sections
endif
endif

# Strip debug symbols and perform dead code elimination
APP_LDFLAGS  := -Wl,--gc-sections,--strip-all
APP_STL      := c++_shared

APP_SUPPORT_FLEXIBLE_PAGE_SIZES := true

# Build only for ARM architectures
APP_ABI := armeabi-v7a arm64-v8a
