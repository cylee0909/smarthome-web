package com.cylee.socket.tcp;

import com.cylee.web.Log;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;

/**
 * Created by cylee on 16/12/19.
 */
public class ConnectManager2 implements Runnable {
    private ServerSocket mSocket;
    private int mPort;
    private Map<String, DataChannel> mClients = Collections.synchronizedMap(new HashMap<>());
    private volatile boolean mStoped;
    private static ConnectManager2 manager;

    public static ConnectManager2 getInstance() {
        if (manager == null) {
            synchronized (ConnectManager2.class) {
                manager = new ConnectManager2();
            }
        }
        return manager;
    }

    public ConnectManager2 init(int port) {
        mPort = port;
        return this;
    }

    public DataChannel getChannel(String id) {
        return mClients.get(id);
    }

    public Map<String, DataChannel> allChannels() {
        return mClients;
    }

    public void registerClientChannel(DataChannel channel) {
        if (channel != null && channel.address != null) {
            Log.d("register, name = "+channel.address.loginName+" id = "+channel.address.appid);
            mClients.put(channel.address.appid, channel);
        }
    }

    public void removeChannel(DataChannel channel) {
        if (channel != null && channel.address != null) {
            mClients.remove(channel.address.appid);
        }
    }

    @Override
    public void run() {
        while (!mStoped)
            try {
                bind(mPort);
            } catch (Exception e) {
            }
    }

    public void bind(int port) throws IOException {
        mSocket = new ServerSocket(port);
        while (!mStoped) {
            Socket socket = mSocket.accept();
            TcpSocketReader reader = null;
            DataChannel channel = null;
            try {
                reader = new TcpSocketReader(socket);
                channel = new DataChannel();
                reader.registerChannel(channel);
                new Thread(reader).start();
            } catch (Exception e) {
                e.printStackTrace();
                if (reader != null) {
                    reader.stop();
                }
                if (channel != null) {
                    channel.closeChannel();
                }
            }
        }
    }

    public void stop() {
        mStoped = true;
        if (mClients != null) {
            for (DataChannel channel : mClients.values()) {
                if (channel != null) {
                    channel.closeChannel();
                }
            }
        }
        if (mSocket != null) {
            try {
                mSocket.close();
            } catch (Exception e) {
            }
        }
    }

    public String getLoginId(String name, String passd) {
        if (mClients != null) {
            Collection<DataChannel> data = mClients.values();
            if (data != null) {
                for (DataChannel channel :
                        data) {
                    if (channel != null && channel.address != null && channel.address.matchLogin(name, passd)) {
                        return channel.address.appid;
                    }
                }
            }
        }
        return null;
    }
}
