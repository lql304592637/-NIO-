package com.bootTest.NettyClient;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.EventLoop;

import java.util.concurrent.TimeUnit;

/**
 * Created by lql on 2016/8/15.
 */
public class ConnectionListener implements ChannelFutureListener {
    private NettyClient client;

    public ConnectionListener(NettyClient client) {
        this.client = client;
    }

    @Override
    public void operationComplete(ChannelFuture channelFuture) throws Exception {
        if (!channelFuture.isSuccess()) {
            System.out.println("请求失败，重新连接");
            final EventLoop loop = channelFuture.channel().eventLoop();
            loop.schedule(new Runnable() {
                @Override
                public void run() {
                    System.out.println("连接中...");
                    client.createBootstrap(new Bootstrap(), loop);
                }
            }, 2L, TimeUnit.SECONDS);
        }


    }
}
