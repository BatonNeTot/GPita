package com.notjustsudio.gpita.thread;

import com.sun.istack.internal.NotNull;

public interface CheckFunction<T> {

    boolean check(@NotNull final T t);

}
