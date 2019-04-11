# 中移物联网有限公司 OneNET Android ODVP SDK 

### 环境准备
- Android Studio 3.1.2
- Gradle 4.4
- Platform API 17~27
- NDK 16
- CPU armv7,armv8,x86,x86_64

### 特性

- 1.0.0
- 支持odvp设备接入协议
- 支持直播流、历史流和onvif设备流的推送
- MP4文件录制(仅供参考)

- 1.0.1
- 支持时间水印
- 支持摄像头对焦，变焦
- 发热优化

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
    //implementation 'com.ont.media:odvp-onvif:1.0.1'
    # without onvif
	implementation 'com.ont.media:odvp:1.0.1'
}
```

### 编译及参数设置

- [odvpsdk配置](https://github.com/cm-heclouds/AndroidODVPSDK/blob/master/odvpsdk/build.properties)
- [sample配置](https://github.com/cm-heclouds/AndroidODVPSDK/blob/master/sample/build.properties)
- [参数配置](https://github.com/cm-heclouds/AndroidODVPSDK/blob/master/sample/app/src/main/res/raw/config)

### api

- [设备接入](https://github.com/cm-heclouds/AndroidODVPSDK/blob/master/odvpsdk/library/src/main/java/com/ont/media/odvp/OntOdvp.java)
- [推流参数配置](https://github.com/cm-heclouds/AndroidODVPSDK/blob/master/odvpsdk/library/src/main/java/com/ont/media/odvp/model/PublishConfig.java)
- [RTMP推流](https://github.com/cm-heclouds/AndroidODVPSDK/blob/master/odvpsdk/library/src/main/java/com/ont/media/odvp/OntRtmp.java)
- [onvif推流](https://github.com/cm-heclouds/AndroidODVPSDK/blob/master/odvpsdk/library/src/main/java/com/ont/media/odvp//OntOnvif.java)

### License
```
Copyright (c) 2019 cmiot
Licensed under LGPLv2.1 or later
```

### 依赖库

- [yasea 2.6:MIT](https://github.com/begeekmyfriend/yasea)
- [libyuv:BSD-style](https://code.google.com/p/libyuv)






