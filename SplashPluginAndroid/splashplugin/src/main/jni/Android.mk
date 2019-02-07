MY_LOCAL_PATH := $(call my-dir)

LOCAL_PATH := $(MY_LOCAL_PATH)
include $(CLEAR_VARS)
LOCAL_MODULE := SplashPlugin
LOCAL_C_INCLUDES :=
LOCAL_LDLIBS := \
	-llog \
	-lz \
	-lc
LOCAL_CFLAGS := \
	-frtti \
	-fexceptions \
	-std=c++11 \
	-g3 \
	-D__GXX_EXPERIMENTAL_CXX0X__
LOCAL_SRC_FILES := \
	../../../src/main/jni/SplashPlugin.cpp
include $(BUILD_SHARED_LIBRARY)
