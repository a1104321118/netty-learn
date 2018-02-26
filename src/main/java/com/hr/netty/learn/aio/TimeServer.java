package com.hr.netty.learn.aio;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.concurrent.CountDownLatch;

/**
 * aio 异步IO服务端
 * 也叫 nio2.0
 *
 * 主要看  AsyncTimeServerHandler 的 run()
 * 嵌套比较多
 * 主要是对 CompletionHandler 这个异步处理器的不同匿名实现，针对accept，read，write 这三个动作
 *
 * CompletionHandler 有两个方法 complete 和 failed，分别对应处理完成的时候进行回调的
 *
 */
public class TimeServer {

    public static void main(String[] args) {

        new Thread(new AsyncTimeServerHandler()).start();
    }

    private static class AsyncTimeServerHandler implements Runnable{

        private AsynchronousServerSocketChannel serverSocketChannel;
        private CountDownLatch countDownLatch = new CountDownLatch(1);

        public AsyncTimeServerHandler() {
            try {
                serverSocketChannel = AsynchronousServerSocketChannel.open();// 初始化
                serverSocketChannel.bind(new InetSocketAddress(8080));
                System.out.println("服务端初始化成功");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void run() {


            serverSocketChannel.accept(this, new CompletionHandler<AsynchronousSocketChannel, AsyncTimeServerHandler>() {//异步处理器
                @Override
                public void completed(final AsynchronousSocketChannel socketChannel, AsyncTimeServerHandler attachment) {//处理完（accept这个动作）后，异步回调方法
                    System.out.println("服务端被连接完成(accept)");
                    //重新注册下一个
                    attachment.serverSocketChannel.accept(attachment, this);

                    //读取消息
                    ByteBuffer dst = ByteBuffer.allocate(1024);
                    socketChannel.read(dst, dst, new CompletionHandler<Integer, ByteBuffer>() {
                        @Override
                        public void completed(Integer result, ByteBuffer attachment) {//处理完（read这个动作）后，异步回调方法

                            //把数据读到 body里面
                            attachment.flip();
                            byte[] body = new byte[attachment.remaining()];
                            attachment.get(body);

                            try {
                                String req = new String(body, "UTF-8");
                                System.out.println("服务端收到消息:" + req + "    服务器开始回复消息");
                                //回复消息
                                reply(req);
                            } catch (UnsupportedEncodingException e) {
                                e.printStackTrace();

                            }


                        }



                        @Override
                        public void failed(Throwable exc, ByteBuffer attachment) {
                            exc.printStackTrace();
                            try {
                                socketChannel.close();
                            } catch (IOException e1) {
                                e1.printStackTrace();
                            }
                        }

                        private void reply(String req) {

                            //处理消息
                            String replyMsg = "服务端已经收到消息:" + req;
                            byte[] bytes = replyMsg.getBytes();
                            ByteBuffer byteBuffer = ByteBuffer.allocate(bytes.length);
                            byteBuffer.put(bytes);
                            byteBuffer.flip();

                            //用这个channel 回复数据
                            socketChannel.write(byteBuffer, byteBuffer, new CompletionHandler<Integer, ByteBuffer>() {//异步处理器
                                @Override
                                public void completed(Integer result, ByteBuffer attachment) {//处理完（write这个动作）后，异步回调方法
                                    if(attachment.hasRemaining()){
                                        socketChannel.write(attachment, attachment, this);//递归处理
                                    }else {
                                        System.out.println("服务器回复消息完成");
                                        //关闭通道释放资源
                                        try {
                                            socketChannel.close();
                                        } catch (IOException e) {
                                            e.printStackTrace();
                                        }
                                    }
                                }

                                @Override
                                public void failed(Throwable exc, ByteBuffer attachment) {
                                    try {
                                        socketChannel.close();
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }

                                }
                            });
                        }
                    });
                }

                @Override
                public void failed(Throwable exc, AsyncTimeServerHandler attachment) {//处理异常后的回调方法
                    exc.printStackTrace();
                    attachment.countDownLatch.countDown();
                }
            });

            try {
                countDownLatch.await();//阻塞等待，除非处理异常
            } catch (InterruptedException e) {
                e.printStackTrace();
                try {
                    if(null != serverSocketChannel){
                        serverSocketChannel.close();//关闭通道
                    }
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }

        }
    }

}


