package cn.edu.scut.service;

import cn.edu.scut.common.config.MqConfig;
import cn.edu.scut.common.result.Result;
import cn.edu.scut.common.utils.RedisIdWorker;
import cn.edu.scut.common.utils.UserHolder;
import cn.edu.scut.entity.VoucherOrder;
import cn.edu.scut.mapper.SeckillVoucherMapper;
import cn.edu.scut.mapper.VoucherOrderMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;

@Service
@RequiredArgsConstructor
@Slf4j
public class VoucherOrderService {

    private final VoucherOrderMapper voucherOrderMapper;
    private final SeckillVoucherMapper seckillVoucherMapper;
    private final RedisIdWorker redisIdWorker;
    private final StringRedisTemplate stringRedisTemplate;
    private final RabbitTemplate rabbitTemplate;

    private static final DefaultRedisScript<Long> SECKILL_SCRIPT;

    static {
        SECKILL_SCRIPT = new DefaultRedisScript<>();
        SECKILL_SCRIPT.setResultType(Long.class);
        SECKILL_SCRIPT.setLocation(new ClassPathResource("seckill.lua"));
    }

    public Result seckillVoucher(Long voucherId) {
        Long userId = UserHolder.getUserId();

        Long result = stringRedisTemplate.execute(
                SECKILL_SCRIPT,
                Collections.emptyList(),
                String.valueOf(voucherId),
                String.valueOf(userId)
        );

        int r = result.intValue();
        if (r != 0) {
            return Result.fail(r == 1 ? "库存不足" : "不能重复下单");
        }
        long orderId = redisIdWorker.nextId("voucherOrder");

        // Lua 成功：扣了 Redis 库存、sadd 了用户，现在发 MQ 消息异步落库
        VoucherOrder voucherOrder = new VoucherOrder();
        voucherOrder.setId(orderId);
        voucherOrder.setUserId(userId);
        voucherOrder.setVoucherId(voucherId);
        rabbitTemplate.convertAndSend(MqConfig.ORDER_EXCHANGE, MqConfig.ORDER_SECKILL_ROUTING_KEY, voucherOrder);

        return Result.ok(orderId);
    }

    /**
     * 消费者调用：幂等检查 + 扣库存 + 插订单（事务）
     */
    @Transactional(rollbackFor = Exception.class)
    public void createVoucherOrder(VoucherOrder voucherOrder) {
        // 幂等：订单已存在则直接返回（重复消息不重复处理）
        VoucherOrder existing = voucherOrderMapper.selectById(voucherOrder.getId());
        if (existing != null) {
            log.info("订单已存在，跳过重复处理，orderId: {}", voucherOrder.getId());
            return;
        }
        // CAS 扣库存
        seckillVoucherMapper.decreaseStock(voucherOrder.getVoucherId());
        // 插订单（靠 uk_user_voucher 唯一索引兜底防重复）
        voucherOrderMapper.insertVoucherOrder(voucherOrder);
    }
}
