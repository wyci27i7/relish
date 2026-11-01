package cn.edu.scut.common.utils;

public class UserHolder {
    private static final ThreadLocal<Long> TL = new ThreadLocal<>();

    public static void setUserId(Long userId) {
        TL.set(userId);
    }

    public static Long getUserId() {
        return TL.get();
    }

    public static void remove() {
        TL.remove();
    }
}
