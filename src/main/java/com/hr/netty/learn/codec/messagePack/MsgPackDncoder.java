package com.hr.netty.learn.codec.messagePack;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;
import org.msgpack.MessagePack;

import java.util.List;

/**
 * Created by huangrui on 2018/2/23.
 *
 * 注意：使用msgpack 编解码 必须要在实体类上打上 @Message 注解
 */
public class MsgPackDncoder extends MessageToMessageDecoder<ByteBuf>{


    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf msg, List<Object> out) throws Exception {

        final byte[] array;
        int length = msg.readableBytes();
        array = new byte[length];
        msg.getBytes(msg.readerIndex(), array, 0, length);
        MessagePack messagePack = new MessagePack();
        out.add(messagePack.read(array));
    }
}
