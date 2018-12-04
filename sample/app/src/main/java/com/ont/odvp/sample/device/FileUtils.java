package com.ont.odvp.sample.device;

import android.content.Context;
import android.database.Cursor;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.provider.MediaStore;
import android.text.TextUtils;

import com.ont.media.odvp.model.VodBaseInfo;
import com.ont.media.odvp.model.VodInfo;
import com.ont.media.odvp.utils.OntLog;
import com.ont.odvp.sample.def.IPathDef;

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.TimeZone;


public class FileUtils {
    private static final String TAG = "FileUtils";
    private static final String SUFFIX_MP4 = ".mp4";
    private static final SimpleDateFormat mp4DateFormat =  new SimpleDateFormat("yyyyMMdd'T'HHmmss.SSS'Z'");
    private static final SimpleDateFormat outputFormat =  new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private static MediaMetadataRetriever retriever = new MediaMetadataRetriever();
    private static String mediaDir;

    public static void scanDir(Context context, int channalId, String pathDir, List<VodInfo> vodInfoList) {

        mp4DateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
        mediaDir = pathDir;
        dfsScanDir(context, channalId, pathDir, vodInfoList);
    }

    private static void dfsScanDir(Context context, int channalId, String pathDir, List<VodInfo> vodInfoList) {

        File parent = new File(pathDir);
        if (!parent.exists()) {
            return;
        }

        for (File file : parent.listFiles()) {

            if (file.isDirectory()) {

                dfsScanDir(context, channalId, file.getAbsolutePath(), vodInfoList);
            } else if (!file.getName().toLowerCase().endsWith(SUFFIX_MP4)) {

                continue;
            } else if (file.length() <= 0) {

                continue;
            } else {

                MediaInfo info = getMediaInfoViaRetriver(file.getAbsolutePath());
                if (info != null) {

                    VodBaseInfo baseInfo = info.toVodInfo(channalId);
                    if (!TextUtils.isEmpty(baseInfo.beginTime) && !TextUtils.isEmpty(baseInfo.endTime)) {

                        vodInfoList.add(new VodInfo(baseInfo, file.getAbsolutePath()));
                    }
                }
            }
        }
    }

    public static class MediaInfo {

        String duration;
        String title;
        long size;

        public MediaInfo(String title, String duration, long size) {
            this.title = title;
            this.duration = duration;
            this.size = size;
        }

        public VodBaseInfo toVodInfo(int channalId) {

            VodBaseInfo vodInfo = new VodBaseInfo();
            vodInfo.channel_id = channalId;
            long begin = 0;
            vodInfo.beginTime = outputFormat.format(begin);
            long end = begin + Long.parseLong(duration);
            vodInfo.endTime = outputFormat.format(end);
            vodInfo.video_title = title;
            vodInfo.size = size;
            return vodInfo;
        }
    }

    public static MediaInfo getMediaInfoViaRetriver(String absFilePath) {

        try {
            retriever.setDataSource(absFilePath);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }

        long size = 0;
        String title = "";
        File file = new File(absFilePath);
        if (file.exists() && file.isFile()) {
            size = file.length();
            title = file.getName();
        }
        String durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
        if(durationStr == null){
            return null;
        }
        return new MediaInfo(title, durationStr, size);
    }

    public static void dumpVideos(Context context) {
        Uri uri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
        String[] projection = { MediaStore.Video.VideoColumns.DATA };
        Cursor c = context.getContentResolver().query(uri, projection, null, null, null);
        int vidsCount = 0;
        if (c != null) {
            vidsCount = c.getCount();
            while (c.moveToNext()) {
                OntLog.d(TAG, c.getString(0));
            }
            c.close();
        }
        OntLog.d(TAG, "Total count of videos: " + vidsCount);
    }
}
