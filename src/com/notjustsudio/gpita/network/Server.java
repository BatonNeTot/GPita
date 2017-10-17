package com.notjustsudio.gpita.network;

import com.notjuststudio.fpnt.FPNTExpander;
import com.notjuststudio.thread.ConcurrentHashSet;
import com.notjustsudio.gpita.thread.LockBoolean;
import com.notjustsudio.gpita.thread.LockInteger;
import com.sun.istack.internal.NotNull;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class Server implements Callable<Boolean> {

    private final Lock statusLock = new ReentrantLock();
    private int status = READY;

    public static final int
            READY = 0,
            STARTING = 1,
            RUNNING = 2,
            STOPPING = 3;

    private final LockInteger port;
    private final LockBoolean printLogs = new LockBoolean(false);
    final LockBoolean printExceptions = new LockBoolean(false);

    final LockInteger threshold = new LockInteger(0);
    final LockInteger count = new LockInteger(0);

    private Set<FPNTExpander> expanders = new ConcurrentHashSet<>();
    final Set<Connection> CONNECTIONS = new ConcurrentHashSet<>();

    private Channel serverChannel = null;

    private EventLoopGroup
            bossGroup,
            workerGroup;

    private HanlderMapInitializer initializer = null;
    private HandlerConnectionCreator
            active = null,
            inactive = null;
    private Handler
            started = null,
            stopped = null;
    private HandlerExceptionCreator
            exception = null;

    public Server(@NotNull final int port) {
        this.port = new LockInteger(port);
    }

    public Set<Connection> getConnections() {
        return new HashSet<>(CONNECTIONS);
    }

    public int connectionCount() {
        return count.get();
    }

    public Server addFPNTExpanders(@NotNull final Set<FPNTExpander> expanders) {
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

    public boolean isRunning() {
        return status() == RUNNING;
    }

    @Override
    public Boolean call() throws Exception {
        statusLock.lock();
        try {
            if (status != READY) {
                return false;
            } else {
                status = STARTING;
            }
        } finally {
            statusLock.unlock();
        }

        final Server server = this;

        bossGroup = new NioEventLoopGroup(1);
        workerGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .option(ChannelOption.SO_BACKLOG, 100)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        public void initChannel(SocketChannel ch) throws Exception {
                            ChannelPipeline p = ch.pipeline();
//                            p.addLast(new LoggingHandler(LogLevel.INFO));
                            final Connection connection = Connection.create(ch, initializer, active, inactive, exception);
                            connection.expanders = expanders;
                            final ServerHandlerManager handlerManager = new ServerHandlerManager(server, connection);
                            p.addLast(handlerManager);
                        }
                    });

            if (printLogs.get())
                b.handler(new LoggingHandler(LogLevel.INFO));

            // Start the server.
            ChannelFuture f = b.bind(port.get()).sync();

            statusLock.lock();
            try {
                status = RUNNING;
            } finally {
                statusLock.unlock();
            }

            serverChannel = f.channel();

            if (started != null)
                started.handle();

            new Thread(() -> {
                try {
                    // Wait until the server socket is closed.
                    serverChannel.closeFuture().sync();
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

    public void shutdown() {
        statusLock.lock();
        try {
            if (status == RUNNING)
                serverChannel.close();
        } finally {
            statusLock.unlock();
        }
    }

    private void clearUp() {
        bossGroup.shutdownGracefully();
        workerGroup.shutdownGracefully();
        serverChannel = null;

        if (stopped != null)
            stopped.handle();

        statusLock.lock();
        try {
            status = READY;
        } finally {
            statusLock.unlock();
        }
    }

    public Server port(@NotNull final int port) {
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

    public Server map(@NotNull final HanlderMapInitializer initializer) {
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

    public Server active(@NotNull final HandlerConnectionCreator initializer) {
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

    public Server inactive(@NotNull final HandlerConnectionCreator initializer) {
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

    public Server started(@NotNull final Handler handler) {
        statusLock.lock();
        try {
            if (status == READY)
                this.started = handler;
        } finally {
            statusLock.unlock();
        }
        return this;
    }

    public Handler started() {
        return started;
    }

    public Server stopped(@NotNull final Handler handler) {
        statusLock.lock();
        try {
            if (status == READY)
                this.stopped = handler;
        } finally {
            statusLock.unlock();
        }
        return this;
    }

    public Handler stopped() {
        return stopped;
    }

    public Server exception(@NotNull final HandlerExceptionCreator initializer) {
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

    public Server threshold(@NotNull final int count) {
        statusLock.lock();
        try {
            if (status == READY)
                this.threshold.set(count);
        } finally {
            statusLock.unlock();
        }
        return this;
    }

    public int threshold() {
        return this.threshold.get();
    }

    public Server printLogs(@NotNull final boolean printLogs) {
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

    public Server printExceptions(@NotNull final boolean printExceptions) {
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
