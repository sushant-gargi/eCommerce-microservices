package com.codingshuttle.ecommerce.order_service.clients;

import com.codingshuttle.ecommerce.order_service.dto.ShippingRequestDto;
import com.codingshuttle.ecommerce.order_service.dto.ShippingResponseDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "shipping-service", path = "/shipping")
public interface ShippingOpenFeignClient {

    @PostMapping("/orders/ship")
    ShippingResponseDto shipOrder(@RequestBody ShippingRequestDto shippingRequestDto);
}