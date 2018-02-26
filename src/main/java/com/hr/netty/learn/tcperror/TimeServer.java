package com.hr.netty.learn.tcperror;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;

/**
 * Created by huangrui on 2018/2/23.
 *
 * 模拟tcp 粘包的情况
 *
 * 不粘包的情况，应该是 客户端发送n次消息，服务端会收到n次消息
 * 但是测试结果表明，n=100时，服务端只收到了两次消息，counter=2，这说明tcp发生了粘包现象
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
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {
                        ch.pipeline().addLast(new ChannelHandlerAdapter(){ //重写这3个事件的方法

                            private int counter;

                            @Override
                            public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
                                cause.printStackTrace();
                                ctx.close();
                            }

                            @Override
                            public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {

                                ByteBuf buf = (ByteBuf) msg;
                                byte[] req = new byte[buf.readableBytes()];
                                buf.readBytes(req);
                                String body = new String(req, "UTF-8");
                                //每收到一次消息，counter + 1
                                counter++;
                                System.out.println("counter=" + counter);

                                //回复消息
                                System.out.println("服务端收到消息 : " + body);
                                String respMsg = "你好，服务端已收到你的消息" + System.getProperty("line.separator");
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
