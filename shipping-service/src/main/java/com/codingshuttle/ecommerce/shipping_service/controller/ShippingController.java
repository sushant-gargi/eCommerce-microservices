package com.codingshuttle.ecommerce.shipping_service.controller;

import com.codingshuttle.ecommerce.shipping_service.dto.ShippingRequestDto;
import com.codingshuttle.ecommerce.shipping_service.dto.ShippingResponseDto;
import com.codingshuttle.ecommerce.shipping_service.service.ShippingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
@Slf4j
public class ShippingController {

    private final ShippingService shippingService;

    @PostMapping("/ship")
    public ResponseEntity<ShippingResponseDto> shipOrder(@RequestBody ShippingRequestDto shippingRequestDto) {
        log.info("Received shipping request for order ID: {}", shippingRequestDto.getOrderId());
        ShippingResponseDto response = shippingService.shipOrder(shippingRequestDto);
        return ResponseEntity.ok(response);
    }
}