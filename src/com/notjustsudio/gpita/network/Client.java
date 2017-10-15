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
    private final LockBoolean printLogs = new LockBoolean(false);
    final LockBoolean printExceptions = new LockBoolean(false);

    private final LockBoolean isConnected = new LockBoolean(false);

    private final Lock connectionLock = new ReentrantLock();
    private Connection connection = null;

    private HanlderMapInitializer initializer = null;
    private HandlerConnectionCreator
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

    public Connection connection() {
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
                            if (printLogs.get())
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

    public Client printExceptions(@NotNull final boolean printExceptions) {
        if (!isConnected()) {
            this.printExceptions.set(printExceptions);
        }
        return this;
    }

    public boolean printExceptions() {
        return printExceptions.get();
    }

    public Client printLogs(@NotNull final boolean printLogs) {
        if (!isConnected()) {
            this.printLogs.set(printLogs);
        }
        return this;
    }

    public boolean printLogs() {
        return printLogs.get();
    }

    public Client map(@NotNull final HanlderMapInitializer initializer) {
        if (!isConnected()) {
            this.initializer = initializer;
        }
        return this;
    }

    public HanlderMapInitializer map() {
        return initializer;
    }

    public Client active(@NotNull final HandlerConnectionCreator initializer) {
        if (!isConnected()) {
            this.active = initializer;
        }
        return this;
    }

    public HandlerConnectionCreator active() {
        return active;
    }

    public Client inactive(@NotNull final HandlerConnectionCreator initializer) {
        if (!isConnected()) {
            this.inactive = initializer;
        }
        return this;
    }

    public HandlerConnectionCreator inactive() {
        return inactive;
    }

    public Client exception(@NotNull final HandlerExceptionCreator initializer) {
        if (!isConnected()) {
            this.exception = initializer;
        }
        return this;
    }

    public HandlerExceptionCreator exception() {
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
