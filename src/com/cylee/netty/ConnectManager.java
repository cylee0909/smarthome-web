package com.cylee.netty;

import com.cylee.web.Log;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LineBasedFrameDecoder;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by cylee on 16/12/19.
 */
public class ConnectManager implements Runnable {
    private int mPort;
    private Map<String, NettyChannel> mClients = Collections.synchronizedMap(new HashMap<>());
    private static ConnectManager manager;

    public static ConnectManager getInstance() {
        if (manager == null) {
            synchronized (ConnectManager.class) {
                manager = new ConnectManager();
            }
        }
        return manager;
    }

    public ConnectManager init(int port) {
        mPort = port;
        return this;
    }

    public NettyChannel getChannel(String id) {
        return mClients.get(id);
    }

    public Map<String, NettyChannel> allChannels() {
        return mClients;
    }

    public void registerClientChannel(NettyChannel channel) {
        if (channel != null && channel.address != null) {
            Log.d("register, name = "+channel.address.loginName+" id = "+channel.address.appid);
            mClients.put(channel.address.appid, channel);
        }
    }

    public void removeChannel(NettyChannel channel) {
        if (channel != null && channel.address != null) {
            mClients.remove(channel.address.appid);
        }
    }

    @Override
    public void run() {
        try {
            new Thread(TimeOutChecker.getInstance()).start();
            bind(mPort);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void bind(int port) throws InterruptedException  {
        EventLoopGroup boss=new NioEventLoopGroup();
        EventLoopGroup worker=new NioEventLoopGroup();
        try {
            ServerBootstrap bootstrap=new ServerBootstrap();
            bootstrap.group(boss,worker);
            bootstrap.channel(NioServerSocketChannel.class);
            bootstrap.option(ChannelOption.SO_BACKLOG, 128);
            //通过NoDelay禁用Nagle,使消息立即发出去，不用等待到一定的数据量才发出去
            bootstrap.option(ChannelOption.TCP_NODELAY, true);
            //保持长连接状态
            bootstrap.childOption(ChannelOption.SO_KEEPALIVE, true);
            bootstrap.childHandler(new ChannelInitializer<NioSocketChannel>() {
                @Override
                protected void initChannel(NioSocketChannel serverChannel) throws Exception {
                    ChannelPipeline p = serverChannel.pipeline();
                    p.addLast(new LineBasedFrameDecoder(Integer.MAX_VALUE));
                    p.addLast(new StringDecoder());
                    p.addLast(new StringEncoder());
                    p.addLast(new NettyServerHandler());
                }
            });
            ChannelFuture f= bootstrap.bind(port).sync();
            if(f.isSuccess()){
                Log.d("server start \n");
            }
            f.channel().closeFuture().sync();
        } finally {
            boss.shutdownGracefully().sync();
            worker.shutdownGracefully().sync();
        }
    }

    public NettyChannel getChannel(SocketChannel socketChannel) {
        Collection<NettyChannel> data = mClients.values();
        if (data != null) {
            for (NettyChannel channel :
                    data) {
                if (channel != null && channel.getSocketChannel() == socketChannel) {
                    return channel;
                }
            }
        }
        return null;
    }

    public void removeByChannel(SocketChannel socketChannel) {
        NettyChannel channel = getChannel(socketChannel);
        if (channel != null) {
            mClients.remove(channel.address.appid);
        }
    }

    public void stop() {
        TimeOutChecker.getInstance().stop();
        if (mClients != null) {
            for (NettyChannel channel : mClients.values()) {
                if (channel != null) {
                    channel.closeChannel();
                }
            }
        }
    }

    public String getLoginId(String name, String passd) {
        if (mClients != null) {
            Collection<NettyChannel> data = mClients.values();
            if (data != null) {
                for (NettyChannel channel :
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
