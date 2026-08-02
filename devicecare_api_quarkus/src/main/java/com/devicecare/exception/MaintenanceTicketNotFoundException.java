package com.devicecare.exception;

public class MaintenanceTicketNotFoundException extends RuntimeException {
    public MaintenanceTicketNotFoundException(String message) {
        super(message);
    }
}
