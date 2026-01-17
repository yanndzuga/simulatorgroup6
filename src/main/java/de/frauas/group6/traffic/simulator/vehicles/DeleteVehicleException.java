package de.frauas.group6.traffic.simulator.vehicles;

public class DeleteVehicleException extends RuntimeException {

	
	public DeleteVehicleException(String message) {
		
		super(message);
	}
	
    public DeleteVehicleException(String message, Throwable e) {
    	
    	super(message,e);
    }
}
