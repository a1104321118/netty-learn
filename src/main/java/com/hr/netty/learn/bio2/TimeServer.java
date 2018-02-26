package com.hr.netty.learn.bio2;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Created by huangrui on 2018/1/31.
 * bio
 */
public class TimeServer {

    public static void main(String[] args) throws IOException {
        ServerSocket serverSocket = null;
        ExecutorService executorService = new ThreadPoolExecutor(Runtime.getRuntime().availableProcessors(),
                50,
                60L,
                TimeUnit.SECONDS,
                new ArrayBlockingQueue<Runnable>(1000));
        try {
            serverSocket = new ServerSocket(8080);
            Socket socket = null;

            while (true){
                socket = serverSocket.accept();//程序会阻塞在这里，直到接受到socket,然后交给线程处理，进入下一次循环
                //这里和上面的区别就是多了个线程池，而不是直接新启动线程
                //本质上，上一步还是阻塞的
                executorService.submit(new TimeServerHandler(socket));
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if(null != serverSocket){
                serverSocket.close();
            }
        }
    }

    private static class TimeServerHandler implements Runnable{

        private Socket socket;

        public TimeServerHandler(Socket socket) {
            this.socket = socket;
        }

        public void run() {
            BufferedReader in = null;
            PrintWriter out = null;
            try {
                in = new BufferedReader(new InputStreamReader(this.socket.getInputStream()));
                out = new PrintWriter(this.socket.getOutputStream(), true);
                String body = null;
                while (true) {
                    body = in.readLine();
                    if (body == null)
                        break;
                    System.out.println(Thread.currentThread().getName() + "---收到消息 : " + body);
                    Thread.sleep(5000);//测试阻塞
                    out.println(body);//回传消息
                }

            } catch (Exception e) {
                if (in != null) {
                    try {
                        in.close();
                    } catch (IOException e1) {
                        e1.printStackTrace();
                    }
                }
                if (out != null) {
                    out.close();
                    out = null;
                }
                if (this.socket != null) {
                    try {
                        this.socket.close();
                    } catch (IOException e1) {
                        e1.printStackTrace();
                    }
                    this.socket = null;
                }
            }
        }
    }
}
