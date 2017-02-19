package com.cylee.netty;

import java.util.*;

/**
 * Created by cylee on 17/2/12.
 */
public class TimeOutChecker implements Runnable {
    public static final int DEFAULT_TIMEOUT = 6000; // 6s
    int mTimeOut = DEFAULT_TIMEOUT;
    private static TimeOutChecker mInstance;
    public Map<String, PacketBindData> mBindDataMap = Collections.synchronizedMap(new HashMap<String, PacketBindData>());
    private List<String> removeIds = new ArrayList<>();
    private volatile boolean mStoped;

    private TimeOutChecker() {}

    public static TimeOutChecker getInstance() {
        if (mInstance == null) {
            synchronized (TimeOutChecker.class) {
                mInstance = new TimeOutChecker();
            }
        }
        return mInstance;
    }

    public void setTimeOut(int timeOut) {
        mTimeOut = timeOut;
    }

    public PacketBindData getBindData(String addressId, String id) {
        return mBindDataMap.get(addressId+"_"+id);
    }

    public void removeBindData(String addressId, String id) {
        mBindDataMap.remove(addressId+"_"+id);
    }

    public void addBindData(String addressId, String id, PacketBindData data) {
        mBindDataMap.put(addressId+"_"+id, data);
    }

    public void stop() {
        mStoped = true;
    }

    @Override
    public void run() {
        while (!mStoped) {
            try {
                Thread.sleep(mTimeOut/3);
            } catch (InterruptedException e){
                e.printStackTrace();
            }
            removeIds.clear();
            if (!mBindDataMap.isEmpty()) {
                Iterator<String> iterator = mBindDataMap.keySet().iterator();
                while (iterator.hasNext()) {
                    String id = iterator.next();
                    PacketBindData pb = mBindDataMap.get(id);
                    if (pb.senTime + mTimeOut <= System.currentTimeMillis()) { // 超时
                        if (pb.mRetryCount >= 0) {
                            if (pb.mListener != null) {
                                pb.mListener.onError(NettyChannel.ERROR_TIME_OUT);
                            }
                            removeIds.add(id);
                        } else {
                            pb.retry();
                        }
                    }
                }
                for (String id : removeIds) {
                    mBindDataMap.remove(id);
                }
                removeIds.clear();
            }
        }
    }
}
