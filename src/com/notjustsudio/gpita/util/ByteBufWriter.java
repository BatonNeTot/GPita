package com.notjustsudio.gpita.util;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import java.io.IOException;
import java.io.OutputStream;
import java.util.LinkedList;
import java.util.List;

public class ByteBufWriter extends OutputStream {

    private final ByteBuf target;

    private int count = 0;
    private final int size = 8192;
    private final byte[] buffer = new byte[size];

    public ByteBufWriter(ByteBuf buffer) {
        this.target = buffer;
    }

    @Override
    public void write(int b) {
        if (count >= size)
            flush();
        buffer[count++] = (byte)b;
    }

    @Override
    public void flush() {
        target.capacity(target.capacity() + count);
        target.writeBytes(buffer, 0, count);
        count = 0;
    }
}
