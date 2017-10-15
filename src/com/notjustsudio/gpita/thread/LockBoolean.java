package com.notjustsudio.gpita.thread;

import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class LockBoolean {

    private boolean value;
    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    public LockBoolean(boolean value) {
        this.value = value;
    }

    public boolean get() {
        lock.readLock().lock();
        try {
            return value;
        } finally {
            lock.readLock().unlock();
        }
    }

    public void set(boolean value) {
        lock.writeLock().lock();
        try {
            this.value = value;
        } finally {
            lock.writeLock().unlock();
        }
    }
}
