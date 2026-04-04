# sysLiux-GBA native Android.mk
# mGBA 集成构建配置

LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)
LOCAL_MODULE := gba
LOCAL_SRC_FILES := mgba/libgba.a
LOCAL_EXPORT_C_INCLUDES := $(LOCAL_PATH)/mgba
include $(PREBUILT_STATIC_LIBRARY)