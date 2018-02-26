package com.hr.netty.learn.netty1;

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
                            @Override
                            public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
                                ctx.close();
                            }

                            @Override
                            public void channelActive(ChannelHandlerContext ctx) throws Exception {
                                //tcp连接完成，可以向服务器发送数据了
                                String msg = "你好，我是客户端";
                                System.out.println("TCP连接已建立，向服务器发送数据:" + msg);
                                byte[] msgBytes = msg.getBytes();
                                ByteBuf buf = Unpooled.buffer(msgBytes.length);
                                //写到buf里面
                                buf.writeBytes(msgBytes);

                                //发送数据
                                ctx.writeAndFlush(buf);

                            }

                            @Override
                            public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
                                //读取服务器返回的信息
                                ByteBuf buf = (ByteBuf) msg;
                                byte[] req = new byte[buf.readableBytes()];
                                buf.readBytes(req);
                                String body = new String(req, "UTF-8");
                                System.out.println("收到服务器返回的信息 : " + body);

                                //关闭通道
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
