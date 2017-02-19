package com.cylee.socket.tcp;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Created by cylee on 16/10/3.
 */

public class TcpSocketWriter implements Runnable{
    private volatile boolean mStoped;
    private LinkedBlockingQueue<String> mDatas;
    private BufferedWriter mBW;
    private long mLastTime;
    private Socket mSocket;

    TcpSocketWriter(Socket socket) throws IOException {
        if (mDatas == null) {
            mDatas = new LinkedBlockingQueue<>();
        }
        mBW = new BufferedWriter(
                new OutputStreamWriter(socket.getOutputStream()));
        mStoped = false;
        mSocket = socket;
    }

    public Socket getSocket() {
        return mSocket;
    }

    public void stop() {
        mStoped = true;
        if (mBW != null) {
            try {
                mBW.close();
            } catch (Exception e){
                e.printStackTrace();
            }
        }
        Thread.currentThread().interrupt();
    }

    public void offerData(String data) {
        mDatas.add(data);
    }

    @Override
    public void run() {
        while (!mStoped) {
            try {
                long timeUsed = System.currentTimeMillis() - mLastTime;
                if (timeUsed < 1000) {
                    try {
                        Thread.sleep(1000 - timeUsed);
                    } catch (Exception e){
                    }
                }
                String packet = mDatas.take();
                if (packet != null) {
                    mBW.write(packet);
                    mBW.newLine();
                    mBW.flush();
                }
                mLastTime = System.currentTimeMillis();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (IOException e){
                e.printStackTrace();
            }
        }
    }
}
