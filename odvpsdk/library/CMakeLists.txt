cmake_minimum_required(VERSION 2.8)

set(LIB_NAME ont_odvp)

# src root path
set(ONT_SDK_ROOT ${CMAKE_SOURCE_DIR}/src/main/cpp/video_sdk)
set(ONT_ANDROID_SDK_ROOT ${CMAKE_SOURCE_DIR}/src/main/cpp/video_sdk_wrapper)
set(PROTOCOL_SRC_PATH ${ONT_SDK_ROOT}/src)
set(ONT_RTMP_SRC_PATH ${PROTOCOL_SRC_PATH}/librtmp)
set(ONT_CJSON_SRC_PATH ${PROTOCOL_SRC_PATH}/cJSON)
set(PLATFORMS_SRC_PATH ${ONT_SDK_ROOT}/platforms/posix)
set(IMPL_SRC_PATH ${ONT_SDK_ROOT}/impl)
set(MP4V2_SRC_PATH ${IMPL_SRC_PATH}/mp4v2)
set(ONVIF_SRC_PATH ${IMPL_SRC_PATH}/onvif)
set(LIVE_SRC_PATH ${IMPL_SRC_PATH}/live)

# add sdk files
set(SRC_LIST  ${PROTOCOL_SRC_PATH}/device.c
			  ${PROTOCOL_SRC_PATH}/ont_bytes.c
			  ${PROTOCOL_SRC_PATH}/platform_cmd.c
			  ${PROTOCOL_SRC_PATH}/protocol.c
			  ${PROTOCOL_SRC_PATH}/rtmp_if.c
			  ${PROTOCOL_SRC_PATH}/rvod.c
			  ${PROTOCOL_SRC_PATH}/security_openssl.c
			  ${PROTOCOL_SRC_PATH}/security_dummy.c
              ${PLATFORMS_SRC_PATH}/allocator.c
              ${PLATFORMS_SRC_PATH}/persistence_util.c
              ${PLATFORMS_SRC_PATH}/string_util.c
              ${PLATFORMS_SRC_PATH}/tcp_util.c
			  ${PLATFORMS_SRC_PATH}/udp_util.c
              ${PLATFORMS_SRC_PATH}/time_util.c
              ${ONT_RTMP_SRC_PATH}/amf.c
              ${ONT_RTMP_SRC_PATH}/hashswf.c
              ${ONT_RTMP_SRC_PATH}/log.c
              ${ONT_RTMP_SRC_PATH}/parseurl.c
              ${ONT_RTMP_SRC_PATH}/rtmp.c
              ${ONT_CJSON_SRC_PATH}/cJSON.c)
include_directories(${ONT_SDK_ROOT}/include
                    ${PROTOCOL_SRC_PATH}
                    ${ONT_RTMP_SRC_PATH}
                    ${ONT_CJSON_SRC_PATH})


# add android sdk files
aux_source_directory(${ONT_ANDROID_SDK_ROOT}/odvp SRC_LIST)
aux_source_directory(${ONT_ANDROID_SDK_ROOT}/rtmp SRC_LIST)
aux_source_directory(${ONT_ANDROID_SDK_ROOT}/utils SRC_LIST)
list(APPEND SRC_LIST ${ONT_ANDROID_SDK_ROOT}/format_jni.c)
list(APPEND SRC_LIST ${ONT_ANDROID_SDK_ROOT}/odvp_jni.c)
list(APPEND SRC_LIST ${ONT_ANDROID_SDK_ROOT}/rtmp_jni.c)
include_directories(${ONT_ANDROID_SDK_ROOT}/odvp
					${ONT_ANDROID_SDK_ROOT}/rtmp
                    ${ONT_ANDROID_SDK_ROOT}/utils
                    ${ONT_ANDROID_SDK_ROOT}/include)

add_definitions(-DLOCALE_NOT_USED)
add_definitions(-DPROTOCOL_RTMP_CRYPT)
add_definitions(-DCRYPTO)

# add openssl files
add_definitions(-DWITH_OPENSSL)
add_definitions(-DPROTOCOL_SECURITY_OPENSSL)
include_directories(${ONVIF_SRC_PATH}/openssl-1.0.1g/include
                    ${ONVIF_SRC_PATH}/openssl-1.0.1g/include/openssl)

# add impl files
list(APPEND SRC_LIST ${IMPL_SRC_PATH}/ont_packet_queue.cpp)
list(APPEND SRC_LIST ${IMPL_SRC_PATH}/ont_list.c)
list(APPEND SRC_LIST ${IMPL_SRC_PATH}/video_record.cpp)
list(APPEND SRC_LIST ${IMPL_SRC_PATH}/ont_video_play.c)
list(APPEND SRC_LIST ${IMPL_SRC_PATH}/video_rvod_mp4.c)
list(APPEND SRC_LIST ${IMPL_SRC_PATH}/video_record_dummy.c)
list(APPEND SRC_LIST ${IMPL_SRC_PATH}/video_rvod_dummy.c)
list(APPEND SRC_LIST ${IMPL_SRC_PATH}/video_onvif_dummy.c)
include_directories(${IMPL_SRC_PATH})

# add live files
include_directories(${LIVE_SRC_PATH}/BasicUsageEnvironment/include
                    ${LIVE_SRC_PATH}/groupsock/include
                    ${LIVE_SRC_PATH}/UsageEnvironment/include)

