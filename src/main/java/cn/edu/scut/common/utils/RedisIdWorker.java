package cn.edu.scut.common.utils;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

@Component
@RequiredArgsConstructor
public class RedisIdWorker {

    private final StringRedisTemplate stringRedisTemplate;

    public long nextId(String keyPrefix) {
        // 获取当前时间戳
        LocalDateTime now = LocalDateTime.now();
        long ts = now.toEpochSecond(ZoneOffset.UTC);
        // 从Redis中读取增长的值
        String day = now.format(DateTimeFormatter.ofPattern("yyyy:MM:dd"));
        String key = "incr:" + keyPrefix + ":" + day;
        long count = stringRedisTemplate.opsForValue().increment(key);
        return ts << 32 | count;
    }

}
