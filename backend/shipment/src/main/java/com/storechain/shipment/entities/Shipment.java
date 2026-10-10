package com.storechain.shipment.entities;

import jakarta.persistence.*;

@Entity
@Table(name = "shipments")
public class Shipment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long orderId;
    private String status;
    private String trackingCode;

    public Shipment() {}

    public Shipment(Long orderId, String status, String trackingCode) {
        this.orderId = orderId;
        this.status = status;
        this.trackingCode = trackingCode;
    }

    public Long getId() { return id; }
    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getTrackingCode() { return trackingCode; }
    public void setTrackingCode(String trackingCode) { this.trackingCode = trackingCode; }
}
