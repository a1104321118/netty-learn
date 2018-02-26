package com.hr.netty.learn.decoder.delimiter;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.DelimiterBasedFrameDecoder;
import io.netty.handler.codec.LineBasedFrameDecoder;
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

                    //分隔符
                    private String separator = "$_";

                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {

                        ByteBuf byteBuf = Unpooled.copiedBuffer(separator.getBytes());//获取分隔符的字节数组

                        ch.pipeline().addLast(new DelimiterBasedFrameDecoder(1024, byteBuf));
                        ch.pipeline().addLast(new StringDecoder());


                        ch.pipeline().addLast(new ChannelHandlerAdapter(){

                            private int counter;

                            @Override
                            public void channelActive(ChannelHandlerContext ctx) {

                                byte[] req = ("你好，服务端" + separator).getBytes();

                                ByteBuf message = null;
                                for (int i = 0; i < 100; i++) {//连续写100次消息
                                    message = Unpooled.buffer(req.length);
                                    message.writeBytes(req);
                                    ctx.writeAndFlush(message);
                                }
                            }

                            @Override
                            public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {

                                //StringDecoder 的作用在这里
                                String body = (String)msg;

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
