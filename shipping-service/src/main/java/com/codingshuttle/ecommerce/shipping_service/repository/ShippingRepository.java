package com.codingshuttle.ecommerce.shipping_service.repository;

import com.codingshuttle.ecommerce.shipping_service.entity.ShippingRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface ShippingRepository extends JpaRepository<ShippingRecord, Long> {
    Optional<ShippingRecord> findByOrderId(Long orderId);
}