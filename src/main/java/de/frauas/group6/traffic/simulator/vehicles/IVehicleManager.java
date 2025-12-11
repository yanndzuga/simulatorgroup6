package de.frauas.group6.traffic.simulator.vehicles;

import java.util.List;

public interface IVehicleManager {
	
	void injectVehicle(String edgeId, String color, int number, double speed);
	void modifyVehicle(String edgeId, String color,int number, double speed );
	void deleteVehicle(String edgeId, String color,int number, double speed);
	List<IVehicle> getAllVehicles();
	void updateVehicles();
}
