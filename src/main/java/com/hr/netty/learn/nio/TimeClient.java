package com.hr.netty.learn.nio;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.Iterator;
import java.util.Set;

/**
 * Created by huangrui on 2018/2/1.
 */
public class TimeClient {

    public static void main(String[] args) {
        new Thread(new TimeClientHandler()).start();
    }

    private static class TimeClientHandler implements Runnable{

        private volatile boolean stop = false;
        private Selector selector;
        private SocketChannel socketChannel;

        public TimeClientHandler() {
            try {
                this.selector = Selector.open();
                socketChannel = SocketChannel.open();
                socketChannel.configureBlocking(false);
            } catch (IOException e) {
                e.printStackTrace();
                System.exit(1);
            }
        }

        public void run() {
            try {
                doConnect();
            } catch (IOException e) {
                e.printStackTrace();
            }

            while (!stop){
                try {
                    int select = selector.select(1000);
                    if(select > 0){
                        Set<SelectionKey> selectionKeys = selector.selectedKeys();
                        Iterator<SelectionKey> iterator = selectionKeys.iterator();
                        while (iterator.hasNext()){
                            SelectionKey key = iterator.next();
                            iterator.remove();
                            try {
                                handlerKey(key);
                            } catch (Exception e){
                                if (null != key){
                                    key.cancel();
                                    if(null != key.channel()){
                                        key.channel().close();
                                    }
                                }
                            }
                        }
                    }
                } catch (IOException e) {

                    e.printStackTrace();
                }
            }

            if(null != selector){
                try {
                    selector.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }


        }

        private void handlerKey(SelectionKey key) throws IOException {
            if(key.isValid()){

                SocketChannel sc = (SocketChannel) key.channel();

                if(key.isConnectable()){
                    if(sc.finishConnect()){
                        sc.configureBlocking(false);
                        sc.register(this.selector, SelectionKey.OP_READ);//直接改到监听read状态
                        doWrite(sc);//向服务器写消息
                    }
                }
                if(key.isReadable()){
                    ByteBuffer readBuffer = ByteBuffer.allocate(1024);
                    int readBytes = sc.read(readBuffer);
                    if (readBytes > 0) {
                        readBuffer.flip();
                        byte[] bytes = new byte[readBuffer.remaining()];
                        readBuffer.get(bytes);
                        String body = new String(bytes, "UTF-8");
                        System.out.println("服务器回传消息 : " + body);
                        this.stop = true;
                    } else if (readBytes < 0) {
                        // 对端链路关闭
                        key.cancel();
                        sc.close();
                    } else{
                         // 读到0字节，忽略
                    }
                }
            }
        }

        private void doConnect() throws IOException {
            // 如果直接连接成功，则注册到多路复用器上，发送请求消息，读应答
            if (socketChannel.connect(new InetSocketAddress(8080))) {
                socketChannel.register(selector, SelectionKey.OP_READ);//直接让selector监听 read状态
                doWrite(socketChannel);//发送消息
            } else
                socketChannel.register(selector, SelectionKey.OP_CONNECT);//否则监听 connect状态
        }

        private void doWrite(SocketChannel sc) throws IOException {
            byte[] req = "你好".getBytes();
            ByteBuffer writeBuffer = ByteBuffer.allocate(req.length);
            writeBuffer.put(req);
            writeBuffer.flip();
            sc.write(writeBuffer);
            if (!writeBuffer.hasRemaining()){
                System.out.println("已成功发送消息");
            }
        }
    }
}
