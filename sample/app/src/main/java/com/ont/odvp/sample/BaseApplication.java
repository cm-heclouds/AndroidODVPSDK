package com.ont.odvp.sample;

import android.app.Activity;
import android.app.Application;
import android.icu.text.MessagePattern;

import com.ont.odvp.sample.def.IPathDef;
import com.tencent.bugly.crashreport.CrashReport;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by betali on 2018/1/18.
 */

public class BaseApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        CrashReport.initCrashReport(getApplicationContext(), "56b6e9e3df", true);
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
