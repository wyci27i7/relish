package cn.edu.scut.common.utils;

import cn.edu.scut.entity.RedisData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

@Component
@RequiredArgsConstructor
@Slf4j
public class CacheClient {

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    private static final ExecutorService CACHE_REBUILD_EXECUTOR = Executors.newFixedThreadPool(10);

    public <T> void set(String key, T value, Long time, TimeUnit unit) {
        String valueJson = objectMapper.writeValueAsString(value);
        stringRedisTemplate.opsForValue().set(key, valueJson, time, unit);
    }

    public <T> void setWithLogicExpireTime(String key, T value, Long expireTime, TimeUnit timeUnit) {
        RedisData<T> redisData = new RedisData<>();
        redisData.setExpireTime(LocalDateTime.now().plusSeconds(timeUnit.toSeconds(expireTime)));
        redisData.setData(value);
        String valueJson = objectMapper.writeValueAsString(redisData);
        stringRedisTemplate.opsForValue().set(key, valueJson);
    }

    /*
     * 缓存穿透（使用设置null值方案） + 缓存击穿（使用逻辑过期方案）
     * */
    public <T> T enhanceGet(String cache_key_prefix, Long id, Class<T> clazz,
                            Function<Long, T> dbFallback, Long time, TimeUnit unit) {
        String key = cache_key_prefix + id;
        String valueJson = stringRedisTemplate.opsForValue().get(key);
        if (valueJson != null) {
            if (valueJson.isBlank() || "\"\"".equals(valueJson)) {
                return null;
            } else {
                /*
                 * 不为空则解析字符串，判断是否过期
                 * 如果过期了，则需要获取锁，获取锁后进行double check，
                 *   然后开一个线程来异步刷新数据，异步线程用 try finally 包围
                 *   在finally中释放锁
                 * 该线程直接先返回旧数据
                 * 如果没有获得锁的也先返回旧数据
                 * 如果没有过期则直接返回数据
                 * */
                RedisData redisData = objectMapper.readValue(valueJson, RedisData.class);
                LocalDateTime expireTime = redisData.getExpireTime();
                T t = objectMapper.convertValue(redisData.getData(), clazz);
                if (expireTime.isAfter(LocalDateTime.now())) {
                    // 还没期了
                    return t;
                }
                // 过期了，需要refresh Redis
                // 尝试获取锁，这里锁的Key后续也可以改造成由用户来传，目前业务简单先这样
                String refreshKey = "lock:T:" + id;
                boolean flag = tryLock(refreshKey);
                if (!flag) {
                    // 没有获取到锁，直接返回旧数据
                    return t;
                }
                // 获取到锁来，需要进行二次检查，看看目前RedisData可能已经被刷新好了
                // 已经被刷新好就不浪费时间了，这一步属于工程优化
                String newValueJson = stringRedisTemplate.opsForValue().get(key);
                RedisData newRedisData = objectMapper.readValue(newValueJson, RedisData.class);
                LocalDateTime newExpireTime = newRedisData.getExpireTime();
                T newT = objectMapper.convertValue(newRedisData.getData(), clazz);
                if (newExpireTime.isAfter(LocalDateTime.now())) {
                    // 已经刷新好了，先把锁释放了
                    unlock(refreshKey);
                    return newT;
                }
                // 还没刷新好，就无事发生，开始查数据库进行刷新
                CACHE_REBUILD_EXECUTOR.submit(() -> {
                    try {
                        // 查数据
                        T refreshT = dbFallback.apply(id);
                        // 重建缓存
                        setWithLogicExpireTime(key, refreshT, time, unit);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    } finally {
                        // 释放锁
                        unlock(refreshKey);
                    }
                });
                // 先返回旧数据
                return newT;
            }
        }

        // TODO 必须提前数据预热才行，先手动执行一遍当前方法，执行以下代码逻辑进行数据预热
        // 如果当前数据在Redis里面真的为空
        // 直接使用用户传进来的函数进行查询数据库数据
        T t = dbFallback.apply(id);
        if (t == null) {
            // 如果数据库中也没有这个数据，则进入缓存穿透的处理模式
            // 这里过期时间可以设置的短一点
            stringRedisTemplate.opsForValue().set(key, "", 300L, TimeUnit.SECONDS);
            return null;
        }
        // 查到了数据，则需要写入Redis里
        setWithLogicExpireTime(key, t, time, unit);
        // 返回 t 给前端即可
        return t;
    }

    // 互斥锁，利用Redis setnx 实现
    private boolean tryLock(String key) {
        // 这里先设置10s，后续线上数据库有可能会查一分钟以上。所以后续要优化
        Boolean flag = stringRedisTemplate.opsForValue().setIfAbsent(key, "1", 10, TimeUnit.SECONDS);
        return Boolean.TRUE.equals(flag);
    }

    private void unlock(String key) {
        stringRedisTemplate.delete(key);
    }

}
