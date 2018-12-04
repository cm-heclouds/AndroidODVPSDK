package com.ont.odvp.sample.def;



import com.ont.media.odvp.model.VodInfo;

import java.util.List;

/**
 * Created by betali on 2018/2/9.
 */

public interface ICmdListener {

    interface EventType {
        int TYPE_DISCONNECT = 1;
    }

    // cmd opt
    int videoLiveStreamCtrl(long deviceReference, int channel, int level);
    int videoLiveStreamStart(long deviceReference, int channel, byte proType, int min, String pushUrl);
    int videoVodStreamStart(long deviceReference, String location, int channel, byte proType, String playFlag, String pushUrl, int ttl);
    int videoDevPtzCtrl(long deviceReference, int channel, int mode, int ptzCmd, int speed);
    QueryFilesRet videoDevQueryFiles(int channel, int startIndex, int max, String startTime, String endTime);

    // event
    void onDisconnect();

    class QueryFilesRet {

        public List<VodInfo> files;
        public int totalNum;
        public int curNum;
        public int pageTotal;

        public QueryFilesRet(List<VodInfo> files, int totalNum, int curNum, int pageTotal) {
            this.files = files;
            this.totalNum = totalNum;
            this.curNum = curNum;
            this.pageTotal = pageTotal;
        }
    }
}
