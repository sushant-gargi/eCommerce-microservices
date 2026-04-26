package com.codingshuttle.ecommerce.order_service.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class KafkaTopicConfig {

    @Value("${kafka.topic.order-created}")
    private String orderCreatedTopic;

    @Value("${kafka.topic.order-status-updated}")
    private String orderStatusUpdatedTopic;

    @Bean
    public NewTopic orderCreatedTopic() {
        return new NewTopic(orderCreatedTopic, 3, (short) 1);
    }

    @Bean
    public NewTopic orderStatusUpdatedTopic() {
        return new NewTopic(orderStatusUpdatedTopic, 3, (short) 1);
    }
}