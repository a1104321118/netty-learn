/**
 * Created by huangrui on 2018/2/23.
 *
 * 解决 tcp 粘包/拆包的问题
 *
 *
 * 解决tcp 拆包/粘包的关键就在这两行
 * ch.pipeline().addLast(new LineBasedFrameDecoder(1024));
 * ch.pipeline().addLast(new StringDecoder());
 *
 * 给它配置两个解码器，
 *
 * LineBasedFrameDecoder 的工作原理
 * 遍历bytebuf 里面的可读字节，如果有 \n 或者 \r\n 这个字符，就以此为结束位置
 * 我们发送的消息，是以 System.getProperty("line.separator") 结束的，所以可以被这个解码器正确地分割
 * LineBasedFrameDecoder 可以设置最大长度，如果遍历的可读字符超出了这个长度，还没有发现换行符的话，会抛异常
 *
 * 但是如果发送的消息不是以换行符作为结束标志的话，还是会发生粘包拆包的问题的
 * 不过netty 已经提供了很多解码器，满足绝大部分的需求
 *
 *
 * StringDecoder 的作用
 * 直接帮我们把msg 给String 化，可以强转成String，方便我们操作
 */
package com.hr.netty.learn.tcpcorrect;