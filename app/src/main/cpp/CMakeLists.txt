# CMakeLists.txt for sysLiux-GBA native code
# Integrates mGBA core library

cmake_minimum_required(VERSION 3.18.1)

project(gba_core)

# Set C standard
set(CMAKE_C_STANDARD 11)
set(CMAKE_C_STANDARD_REQUIRED ON)
set(CMAKE_CXX_STANDARD 14)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# Enable mGBA build options for Android
set(M_CORE_GBA ON)
set(M_CORE_GB OFF)
set(BUILD_GL OFF)
set(BUILD_GLES2 ON)
set(BUILD_GLES3 ON)
set(USE_ELF OFF)
set(USE_FFMPEG OFF)
set(USE_PNG OFF)
set(USE_ZLIB OFF)
set(USE_LIBAO OFF)
set(BUILD_STATIC ON)
set(BUILD_SHARED OFF)
set(ENABLE_DEBUGGERS OFF)
set(ENABLE_SCRIPTING OFF)
set(USE_LUA OFF)
set(USE_JSON_C OFF)
set(USE_SQLITE3 OFF)
set(USE_LIBZIP OFF)
set(USE_MINIZIP OFF)
set(DISABLE_DEPS ON)
set(LIBMGBA_ONLY ON)
set(ENABLE_VFS ON)
set(ENABLE_VFS_FD ON)
set(ENABLE_DIRECTORIES ON)

# Add mGBA as subdirectory (relative to project root)
add_subdirectory(${CMAKE_CURRENT_SOURCE_DIR}/../jni/mgba mgba-build)

# Source files for JNI wrapper
set(JNI_WRAPPER_SOURCES
    com_sysliux_gba_GbaCore.cpp
)

# Create shared library
add_library(gba SHARED ${JNI_WRAPPER_SOURCES})

# Include directories
target_include_directories(gba PRIVATE
    ${CMAKE_CURRENT_SOURCE_DIR}/../jni/mgba/src
    ${CMAKE_CURRENT_SOURCE_DIR}/../jni/mgba/include
)

# Link mGBA core library
target_link_libraries(gba
    mgba
)

# Find required NDK libraries
find_library(log-lib log)
find_library(android-lib android)
find_library(opensles-lib OpenSLES)

# Link libraries
target_link_libraries(gba
    ${log-lib}
    ${android-lib}
    ${opensles-lib}
    m
    dl
    atomic
)

# Compiler flags for Android
target_compile_options(gba PRIVATE
    -Wall
    -Wextra
    -Wno-unused-parameter
    -fPIC
)

# mGBA compile options
target_compile_options(mgba PRIVATE
    -Wno-unused-parameter
    -Wno-missing-field-initializers
)
