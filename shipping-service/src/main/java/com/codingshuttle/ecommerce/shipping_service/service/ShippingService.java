package com.codingshuttle.ecommerce.shipping_service.service;

import com.codingshuttle.ecommerce.shipping_service.dto.ShippingRequestDto;
import com.codingshuttle.ecommerce.shipping_service.dto.ShippingResponseDto;
import com.codingshuttle.ecommerce.shipping_service.entity.ShippingRecord;
import com.codingshuttle.ecommerce.shipping_service.entity.ShippingStatus;
import com.codingshuttle.ecommerce.shipping_service.repository.ShippingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class ShippingService {

    private final ShippingRepository shippingRepository;
    private final ModelMapper modelMapper;

    public ShippingResponseDto shipOrder(ShippingRequestDto shippingRequestDto) {
        log.info("Shipping order with ID: {}", shippingRequestDto.getOrderId());

        ShippingRecord record = new ShippingRecord();
        record.setOrderId(shippingRequestDto.getOrderId());
        record.setDeliveryAddress(shippingRequestDto.getDeliveryAddress());
        record.setStatus(ShippingStatus.SHIPPED);
        record.setMessage("Order shipped successfully for order id: " + shippingRequestDto.getOrderId());

        shippingRepository.save(record);

        ShippingResponseDto response = new ShippingResponseDto();
        response.setOrderId(record.getOrderId());
        response.setStatus(record.getStatus().name());
        response.setMessage(record.getMessage());

        return response;
    }

    public ShippingResponseDto getShippingByOrderId(Long orderId) {
        log.info("Fetching shipping record for order ID: {}", orderId);
        ShippingRecord record = shippingRepository.findByOrderId(orderId)
                .orElseThrow(() -> new RuntimeException("Shipping record not found for order: " + orderId));

        ShippingResponseDto response = new ShippingResponseDto();
        response.setOrderId(record.getOrderId());
        response.setStatus(record.getStatus().name());
        response.setMessage(record.getMessage());
        return response;
    }

    public List<ShippingResponseDto> getAllShipments() {
        log.info("Fetching all shipping records");
        return shippingRepository.findAll().stream()
                .map(record -> {
                    ShippingResponseDto dto = new ShippingResponseDto();
                    dto.setOrderId(record.getOrderId());
                    dto.setStatus(record.getStatus().name());
                    dto.setMessage(record.getMessage());
                    return dto;
                }).toList();
    }
}