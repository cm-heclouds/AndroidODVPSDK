package com.ont.media.odvp.model;

/**
 * Created by betali on 2018/2/13.
 */

public class VodBaseInfo {

    public int channel_id;      ///<    视频源通道
    public String video_title;   ///<    视频描述
    public String beginTime;      ///<    开始时间，UTC时间单位ms
    public String endTime;        ///<    结束时间，UTC时间单位ms
    public long size;
}
