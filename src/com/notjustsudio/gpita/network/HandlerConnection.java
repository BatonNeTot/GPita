package com.notjustsudio.gpita.network;

import com.sun.istack.internal.Nullable;

public interface HandlerConnection {

    void handle(@Nullable final Connection connection);
}
