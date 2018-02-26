/**
 * Created by huangrui on 2018/2/23.
 *
 * 编解码，序列化机制
 *
 * 1.messagePack  不重点介绍
 *
 * 2.protobuf   谷歌的编解码工具
 * 需要编写 .proto 文件，并下载 protobuf 生成java类 ，idea可以安装 .proto的插件
 * 下载地址  v2.5.0   https://github.com/google/protobuf/releases/download/v2.5.0/protobuf-2.5.0.zip
 *
 * 3.marshalling  jboss 提供的序列化工具
 * 实体类必须声明  Serializable接口
 *
 *
 */
package com.hr.netty.learn.codec;