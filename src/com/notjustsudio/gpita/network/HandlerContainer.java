package com.notjustsudio.gpita.network;

import com.notjuststudio.fpnt.FPNTContainer;
import com.sun.istack.internal.NotNull;

public interface HandlerContainer {

    void handle(@NotNull final Connection connection, @NotNull final FPNTContainer container);

}
