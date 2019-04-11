package com.ont.odvp.sample;

import android.app.Application;

import com.ont.odvp.sample.def.IPathDef;

import java.io.File;

/**
 * Created by betali on 2018/1/18.
 */

public class BaseApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        initVideosDir();
    }

    private void initVideosDir() {

        File fileRootDir = new File(IPathDef.ONT_ROOT_DIR);
        if (!fileRootDir.exists()) {

            fileRootDir.mkdir();
        }

        File fileVideosDir = new File(IPathDef.VOD_FILE_PATH);
        if (!fileVideosDir.exists()) {

            fileVideosDir.mkdir();
        }
    }
}
