package com.shopsphere.exception;

/**
 * Thrown when a requested quantity of a product exceeds available stock.
 * Mapped to HTTP 409 (Conflict) since it reflects a state conflict, not a
 * malformed request.
 */
public class OutOfStockException extends RuntimeException {

    public OutOfStockException(String message) {
        super(message);
    }

    public OutOfStockException(String productName, int requestedQuantity, int availableQuantity) {
        super(String.format("Insufficient stock for '%s': requested %d, only %d available",
                productName, requestedQuantity, availableQuantity));
    }
}
