package com.codingshuttle.ecommerce.order_service.repoitory;

import com.codingshuttle.ecommerce.order_service.entity.Orders;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrdersRepository extends JpaRepository<Orders, Long> {
}
