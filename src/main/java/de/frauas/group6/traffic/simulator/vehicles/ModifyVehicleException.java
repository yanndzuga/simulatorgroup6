package de.frauas.group6.traffic.simulator.vehicles;

public class ModifyVehicleException extends RuntimeException{

	// Basic constructor
    public ModifyVehicleException(String message) {
        super(message);
    }

    // Constructor with the original cause (e.g., SUMO connection error)
    public ModifyVehicleException(String message, Throwable cause) {
        super(message, cause);
	
	
}
}
