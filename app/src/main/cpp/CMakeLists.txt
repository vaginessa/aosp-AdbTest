cmake_minimum_required(VERSION 3.22.1)
project("adb")

add_library(${CMAKE_PROJECT_NAME} SHARED
        adb_jni.cpp
        adb.cpp
        auth.cpp
        utils.cpp
        crypto_utils.cpp)

find_package(boringssl REQUIRED CONFIG)
target_link_libraries(${CMAKE_PROJECT_NAME}
        boringssl::crypto_static
        android
        log)
