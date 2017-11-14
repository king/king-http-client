package com.king.platform.net.http.netty;

import io.netty.channel.ChannelHandlerContext;

public interface ResponseHandler {
    void handleResponse(ChannelHandlerContext ctx, Object msg) throws Exception;
    void handleChannelInactive(ChannelHandlerContext ctx) throws Exception;
}
