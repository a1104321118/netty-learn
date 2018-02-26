package com.hr.netty.learn.codec.jdk;

import com.hr.netty.learn.codec.User;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.nio.ByteBuffer;


/**
 * Created by huangrui on 2018/2/23.
 * <p>
 * 测试比较 jdk序列化后的大小 和 自己实现序列化后的大小
 * 以及比较他们的速度
 * </p>
 */
public class SerializableTest {

    public static void main(String[] args) throws IOException {

        System.out.println("\n\n==========大小测试");

        User user = new User();
        user.setId(100);
        user.setName("哈哈哈");
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream os = new ObjectOutputStream(bos);
        os.writeObject(user);
        os.flush();
        os.close();
        byte[] b = bos.toByteArray();
        bos.close();
        System.out.println("jdk serializable 序列化后的大小: " + b.length);
        System.out.println("自己实现后的序列化的大小: " + user.codeC().length);

        System.out.println("\n\n==========性能测试");

        int loop = 1000;

        long start = System.currentTimeMillis();
        for (int i = 0; i < loop; i++) {
            jdkSerializable(user);
        }
        System.out.println("jdk serializable 耗时: " + (System.currentTimeMillis()-start));


        ByteBuffer buffer = ByteBuffer.allocate(1024);
        start = System.currentTimeMillis();
        for (int i = 0; i < loop; i++) {
            user.codeC(buffer);
        }
        System.out.println("自己实现后的序列化 耗时: " + (System.currentTimeMillis()-start));

    }

    public static void jdkSerializable(User user) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream os = new ObjectOutputStream(bos);
        os.writeObject(user);
        os.flush();
        os.close();
        bos.toByteArray();
        bos.close();
    }
}
