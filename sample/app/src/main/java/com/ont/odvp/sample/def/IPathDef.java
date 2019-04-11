package com.ont.odvp.sample.def;

import android.os.Environment;

import java.io.File;

/**
 * Created by betali on 2018/1/31.
 */

public interface IPathDef {

    String ONT_ROOT_DIR = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + "OntRoot";
    String VOD_FILE_PATH = ONT_ROOT_DIR + File.separator + "OntVideos";
    String DEVICE_CONFIG_PATH = "/sdcard/device_config.json";
    String DEVICE_VOD = "/sdcard/OntRoot/OntVideos/device_vod.mp4";
}
