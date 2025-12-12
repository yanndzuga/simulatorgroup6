package de.frauas.group6.traffic.simulator.vehicles;


import java.util.Collection;
import java.util.List;



public interface IVehicleManager {
	
	void injectVehicle(String edgeId,String lane,String VehicleType, String color,int number, double speed);
    void modifyVehicle(String vehicleId, String newcolor, double newspeed ) throws Exception;	
    void deleteVehicle(String requestedEdgeId, String  requestedColor, double requestedSpeed,int requestnumber);
    Collection<IVehicle> getAllVehicles();
    void updateVehicles();

}
