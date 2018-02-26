package com.hr.netty.learn.codec.protobuf;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.DelimiterBasedFrameDecoder;
import io.netty.handler.codec.protobuf.ProtobufDecoder;
import io.netty.handler.codec.protobuf.ProtobufEncoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32FrameDecoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32LengthFieldPrepender;
import io.netty.handler.codec.string.StringDecoder;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by huangrui on 2018/2/23.
 *
 */
public class SubReqClient {

    public static void main(String[] args) {
        new SubReqClient().connect(8080);
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

                        ch.pipeline().addLast(new ProtobufVarint32FrameDecoder());
                        ch.pipeline().addLast(new ProtobufDecoder(SubscribeRespProto.SubscribeResp.getDefaultInstance()));
                        ch.pipeline().addLast(new ProtobufVarint32LengthFieldPrepender());
                        ch.pipeline().addLast(new ProtobufEncoder());


                        ch.pipeline().addLast(new ChannelHandlerAdapter(){


                            @Override
                            public void channelActive(ChannelHandlerContext ctx) {
                                for (int i = 0; i < 10; i++) {
                                    ctx.write(subReq(i));
                                }
                                ctx.flush();
                            }

                            @Override
                            public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
                                //因为 protobuf 已经对消息进行了自动解码，所以可以这样操作
                                System.out.println("收到服务器返回的消息: [" + msg + "]");
                            }

                            @Override
                            public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
                                ctx.flush();
                            }

                            @Override
                            public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
                                // 释放资源
                                cause.printStackTrace();
                                ctx.close();
                            }

                            private SubscribeReqProto.SubscribeReq subReq(int i) {
                                SubscribeReqProto.SubscribeReq.Builder builder = SubscribeReqProto.SubscribeReq
                                        .newBuilder();
                                builder.setSubReqID(i);
                                builder.setUserName("Lilinfeng");
                                builder.setProductName("Netty Book For Protobuf");
                                List<String> address = new ArrayList<>();
                                address.add("NanJing YuHuaTai");
                                address.add("BeiJing LiuLiChang");
                                address.add("ShenZhen HongShuLin");
                                builder.addAllAddress(address);
                                return builder.build();
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
