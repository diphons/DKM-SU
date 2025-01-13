LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)
LOCAL_MODULE := dkmsvc
LOCAL_SRC_FILES := dkmsvc.c
include $(BUILD_EXECUTABLE)
