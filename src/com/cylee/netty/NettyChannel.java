package com.cylee.netty;

import com.cylee.smarthome.model.ClientAddress;
import io.netty.channel.socket.SocketChannel;

import java.io.File;
import java.io.FileInputStream;

/**
 * Created by cylee on 17/2/11.
 */
public class NettyChannel {
    public static final int ERROR_DATA_INVALID = -1;
    public static final int ERROR_SEND_ERROR = -2;
    public static final int ERROR_TIME_OUT = -3;

    private int mId;
    private SocketChannel mSocketChannel;
    ClientAddress address;

    public NettyChannel(SocketChannel mSocketChannel) {
        this.mSocketChannel = mSocketChannel;
    }

    public SocketChannel getSocketChannel() {
        return mSocketChannel;
    }
    public ClientAddress getAddress() {
        return address;
    }

    public void closeChannel() {
        try {
            mSocketChannel.closeFuture().sync();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void write(String data) {
        mSocketChannel.write(data);
        mSocketChannel.flush();
    }

    public void sendString(String data, SendDataListener listener) {
        if (data == null) {
            if (listener != null) {
                listener.onError(ERROR_DATA_INVALID);
            }
            return;
        }

        PacketBindData oldData = TimeOutChecker.getInstance().getBindData(address.appid, data);
        if (oldData == null) {
            String id = createRequestId();
            oldData = new PacketBindData();
            oldData.senTime = System.currentTimeMillis();
            oldData.mSendId = id;
            oldData.mListener = listener;
            oldData.mChannel = this;
            TimeOutChecker.getInstance().addBindData(address.appid, id, oldData);
            data = correctLength(data, id);
            oldData.mSendData = data;
        } else {
            oldData.senTime = System.currentTimeMillis();
            data = oldData.mSendData;
        }

        try {
            write(data);
        } catch (Exception e) {
            TimeOutChecker.getInstance().removeBindData(address.appid, oldData.mSendId);
            if (listener != null) {
                listener.onError(ERROR_SEND_ERROR);
            }
        }
    }

    private synchronized String createRequestId() {
        mId ++;
        mId %= 0xFF;
        return String.format("%02x", mId);
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

    public void onReceive(String data) {
        String id = data.substring(0, 2);
        PacketBindData pb = TimeOutChecker.getInstance().getBindData(address.appid, id);
        if (pb != null) {
            if (pb.mListener != null) {
                int endIndex = data.indexOf("^");
                if (endIndex > 2) {
                    String result = data.substring(2, endIndex);
                    pb.mListener.onSuccess(result);
                }
            }
            TimeOutChecker.getInstance().removeBindData(address.appid, id);
        }
    }
}