# add mp4v2 src files
add_definitions(-DONT_VIDEO_RVOD_MP4SUPPORTED)
aux_source_directory(${MP4V2_SRC_PATH}/src SRC_LIST)
aux_source_directory(${MP4V2_SRC_PATH}/src/bmff SRC_LIST)
aux_source_directory(${MP4V2_SRC_PATH}/src/itmf SRC_LIST)
aux_source_directory(${MP4V2_SRC_PATH}/src/qtff SRC_LIST)
list(APPEND SRC_LIST ${MP4V2_SRC_PATH}/libplatform/io/File.cpp)
list(APPEND SRC_LIST ${MP4V2_SRC_PATH}/libplatform/io/FileSystem.cpp)
list(APPEND SRC_LIST ${MP4V2_SRC_PATH}/libplatform/time/time.cpp)
list(APPEND SRC_LIST ${MP4V2_SRC_PATH}/libplatform/io/File_posix.cpp)
list(APPEND SRC_LIST ${MP4V2_SRC_PATH}/libplatform/io/FileSystem_posix.cpp)
list(APPEND SRC_LIST ${MP4V2_SRC_PATH}/libplatform/number/random_posix.cpp)
list(APPEND SRC_LIST ${MP4V2_SRC_PATH}/libplatform/process/process_posix.cpp)
list(APPEND SRC_LIST ${MP4V2_SRC_PATH}/libplatform/sys/error.cpp)
list(APPEND SRC_LIST ${MP4V2_SRC_PATH}/libplatform/time/time_posix.cpp)
include_directories(${MP4V2_SRC_PATH}
                    ${MP4V2_SRC_PATH}/include)

if(NOT _ONVIF)
    option(_ONVIF "onvif" 0)
endif()

# add onvif files
include_directories(${ONVIF_SRC_PATH}
                    ${ONT_ANDROID_SDK_ROOT}/onvif)
list(APPEND SRC_LIST ${ONT_ANDROID_SDK_ROOT}/onvif/onvif_config.c)
if(_ONVIF)
# add live files
add_definitions(-DOMIT_REGISTER_HANDLING)
aux_source_directory(${LIVE_SRC_PATH}/groupsock SRC_LIST)
aux_source_directory(${LIVE_SRC_PATH}/liveMedia SRC_LIST)
aux_source_directory(${LIVE_SRC_PATH}/BasicUsageEnvironment SRC_LIST)
aux_source_directory(${LIVE_SRC_PATH}/UsageEnvironment SRC_LIST)
include_directories(${LIVE_SRC_PATH}/liveMedia/include)
# add onvif files
add_definitions(-DONT_VIDEO_ONVIF_SUPPORTED)
aux_source_directory(${ONVIF_SRC_PATH} SRC_LIST)
aux_source_directory(${ONVIF_SRC_PATH}/protocol SRC_LIST)
list(APPEND SRC_LIST ${ONT_ANDROID_SDK_ROOT}/onvif/onvif_odvp.c)
list(APPEND SRC_LIST ${ONT_ANDROID_SDK_ROOT}/onvif_jni.c)
include_directories(${ONVIF_SRC_PATH}/protocol)
endif(_ONVIF)

set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -DONT_OS_POSIX -DONT_DEBUG")
set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -Wall -fPIC")
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -Wall -fPIC -fpermissive")
set(CMAKE_C_FLAGS_DEBUG "${CMAKE_C_FLAGS_DEBUG} -O0 -ggdb3")
set(CMAKE_C_FLAGS_RELEASE "${CMAKE_C_FLAGS_RELEASE} -Os")
set(CMAKE_CXX_FLAGS_DEBUG "${CMAKE_CXX_FLAGS_DEBUG} -O0 -ggdb3")
set(CMAKE_CXX_FLAGS_RELEASE "${CMAKE_CXX_FLAGS_RELEASE} -Os")
set(CMAKE_SHARED_LINKER_FLAGS "${CMAKE_SHARED_LINKER_FLAGS} -Wl,-rpath,.")
set(CMAKE_EXE_LINKER_FLAGS "${CMAKE_EXE_LINKER_FLAGS} -Wl,-rpath,.")
add_definitions(-DONT_VIDEO_PROTOCOL)

if(NOT ONT_SOCK_SEND_BUF_SIZE)
    set(ONT_SOCK_SEND_BUF_SIZE 4096)
endif()

if(NOT ONT_SOCK_RECV_BUF_SIZE)
    set(ONT_SOCK_RECV_BUF_SIZE 2048)
endif()
if(NOT ONT_SERVER_ADDRESS)
    set(ONT_SERVER_ADDRESS "183.230.40.42")
endif()
if(NOT ONT_SERVER_PORT)
    set(ONT_SERVER_PORT 9101)
endif()
configure_file(${ONT_SDK_ROOT}/include/config.h.in ${ONT_SDK_ROOT}/include/config.h)

link_directories(${CMAKE_SOURCE_DIR}/src/main/jniLibs/${ANDROID_ABI})
add_library(${LIB_NAME} SHARED ${SRC_LIST})
find_library(log-lib log)
target_link_libraries(${LIB_NAME} ${log-lib} yuv ssl crypto)