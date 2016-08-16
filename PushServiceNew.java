package com.bootTest.PushService;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.LineBasedFrameDecoder;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Created by lql on 2016/8/8.
 */
public class PushServiceNew {
    private Map<String, ChannelHandlerContext> pushInfo = new HashMap<>();
    private final int port = 8888;

    public PushServiceNew() {
        new Thread() {
            public void run() {
                EventLoopGroup bossGroup= new NioEventLoopGroup();
                EventLoopGroup workGroup = new NioEventLoopGroup();
                try {
                    ServerBootstrap b = new ServerBootstrap();
                    b.group(bossGroup, workGroup).channel(NioServerSocketChannel.class).option(ChannelOption.SO_BACKLOG, 1024).childHandler(
                            new ChannelInitializer<SocketChannel>() {
                                @Override
                                protected void initChannel(SocketChannel socketChannel) throws Exception {
                                    socketChannel.pipeline().addLast(new LineBasedFrameDecoder(1024));
                                    socketChannel.pipeline().addLast(new StringDecoder());
                                    socketChannel.pipeline().addLast(new IdleStateHandler(5, 0, 0, TimeUnit.SECONDS));
                                    socketChannel.pipeline().addLast(new NettyServerHandler());
                                }
                            }
                    );
                    try {
                        ChannelFuture future = b.bind(port).sync();
                        future.channel().closeFuture().sync();
                    }
                    catch (InterruptedException e) {
                        System.out.println(e.getMessage());
                    }
                }
                finally {
                    bossGroup.shutdownGracefully();
                    workGroup.shutdownGracefully();
                }
            }
        }.start();
    }

    class NettyServerHandler extends ChannelHandlerAdapter {
        private String ClientId = "test";
        private volatile boolean connect = false;

        @Override
        public void channelActive(ChannelHandlerContext ctx) throws Exception {
            System.out.println("连接成功");
            System.out.println("开始心跳检测");
            connect = true;
            new Thread() {
                public void run() {
                    while(connect) {
                        String info = "ping" + System.getProperty("line.separator");
                        ByteBuf resp = Unpooled.copiedBuffer(info.getBytes());
                        ctx.writeAndFlush(resp);
                        try {
                            Thread.sleep(2000);
                        }
                        catch (InterruptedException e) {
                            System.out.println(e.getMessage());
                        }
                    }
                }
            }.start();
        }

        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
            String clientId = (String)msg;
            if(!clientId.equals("ok")) {
                System.out.println("ClientId is : " + clientId);
                ChannelHandlerContext temp = pushInfo.get(clientId);
                if (null != temp) {
                    temp.close();
                }
                pushInfo.put(clientId, ctx);
                ClientId = clientId;

                new Thread() {
                    public void run() {
                        int i = 0;
                        while(i++ != 1000000) {
                            push(ClientId, "This is new push+++++++++++++++++++++++++++++++++++This is new push of " + i);
                        }
                    }
                }.start();
            }
        }

        @Override
        public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
            ctx.flush();
        }

        @Override
        public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
            if(evt instanceof IdleStateEvent) {
                IdleStateEvent e = (IdleStateEvent)evt;
                if(e.state() == IdleState.READER_IDLE && connect) {
                    System.out.println("断开连接");
                    ctx.close();
                    pushInfo.remove(ClientId);
                    connect = false;
                }
            }
        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) throws Exception {
            if(connect) {
                System.out.println("断开连接");
                ctx.close();
                pushInfo.remove(ClientId);
                connect = false;
            }
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
            System.out.println(cause.getMessage());
            ctx.close();
        }
    }

    public void push(String clientId, String msg) {
        ChannelHandlerContext tempPush = pushInfo.get(clientId);
        if(null == tempPush) {
            System.out.println(clientId + "-->push info is null.Please check internet connect.");
        }
        else {
            String pushMsg = msg + System.getProperty("line.separator");
            ByteBuf resp = Unpooled.copiedBuffer(pushMsg.getBytes());
            tempPush.writeAndFlush(resp);
        }
    }
}
