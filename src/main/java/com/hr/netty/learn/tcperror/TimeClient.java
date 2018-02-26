package com.hr.netty.learn.tcperror;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;

import java.net.InetSocketAddress;

/**
 * Created by huangrui on 2018/2/23.
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
                        ch.pipeline().addLast(new ChannelHandlerAdapter(){

                            private int counter;

                            @Override
                            public void channelActive(ChannelHandlerContext ctx) {

                                byte[] req = ("你好，服务端" + System.getProperty("line.separator")).getBytes();

                                ByteBuf message = null;
                                for (int i = 0; i < 100; i++) {//连续写100次消息
                                    message = Unpooled.buffer(req.length);
                                    message.writeBytes(req);
                                    ctx.writeAndFlush(message);
                                }
                            }

                            @Override
                            public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
                                ByteBuf buf = (ByteBuf) msg;
                                byte[] req = new byte[buf.readableBytes()];
                                buf.readBytes(req);
                                String body = new String(req, "UTF-8");
                                //每一次收到回复 counter + 1
                                counter++;
                                System.out.println("counter=" + counter);
                                System.out.println("收到服务端返回的消息 : " + body);
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
