package de.frauas.group6.traffic.simulator.core;

import de.frauas.group6.traffic.simulator.analytics.IStatsCollector;
import de.frauas.group6.traffic.simulator.infrastructure.ITrafficLightManager;
import de.frauas.group6.traffic.simulator.vehicles.IVehicleManager;
import de.frauas.group6.traffic.simulator.vehicles.VehicleManager;
import de.frauas.group6.traffic.simulator.view.GuiManager;
import de.frauas.group6.traffic.simulator.view.IMapObserver;

// NOTE: Ideally, import the concrete implementations from other packages
// import vehicles.VehicleManager;
// import infrastructure.TrafficLightManager;
// import view.MapCanvas;

/**
 * ENTRY POINT
 * This class is responsible for bootstrapping the application.
 */
public class App {

    public static void main(String[] args) {
        System.out.println(">>> Initializing Traffic Simulation System...");

        // 1. Create the Core Engine
        SimulationEngine engine = new SimulationEngine();

        // 2. Create the Components (Members 2, 3, 4, 5)
        
        IStatsCollector statsCollector = null; // statsCollector = new StatsCollector(engine);
        IVehicleManager vehicleMgr = null; // 
        vehicleMgr = new VehicleManager(engine);
        ITrafficLightManager lightMgr = null; // lightMgr = new TrafficLightManager(engine);
        ; // mapView = new MapCanvas(engine, statsCollector);
        
        GuiManager.startUI( engine,  vehicleMgr);

        // 3. Dependency Injection (Wiring everything together)
        // This is why we need Setters in SimulationEngine!
        engine.setVehicleManager(vehicleMgr);
        engine.setTrafficLightManager(lightMgr);
        

        // 4. Initialize SUMO Connection
        try {
            engine.initialize();
            
            // 5. Start the Loop
            engine.start();
            
            System.out.println(">>> System running. Close the GUI window to stop.");
            
        } catch (RuntimeException e) {
            System.err.println("CRITICAL FAILURE: " + e.getMessage());
            e.printStackTrace();
        }
    }
}


