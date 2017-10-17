package com.notjustsudio.gpita.network;

import com.notjuststudio.fpnt.FPNTContainer;
import com.notjuststudio.fpnt.FPNTExpander;
import com.notjuststudio.thread.ConcurrentHashSet;
import com.notjustsudio.gpita.thread.LockBoolean;
import com.notjustsudio.gpita.thread.LockInteger;
import com.sun.istack.internal.NotNull;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;

import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class Client implements Callable<Boolean> {

    private final Lock statusLock = new ReentrantLock();
    private int status = READY;

    public static final int
            READY = 0,
            CONNECTING = 1,
            CONNECTED = 2,
            CLOSING = 3;

    public final LockInteger port;
    private String host;
    private final LockBoolean printLogs = new LockBoolean(false);
    final LockBoolean printExceptions = new LockBoolean(false);

    private Set<FPNTExpander> expanders = new ConcurrentHashSet<>();

    private Connection connection = null;

    private EventLoopGroup
            group;

    private HanlderMapInitializer initializer = null;
    private HandlerConnectionCreator
            active = null,
            inactive = null;
    private HandlerExceptionCreator
            exception = null;

    public Client(@NotNull final String host, @NotNull final int port) {
        this.port = new LockInteger(port);
        this.host = host;
    }

    public Connection connection() {
        statusLock.lock();
        try {
            if (status == CONNECTED)
                return connection;
            throw new IllegalStateException("Connection hasn't yet been established");
        } finally {
            statusLock.unlock();
        }
    }

    public Client addFPNTExpanders(@NotNull final Set<FPNTExpander> expanders) {
        this.expanders.addAll(expanders);
        return this;
    }

    public int status() {
        statusLock.lock();
        try {
            return status;
        } finally {
            statusLock.unlock();
        }
    }

    public boolean isConnected() {
        return status() == CONNECTED;
    }

    @Override
    public Boolean call() throws Exception {
        statusLock.lock();
        try {
            if (status != READY) {
                return false;
            } else {
                status = CONNECTING;
            }
        } finally {
            statusLock.unlock();
        }

        group = new NioEventLoopGroup();
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
                            connection = Connection.create(ch, initializer, active, inactive, exception);
                            connection.expanders = expanders;
                            final HandlerManager handlerManager = new HandlerManager(connection, printExceptions.get());
                            p.addLast(handlerManager);
                        }
                    });

            // Start the client.
            ChannelFuture f = b.connect(host, port.get()).sync();

            statusLock.lock();
            try {
                status = CONNECTED;
            } finally {
                statusLock.unlock();
            }

            new Thread(() -> {
                try {
                    // Wait until the connection is closed.
                    f.channel().closeFuture().sync();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } finally {
                    clearUp();
                }
            }).start();

            return true;

        } catch (Throwable t) {
            if (printExceptions.get())
                t.printStackTrace();
            clearUp();
            return false;
        }
    }

    public void close() {
        statusLock.lock();
        try {
            if (status == CONNECTED)
                connection.close();
        } finally {
            statusLock.unlock();
        }
    }

    private void clearUp() {
        group.shutdownGracefully();
        connection = null;

        statusLock.lock();
        try {
            status = READY;
        } finally {
            statusLock.unlock();
        }

    }

    public Client host(@NotNull final String host) {
        statusLock.lock();
        try {
            if (status == READY)
                this.host = host;
        } finally {
            statusLock.unlock();
        }
        return this;
    }

    public String host() {
        return host;
    }

    public Client port(@NotNull final int port) {
        statusLock.lock();
        try {
            if (status == READY)
                this.port.set(port);
        } finally {
            statusLock.unlock();
        }
        return this;
    }

    public int port() {
        return port.get();
    }

    public Client map(@NotNull final HanlderMapInitializer initializer) {
        statusLock.lock();
        try {
            if (status == READY)
                this.initializer = initializer;
        } finally {
            statusLock.unlock();
        }
        return this;
    }

    public HanlderMapInitializer map() {
        return initializer;
    }

    public Client active(@NotNull final HandlerConnectionCreator initializer) {
        statusLock.lock();
        try {
            if (status == READY)
                this.active = initializer;
        } finally {
            statusLock.unlock();
        }
        return this;
    }

    public HandlerConnectionCreator active() {
        return active;
    }

    public Client inactive(@NotNull final HandlerConnectionCreator initializer) {
        statusLock.lock();
        try {
            if (status == READY)
                this.inactive = initializer;
        } finally {
            statusLock.unlock();
        }
        return this;
    }

    public HandlerConnectionCreator inactive() {
        return inactive;
    }

    public Client exception(@NotNull final HandlerExceptionCreator initializer) {
        statusLock.lock();
        try {
            if (status == READY)
                this.exception = initializer;
        } finally {
            statusLock.unlock();
        }
        return this;
    }

    public HandlerExceptionCreator exception() {
        return exception;
    }

    public Client printLogs(@NotNull final boolean printLogs) {
        statusLock.lock();
        try {
            if (status == READY)
                this.printLogs.set(printLogs);
        } finally {
            statusLock.unlock();
        }
        return this;
    }

    public boolean printLogs() {
        return printLogs.get();
    }

    public Client printExceptions(@NotNull final boolean printExceptions) {
        statusLock.lock();
        try {
            if (status == READY)
                this.printExceptions.set(printExceptions);
        } finally {
            statusLock.unlock();
        }
        return this;
    }

    public boolean printExceptions() {
        return printExceptions.get();
    }
}
