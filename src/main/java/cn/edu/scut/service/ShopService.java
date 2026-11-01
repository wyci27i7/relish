package cn.edu.scut.service;

import cn.edu.scut.common.result.Result;
import cn.edu.scut.common.utils.CacheClient;
import cn.edu.scut.entity.RedisData;
import cn.edu.scut.entity.Shop;
import cn.edu.scut.entity.ShopType;
import cn.edu.scut.mapper.ShopMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class ShopService {

    private final ShopMapper shopMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;
    private final CacheClient cacheClient;

    public Result _getShopById(Long id) {
        String prefix = "cache:shop:";
        Shop shop = cacheClient.enhanceGet(prefix, id, Shop.class,
                shopMapper::selectShopById, 300L, TimeUnit.SECONDS);
        return Result.ok(shop);
    }

    public Result getShopById(Long id) {
        // 先看Redis中有没有这个缓存
        String key = "cache:shop:" + id;
        String shopJson = stringRedisTemplate.opsForValue().get(key);
        // 如果有这个缓存，直接取即可返回
        if (shopJson != null) {
            if (!shopJson.isEmpty()) {
                Shop shop = objectMapper.readValue(shopJson, Shop.class);
                log.info("Redis Cache Hit: getShopById({})", id);
                return Result.ok(shop);
            } else {
                // 进入缓存的空值阶段
                return Result.fail("店铺信息不存在");
            }
        }
        String refreshShopKey = null;
        Shop shop;
        try {
            // 获取锁，获取刷新的资格
            refreshShopKey = "lock:shop:" + id;
            if (!tryLock(refreshShopKey)) {
                // 睡眠
                Thread.sleep(500);
                return getShopById(id);
            }
            // 如果没有缓存，则去数据库中取
            shop = shopMapper.selectShopById(id);
            if (shop == null) {
                // 这里使用缓存空值的方式去解决 缓存穿透
                stringRedisTemplate.opsForValue().set(key, "", 2, TimeUnit.MINUTES);
                return Result.fail("没有该店铺！");
            }
            // 把读取出来的数据写入Redis中
            String WriteCacheShopJson = objectMapper.writeValueAsString(shop);
            stringRedisTemplate.opsForValue().set(key, WriteCacheShopJson, 30, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            // 释放锁
            unlock(refreshShopKey);
        }

        return Result.ok(shop);
    }

    private boolean tryLock(String key) {
        Boolean flag = stringRedisTemplate.opsForValue().setIfAbsent(key, "1", 10, TimeUnit.SECONDS);
        return Boolean.TRUE.equals(flag);
    }

    private void unlock(String key) {
        stringRedisTemplate.delete(key);
    }

    /*
     * 使用逻辑删除来解决缓存击穿
     * 在设置数据的时候增加一个逻辑时间去查看数据什么时候过期，如果数据过期了则
     * 需要去抢一个更新数据的锁，
     * 抢到锁的线程则去异步调用一个线程去更新数据到Redis上，返回Redis上的旧数据即可
     * 没有抢到锁则直接返回旧数据
     * 一般需要提前把数据预热进Redis里面
     * */
    public Result getShopByIdWithLogicDelete(Long id) {
        // 把数据读取出来
        String key = "cache:shop:" + id;
        String dataJson = stringRedisTemplate.opsForValue().get(key);
        if (dataJson == null) {
            return Result.fail("没有这个数据！！！(开发中)");
        }
        // 判断有没有过期
        RedisData redisData = objectMapper.readValue(dataJson, RedisData.class);
        LocalDateTime expireTime = redisData.getExpireTime();
        // 直接直接强转了???
//        Shop shop = (Shop) redisData.getData();
        Shop shop = objectMapper.convertValue(redisData.getData(), Shop.class);

        // 过期了
        if (LocalDateTime.now().isAfter(expireTime)) {
            // 看能不能抢到锁
            String refreshShopKey = "lock:shop:" + id;
            // 没有抢到锁，直接返回旧数据
            if (!tryLock(refreshShopKey)) {
                return Result.ok(shop);
            }

//               TODO 进行DOUBLE CHECK
//              先去查询 key = "cache:shop:" + id;
//              看看此时是否还是过期，因为此时有可能其他线程已经刷新好了，此时不需要去刷新了
            String newDataJson = stringRedisTemplate.opsForValue().get(key);
            RedisData redisDataNew = objectMapper.readValue(newDataJson, RedisData.class);
            LocalDateTime expireTimeNew = redisDataNew.getExpireTime();
            if (LocalDateTime.now().isBefore(expireTimeNew)) {
                // 不需要刷新了，直接返回
                return Result.ok(shop);
            }
            // TODO 开启一个异步线程去更新Redis，然后直接先返回旧数据
            CompletableFuture.runAsync(() -> {
                try {
                    // 查数据库 + 刷新Redis
                    saveShop2RedisCache(id, 30L);
                } catch (Exception e) {
                    // 异步更新失败只打日志，不影响主线程返回
                    e.printStackTrace();
                } finally {
                    // 无论更新成功失败，都要释放锁
                    unlock(refreshShopKey);
                }
            });
            return Result.ok(shop);
        }
        // 没过期直接放行即可
        return Result.ok(shop);
    }

    /*
      Redis的预热逻辑
      */
    public void saveShop2RedisCache(Long id, Long expireSeconds) {
        RedisData redisData = new RedisData();
        redisData.setExpireTime(LocalDateTime.now().plusSeconds(expireSeconds));
        Shop shop = shopMapper.selectShopById(id);
        redisData.setData(shop);
        String key = "cache:shop:" + id;
        String value = objectMapper.writeValueAsString(redisData);
        stringRedisTemplate.opsForValue().set(key, value);
    }


    @Transactional(rollbackFor = Exception.class)
    public Result updateShop(Shop shop) {
        shopMapper.updateShop(shop);
        // 事务提交后执行删缓存
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                String key = "cache:shop:" + shop.getId();
                stringRedisTemplate.delete(key);
            }
        });
        return Result.ok();
    }


    // TODO 后续注意穿透，雪崩，击穿
    public Result getShopTypeList() {
        String key = "cache:shopTypeList";
        String shopTypeListJson = stringRedisTemplate.opsForValue().get(key);
        if (shopTypeListJson != null && !shopTypeListJson.isEmpty()) {
            List<ShopType> shopTypeList = objectMapper.readValue(shopTypeListJson, new TypeReference<>() {
            });
            log.info("Redis Cache Hit: getShopTypeList");
            return Result.ok(shopTypeList);
        }
        List<ShopType> shopTypes = shopMapper.selectShopTypeList();
        String writeValueAsString = objectMapper.writeValueAsString(shopTypes);
        stringRedisTemplate.opsForValue().set(key, writeValueAsString);
        return Result.ok(shopTypes);
    }


}
