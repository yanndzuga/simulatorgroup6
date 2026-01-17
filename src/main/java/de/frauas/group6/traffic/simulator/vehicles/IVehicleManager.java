package de.frauas.group6.traffic.simulator.vehicles;



import java.util.Collection;




public interface IVehicleManager {
	
	void injectVehicle(String RouteId, String VehicleType, String color, int number, double speed,String Currentcolor,double Currentspeed);
    void modifyVehicle(String vehicleId, String newcolor, double newspeed ) throws Exception;	
    void deleteVehicle(String requestedEdgeId, String  requestedColor, double requestedSpeed,int requestnumber);   
    void SelectVehicle(String Currentcolor, double Currentspeed);
    Collection<IVehicle> getAllVehicles();
    void updateVehicles();

}
