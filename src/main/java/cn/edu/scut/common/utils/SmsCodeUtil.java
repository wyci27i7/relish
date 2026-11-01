package cn.edu.scut.common.utils;

import java.security.SecureRandom;

public class SmsCodeUtil {
    // 全局单例，SecureRandom本身线程安全
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String CODE_FORMAT = "%06d";

    public static String generateCode() {
        // 生成 0~999999 之间的整数
        int num = RANDOM.nextInt(1_000_000);
        // 格式化为6位字符串，不足补前导零
        return String.format(CODE_FORMAT, num);
    }
}
