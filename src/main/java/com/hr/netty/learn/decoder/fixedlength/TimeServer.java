package com.hr.netty.learn.decoder.fixedlength;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.DelimiterBasedFrameDecoder;
import io.netty.handler.codec.FixedLengthFrameDecoder;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;

/**
 * Created by huangrui on 2018/2/23.
 *
 * FixedLengthFrameDecoder
 * 定长消息解码器，初始化的时候指定长度，超出长度的直接不接收
 *
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

                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {

                        ch.pipeline().addLast(new FixedLengthFrameDecoder(10));//在这里设置最多接受10个字节的长度
                        ch.pipeline().addLast(new StringDecoder());

                        ch.pipeline().addLast(new ChannelHandlerAdapter(){

                            @Override
                            public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
                                cause.printStackTrace();
                                ctx.close();
                            }

                            @Override
                            public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {

                                String body = (String)msg;
                                System.out.println("服务端收到消息 : " + body);
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
