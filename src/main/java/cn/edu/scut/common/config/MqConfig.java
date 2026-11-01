package cn.edu.scut.common.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.ExchangeBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MqConfig {

    // ===== 业务交换机 & 队列常量 =====
    public static final String ORDER_EXCHANGE = "order.exchange";
    public static final String ORDER_QUEUE = "order.queue";
    public static final String ORDER_SECKILL_ROUTING_KEY = "order.seckill";

    // ===== 死信交换机 & 队列常量 =====
    public static final String ORDER_DLX = "order.dlx";
    public static final String ORDER_DLQ = "order.dlq";
    public static final String ORDER_DLQ_ROUTING_KEY = "order.dlq";

    // ===== 业务交换机 =====
    @Bean
    public TopicExchange orderExchange() {
        return ExchangeBuilder.topicExchange(ORDER_EXCHANGE).durable(true).build();
    }

    // ===== 业务队列（带死信参数）=====
    @Bean
    public Queue orderQueue() {
        return QueueBuilder.durable(ORDER_QUEUE)
                .withArgument("x-dead-letter-exchange", ORDER_DLX)
                .withArgument("x-dead-letter-routing-key", ORDER_DLQ_ROUTING_KEY)
                .build();
    }

    // ===== 业务队列绑定到业务交换机 =====
    @Bean
    public Binding orderQueueBinding(Queue orderQueue, TopicExchange orderExchange) {
        return BindingBuilder.bind(orderQueue).to(orderExchange).with(ORDER_SECKILL_ROUTING_KEY);
    }

    // ===== 死信交换机 =====
    @Bean
    public TopicExchange orderDlx() {
        return ExchangeBuilder.topicExchange(ORDER_DLX).durable(true).build();
    }

    // ===== 死信队列 =====
    @Bean
    public Queue orderDlq() {
        return QueueBuilder.durable(ORDER_DLQ).build();
    }

    // ===== 死信队列绑定到死信交换机 =====
    @Bean
    public Binding orderDlqBinding(Queue orderDlq, TopicExchange orderDlx) {
        return BindingBuilder.bind(orderDlq).to(orderDlx).with(ORDER_DLQ_ROUTING_KEY);
    }

    // ===== Jackson JSON 消息转换器（替换默认的 SimpleMessageConverter）=====
    @Bean
    public MessageConverter jacksonJsonMessageConverter() {
        return new JacksonJsonMessageConverter();
    }
}
