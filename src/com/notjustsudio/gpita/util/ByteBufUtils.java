package com.notjustsudio.gpita.util;

import com.sun.istack.internal.NotNull;
import io.netty.buffer.ByteBuf;

public class ByteBufUtils {

    public static String readString(@NotNull final ByteBuf buffer) {
        final int length = buffer.readInt();
        final byte[] source = new byte[length];
        buffer.readBytes(source);
        return new String(source);
    }

    public static void writeString(@NotNull final String string, @NotNull final ByteBuf buffer) {
        final byte[] source = string.getBytes();
        buffer.capacity(buffer.capacity() + source.length + 4);
        buffer.writeInt(source.length);
        buffer.writeBytes(source);
    }
}
