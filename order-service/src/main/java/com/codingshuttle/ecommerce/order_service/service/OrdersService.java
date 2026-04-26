package com.codingshuttle.ecommerce.order_service.service;

import com.codingshuttle.ecommerce.events.OrderCreatedEvent;
import com.codingshuttle.ecommerce.events.OrderItemEvent;
import com.codingshuttle.ecommerce.events.OrderStatusUpdatedEvent;
import com.codingshuttle.ecommerce.order_service.dto.OrderRequestDto;
import com.codingshuttle.ecommerce.order_service.entity.OrderItem;
import com.codingshuttle.ecommerce.order_service.entity.OrderStatus;
import com.codingshuttle.ecommerce.order_service.entity.Orders;
import com.codingshuttle.ecommerce.order_service.repoitory.OrdersRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrdersService {

    private final OrdersRepository orderRepository;
    private final ModelMapper modelMapper;
    private final KafkaTemplate<Long, OrderCreatedEvent> kafkaTemplate;

    @Value("${kafka.topic.order-created}")
    private String orderCreatedTopic;

    public List<OrderRequestDto> getAllOrders() {
        log.info("Fetching all orders");
        return orderRepository.findAll().stream()
                .map(order -> modelMapper.map(order, OrderRequestDto.class))
                .toList();
    }

    public OrderRequestDto getOrderById(Long id) {
        log.info("Fetching order with ID: {}", id);
        Orders order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Order not found"));
        return modelMapper.map(order, OrderRequestDto.class);
    }

    public OrderRequestDto createOrder(OrderRequestDto orderRequestDto) {
        log.info("Creating order and publishing OrderCreatedEvent");

        Orders orders = modelMapper.map(orderRequestDto, Orders.class);
        for (OrderItem item : orders.getItems()) {
            item.setOrder(orders);
        }
        orders.setOrderStatus(OrderStatus.PENDING);
        Orders savedOrder = orderRepository.save(orders);

        // Build Avro event
        List<OrderItemEvent> itemEvents = savedOrder.getItems().stream()
                .map(item -> OrderItemEvent.newBuilder()
                        .setProductId(item.getProductId())
                        .setQuantity(item.getQuantity())
                        .build())
                .toList();

        OrderCreatedEvent event = OrderCreatedEvent.newBuilder()
                .setOrderId(savedOrder.getId())
                .setDeliveryAddress(orderRequestDto.getDeliveryAddress())
                .setItems(itemEvents)
                .build();

        kafkaTemplate.send(orderCreatedTopic, savedOrder.getId(), event);
        log.info("Published OrderCreatedEvent for orderId: {}", savedOrder.getId());

        return modelMapper.map(savedOrder, OrderRequestDto.class);
    }

    @KafkaListener(topics = "${kafka.topic.order-status-updated}", groupId = "${spring.kafka.consumer.group-id}")
    public void handleOrderStatusUpdated(OrderStatusUpdatedEvent event) {
        log.info("Received OrderStatusUpdatedEvent: orderId={}, status={}", event.getOrderId(), event.getOrderStatus());

        Orders order = orderRepository.findById(event.getOrderId())
                .orElseThrow(() -> new RuntimeException("Order not found: " + event.getOrderId()));

        order.setOrderStatus(OrderStatus.valueOf(event.getOrderStatus().toString()));
        orderRepository.save(order);
        log.info("Updated order {} status to {}", event.getOrderId(), event.getOrderStatus());
    }

    public OrderRequestDto cancelOrder(Long orderId) {
        log.info("Cancelling order with ID: {}", orderId);
        Orders order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found with id: " + orderId));

        if (order.getOrderStatus() == OrderStatus.CANCELLED) {
            throw new RuntimeException("Order is already cancelled");
        }
        order.setOrderStatus(OrderStatus.CANCELLED);
        orderRepository.save(order);
        return modelMapper.map(order, OrderRequestDto.class);
    }
}