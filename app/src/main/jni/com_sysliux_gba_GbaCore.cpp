/*
 * JNI wrapper for mGBA core
 * Implements the Java native methods declared in GbaCore.kt
 */

#include <jni.h>
#include <android/log.h>
#include <SLES/OpenSLES.h>
#include <SLES/OpenSLES_Android.h>
#include <pthread.h>
#include <cstring>
#include <cstdlib>
#include <cstdint>

#define LOG_TAG "GbaCore"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

// mGBA headers
#include <mgba/core/core.h>
#include <mgba/gba/core.h>
#include <mgba-util/vfs.h>
#include <mgba-util/image.h>
#include <mgba-util/audio-buffer.h>

// GBA video dimensions
#define GBA_WIDTH 240
#define GBA_HEIGHT 160

// Save state flags
#define SAVESTATE_FLAGS SAVESTATE_ALL

// GBA key bitmask (from GBA hardware)
#define GBA_KEY_A      0x001
#define GBA_KEY_B      0x002
#define GBA_KEY_SELECT 0x004
#define GBA_KEY_START  0x008
#define GBA_KEY_RIGHT  0x010
#define GBA_KEY_LEFT   0x020
#define GBA_KEY_UP     0x040
#define GBA_KEY_DOWN   0x080
#define GBA_KEY_R      0x100
#define GBA_KEY_L      0x200

static struct {
    mCore* core;
    bool running;
    bool paused;
    float speed;
    int frameSkip;
    pthread_mutex_t frameMutex;
    uint32_t* frameBuffer;  // RGBA8888 format for Java
    int16_t* audioBuffer;
    int audioBufferSize;
    uint32_t keysHeld;
    size_t audioSamplesRead;
} gba_state = {0};

