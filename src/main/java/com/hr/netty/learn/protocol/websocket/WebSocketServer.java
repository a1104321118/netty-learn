package com.hr.netty.learn.protocol.websocket;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.websocketx.*;
import io.netty.handler.stream.ChunkedWriteHandler;
import io.netty.util.CharsetUtil;

import static io.netty.handler.codec.http.HttpHeaders.isKeepAlive;
import static io.netty.handler.codec.http.HttpHeaders.setContentLength;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

/**
 * Created by huangrui on 2018/2/28.
 *
 * netty 支持的 ws 协议，   ws协议：类似于http但是比http更简单地协议
 *
 * 测试方式
 * 1.启动该类
 * 2.打开web-info 下面的html文件
 */
public class WebSocketServer {

    public static void main(String[] args) {
        new WebSocketServer().bind(8080);
    }

    public void bind(int port){
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup();

        ServerBootstrap serverBootstrap = new ServerBootstrap();
        serverBootstrap.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {

                        ch.pipeline().addLast(new HttpServerCodec());
                        ch.pipeline().addLast(new HttpObjectAggregator(65536));
                        ch.pipeline().addLast(new ChunkedWriteHandler());
                        ch.pipeline().addLast(new SimpleChannelInboundHandler<Object>() {

                            private WebSocketServerHandshaker handshaker;

                            @Override
                            protected void messageReceived(ChannelHandlerContext ctx, Object msg) throws Exception {
                                // 传统的HTTP接入
                                if (msg instanceof FullHttpRequest) {
                                    handleHttpRequest(ctx, (FullHttpRequest) msg);
                                }
                                // WebSocket接入
                                else if (msg instanceof WebSocketFrame) {
                                    handleWebSocketFrame(ctx, (WebSocketFrame) msg);
                                }
                            }

                            private void handleWebSocketFrame(ChannelHandlerContext ctx, WebSocketFrame frame) {
                                // 判断是否是关闭链路的指令
                                if (frame instanceof CloseWebSocketFrame) {
                                    handshaker.close(ctx.channel(),
                                            (CloseWebSocketFrame) frame.retain());
                                    return;
                                }
                                // 判断是否是Ping消息
                                if (frame instanceof PingWebSocketFrame) {
                                    ctx.channel().write(
                                            new PongWebSocketFrame(frame.content().retain()));
                                    return;
                                }
                                // 本例程仅支持文本消息，不支持二进制消息
                                if (!(frame instanceof TextWebSocketFrame)) {
                                    throw new UnsupportedOperationException(String.format(
                                            "%s frame types not supported", frame.getClass().getName()));
                                }

                                // 返回应答消息
                                String request = ((TextWebSocketFrame) frame).text();
                                System.out.println("服务端收到消息："+request);
                                ctx.channel().write(
                                        new TextWebSocketFrame(request
                                                + " , 欢迎使用Netty WebSocket服务，现在时刻："
                                                + new java.util.Date().toString()));
                            }

                            private void handleHttpRequest(ChannelHandlerContext ctx, FullHttpRequest req) {
                                // 如果HTTP解码失败，返回HHTP异常
                                if (!req.getDecoderResult().isSuccess() || (!"websocket".equals(req.headers().get("Upgrade")))) {
                                    sendHttpResponse(ctx, req, new DefaultFullHttpResponse(HTTP_1_1, BAD_REQUEST));
                                    return;
                                }

                                // 构造握手响应返回，本机测试
                                WebSocketServerHandshakerFactory wsFactory = new WebSocketServerHandshakerFactory(
                                        "ws://localhost:8080/websocket", null, false);
                                handshaker = wsFactory.newHandshaker(req);
                                if (handshaker == null) {
                                    WebSocketServerHandshakerFactory
                                            .sendUnsupportedWebSocketVersionResponse(ctx.channel());
                                } else {
                                    handshaker.handshake(ctx.channel(), req);
                                }
                            }

                            private void sendHttpResponse(ChannelHandlerContext ctx,
                                                                 FullHttpRequest req, FullHttpResponse res) {
                                // 返回应答给客户端
                                if (res.getStatus().code() != 200) {
                                    ByteBuf buf = Unpooled.copiedBuffer(res.getStatus().toString(),
                                            CharsetUtil.UTF_8);
                                    res.content().writeBytes(buf);
                                    buf.release();
                                    setContentLength(res, res.content().readableBytes());
                                }

                                // 如果是非Keep-Alive，关闭连接
                                ChannelFuture f = ctx.channel().writeAndFlush(res);
                                if (!isKeepAlive(req) || res.getStatus().code() != 200) {
                                    f.addListener(ChannelFutureListener.CLOSE);
                                }
                            }

                            @Override
                            public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
                                ctx.flush();
                            }

                            @Override
                            public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
                                cause.printStackTrace();
                                ctx.close();
                            }
                        });


                    }
                });

        try {
            Channel channel = serverBootstrap.bind(port).sync().channel();
            System.out.println("websocket 服务端已启动，监听端口：" + port);
            channel.closeFuture().sync();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            // 优雅停机
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }

    }
}