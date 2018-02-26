package com.hr.netty.learn.decoder.fixedlength;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.DelimiterBasedFrameDecoder;
import io.netty.handler.codec.FixedLengthFrameDecoder;
import io.netty.handler.codec.string.StringDecoder;

import java.net.InetSocketAddress;

/**
 * Created by huangrui on 2018/2/23.
 *
 */
public class TimeClient {

    public static void main(String[] args) {
        new TimeClient().connect(8080);
    }

    public void connect(int port){

        EventLoopGroup group = new NioEventLoopGroup();

        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(group)
                .channel(NioSocketChannel.class)
                .option(ChannelOption.TCP_NODELAY, true)
                .handler(new ChannelInitializer<SocketChannel>() {


                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {

                        ch.pipeline().addLast(new FixedLengthFrameDecoder(1024));
                        ch.pipeline().addLast(new StringDecoder());

                        ch.pipeline().addLast(new ChannelHandlerAdapter(){

                            @Override
                            public void channelActive(ChannelHandlerContext ctx) {

                                ByteBuf byteBuf = Unpooled.copiedBuffer("hello world!!!!".getBytes());
                                ctx.writeAndFlush(byteBuf);
                            }

                            @Override
                            public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
                                // 释放资源
                                cause.printStackTrace();
                                ctx.close();
                            }
                        });
                    }
                });

        try {
            //阻塞等待连接成功
            ChannelFuture future = bootstrap.connect(new InetSocketAddress(port)).sync();

            //阻塞等待客户端链路关闭
            future.channel().closeFuture().sync();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            // 优雅退出，释放NIO线程组
            group.shutdownGracefully();
        }
    }
}
