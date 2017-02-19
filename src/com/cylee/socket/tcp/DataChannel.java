package com.cylee.socket.tcp;

import com.cylee.smarthome.model.ClientAddress;
import com.cylee.web.Log;

import java.io.IOException;
import java.util.*;

/**
 * Created by cylee on 16/9/25.
 */
public class DataChannel implements TcpSocketReader.ReadListener{
    private static final int DEFAULT_TIMEOUT = 6000; // 5s
    private static final int ERROR_DATA_INVALID = -1;
    private static final int ERROR_SEND_ERROR = -2;
    private static final int ERROR_TIME_OUT = -3;
    int mTimeOut = DEFAULT_TIMEOUT;

    TcpSocketReader reader;
    TcpSocketWriter writer;
    ClientAddress address;

    private volatile boolean mStoped;
    public Map<String, PacketBindData> mBindDataMap = Collections.synchronizedMap(new HashMap<String, PacketBindData>());
    private int mId;

    public DataChannel() {
    }

    public ClientAddress getAddress() {
        return address;
    }

    public void setReader(TcpSocketReader reader) {
        this.reader = reader;
    }

    public void sendString(String data, SendDataListener listener) {
        if (data == null) {
            if (listener != null) {
                listener.onError(ERROR_DATA_INVALID);
            }
            return;
        }
        PacketBindData oldData = mBindDataMap.get(data);
        if (oldData == null) {
            String id = createRequestId();
            oldData = new PacketBindData();
            oldData.senTime = System.currentTimeMillis();
            oldData.mSendId = id;
            oldData.mListener = listener;
            mBindDataMap.put(id, oldData);
            data = correctLength(data, id);
            oldData.mSendData = data;
        } else {
            oldData.senTime = System.currentTimeMillis();
            data = oldData.mSendData;
        }

        try {
            writer.offerData(data);
        } catch (Exception e) {
            mBindDataMap.remove(oldData.mSendId);
            if (listener != null) {
                listener.onError(ERROR_SEND_ERROR);
            }
        }
    }

    private String correctLength(String rawData, String id) {
        if (rawData == null || id == null) return  "";
        int len = rawData.length();
        if (len < 5) { // 不足5位,补齐
            for (int i = 0; i < 5 - len; i++) {
                rawData = rawData.concat("0");
            }
        }

        String op = rawData.substring(0, 5); // 前五位为指令码
        String data = rawData.substring(5);
        String result = op + id + data;
        if (len < 6) { // 不足6位,补齐
            for (int i = 0; i < 6 - len; i++) {
                result = result.concat("0");
            }
        }
        return result+"^";
    }

    public void closeChannel() {
        if (writer != null) {
            writer.stop();
        }
        if (reader != null) {
            reader.stop();
        }
        mStoped = true;
        ConnectManager2.getInstance().removeChannel(this);
    }

    @Override
    public void onReadClose() {
        closeChannel();
    }

    @Override
    public void onReceive(String receiveData) {
        if (receiveData != null) {
            if (receiveData.startsWith("SETID")) {
                String addressDataWithId = receiveData.substring(5);
                if (addressDataWithId.length() > 2) {
                    String addressData = addressDataWithId.substring(2);
                    String id = addressDataWithId.substring(0, 2);
                    // 老的连接还在,但是客户端再次初始化了,我们关闭之前的连接
                    DataChannel oldChannel = ConnectManager2.getInstance().getChannel(id);
                    if (oldChannel != null) {
                        Log.d("close old channel = "+oldChannel.address);
                        oldChannel.closeChannel();
                    }
                    Log.d("rec SETID address = "+addressData);
                    address = ClientAddress.fromJson(addressData);
                    if (address != null) {
                        try {
                            writer = new TcpSocketWriter(reader.getSocket());
                            new Thread(writer).start();
                            new Thread(new TimeOutChecker()).start();
                        } catch (IOException e){
                            e.printStackTrace();
                            closeChannel();
                            return;
                        }
                        ConnectManager2.getInstance().registerClientChannel(this);
                        writer.offerData(id + "OK^");
                    }
                }
            } else if (receiveData.startsWith("HEART")) {
                if (receiveData.length() >= 7) {
                    String id = receiveData.substring(5, 7);
                    writer.offerData(id + "OK^");
                }
            } else if (receiveData.startsWith("#") && receiveData.length() > 3) {
                String id = receiveData.substring(1, 3);
                PacketBindData pb = mBindDataMap.get(id);
                if (pb != null) {
                    if (pb.mListener != null) {
                        int endIndex = receiveData.indexOf("^");
                        if (endIndex > 3) {
                            String result = receiveData.substring(3, endIndex);
                            pb.mListener.onSuccess(result);
                        }
                    }
                    mBindDataMap.remove(id);
                }
            }
        }
    }

    private synchronized String createRequestId() {
        mId ++;
        mId %= 0xFF;
        return String.format("%02x", mId);
    }

    static class PacketBindData {
        public long senTime;
        public String mSendId;
        public int mRetryCount;
        public String mSendData;
        public SendDataListener mListener;
    }

    class TimeOutChecker implements Runnable {
        private List<String> removeIds = new ArrayList<>();
        @Override
        public void run() {
            while (!mStoped) {
                try {
                    Thread.sleep(mTimeOut / 2);
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
                                    pb.mListener.onError(ERROR_TIME_OUT);
                                }
                                removeIds.add(id);
                            } else {
                                pb.mRetryCount++;
                                sendString(pb.mSendId, pb.mListener);
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
 }
