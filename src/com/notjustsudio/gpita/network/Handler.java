package com.notjustsudio.gpita.network;

import com.sun.istack.internal.Nullable;

public interface Handler {

    void handle(@Nullable final Connection connection);
}
