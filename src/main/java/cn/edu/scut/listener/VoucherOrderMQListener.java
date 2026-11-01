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
    public void handleVoucherOrder(VoucherOrder voucherOrder, Channel channel, Message message) throws IOException {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        try {
            // 幂等检查 + 扣库存 + 插订单（事务方法）
            voucherOrderService.createVoucherOrder(voucherOrder);
            // 事务提交成功，手动 ACK
            channel.basicAck(deliveryTag, false);
            log.info("订单处理成功，orderId: {}", voucherOrder.getId());
        } catch (DuplicateKeyException e) {
            // 唯一索引冲突 = 重复消息，不算错误，直接 ACK 丢弃
            log.info("订单重复，ACK 丢弃，orderId: {}", voucherOrder.getId());
            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            // 其他异常：不重回队列，直接进死信队列，后面对账任务处理
            log.error("订单处理失败，进入死信队列，orderId: {}", voucherOrder.getId(), e);
            channel.basicNack(deliveryTag, false, false);
        }
    }
}
