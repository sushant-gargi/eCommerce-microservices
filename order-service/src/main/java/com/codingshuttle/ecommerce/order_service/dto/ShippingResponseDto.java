package com.codingshuttle.ecommerce.order_service.dto;

import lombok.Data;

@Data
public class ShippingResponseDto {
    private Long orderId;
    private String status;
    private String message;
}