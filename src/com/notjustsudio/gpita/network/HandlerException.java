package com.notjustsudio.gpita.network;

import com.sun.istack.internal.NotNull;
import com.sun.istack.internal.Nullable;

public interface HandlerException {

    void handle(@Nullable final Connection connection, @NotNull final Throwable throwable);
}
