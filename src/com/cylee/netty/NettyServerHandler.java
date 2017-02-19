package com.cylee.netty;

import com.cylee.smarthome.model.ClientAddress;
import com.cylee.web.Log;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.SocketChannel;
import io.netty.util.ReferenceCountUtil;

/**
 * Created by cylee on 17/2/11.
 */
public class NettyServerHandler extends SimpleChannelInboundHandler<String> {

    @Override
    protected void channelRead0(ChannelHandlerContext channelHandlerContext, String receiveData) throws Exception {
        if (receiveData != null) {
            if (receiveData.startsWith("SETID")) {
                String addressDataWithId = receiveData.substring(5);
                if (addressDataWithId.length() > 2) {
                    String addressData = addressDataWithId.substring(2);
                    String id = addressDataWithId.substring(0, 2);
                    // 老的连接还在,但是客户端再次初始化了,我们关闭之前的连接
                    Log.d("rec SETID address = " + addressData);
                    ClientAddress address = ClientAddress.fromJson(addressData);
                    if (address != null && address.appid != null && !"".equals(address.appid.trim())) {
                        NettyChannel oldChannel = ConnectManager.getInstance().getChannel(address.appid);
                        if (oldChannel != null) {
                            Log.d("close old channel = " + oldChannel.address);
                            oldChannel.closeChannel();
                        }
                        NettyChannel channel = new NettyChannel((SocketChannel) channelHandlerContext.channel());
                        channel.address = address;
                        ConnectManager.getInstance().registerClientChannel(channel);
                        channel.write(id+"OK^\n");
                    }
                }
            } else if (receiveData.startsWith("HEART")) {
//                Log.d("receive heart");
                if (receiveData.length() >= 7) {
                    String id = receiveData.substring(5, 7);
                    NettyChannel channel = ConnectManager.getInstance().getChannel((SocketChannel) channelHandlerContext.channel());
                    if (channel != null) {
//                        Log.d("replay heart id = "+id);
                        channel.write(id + "OK^\n");
                    }
                }
            } else if (receiveData.startsWith("#") && receiveData.length() > 3) {
                NettyChannel channel = ConnectManager.getInstance().getChannel((SocketChannel) channelHandlerContext.channel());
                if (channel != null) {
                    channel.onReceive(receiveData.substring(1));
                }
            } else if (receiveData.startsWith("test")) {
                NettyChannel channel = new NettyChannel((SocketChannel) channelHandlerContext.channel());
                ClientAddress address = new ClientAddress();
                address.appid = "testid";
                channel.address = address;
                ConnectManager.getInstance().registerClientChannel(channel);
                channel.write("OK^\n");
            }
            ReferenceCountUtil.release(receiveData);
        }
    }
}
