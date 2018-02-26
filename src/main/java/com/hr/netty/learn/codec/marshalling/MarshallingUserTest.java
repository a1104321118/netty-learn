package com.hr.netty.learn.codec.marshalling;

import java.io.Serializable;

/**
 * Created by huangrui on 2018/2/23.
 *
 * 实体类，测试marshalling 编解码
 */
public class MarshallingUserTest implements Serializable{

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


    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", name='" + name + '\'' +
                '}';
    }
}
