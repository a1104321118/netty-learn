package com.hr.netty.learn.codec.messagePack;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import org.msgpack.MessagePack;

/**
 * Created by huangrui on 2018/2/23.
 *
 * 注意：使用msgpack 编解码 必须要在实体类上打上 @Message 注解
 */
public class MsgPackEncoder extends MessageToByteEncoder<Object>{

    @Override
    protected void encode(ChannelHandlerContext ctx, Object msg, ByteBuf out) throws Exception {
        MessagePack msgPack = new MessagePack();

        //序列化
        byte[] bytes = msgPack.write(msg);
        out.writeBytes(bytes);
    }
}
