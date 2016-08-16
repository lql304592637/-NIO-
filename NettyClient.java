package com.bootTest.NettyClient;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LineBasedFrameDecoder;
import io.netty.handler.codec.string.StringDecoder;

/**
 * Created by lql on 2016/7/14.
 */
public class NettyClient {
    private String ip = "127.0.0.1";
    private int port = 8888;
    private String clientId = "test";
    private EventLoopGroup group = new NioEventLoopGroup();

    public void createBootstrap(Bootstrap bootstrap, EventLoopGroup group) {
        final NettyClientHandler handler = new NettyClientHandler(clientId, this);
        try {
            bootstrap.group(group).channel(NioSocketChannel.class).option(ChannelOption.TCP_NODELAY, true).handler(
                    new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel socketChannel) throws Exception {
                            socketChannel.pipeline().addLast(new LineBasedFrameDecoder(1024));
                            socketChannel.pipeline().addLast(new StringDecoder());
                            socketChannel.pipeline().addLast(handler);
                        }
                    }
            );
            bootstrap.remoteAddress(ip, port);
            ChannelFuture f = bootstrap.connect().addListener(new ConnectionListener(this));
            f.channel().closeFuture().sync();
        }
        catch (InterruptedException e) {
            System.out.println(e.getMessage());
        }
    }

    public void connect() {
        createBootstrap(new Bootstrap(), group);
    }

    public static void main(String[] args) throws Exception{
        new NettyClient().connect();
    }
}
