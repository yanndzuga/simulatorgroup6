package de.frauas.group6.traffic.simulator.view;

import de.frauas.group6.traffic.simulator.core.ISimulationEngine;
import de.frauas.group6.traffic.simulator.core.SimulationEngine; 
import de.frauas.group6.traffic.simulator.vehicles.IVehicleManager;
import de.frauas.group6.traffic.simulator.infrastructure.ITrafficLightManager;
import java.util.List;
import java.util.Collections;

public class SimulationController {
    
    // --- Attributes ---
    private ISimulationEngine engine;
    private ITrafficLightManager trafficLightManager;
    private IVehicleManager vehicleManager;
    
    // --- Constructor ---
    public SimulationController(ISimulationEngine engine, IVehicleManager vehicleManager, ITrafficLightManager trafficLightManager) { 
        this.engine = engine; 
        this.vehicleManager = vehicleManager; 
        this.trafficLightManager = trafficLightManager; 
    }
    
    // --- Simulation Control (Start/Pause/Step) ---
    public void startSimulation() { 
        if (engine != null) { engine.start(); engine.resume(); } 
    }
    
    public void pauseSimulation() { 
        if (engine != null) { engine.pause(); } 
    }
    
    public void singleStep() { 
        if (engine != null) { 
            engine.step(); 
            // Optional: Logic to handle auto-congestion if implemented in Engine
            engine.checkAndHandleCongestion();
        } 
    }
    
    // --- Vehicle Management Methods ---
    public void spawnCar(String edgeId, String lane, String type, String color, int count, double speed) {
        if (vehicleManager != null) vehicleManager.injectVehicle(edgeId, lane, type, color, count, speed);  
    }

    public void removeCar(String edgeId, String color, double speed, int count) {
        if (vehicleManager != null) vehicleManager.deleteVehicle(edgeId, color, speed, count);  
    }
    
    public void modifyVehicle(String vehicleId, String newColor, double newSpeed) throws Exception {
        if(vehicleManager != null) vehicleManager.modifyVehicle(vehicleId, newColor, newSpeed);
    }
    
    // --- Traffic Light Control Methods ---
    
    /** Gets the list of Traffic Light IDs from the Engine */
    public List<String> getTrafficLightIds() {
        if (engine != null) return engine.getTrafficLightIdList();
        return Collections.emptyList();
    }

    /** Forces a Green Wave on the selected traffic light */
    public void forceGreen(String tlId) {
        if (engine != null) engine.forceGreenWave(tlId);
    }

    /** Forces a Red Stop on the selected traffic light */
    public void forceRed(String tlId) {
        if (engine != null) engine.forceRedStop(tlId);
    }

    /** Gets Debug Info (Color State & Duration) */
    public String getTrafficLightInfo(String tlId) {
        // Casting to access the specific debug method
        if (engine != null && engine instanceof SimulationEngine) {
            return ((SimulationEngine) engine).getTrafficLightDebugInfo(tlId);
        }
        return "N/A";
    }
}