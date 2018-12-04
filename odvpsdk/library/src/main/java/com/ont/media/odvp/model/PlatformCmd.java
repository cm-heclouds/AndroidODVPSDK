package com.ont.media.odvp.model;

/**
 * Created by betali on 2018/4/9.
 */

public class PlatformCmd {

    public boolean need_resp; // 是否需要回复，1 需要回复，0不需要回复
    public String id;         // 命令的UUID
    public byte[] req;        // 命令内容
    public int size;          // 命令长度
}
