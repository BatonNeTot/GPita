package com.notjustsudio.gpita.network;

import com.notjuststudio.fpnt.FPNTContainer;
import com.notjuststudio.fpnt.FPNTExpander;
import com.notjuststudio.thread.ConcurrentHashSet;
import com.notjustsudio.gpita.thread.LockBoolean;
import com.sun.istack.internal.NotNull;
import com.sun.istack.internal.Nullable;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;

import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class Client {

    public final int PORT;
    public final String HOST;
    private final LockBoolean needToPrintLogs = new LockBoolean(false);
    final LockBoolean needToPrintExceptions = new LockBoolean(false);

    private final LockBoolean isConnected = new LockBoolean(false);

    private final Lock connectionLock = new ReentrantLock();
    private Connection connection = null;

    private HanlderMapInitializer initializer = null;
    private HandlerCreator
            active = null,
            inactive = null;
    private HandlerExceptionCreator
            exception = null;

    private Set<FPNTExpander> expanders = new ConcurrentHashSet<>();

    public boolean isConnected() {
        return isConnected.get();
    }

    public Client(@NotNull final String host, @NotNull final int port) {
        this.PORT = port;
        this.HOST = host;
    }

    public void close() {
        connectionLock.lock();
        try {
            if (connection == null)
                return;
            connection.close();
        } finally {
            connectionLock.unlock();
        }
    }

    void setConnection(@Nullable final Connection connection) {
        connectionLock.lock();
        try {
            this.connection = connection;
        } finally {
            connectionLock.unlock();
        }
    }

    public Connection getConnection() {
        connectionLock.lock();
        try {
            return connection;
        } finally {
            connectionLock.unlock();
        }
    }

    public void connect() {
        if (isConnected())
            return;
        isConnected.set(true);

        final Client client = this;

        EventLoopGroup group = new NioEventLoopGroup();
        try {
            Bootstrap b = new Bootstrap();
            b.group(group)
                    .channel(NioSocketChannel.class)
                    .option(ChannelOption.TCP_NODELAY, true)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        public void initChannel(SocketChannel ch) throws Exception {
                            ChannelPipeline p = ch.pipeline();
                            if (needToPrintLogs.get())
                                p.addLast(new LoggingHandler(LogLevel.INFO));
                            final Connection connection = Connection.create(ch, initializer, active, inactive, exception);
                            connection.expanders = expanders;
                            final ClientHandlerManager handlerManager = new ClientHandlerManager(client, connection);
                            p.addLast(handlerManager);
                        }
                    });

            // Start the client.
            ChannelFuture f = b.connect(HOST, PORT).sync();

            // Wait until the connection is closed.
            f.channel().closeFuture().sync();

        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            // Shut down the event loop to terminate all threads.
            group.shutdownGracefully();
            isConnected.set(false);
        }
    }

    public Client addFPNTExpanders(@NotNull final Set<FPNTExpander> expanders) {
        this.expanders.addAll(expanders);
        return this;
    }

    public Client setNeedToPrintExceptions(@NotNull final boolean needToPrintExceptions) {
        if (!isConnected()) {
            this.needToPrintExceptions.set(needToPrintExceptions);
        }
        return this;
    }

    public boolean needToPrintExceptions() {
        return needToPrintExceptions.get();
    }

    public Client setNeedToPrintLogs(@NotNull final boolean needToPrintLogs) {
        if (!isConnected()) {
            this.needToPrintLogs.set(needToPrintLogs);
        }
        return this;
    }

    public boolean needToPrintLogs() {
        return needToPrintLogs.get();
    }

    public Client setMapInitializer(@NotNull final HanlderMapInitializer initializer) {
        if (!isConnected()) {
            this.initializer = initializer;
        }
        return this;
    }

    public HanlderMapInitializer getMapInitializer() {
        return initializer;
    }

    public Client setActiveInitializer(@NotNull final HandlerCreator initializer) {
        if (!isConnected()) {
            this.active = initializer;
        }
        return this;
    }

    public HandlerCreator getActiveInitializer() {
        return active;
    }

    public Client setInactiveInitializer(@NotNull final HandlerCreator initializer) {
        if (!isConnected()) {
            this.inactive = initializer;
        }
        return this;
    }

    public HandlerCreator getInactiveInitializer() {
        return inactive;
    }

    public Client setExceptionInitializer(@NotNull final HandlerExceptionCreator initializer) {
        if (!isConnected()) {
            this.exception = initializer;
        }
        return this;
    }

    public HandlerExceptionCreator getExceptionInitializer() {
        return exception;
    }

    public void send(@NotNull final String target, @NotNull final FPNTContainer container) {
        connectionLock.lock();
        try {
            if (connection == null)
                return;
            connection.send(target, container);
        } finally {
            connectionLock.unlock();
        }
    }
}
