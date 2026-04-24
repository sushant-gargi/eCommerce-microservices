package com.codingshuttle.ecommerce.shipping_service.controller;

import com.codingshuttle.ecommerce.shipping_service.dto.ShippingRequestDto;
import com.codingshuttle.ecommerce.shipping_service.dto.ShippingResponseDto;
import com.codingshuttle.ecommerce.shipping_service.service.ShippingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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

    @GetMapping
    public ResponseEntity<List<ShippingResponseDto>> getAllShipments() {
        log.info("Fetching all shipments");
        return ResponseEntity.ok(shippingService.getAllShipments());
    }

    @GetMapping("/order/{orderId}")
    public ResponseEntity<ShippingResponseDto> getShippingByOrderId(@PathVariable Long orderId) {
        log.info("Fetching shipping for order ID: {}", orderId);
        return ResponseEntity.ok(shippingService.getShippingByOrderId(orderId));
    }
}