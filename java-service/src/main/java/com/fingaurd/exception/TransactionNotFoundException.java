package com.fingaurd.exception;

/**
 * Exception thrown when a transaction is not found
 */
public class TransactionNotFoundException extends RuntimeException {
    
    public TransactionNotFoundException(String message) {
        super(message);
    }
    
    public TransactionNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
