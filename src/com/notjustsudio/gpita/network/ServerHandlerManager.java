package com.notjustsudio.gpita.network;

import com.sun.istack.internal.NotNull;
import io.netty.channel.ChannelHandlerContext;

class ServerHandlerManager extends HandlerManager {

    private final Server SERVER;

    ServerHandlerManager(@NotNull final Server server, @NotNull final Connection connection) {
        super(connection, server.needToPrintExceptions.get());
        this.SERVER = server;
    }

    @Override
    public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
        if (SERVER.count.incrementIf((value) -> SERVER.threshold.get() == 0 || value < SERVER.threshold.get())) {
            SERVER.CONNECTIONS.add(CONNECTION);
        } else {
            ctx.close();
        }
    }

    @Override
    public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
        SERVER.CONNECTIONS.remove(CONNECTION);
        SERVER.count.decrement();
    }
}
