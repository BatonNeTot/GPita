package com.notjustsudio.gpita.network;

import com.notjuststudio.fpnt.FPNTContainer;
import com.notjuststudio.fpnt.FPNTDecoder;
import com.notjustsudio.gpita.util.ByteBufReader;
import com.notjustsudio.gpita.util.ByteBufUtils;
import com.sun.istack.internal.NotNull;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

class HandlerManager extends ChannelInboundHandlerAdapter {

    protected final Connection CONNECTION;
    private final boolean NEED_TO_PRINT_EXCEPTIONS;

    HandlerManager(@NotNull final Connection connection, @NotNull final boolean exceptions) {
        this.CONNECTION = connection;
        this.NEED_TO_PRINT_EXCEPTIONS = exceptions;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        if (CONNECTION.active != null)
            CONNECTION.active.handle(CONNECTION);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        if (CONNECTION.inactive != null)
            CONNECTION.inactive.handle(CONNECTION);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        final ByteBuf input = (ByteBuf) msg;
        final String key = ByteBufUtils.readString(input);
        if (CONNECTION.handlers.containsKey(key)) {
            final HandlerContainer handler = CONNECTION.handlers.get(key);
            final FPNTContainer container = new FPNTContainer(CONNECTION.getExpanders());
            FPNTDecoder.decode(new ByteBufReader(input), container);
            handler.handle(CONNECTION, container);
        }
    }



    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        if (NEED_TO_PRINT_EXCEPTIONS)
            cause.printStackTrace();
        if (CONNECTION.exception != null)
            CONNECTION.exception.handle(CONNECTION, cause);
        ctx.close();
    }
}
