package com.codingshuttle.ecommerce.shipping_service.service;

import com.codingshuttle.ecommerce.shipping_service.dto.ShippingRequestDto;
import com.codingshuttle.ecommerce.shipping_service.dto.ShippingResponseDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class ShippingService {

    public ShippingResponseDto shipOrder(ShippingRequestDto shippingRequestDto) {
        log.info("Shipping order with ID: {}", shippingRequestDto.getOrderId());

        ShippingResponseDto response = new ShippingResponseDto();
        response.setOrderId(shippingRequestDto.getOrderId());
        response.setStatus("SHIPPED");
        response.setMessage("Order shipped successfully for order id: " + shippingRequestDto.getOrderId());

        return response;
    }
}