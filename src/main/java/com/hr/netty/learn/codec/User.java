package com.hr.netty.learn.codec;

import org.msgpack.annotation.Message;

import java.io.Serializable;
import java.nio.ByteBuffer;

/**
 * Created by huangrui on 2018/2/23.
 */
@Message
public class User implements Serializable{

    /**
     * 默认的序列号
     */
    private static final long serialVersionUID = 1L;

    private int id;
    private String name;

    public void setId(int id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    //自己提供序列化机制
    public byte[] codeC(){
        ByteBuffer buffer = ByteBuffer.allocate(1024);
        byte[] value = this.name.getBytes();
        buffer.putInt(value.length);
        buffer.put(value);
        buffer.putInt(this.id);
        buffer.flip();
        byte[] result = new byte[buffer.remaining()];
        buffer.get(result);
        return result;
    }

    public byte[] codeC(ByteBuffer buffer) {
        buffer.clear();
        byte[] value = this.name.getBytes();
        buffer.putInt(value.length);
        buffer.put(value);
        buffer.putInt(this.id);
        buffer.flip();
        byte[] result = new byte[buffer.remaining()];
        buffer.get(result);
        return result;
    }

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", name='" + name + '\'' +
                '}';
    }
}
