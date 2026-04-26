package com.codingshuttle.ecommerce.inventory_service.consumer;

import com.codingshuttle.ecommerce.events.OrderCreatedEvent;
import com.codingshuttle.ecommerce.events.OrderStatusUpdatedEvent;
import com.codingshuttle.ecommerce.inventory_service.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class OrderCreatedKafkaConsumer {

    private final ProductService productService;
    private final KafkaTemplate<Long, OrderStatusUpdatedEvent> kafkaTemplate;

    @Value("${kafka.topic.order-status-updated}")
    private String orderStatusUpdatedTopic;

    @KafkaListener(topics = "${kafka.topic.order-created}", groupId = "${spring.kafka.consumer.group-id}")
    public void handleOrderCreated(OrderCreatedEvent event) {
        log.info("Received OrderCreatedEvent for orderId: {}", event.getOrderId());
        String status;

        try {
            productService.reduceStocksFromEvent(event);
            status = "FULFILLED";
            log.info("Stocks reduced successfully for orderId: {}", event.getOrderId());
        } catch (Exception e) {
            status = "OUT_OF_STOCK";
            log.error("Stock reduction failed for orderId: {}. Reason: {}", event.getOrderId(), e.getMessage());
        }

        OrderStatusUpdatedEvent statusEvent = OrderStatusUpdatedEvent.newBuilder()
                .setOrderId(event.getOrderId())
                .setOrderStatus(status)
                .build();

        kafkaTemplate.send(orderStatusUpdatedTopic, event.getOrderId(), statusEvent);
        log.info("Published OrderStatusUpdatedEvent: orderId={}, status={}", event.getOrderId(), status);
    }
}