package cn.edu.scut.common.utils;

public interface ILock {
    boolean tryLock(long timeout);
    void unlock();
}
