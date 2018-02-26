package com.hr.netty.learn.tcpcorrect;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LineBasedFrameDecoder;
import io.netty.handler.codec.string.StringDecoder;

import java.net.InetSocketAddress;

/**
 * Created by huangrui on 2018/2/23.
 *
 * 解决tcp 粘包和拆包的问题
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
                        /*
                         * 解决tcp 拆包/粘包的关键就在这两行
                         * ch.pipeline().addLast(new LineBasedFrameDecoder(1024));
                         * ch.pipeline().addLast(new StringDecoder());
                         *
                         * 给它配置两个解码器，
                         *
                         * LineBasedFrameDecoder 的工作原理
                         * 遍历bytebuf 里面的可读字节，如果有 \n 或者 \r\n 这个字符，就以此为结束位置
                         * 我们发送的消息，是以 System.getProperty("line.separator") 结束的，所以可以被这个解码器正确地分割
                         * LineBasedFrameDecoder 可以设置最大长度，如果遍历的可读字符超出了这个长度，还没有发现换行符的话，会抛异常
                         *
                         *
                         * StringDecoder 的作用
                         * 直接帮我们把msg 给String 化，可以强转成String，方便我们操作
                         */
                        ch.pipeline().addLast(new LineBasedFrameDecoder(1024));
                        ch.pipeline().addLast(new StringDecoder());


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
