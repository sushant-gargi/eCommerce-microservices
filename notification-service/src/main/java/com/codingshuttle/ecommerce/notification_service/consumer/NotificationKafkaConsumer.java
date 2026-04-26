package com.codingshuttle.ecommerce.notification_service.consumer;

import com.codingshuttle.ecommerce.events.OrderCreatedEvent;
import com.codingshuttle.ecommerce.events.OrderStatusUpdatedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class NotificationKafkaConsumer {

    @KafkaListener(topics = "${kafka.topic.order-created}", groupId = "${spring.kafka.consumer.group-id}")
    public void handleOrderCreated(OrderCreatedEvent event) {
        log.info("[NOTIFICATION] OrderCreatedEvent received: orderId={}, address='{}', items={}",
                event.getOrderId(),
                event.getDeliveryAddress(),
                event.getItems());
    }

    @KafkaListener(topics = "${kafka.topic.order-status-updated}", groupId = "${spring.kafka.consumer.group-id}")
    public void handleOrderStatusUpdated(OrderStatusUpdatedEvent event) {
        log.info("[NOTIFICATION] OrderStatusUpdatedEvent received: orderId={}, status={}",
                event.getOrderId(),
                event.getOrderStatus());
    }
}