extern "C" {

// Helper function to get environment
static JNIEnv* getJNIEnv(JavaVM* vm) {
    JNIEnv* env;
    if (vm->GetEnv((void**)&env, JNI_VERSION_1_6) != JNI_OK) {
        return nullptr;
    }
    return env;
}

// Convert mGBA XBGR8888 to RGBA8888
static inline uint32_t xbgrToRgba(uint32_t pixel) {
    uint8_t r = (pixel >> 16) & 0xFF;
    uint8_t g = (pixel >> 8) & 0xFF;
    uint8_t b = pixel & 0xFF;
    uint8_t a = (pixel >> 24) & 0xFF;
    if (a == 0) a = 0xFF; // Full alpha if not set
    return (a << 24) | (r << 16) | (g << 8) | b;
}

JNIEXPORT jboolean JNICALL
Java_com_sysliux_gba_core_GbaCore_loadRom(JNIEnv* env, jobject thiz, jstring path) {
    const char* romPath = env->GetStringUTFChars(path, nullptr);
    if (!romPath) {
        LOGE("Failed to get ROM path");
        return JNI_FALSE;
    }

    LOGI("Loading ROM: %s", romPath);

    // Initialize state
    pthread_mutex_init(&gba_state.frameMutex, nullptr);
    gba_state.frameBuffer = new uint32_t[GBA_WIDTH * GBA_HEIGHT]();
    gba_state.audioBufferSize = 2048;
    gba_state.audioBuffer = new int16_t[gba_state.audioBufferSize * 2](); // Stereo
    gba_state.speed = 1.0f;
    gba_state.frameSkip = 0;
    gba_state.running = false;
    gba_state.paused = false;
    gba_state.keysHeld = 0;
    gba_state.audioSamplesRead = 0;

    // Create GBA core
    gba_state.core = mCoreCreate(mPLATFORM_GBA);
    if (!gba_state.core) {
        LOGE("Failed to create GBA core");
        env->ReleaseStringUTFChars(path, romPath);
        return JNI_FALSE;
    }

    // Initialize config
    mCoreInitConfig(gba_state.core, "gba");
    mCoreLoadConfig(gba_state.core);

    // Set audio buffer size
    gba_state.core->setAudioBufferSize(gba_state.core, gba_state.audioBufferSize);

    // Set video buffer for rendering
    gba_state.core->setVideoBuffer(gba_state.core, gba_state.frameBuffer, GBA_WIDTH);

    // Load ROM file
    if (!mCoreLoadFile(gba_state.core, romPath)) {
        LOGE("Failed to load ROM: %s", romPath);
        gba_state.core->deinit(gba_state.core);
        gba_state.core = nullptr;
        env->ReleaseStringUTFChars(path, romPath);
        return JNI_FALSE;
    }

    // Autoload save if exists
    mCoreAutoloadSave(gba_state.core);

    env->ReleaseStringUTFChars(path, romPath);
    LOGI("ROM loaded successfully");
    return JNI_TRUE;
}

JNIEXPORT void JNICALL
Java_com_sysliux_gba_core_GbaCore_unloadRom(JNIEnv* env, jobject thiz) {
    if (gba_state.core) {
        // Save battery before unloading
        mCoreAutoloadSave(gba_state.core);

        gba_state.core->deinit(gba_state.core);
        gba_state.core = nullptr;
    }

    pthread_mutex_destroy(&gba_state.frameMutex);
    delete[] gba_state.frameBuffer;
    delete[] gba_state.audioBuffer;

    LOGI("ROM unloaded");
}

JNIEXPORT void JNICALL
Java_com_sysliux_gba_core_GbaCore_start(JNIEnv* env, jobject thiz) {
    if (!gba_state.core) {
        LOGE("No ROM loaded");
        return;
    }
    gba_state.running = true;
    gba_state.paused = false;
    LOGI("GBA started");
}

JNIEXPORT void JNICALL
Java_com_sysliux_gba_core_GbaCore_pause(JNIEnv* env, jobject thiz) {
    gba_state.paused = true;
    LOGI("GBA paused");
}

JNIEXPORT void JNICALL
Java_com_sysliux_gba_core_GbaCore_reset(JNIEnv* env, jobject thiz) {
    if (gba_state.core) {
        gba_state.core->reset(gba_state.core);
    }
    LOGI("GBA reset");
}

JNIEXPORT void JNICALL
Java_com_sysliux_gba_core_GbaCore_stepFrame(JNIEnv* env, jobject thiz) {
    if (!gba_state.core || gba_state.paused) {
        return;
    }

    // Set key states
    gba_state.core->setKeys(gba_state.core, gba_state.keysHeld);

    // Run frame(s)
    for (int i = 0; i <= gba_state.frameSkip; i++) {
        gba_state.core->runFrame(gba_state.core);
    }

    // Get audio samples from the audio buffer
    struct mAudioBuffer* audioBuf = gba_state.core->getAudioBuffer(gba_state.core);
    if (audioBuf) {
        size_t available = mAudioBufferAvailable(audioBuf);
        if (available > 0) {
            size_t toRead = available;
            if (toRead > (size_t)(gba_state.audioBufferSize * 2)) {
                toRead = gba_state.audioBufferSize * 2;
            }
            mAudioBufferRead(audioBuf, gba_state.audioBuffer, toRead);
        }
    }
}

JNIEXPORT void JNICALL
Java_com_sysliux_gba_core_GbaCore_setSpeed(JNIEnv* env, jobject thiz, jfloat speed) {
    gba_state.speed = speed;
    LOGI("Speed set to %.2fx", speed);
}

JNIEXPORT void JNICALL
Java_com_sysliux_gba_core_GbaCore_setFrameSkip(JNIEnv* env, jobject thiz, jint frames) {
    gba_state.frameSkip = frames;
    LOGI("Frame skip set to %d", frames);
}

JNIEXPORT jint JNICALL
Java_com_sysliux_gba_core_GbaCore_getFps(JNIEnv* env, jobject thiz) {
    if (!gba_state.core) {
        return 0;
    }
    // Return nominal GBA FPS
    return 60;
}

JNIEXPORT jlong JNICALL
Java_com_sysliux_gba_core_GbaCore_getCycles(JNIEnv* env, jobject thiz) {
    if (!gba_state.core) {
        return 0;
    }
    // GBA frame cycles
    return gba_state.core->frameCycles(gba_state.core);
}

JNIEXPORT void JNICALL
Java_com_sysliux_gba_core_GbaCore_keyDown(JNIEnv* env, jobject thiz, jint button) {
    if (!gba_state.core) {
        return;
    }

    // Map button to GBA key bit
    // Button constants from GbaCore.kt: BUTTON_A=0, BUTTON_B=1, BUTTON_SELECT=2, BUTTON_START=3,
    // BUTTON_RIGHT=4, BUTTON_LEFT=5, BUTTON_UP=6, BUTTON_DOWN=7, BUTTON_R=8, BUTTON_L=9, BUTTON_X=10, BUTTON_Y=11
    uint32_t keyBit = 0;
    switch (button) {
        case 0:  keyBit = GBA_KEY_A; break;
        case 1:  keyBit = GBA_KEY_B; break;
        case 2:  keyBit = GBA_KEY_SELECT; break;
        case 3:  keyBit = GBA_KEY_START; break;
        case 4:  keyBit = GBA_KEY_RIGHT; break;
        case 5:  keyBit = GBA_KEY_LEFT; break;
        case 6:  keyBit = GBA_KEY_UP; break;
        case 7:  keyBit = GBA_KEY_DOWN; break;
        case 8:  keyBit = GBA_KEY_R; break;
        case 9:  keyBit = GBA_KEY_L; break;
        case 10: keyBit = GBA_KEY_A; break;  // X = A
        case 11: keyBit = GBA_KEY_B; break;  // Y = B
        default: break;
    }

    gba_state.keysHeld |= keyBit;
    gba_state.core->setKeys(gba_state.core, gba_state.keysHeld);
    LOGI("Key down: %d (bit: 0x%x)", button, keyBit);
}

JNIEXPORT void JNICALL
Java_com_sysliux_gba_core_GbaCore_keyUp(JNIEnv* env, jobject thiz, jint button) {
    if (!gba_state.core) {
        return;
    }

    uint32_t keyBit = 0;
    switch (button) {
        case 0:  keyBit = GBA_KEY_A; break;
        case 1:  keyBit = GBA_KEY_B; break;
        case 2:  keyBit = GBA_KEY_SELECT; break;
        case 3:  keyBit = GBA_KEY_START; break;
        case 4:  keyBit = GBA_KEY_RIGHT; break;
        case 5:  keyBit = GBA_KEY_LEFT; break;
        case 6:  keyBit = GBA_KEY_UP; break;
        case 7:  keyBit = GBA_KEY_DOWN; break;
        case 8:  keyBit = GBA_KEY_R; break;
        case 9:  keyBit = GBA_KEY_L; break;
        case 10: keyBit = GBA_KEY_A; break;  // X = A
        case 11: keyBit = GBA_KEY_B; break;  // Y = B
        default: break;
    }

    gba_state.keysHeld &= ~keyBit;
    gba_state.core->setKeys(gba_state.core, gba_state.keysHeld);
    LOGI("Key up: %d", button);
}

JNIEXPORT jboolean JNICALL
Java_com_sysliux_gba_core_GbaCore_saveState(JNIEnv* env, jobject thiz, jint slot) {
    if (!gba_state.core) {
        return JNI_FALSE;
    }

    bool success = mCoreSaveState(gba_state.core, slot, SAVESTATE_FLAGS);
    LOGI("Save state to slot %d: %s", slot, success ? "success" : "failed");
    return success ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jboolean JNICALL
Java_com_sysliux_gba_core_GbaCore_loadState(JNIEnv* env, jobject thiz, jint slot) {
    if (!gba_state.core) {
        return JNI_FALSE;
    }

    bool success = mCoreLoadState(gba_state.core, slot, SAVESTATE_FLAGS);
    LOGI("Load state from slot %d: %s", slot, success ? "success" : "failed");
    return success ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jbyteArray JNICALL
Java_com_sysliux_gba_core_GbaCore_getBatterySave(JNIEnv* env, jobject thiz) {
    if (!gba_state.core) {
        return nullptr;
    }

    // Get battery save size
    size_t size = 0;
    void* sram = nullptr;
    size = gba_state.core->savedataClone(gba_state.core, &sram);

    if (size == 0 || !sram) {
        // Try to get save data another way
        jbyteArray empty = env->NewByteArray(0);
        return empty;
    }

    jbyteArray result = env->NewByteArray(size);
    if (result) {
        env->SetByteArrayRegion(result, 0, size, (jbyte*)sram);
    }

    free(sram);
    return result;
}

JNIEXPORT void JNICALL
Java_com_sysliux_gba_core_GbaCore_loadBatterySave(JNIEnv* env, jobject thiz, jbyteArray data) {
    if (!gba_state.core) {
        return;
    }

    jsize size = env->GetArrayLength(data);
    if (size == 0) {
        return;
    }

    jbyte* bytes = env->GetByteArrayElements(data, nullptr);

    if (bytes && size > 0) {
        gba_state.core->savedataRestore(gba_state.core, bytes, size, true);
    }

    if (bytes) {
        env->ReleaseByteArrayElements(data, bytes, 0);
    }
    LOGI("Battery save loaded (%d bytes)", size);
}

JNIEXPORT jbyteArray JNICALL
Java_com_sysliux_gba_core_GbaCore_getFrameBuffer(JNIEnv* env, jobject thiz) {
    jbyteArray result = env->NewByteArray(GBA_WIDTH * GBA_HEIGHT * 4);
    if (!result) {
        return nullptr;
    }

    pthread_mutex_lock(&gba_state.frameMutex);
    if (gba_state.frameBuffer) {
        // Convert from mGBA format to RGBA
        // mGBA uses XBGR8888 format in 32-bit mode
        for (int i = 0; i < GBA_WIDTH * GBA_HEIGHT; i++) {
            uint32_t pixel = gba_state.frameBuffer[i];
            gba_state.frameBuffer[i] = xbgrToRgba(pixel);
        }
        env->SetByteArrayRegion(result, 0, GBA_WIDTH * GBA_HEIGHT * 4, (jbyte*)gba_state.frameBuffer);
    }
    pthread_mutex_unlock(&gba_state.frameMutex);

    return result;
}

JNIEXPORT jshortArray JNICALL
Java_com_sysliux_gba_core_GbaCore_getAudioSamples(JNIEnv* env, jobject thiz) {
    jshortArray result = env->NewShortArray(gba_state.audioBufferSize * 2);
    if (!result) {
        return nullptr;
    }

    // Return the audio buffer
    env->SetShortArrayRegion(result, 0, gba_state.audioBufferSize * 2, gba_state.audioBuffer);

    return result;
}

JNIEXPORT void JNICALL
Java_com_sysliux_gba_core_GbaCore_setVolume(JNIEnv* env, jobject thiz, jint volume) {
    LOGI("Volume set to %d", volume);
    // Volume is handled at the Java/Kotlin level with OpenSLES
}

JNIEXPORT jboolean JNICALL
Java_com_sysliux_gba_core_GbaCore_isRomLoaded(JNIEnv* env, jobject thiz) {
    return gba_state.core != nullptr;
}

JNIEXPORT jboolean JNICALL
Java_com_sysliux_gba_core_GbaCore_isRunning(JNIEnv* env, jobject thiz) {
    return gba_state.running && !gba_state.paused;
}

} // extern "C"
