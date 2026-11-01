package cn.edu.scut.common.utils;

import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@RequiredArgsConstructor
public class RedisLock implements ILock {

    private final String name;
    private final StringRedisTemplate stringRedisTemplate;
    private static final String KEY_PREFIX = "RedisLock:";
    private static final String ID_PREFIX = UUID.randomUUID() + "-";

    private static final DefaultRedisScript<Long> UNLOCK_SCRIPT;

    static {
        UNLOCK_SCRIPT = new DefaultRedisScript<>();
        UNLOCK_SCRIPT.setResultType(Long.class);
        UNLOCK_SCRIPT.setLocation(new ClassPathResource("unlock.lua"));
    }


    @Override
    public boolean tryLock(long timeout) {
        String key = KEY_PREFIX + name;
        String value = ID_PREFIX + Thread.currentThread().getId();
        Boolean flag = stringRedisTemplate.opsForValue().setIfAbsent(key, value, timeout, TimeUnit.SECONDS);
        return Boolean.TRUE.equals(flag);
    }

    @Override
    public void unlock() {
        String key = KEY_PREFIX + name;
        String value = ID_PREFIX + Thread.currentThread().getId();
        /*
        * 写法一：非原子化，可能有问题
        if (value != null && value.equals(ID_PREFIX + Thread.currentThread().getId())) {
            stringRedisTemplate.delete(key);
        }
        * */
        // 执行LUA脚本确保原子操作
        stringRedisTemplate.execute(UNLOCK_SCRIPT, List.of(key), value);
    }
}
