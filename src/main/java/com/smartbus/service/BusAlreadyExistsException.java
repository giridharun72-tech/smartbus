package com.smartbus.service;

public class BusAlreadyExistsException extends RuntimeException {
    public BusAlreadyExistsException(String busNumber) {
        super("A bus with number '" + busNumber + "' already exists.");
    }
}
