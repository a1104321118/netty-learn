package com.hr.netty.learn.codec.protobuf;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.DelimiterBasedFrameDecoder;
import io.netty.handler.codec.protobuf.ProtobufDecoder;
import io.netty.handler.codec.protobuf.ProtobufEncoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32FrameDecoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32LengthFieldPrepender;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;

/**
 * Created by huangrui on 2018/2/23.
 *
 * 用 protobuf 作为编解码器
 */
public class SubReqServer {

    public static void main(String[] args) {
        new SubReqServer().bind(8080);
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


                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {

                        ch.pipeline().addLast(new ProtobufVarint32FrameDecoder());//用作半包处理，netty提供
                        ch.pipeline().addLast(new ProtobufDecoder(SubscribeReqProto.SubscribeReq.getDefaultInstance()));//确定要解码的目标类
                        ch.pipeline().addLast(new ProtobufVarint32LengthFieldPrepender());// netty提供
                        ch.pipeline().addLast(new ProtobufEncoder());

                        ch.pipeline().addLast(new ChannelHandlerAdapter(){ //重写这3个事件的方法


                            @Override
                            public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
                                cause.printStackTrace();
                                ctx.close();
                            }

                            @Override
                            public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {

                                //解码器的作用
                                SubscribeReqProto.SubscribeReq req = (SubscribeReqProto.SubscribeReq) msg;
                                System.out.println("服务器收到消息：" + req.toString());

                                //回复消息
                                ctx.writeAndFlush(resp(req.getSubReqID()));
                            }

                            private SubscribeRespProto.SubscribeResp resp(int subReqID) {
                                SubscribeRespProto.SubscribeResp.Builder builder = SubscribeRespProto.SubscribeResp
                                        .newBuilder();
                                builder.setSubReqID(subReqID);
                                builder.setRespCode(0);
                                builder.setDesc("我已收到你发送的信息");
                                return builder.build();
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
