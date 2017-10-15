package com.notjustsudio.gpita.network;

import com.notjuststudio.fpnt.FPNTContainer;
import com.notjuststudio.fpnt.FPNTDecoder;
import com.notjuststudio.fpnt.FPNTExpander;
import com.notjustsudio.gpita.util.ByteBufUtils;
import com.notjustsudio.gpita.util.ByteBufWriter;
import com.sun.istack.internal.NotNull;
import com.sun.istack.internal.Nullable;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class Connection {

    private final Channel channel;

    final Map<String, HandlerContainer> handlers = new ConcurrentHashMap<>();
    Handler
            active = null,
            inactive = null;
    HandlerException
            exception = null;

    Set<FPNTExpander> expanders = null;

    static Connection create(@NotNull final Channel channel, @Nullable final HanlderMapInitializer initializer, @Nullable final HandlerCreator active, @Nullable final HandlerCreator inactive, @Nullable final HandlerExceptionCreator exception) {
        final Map<String, HandlerContainer> handlerMap = new HashMap<>();
        if (initializer != null)
            initializer.createHandlerMap(handlerMap);
        return new Connection(channel, handlerMap, (active == null ? null : active.createHandler()), (inactive == null ? null : inactive.createHandler()), (exception == null ? null : exception.createHandler()));
    }

    private Connection(@NotNull final Channel channel, @NotNull final Map<String, HandlerContainer> handlers, @Nullable final Handler active, @Nullable final Handler inactive, @Nullable final HandlerException exception) {
        this.channel = channel;
        this.handlers.putAll(handlers);
        this.active = active;
        this.inactive = inactive;
        this.exception = exception;
    }

    public Channel getChannel() {
        return channel;
    }

    public void addHandler(@NotNull final String key, @NotNull final HandlerContainer handler) {
        handlers.put(key, handler);
    }

    public void removeHandler(@NotNull final String key) {
        handlers.remove(key);
    }

    void setExpanders(@NotNull final Set<FPNTExpander> expanders) {
        this.expanders = expanders;
    }

    Set<FPNTExpander> getExpanders() {
        return Collections.EMPTY_SET;
    }

    public void setActive(Handler active) {
        this.active = active;
    }

    public void setInactive(Handler inactive) {
        this.inactive = inactive;
    }

    public void setException(HandlerException exception) {
        this.exception = exception;
    }

    public boolean isAlive() {
        return channel.isActive();
    }

    public void close() {
        channel.close();
    }

    public void send(@NotNull final String target, @NotNull final FPNTContainer container) {
        if (!channel.isActive())
            return;

        final ByteBuf buffer = Unpooled.buffer(0);
        ByteBufUtils.writeString(target, buffer);
        final ByteBufWriter writer = new ByteBufWriter(buffer);
        FPNTDecoder.encode(writer, container);
        writer.flush();
        channel.writeAndFlush(buffer);
    }

}
