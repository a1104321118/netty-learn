/**
 * Created by huangrui on 2018/2/1.
 * nio 总结
 *
 * Selector  多路复用选择器
 *
 * 两种通道  通道是双向的，可读可写，流是单向的
 * ServerSocketChannel
 * SocketChannel
 *
 * 一个Selector 上可以注册（register）多个通道，包括上面两种通道
 * 注册的时候可以选择注册通道感兴趣的状态，可以是复合状态
 * 四个状态
 * connect
 * accept
 * read
 * write
 *
 * 调用selector.select() 会返回注册在其上，并且处于就绪状态（即注册时感兴趣的状态）通道的个数
 * 调用selector.selectKeys() 会返回注册在其上，并且处于就绪状态的SelectionKey集合
 *
 * 然后可以遍历这个SelectionKey集合
 * 通过这个SelectionKey 可以拿到注册的通道，然后可以根据发生的事件，转换成对应的通道类型，并对其进行操作
 *
 *
 *
 */
package com.hr.netty.learn.nio;