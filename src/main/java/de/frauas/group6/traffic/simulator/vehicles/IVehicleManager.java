package de.frauas.group6.traffic.simulator.vehicles;

import java.util.List;

public interface IVehicleManager {
	
	void injectVehicle(String edgeId, String color, int number, double speed);
	void modifyVehicle(double speed, String color );
	void deleteVehicle();
	List<IVehicle> getAllVehicles();
	void updateVehicles();
}
