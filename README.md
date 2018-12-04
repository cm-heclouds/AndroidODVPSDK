# 中移物联网有限公司 OneNET Android ODVP SDK 

### 环境准备
- Android Studio 3.1.2
- Gradle 4.4
- Platform API 17~27
- NDK 16
- CPU armv7,armv8,x86,x86_64

### 特性

- 支持odvp设备接入协议
- 支持直播流、历史流和onvif设备流的推送
- MP4文件录制(仅供参考)

### 获取
    
```
allprojects {
    repositories {
        maven {
            url 'https://dl.bintray.com/video-onenet/maven/'
        }
    }
}

dependencies {
    # with onvif
    //implementation 'com.ont.media:odvp-onvif:1.0.0'
    # without onvif
	implementation 'com.ont.media:odvp:1.0.0'
}
```

### 编译选项

``` 
# build.gradle
externalNativeBuild {
    cmake {
        arguments '-D_ONVIF=1' // 0或者不写：不编译onvif
        abiFilters "armeabi-v7a","arm64-v8a","x86","x86_64"
    }
}

# CMakeList.txt
if(NOT ONT_SERVER_ADDRESS)
    set(ONT_SERVER_ADDRESS "183.230.40.42")
endif()
if(NOT ONT_SERVER_PORT)
    set(ONT_SERVER_PORT 9101)
endif()
``` 

### api

- [设备接入](https://github.com/cm-heclouds/AndroidODVPSDK/blob/master/odvpsdk/library/src/main/java/com/ont/media/odvp/OntOdvp.java)
- [RTMP推流](https://github.com/cm-heclouds/AndroidODVPSDK/blob/master/odvpsdk/library/src/main/java/com/ont/media/odvp/OntRtmp.java)
- [onvif推流](https://github.com/cm-heclouds/AndroidODVPSDK/blob/master/odvpsdk/library/src/main/java/com/ont/media/odvp//OntOnvif.java)

### License
```
Copyright (c) 2018 cmiot
Licensed under LGPLv2.1 or later
```

### 依赖库

- [yasea 2.6:MIT](https://github.com/begeekmyfriend/yasea)
- [libyuv:BSD-style](https://code.google.com/p/libyuv)






