package de.frauas.group6.traffic.simulator.core;

public class PathException extends RuntimeException {
	
	PathException(String message){
		super(message);
	}
	
	public PathException(String message, Throwable cause) {
        super(message, cause);
    }


}
