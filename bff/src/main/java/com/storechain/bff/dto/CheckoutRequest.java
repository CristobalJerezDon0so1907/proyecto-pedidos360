package com.storechain.bff.dto;

import java.util.List;

public class CheckoutRequest {
    private String customerId;
    private String shippingAddress;
    private List<OrderItemDto> items;

    public CheckoutRequest() {}

    public CheckoutRequest(String customerId, String shippingAddress, List<OrderItemDto> items) {
        this.customerId = customerId;
        this.shippingAddress = shippingAddress;
        this.items = items;
    }

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public String getShippingAddress() {
        return shippingAddress;
    }

    public void setShippingAddress(String shippingAddress) {
        this.shippingAddress = shippingAddress;
    }

    public List<OrderItemDto> getItems() {
        return items;
    }

    public void setItems(List<OrderItemDto> items) {
        this.items = items;
    }

    public static class OrderItemDto {
        private String productId;
        private Integer quantity;
        private Double price;

        public OrderItemDto() {}

        public String getProductId() {
            return productId;
        }

        public void setProductId(String productId) {
            this.productId = productId;
        }

        public Integer getQuantity() {
            return quantity;
        }

        public void setQuantity(Integer quantity) {
            this.quantity = quantity;
        }

        public Double getPrice() {
            return price;
        }

        public void setPrice(Double price) {
            this.price = price;
        }
    }
}