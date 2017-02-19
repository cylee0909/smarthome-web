package com.cylee.netty;

/**
 * Created by cylee on 17/2/12.
 */
public class PacketBindData {
    public long senTime;
    public String mSendId;
    public int mRetryCount;
    public String mSendData;
    public SendDataListener mListener;
    public NettyChannel mChannel;

    public void retry() {
        mRetryCount++;
        mChannel.sendString(mSendId, mListener);
    }
}
