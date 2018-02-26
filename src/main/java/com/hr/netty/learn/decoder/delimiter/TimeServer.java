package com.hr.netty.learn.decoder.delimiter;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.DelimiterBasedFrameDecoder;
import io.netty.handler.codec.LineBasedFrameDecoder;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;

/**
 * Created by huangrui on 2018/2/23.
 *
 * DelimiterBasedFrameDecoder
 * 自定义消息的分割符，以 $_ 作为分隔符
 *
 * 和 LineBasedFrameDecoder 的区别就是 可以自定义分隔符，而line 是固定的
 */
public class TimeServer {

    public static void main(String[] args) {
        new TimeServer().bind(8080);
    }

    public void bind(int port){
        //两个线程组，一个负责处理网络连接，一个负责处理网络读写
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup();

        ServerBootstrap serverBootstrap = new ServerBootstrap();

        //为这个server 设置参数，处理器等
        serverBootstrap.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .option(ChannelOption.SO_BACKLOG, 1024)
                .handler(new LoggingHandler(LogLevel.INFO))
                .childHandler(new ChannelInitializer<SocketChannel>() {

                    //分隔符
                    private String separator = "$_";

                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {

                        ByteBuf byteBuf = Unpooled.copiedBuffer(separator.getBytes());//获取分隔符的字节数组
                        ch.pipeline().addLast(new DelimiterBasedFrameDecoder(1024, byteBuf));
                        ch.pipeline().addLast(new StringDecoder());

                        ch.pipeline().addLast(new ChannelHandlerAdapter(){ //重写这3个事件的方法

                            private int counter;

                            @Override
                            public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
                                cause.printStackTrace();
                                ctx.close();
                            }

                            @Override
                            public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {

                                //StringDecoder 的作用在这里
                                String body = (String)msg;

                                //每收到一次消息，counter + 1
                                counter++;
                                System.out.println("counter=" + counter);

                                //回复消息
                                System.out.println("服务端收到消息 : " + body);
                                String respMsg = "你好，服务端已收到你的消息" + separator;
                                ByteBuf resp = Unpooled.copiedBuffer(respMsg.getBytes());
                                ctx.writeAndFlush(resp);
                            }

                        });
                    }
                });

        try {
            //阻塞等待绑定端口完成
            ChannelFuture future = serverBootstrap.bind(port).sync();
            System.out.println("服务器已启动，监听端口:" + port);

            //阻塞等待服务端口关闭
            future.channel().closeFuture().sync();

        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {

            // 优雅退出，释放线程池资源
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }
}
