package com.hr.netty.learn.protocol.netty.server;

import com.hr.netty.learn.protocol.netty.codec.NettyMessageDecoder;
import com.hr.netty.learn.protocol.netty.codec.NettyMessageEncoder;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.timeout.ReadTimeoutHandler;

/**
 * Created by huangrui on 2018/3/19.
 */
public class NettyServer {

    public static void main(String[] args) throws Exception {
        new NettyServer().bind(8080);
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
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {
                        ch.pipeline().addLast(
                                new NettyMessageDecoder(1024 * 1024, 4, 4));
                        ch.pipeline().addLast(new NettyMessageEncoder());
                        ch.pipeline().addLast("readTimeoutHandler",
                                new ReadTimeoutHandler(50));
                        ch.pipeline().addLast(new LoginAuthRespHandler());
                        ch.pipeline().addLast("HeartBeatHandler",
                                new HeartBeatRespHandler());
                    }
                });

        try {
            //阻塞等待绑定端口完成
            ChannelFuture future = serverBootstrap.bind(port).sync();
            System.out.println("服务器已启动，监听端口:" + port);

            //阻塞等待服务端口关闭
            //future.channel().closeFuture().sync();


        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            System.out.println(1);

            // 不退出，一直等待
//            bossGroup.shutdownGracefully();
//            workerGroup.shutdownGracefully();
        }

    }
}
