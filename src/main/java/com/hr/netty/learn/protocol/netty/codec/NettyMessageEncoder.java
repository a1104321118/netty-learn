package com.hr.netty.learn.protocol.netty.codec;

import com.hr.netty.learn.protocol.netty.struct.NettyMessage;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import org.jboss.marshalling.Marshaller;

import java.io.IOException;
import java.util.Map;

/**
 * Created by huangrui on 2018/3/7.
 */
public class NettyMessageEncoder extends MessageToByteEncoder<NettyMessage> {

    private static Marshaller marshaller;
    private static final byte[] LENGTH_PLACEHOLDER = new byte[4];

    static {
        try {
            marshaller = MarshallingCodecFactory.buildMarshalling();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    @Override
    protected void encode(ChannelHandlerContext ctx, NettyMessage msg, ByteBuf sendBuf) throws Exception {
        if(null == msg || null == msg.getHeader()){
            throw new RuntimeException("消息缺失");
        }

        //写消息头
        sendBuf.writeInt((msg.getHeader().getCrcCode()));
        //注意，这里的length 是先随便写的，用来占位，但实际协议栈里面的长度是指 消息头+ 消息体的长度
        //此处是从第5个字节开始写的，因为前面写了个int，占了四个字节
        //所以本方法的最后是这么写的： sendBuf.setInt(4, sendBuf.readableBytes());
        sendBuf.writeInt((msg.getHeader().getLength()));
        sendBuf.writeLong((msg.getHeader().getSessionID()));
        sendBuf.writeByte((msg.getHeader().getType()));
        sendBuf.writeByte((msg.getHeader().getPriority()));
        sendBuf.writeInt((msg.getHeader().getAttachment().size()));

        //写消息头里面的附件
        String key;
        byte[] keyArray;
        Object value;
        for (Map.Entry<String, Object> param : msg.getHeader().getAttachment()
                .entrySet()) {
            key = param.getKey();
            keyArray = key.getBytes("UTF-8");
            sendBuf.writeInt(keyArray.length);
            sendBuf.writeBytes(keyArray);
            value = param.getValue();
            marsEncode(value, sendBuf);
        }

        //指向null 释放内存
        key = null;
        keyArray = null;
        value = null;

        //写消息体
        if (msg.getBody() != null) {
            marsEncode(msg.getBody(), sendBuf);
        } else {
            sendBuf.writeInt(0);
        }

        //第5位开始是放消息的长度的
        //为什么要 -8 因为设置的 decoder   lengthFieldOffset, lengthFieldLength = 4, 4
        //意思是  decoder 会认为 消息长度之前会空4字节（lengthFieldOffset）， 而消息的长度本身有4字节（lengthFieldLength）
        //如果这里不 -8 ，那么netty 在解码的时候会比较填写消息的长度值，和真实消息的长度值，如果不相等的话，会返回null
        /**
         * 我们在构造方法里告诉netty，lengthFieldOffset, lengthFieldLength = 4，4
         * netty 就会从 第5个字节（因为lengthFieldOffset=4，所以netty会往后空4个字节开始取）到第8个字节（因为lengthFieldLength=4，5~8 是4个字节的长度）
         * 取出我们填写的长度， 即 aLength = sendBuf.readableBytes() - 8
         * 并且从第9个字节开始到最后一个字节，开始计算消息的真实长度=bLength
         * 如果 aLength != bLength 则解码结果会返回null
         */
        sendBuf.setInt(4, sendBuf.readableBytes() - 8);
    }

    private void marsEncode(Object msg, ByteBuf byteBuf) throws IOException {
        try {
            int lengthPos = byteBuf.writerIndex();
            byteBuf.writeBytes(LENGTH_PLACEHOLDER);//预留出4个字节的长度(因为int在java中占4个字节)用来放消息体的长度，使netty能正确地编解码
            ChannelBufferByteOutput output = new ChannelBufferByteOutput(byteBuf);
            marshaller.start(output);
            marshaller.writeObject(msg);
            marshaller.finish();
            //byteBuf.writerIndex() - lengthPos - 4,这个就是消息体的长度
            byteBuf.setInt(lengthPos, byteBuf.writerIndex() - lengthPos - 4);
        } finally {
            marshaller.close();
        }
    }
}
