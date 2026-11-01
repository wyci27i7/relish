package cn.edu.scut;

import cn.edu.scut.entity.Shop;
import cn.edu.scut.service.ShopService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;

@SpringBootTest
@Slf4j
class RelishApplicationTests {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private ShopService shopService;

    @Test
    void contextLoads() {
        String test = stringRedisTemplate.opsForValue().get("test");
        log.info(test);
    }

    @Test
    void testShopRedis() {
        shopService.saveShop2RedisCache(2L, 60L);
    }

}
