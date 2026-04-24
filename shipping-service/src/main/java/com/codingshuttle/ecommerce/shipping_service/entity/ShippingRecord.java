// entity/ShippingRecord.java
package com.codingshuttle.ecommerce.shipping_service.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
public class ShippingRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long orderId;

    private String deliveryAddress;

    @Enumerated(EnumType.STRING)
    private ShippingStatus status;

    private String message;
}