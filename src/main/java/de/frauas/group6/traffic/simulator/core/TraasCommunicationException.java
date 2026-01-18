package de.frauas.group6.traffic.simulator.core;

public class TraasCommunicationException extends RuntimeException {
   
	
	private static final long serialVersionUID = 1L;
	public TraasCommunicationException(String message, Throwable cause) {
        super(message, cause);
    }
    public TraasCommunicationException(String message) {
        super(message);
    }
}
