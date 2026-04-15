package com.codingshuttle.ecommerce.order_service.dto;

import lombok.Data;

@Data
public class ShippingRequestDto {
    private Long orderId;
    private String deliveryAddress;
}