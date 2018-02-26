package com.hr.netty.learn.aio;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.concurrent.CountDownLatch;

/**
 * 客户端
 */
public class TimeClient {

    public static void main(String[] args) {
        new Thread(new AsyncTimeClient()).start();
    }

    private static class AsyncTimeClient implements Runnable{

        private AsynchronousSocketChannel socketChannel;
        private CountDownLatch countDownLatch = new CountDownLatch(1);

        public AsyncTimeClient() {
            try {
                socketChannel = AsynchronousSocketChannel.open();//初始化
                System.out.println("客户端初始化完成");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void run() {

            socketChannel.connect(new InetSocketAddress(8080), this, new CompletionHandler<Void, AsyncTimeClient>() {//异步处理器(处理connect这个动作)
                @Override
                public void completed(Void result, final AsyncTimeClient client) {
                    System.out.println("客户端connect完成");
                    //connect 完成之后就可以写数据了
                    String msg = "你好";
                    ByteBuffer byteBuffer = ByteBuffer.allocate(msg.getBytes().length);
                    byteBuffer.put(msg.getBytes());
                    byteBuffer.flip();

                    client.socketChannel.write(byteBuffer, byteBuffer, new CompletionHandler<Integer, ByteBuffer>() {
                        @Override
                        public void completed(Integer result, ByteBuffer attachment) {
                            if(attachment.hasRemaining()){
                                client.socketChannel.write(attachment, attachment, this);//递归处理
                            } else {
                                System.out.println("客户端发送消息完成，开始接受服务器返回的消息");
                                //写完了可以读取服务端返回回来的数据了
                                /*
                                 * tips : 这里的 socketChannel 和 client.socketChannel 是同一个 socketChannel
                                 * 因为 client 是作为attachment(附件) 传进来的，而代码都在同一个类里面
                                 * 所以可以直接访问到 socketChannel，和通过attachment访问 是同一个socketChannel
                                 */
                                ByteBuffer dst = ByteBuffer.allocate(1024);
                                socketChannel.read(dst, dst, new CompletionHandler<Integer, ByteBuffer>() {
                                    @Override
                                    public void completed(Integer result, ByteBuffer attachment) { //这里的attachment 就是上面的 dst

                                        //把数据读到 body里面
                                        attachment.flip();
                                        byte[] body = new byte[attachment.remaining()];
                                        attachment.get(body);

                                        try {
                                            String req = new String(body, "UTF-8");
                                            System.out.println("收到服务端回复的消息:" + req);

                                        } catch (UnsupportedEncodingException e) {
                                            e.printStackTrace();
                                        } finally {
                                            //处理完成之后 释放连接
                                            countDownLatch.countDown();
                                        }

                                    }

                                    @Override
                                    public void failed(Throwable exc, ByteBuffer attachment) {
                                        exc.printStackTrace();
                                        countDownLatch.countDown();
                                    }
                                });
                            }
                        }

                        @Override
                        public void failed(Throwable exc, ByteBuffer attachment) {
                            exc.printStackTrace();
                            countDownLatch.countDown();
                        }
                    });
                }

                @Override
                public void failed(Throwable exc, AsyncTimeClient attachment) {
                    exc.printStackTrace();
                    countDownLatch.countDown();
                }
            });

            try {
                countDownLatch.await();//阻塞等待，除非处理异常
            } catch (InterruptedException e) {
                e.printStackTrace();
                try {
                    if(null != socketChannel){
                        socketChannel.close();//关闭通道
                    }
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
        }
    }
}
