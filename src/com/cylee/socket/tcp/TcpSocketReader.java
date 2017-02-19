package com.cylee.socket.tcp;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.Socket;

/**
 * Created by cylee on 16/10/3.
 */

public class TcpSocketReader implements Runnable{
    private BufferedReader mBR;
    private volatile boolean mStoped;
    private ReadListener mReadListener;
    private Socket mSocket;
    private int NULL_COUNT_CLOSE = 10; // 连续10个null即关闭channel
    private int mNullCount;
    TcpSocketReader(Socket socket) throws Exception {
        mBR = new BufferedReader(
                new InputStreamReader(socket.getInputStream(), "utf-8"));
        mSocket = socket;
        mStoped = false;
    }

    public Socket getSocket() {
        return mSocket;
    }

    public void setReadListener(ReadListener readListener) {
        mReadListener = readListener;
    }

    public void registerChannel(DataChannel channel) {
        channel.setReader(this);
        setReadListener(channel);
    }

    @Override
    public void run() {
        while (!mStoped) {
            try {
                String current = mBR.readLine();
                if (current != null) {
                    if (mReadListener != null) {
                        mReadListener.onReceive(current);
                    }
                    mNullCount = 0;
                } else {
                    mNullCount ++;
                    if (mNullCount >= NULL_COUNT_CLOSE) {
                        if (mReadListener != null) {
                            mReadListener.onReadClose();
                        }
                        stop();
                    }
                }
            } catch (Exception e){
                e.printStackTrace();
                mNullCount ++;
                if (mNullCount >= NULL_COUNT_CLOSE) {
                    if (mReadListener != null) {
                        mReadListener.onReadClose();
                    }
                    stop();
                }
            }
        }
    }

    void stop() {
        mStoped = true;
        if (mBR != null) {
            try {
                mBR.close();
            } catch (Exception e) {
            }
        }
        Thread.currentThread().interrupt();
    }

    public interface ReadListener {
        void onReceive(String data);

        void onReadClose();
    }
}
