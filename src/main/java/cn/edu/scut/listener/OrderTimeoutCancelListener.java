package cn.edu.scut.listener;

import cn.edu.scut.entity.VoucherOrder;
import cn.edu.scut.mapper.VoucherOrderMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderTimeoutCancelListener {

    private final VoucherOrderMapper voucherOrderMapper;
    private final StringRedisTemplate stringRedisTemplate;

    @RabbitListener(queues = "order.timeout.dlq")
    public void handleTimeoutOrder(VoucherOrder voucherOrder) {
        log.info("收到超时取消订单消息，orderId={}", voucherOrder.getId());

        // CAS取消订单：只有状态是1未支付才能取消
        int updated = voucherOrderMapper.cancelOrder(voucherOrder.getId(), voucherOrder.getUserId());

        if (updated == 0) {
            // 用户已经支付了，什么都不做
            log.info("订单已支付，取消取消，orderId={}", voucherOrder.getId());
            return;
        }

        // 真的取消了，回补Redis库存
        stringRedisTemplate.opsForValue().increment("seckill:stock:" + voucherOrder.getVoucherId());

        // 把用户从已下单集合删掉，下次还能秒杀
        stringRedisTemplate.opsForSet().remove("seckill:user:" + voucherOrder.getVoucherId(), String.valueOf(voucherOrder.getUserId()));

        log.info("订单超时取消成功，回补库存，orderId={}", voucherOrder.getId());
    }
}
