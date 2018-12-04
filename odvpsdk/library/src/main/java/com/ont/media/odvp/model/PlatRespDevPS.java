package com.ont.media.odvp.model;

public class PlatRespDevPS {

    public int net_result; /**< 请求执行结果（网络结果） */
    public int result;		/**< 请求执行结果 */
    public int chan_id;		/**< 通道ID */
    public int pro_type;		/**< 推流协议，0:RTMP,其他 */
    public String push_url; /**< 推流地址 */
    public int url_len;		/**< 推流地址长度 */
    public int url_ttl_min;   /**< 链接有效期，单位分钟 */
}
