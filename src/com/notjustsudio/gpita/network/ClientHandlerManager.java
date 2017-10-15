package com.notjustsudio.gpita.network;

import com.sun.istack.internal.NotNull;
import io.netty.channel.ChannelHandlerContext;

class ClientHandlerManager extends HandlerManager {

    private final Client CLIENT;

    ClientHandlerManager(@NotNull final Client client, @NotNull final Connection connection) {
        super(connection, client.needToPrintExceptions.get());
        this.CLIENT = client;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        CLIENT.setConnection(CONNECTION);
        super.channelActive(ctx);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        CLIENT.setConnection(null);
        super.channelInactive(ctx);
    }
}
