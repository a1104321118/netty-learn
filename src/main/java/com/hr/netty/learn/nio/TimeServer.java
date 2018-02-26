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
public class TimeServer {

    public static void main(String[] args) throws IOException {

        new Thread(new TimeServerHandler()).start();

    }

    private static class TimeServerHandler implements Runnable{

        private volatile boolean stop = false;
        private Selector selector;
        private ServerSocketChannel serverSocketChannel;

        public TimeServerHandler() throws IOException {
            this.selector = Selector.open();
            serverSocketChannel = ServerSocketChannel.open();
            serverSocketChannel.socket().bind(new InetSocketAddress(8080));
            //设置为非阻塞
            serverSocketChannel.configureBlocking(false);
            //把 serverSocketChannel 这个通道注册到  selector这个多路复用器上面
            //表示selector 只关心该通道的读就绪状态
            serverSocketChannel.register(this.selector, SelectionKey.OP_ACCEPT);
        }

        public void run() {
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

            // 多路复用器关闭后，所有注册在上面的Channel和Pipe等资源都会被自动去注册并关闭，所以不需要重复释放资源
            if (selector != null){

                try {
                    selector.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        private void handlerKey(SelectionKey key) throws IOException {

            System.out.println(this.selector.keys());
            System.out.println(key.isValid());
            System.out.println(key.isAcceptable());
            System.out.println(key.isReadable());

            if(key.isValid()){

                if(key.isAcceptable()){
                    ServerSocketChannel ssc = (ServerSocketChannel) key.channel();
                    SocketChannel sc = ssc.accept();
                    sc.configureBlocking(false);
                    sc.register(this.selector, SelectionKey.OP_READ);
                    System.out.println("现在Selector已经开始监听read动作");
                }
                if(key.isReadable()){
                    SocketChannel sc = (SocketChannel) key.channel();
                    ByteBuffer byteBuffer = ByteBuffer.allocate(1024); // 此时先不考虑读半包问题
                    int read = sc.read(byteBuffer); // 从channel读消息，读到bytebuffer里
                    if(read > 0){
                        byteBuffer.flip();
                        byte[] bytes = new byte[byteBuffer.remaining()];
                        byteBuffer.get(bytes);
                        String body = new String(bytes, "UTF-8");//转成字符串
                        System.out.println("收到消息：" + body);
                        reply(sc, body);//回复消息

                    }else if (read < 0){//代表读完了
                        key.cancel();
                        sc.close();
                    }else {
                        //do nothing
                    }

                }
            }
        }

        private void reply(SocketChannel sc, String body) throws IOException {
            ByteBuffer byteBuffer = ByteBuffer.allocate(body.getBytes().length);
            byteBuffer.put(body.getBytes());
            byteBuffer.flip();
            sc.write(byteBuffer);
        }
    }
}
