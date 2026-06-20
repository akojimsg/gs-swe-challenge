package com.gsswec.ecommerce.products.domain.exception;

public class ProductNotFoundException extends RuntimeException {

    public ProductNotFoundException() {
        super("Product not found");
    }
}
