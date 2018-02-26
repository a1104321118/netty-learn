package com.hr.netty.learn.bio;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

/**
 * Created by huangrui on 2018/1/31.
 */
public class TimeClient {

    public static void main(String[] args) throws IOException {
        Socket socket = null;
        BufferedReader reader = null;
        PrintWriter writer = null;

        try {
            socket = new Socket("localhost", 8080);
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            writer = new PrintWriter(socket.getOutputStream(), true);

            writer.println("hello world 啦啦啦"); // 通过socket向8080写数据
            String s = reader.readLine();  // 通过socket向8080读数据，会阻塞等待
            System.out.println(s);

        }catch (Exception e){

        }finally {
            if (writer != null) {
                writer.close();
                writer = null;
            }

            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                reader = null;
            }

            if (socket != null) {
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                socket = null;
            }
        }
    }
}
