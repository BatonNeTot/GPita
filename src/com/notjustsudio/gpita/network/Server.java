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
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class Server implements Runnable {

    public final int PORT;
    private final LockBoolean needToPrintLogs = new LockBoolean(true);
    final LockBoolean needToPrintExceptions = new LockBoolean(false);
    final LockInteger threshold = new LockInteger(0);
    final LockInteger count = new LockInteger(0);

    final Set<Connection> CONNECTIONS = new ConcurrentHashSet<>();

    private final LockBoolean isRunning = new LockBoolean(false);

    private final Lock serverChannelLock = new ReentrantLock();
    private Channel serverChannel = null;

    private HanlderMapInitializer initializer = null;
    private HandlerCreator
            active = null,
            inactive = null;
    private HandlerExceptionCreator
            exception = null;

    private Set<FPNTExpander> expanders = new ConcurrentHashSet<>();

    public Server(@NotNull final int port) {
        this.PORT = port;
    }

    public boolean isRunning() {
        return isRunning.get();
    }

    public Server setMapInitializer(@NotNull final HanlderMapInitializer initializer) {
        if (!isRunning()) {
            this.initializer = initializer;
        }
        return this;
    }

    public HanlderMapInitializer getMapInitializer() {
        return initializer;
    }

    public Server setActiveInitializer(@NotNull final HandlerCreator initializer) {
        if (!isRunning()) {
            this.active = initializer;
        }
        return this;
    }

    public HandlerCreator getActiveInitializer() {
        return active;
    }

    public Server setInactiveInitializer(@NotNull final HandlerCreator initializer) {
        if (!isRunning()) {
            this.inactive = initializer;
        }
        return this;
    }

    public HandlerCreator getInactiveInitializer() {
        return inactive;
    }

    public Server setExceptionInitializer(@NotNull final HandlerExceptionCreator initializer) {
        if (!isRunning()) {
            this.exception = initializer;
        }
        return this;
    }

    public HandlerExceptionCreator getExceptionInitializer() {
        return exception;
    }

    public Server setThreshold(@NotNull final int count) {
        this.threshold.set(count);
        return this;
    }

    public int getThreshold() {
        return this.threshold.get();
    }

    public void shutdown() {
        serverChannelLock.lock();
        try {
            if (serverChannel != null)
                serverChannel.close();
        }finally {
            serverChannelLock.unlock();
        }
    }

    private void start() {
        if (isRunning())
            return;
        isRunning.set(true);

        final Server server = this;

        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        EventLoopGroup workerGroup = new NioEventLoopGroup();
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

            if (needToPrintLogs.get())
                b.handler(new LoggingHandler(LogLevel.INFO));

            // Start the server.
            ChannelFuture f = b.bind(PORT).sync();
            serverChannelLock.lock();
            try {
                serverChannel = f.channel();
            } finally {
                serverChannelLock.unlock();
            }

            // Wait until the server socket is closed.
            serverChannel.closeFuture().sync();

            serverChannelLock.lock();
            try {
                serverChannel = null;
            } finally {
                serverChannelLock.unlock();
            }

        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            // Shut down all event loops to terminate all threads.
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
            isRunning.set(false);
        }
    }

    @Override
    public void run() {
        start();
    }

    public Set<Connection> getConnections() {
        return new HashSet<>(CONNECTIONS);
    }

    public int getConnectionCount() {
        return count.get();
    }

    public Server addFPNTExpanders(@NotNull final Set<FPNTExpander> expanders) {
        this.expanders.addAll(expanders);
        return this;
    }

    public Server setNeedToPrintExceptions(@NotNull final boolean needToPrintExceptions) {
        if (!isRunning()) {
            this.needToPrintExceptions.set(needToPrintExceptions);
        }
        return this;
    }

    public boolean needToPrintExceptions() {
        return needToPrintExceptions.get();
    }

    public Server setNeedToPrintLogs(@NotNull final boolean needToPrintLogs) {
        if (!isRunning()) {
            this.needToPrintLogs.set(needToPrintLogs);
        }
        return this;
    }

    public boolean needToPrintLogs() {
        return needToPrintLogs.get();
    }
}
