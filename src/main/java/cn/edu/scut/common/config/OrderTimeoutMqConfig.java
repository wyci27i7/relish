package cn.edu.scut.common.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.ExchangeBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
public class OrderTimeoutMqConfig {

    // ===== 超时取消延时交换机 & 队列常量 =====
    public static final String ORDER_TIMEOUT_EXCHANGE = "order.timeout.exchange";
    public static final String ORDER_TIMEOUT_QUEUE = "order.timeout.queue";
    public static final String ORDER_TIMEOUT_ROUTING_KEY = "order.timeout";

    // ===== 超时取消死信交换机 & 队列常量 =====
    public static final String ORDER_TIMEOUT_DLX = "order.timeout.dlx";
    public static final String ORDER_TIMEOUT_DLQ = "order.timeout.dlq";
    public static final String ORDER_TIMEOUT_DLQ_ROUTING_KEY = "order.timeout.dlq";

    // ===== 延时交换机 =====
    @Bean
    public TopicExchange orderTimeoutExchange() {
        return ExchangeBuilder.topicExchange(ORDER_TIMEOUT_EXCHANGE).durable(true).build();
    }

    // ===== 延时队列（30分钟TTL，死信转发到死信交换机）=====
    @Bean
    public Queue orderTimeoutQueue() {
        return QueueBuilder.durable(ORDER_TIMEOUT_QUEUE)
                .withArgument("x-message-ttl", 180000)
                .withArgument("x-dead-letter-exchange", ORDER_TIMEOUT_DLX)
                .withArgument("x-dead-letter-routing-key", ORDER_TIMEOUT_DLQ_ROUTING_KEY)
                .build();
    }

    // ===== 延时队列绑定到延时交换机 =====
    @Bean
    public Binding orderTimeoutQueueBinding(Queue orderTimeoutQueue, TopicExchange orderTimeoutExchange) {
        return BindingBuilder.bind(orderTimeoutQueue).to(orderTimeoutExchange).with(ORDER_TIMEOUT_ROUTING_KEY);
    }

    // ===== 死信交换机 =====
    @Bean
    public TopicExchange orderTimeoutDlx() {
        return ExchangeBuilder.topicExchange(ORDER_TIMEOUT_DLX).durable(true).build();
    }

    // ===== 死信队列 =====
    @Bean
    public Queue orderTimeoutDlq() {
        return QueueBuilder.durable(ORDER_TIMEOUT_DLQ).build();
    }

    // ===== 死信队列绑定到死信交换机 =====
    @Bean
    public Binding orderTimeoutDlqBinding(Queue orderTimeoutDlq, TopicExchange orderTimeoutDlx) {
        return BindingBuilder.bind(orderTimeoutDlq).to(orderTimeoutDlx).with(ORDER_TIMEOUT_DLQ_ROUTING_KEY);
    }
}
