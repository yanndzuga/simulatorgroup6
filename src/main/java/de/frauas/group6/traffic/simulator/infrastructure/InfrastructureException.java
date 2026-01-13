package de.frauas.group6.traffic.simulator.infrastructure;

public class InfrastructureException extends RuntimeException {

    // Constructor 1: Message bo7dha
    public InfrastructureException(String message) {
        super(message);
    }

    // Constructor 2: Message + Cause (l-error l-asliya)
    public InfrastructureException(String message, Throwable cause) {
        super(message, cause);
    }
}