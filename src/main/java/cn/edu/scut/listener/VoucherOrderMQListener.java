package cn.edu.scut.listener;

import cn.edu.scut.common.config.MqConfig;
import cn.edu.scut.entity.VoucherOrder;
import cn.edu.scut.service.VoucherOrderService;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
@Slf4j
public class VoucherOrderMQListener {

    private final VoucherOrderService voucherOrderService;

    @RabbitListener(queues = MqConfig.ORDER_QUEUE)
    public void handleVoucherOrder(VoucherOrder voucherOrder) {
        try {
            voucherOrderService.createVoucherOrder(voucherOrder);
            log.info("订单处理成功，orderId: {}", voucherOrder.getId());
        } catch (DuplicateKeyException e) {
            // 重复消息，不算错误，直接 return → 容器自动 ACK
            log.info("订单重复，ACK 丢弃，orderId: {}", voucherOrder.getId());
        }
        // 其他异常不 catch，抛给 retry → 重试 3 次 → 容器自动 NACK(requeue=false) → 进死信
    }


}
