package com.bootTest.NettyClient;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.EventLoop;

import java.util.concurrent.TimeUnit;

/**
 * Created by lql on 2016/8/15.
 */
public class NettyClientHandler extends ChannelHandlerAdapter {
    private String clientId = "test";
    private NettyClient client;
    private volatile int connect = 1;
    private volatile boolean connecting = false;

    public NettyClientHandler(String clientId, NettyClient client) {
        this.clientId = clientId;
        this.client = client;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        new Thread() {
            public void run() {
                while(true) {
                    if (connect == 1) {
                        connect = 0;
                    }
                    else {
                        if (!connecting) {
                            connecting = true;
                            System.out.println("连接中断，重新连接");
                            final EventLoop eventLoop = ctx.channel().eventLoop();
                            eventLoop.schedule(new Runnable() {
                                @Override
                                public void run() {
                                    System.out.println("连接中...");
                                    client.createBootstrap(new Bootstrap(), eventLoop);
                                }
                            }, 2L, TimeUnit.SECONDS);
                        }
                        break;
                    }
                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException e) {
                        System.out.println(e.getMessage());
                    }
                }
            }
        }.start();
        String id = clientId + System.getProperty("line.separator");
        ByteBuf resp = Unpooled.copiedBuffer(id.getBytes());
        ctx.writeAndFlush(resp);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        String body = (String)msg;
        if(body.equals("ping")) {
            connect = 1;
            String info = "ok" + System.getProperty("line.separator");
            ByteBuf resp = Unpooled.copiedBuffer(info.getBytes());
            ctx.writeAndFlush(resp);
        }
        else {
            System.out.println("Server message is : " + body);
        }
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        ctx.flush();
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        if(!connecting) {
            connecting = true;
            System.out.println("连接中断，重新连接");
            final EventLoop eventLoop = ctx.channel().eventLoop();
            eventLoop.schedule(new Runnable() {
                @Override
                public void run() {
                    System.out.println("连接中...");
                    client.createBootstrap(new Bootstrap(), eventLoop);
                }
            }, 2L, TimeUnit.SECONDS);
            super.channelInactive(ctx);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        super.exceptionCaught(ctx, cause);
    }
}
