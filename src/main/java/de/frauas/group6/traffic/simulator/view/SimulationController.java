package de.frauas.group6.traffic.simulator.view;
import de.frauas.group6.traffic.simulator.core.ISimulationEngine;
import de.frauas.group6.traffic.simulator.vehicles.IVehicleManager;
import de.frauas.group6.traffic.simulator.infrastructure.ITrafficLightManager;

public class SimulationController {
    //Atributte
	private ISimulationEngine engine;
	private ITrafficLightManager traffclightmanager;
	private IVehicleManager vehcilemanager;
	
	//Konsturktor
	public SimulationController(ISimulationEngine engine, IVehicleManager vehicleManager, ITrafficLightManager trafficLightManager) { this.engine = engine; this.vehcilemanager = vehicleManager; this.traffclightmanager = trafficLightManager; }
	
	
	//Methoden
	
	 public void startSimulation() { if (engine != null) { engine.start(); engine.resume(); } } //Simulation starten
	 
	 public void pauseSimulation() { if (engine != null) { engine.pause(); } }// simulation pausieren
	 
	 public void singleStep() { if (engine != null) { engine.step(); } }//mach ein timestep
	 
	 public void spawnCar(String edgeId, String lane, String type, String color, int count, double speed) {
		 
	 if (vehcilemanager != null) { vehcilemanager.injectVehicle(edgeId, lane, type, color, count, speed); /*ein oder mehrer AUtos erstellen*/ }
		 
		 
		
	 }
	 

	 public void removeCar(String edgeId, String color, double speed, int count) {
		if (vehcilemanager != null) { vehcilemanager.deleteVehicle(edgeId, color, speed, count); /*ein oder mehrer AUtos auf einem bestimmetn Edge löschen*/ }
	     
	 }
	 
	 
	public void  modifyVehicle( String vehicleId, String newcolor, double newspeed)throws Exception {
		
		if(vehcilemanager!=null) {vehcilemanager.modifyVehicle(vehicleId,newcolor,newspeed) ;}
		 
		 
		 
	 }
	 
	 public void switchTrafficLight(String tlId, int phaseIndex, int duration) {
		 if (traffclightmanager != null) { traffclightmanager.switchToPhase(tlId, phaseIndex, duration); }
	 }
	
	
}
