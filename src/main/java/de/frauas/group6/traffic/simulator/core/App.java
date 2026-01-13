package de.frauas.group6.traffic.simulator.core;

import de.frauas.group6.traffic.simulator.analytics.IStatsCollector;
import de.frauas.group6.traffic.simulator.analytics.StatsCollector;
import de.frauas.group6.traffic.simulator.infrastructure.IInfrastructureManager;
import de.frauas.group6.traffic.simulator.infrastructure.ITrafficLightManager;
import de.frauas.group6.traffic.simulator.infrastructure.InfrastructureManager;
import de.frauas.group6.traffic.simulator.infrastructure.TrafficLightManager;
import de.frauas.group6.traffic.simulator.vehicles.IVehicleManager;
import de.frauas.group6.traffic.simulator.vehicles.VehicleManager;
import de.frauas.group6.traffic.simulator.view.GuiManager;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * ENTRY POINT
 * This class orchestrates the application startup:
 * 1. Initializes the Core Engine
 * 2. Creates the Managers (Components)
 * 3. Wires dependencies (Dependency Injection)
 * 4. Launches the GUI
 */
public class App {

    private static final Logger LOGGER = Logger.getLogger(App.class.getName());

    public static void main(String[] args) {
        LOGGER.info(">>> Initializing Traffic Simulation System...");

        try {
            // ------------------------------------------------------------
            // 1. Create the Core Engine
            // ------------------------------------------------------------
            SimulationEngine engine = new SimulationEngine();

            // ------------------------------------------------------------
            // 2. Create the Component Managers
            // ------------------------------------------------------------
            // Member 2: Vehicles
            IVehicleManager vehicleMgr = new VehicleManager(engine);
            
            // Member 3: Infrastructure (Traffic Lights) - Placeholder if not ready
            ITrafficLightManager lightMgr = new TrafficLightManager(engine);
            IInfrastructureManager infraMgr = new InfrastructureManager(engine);
         // Member 5:
            IStatsCollector statsCollector = new StatsCollector(vehicleMgr,infraMgr,engine);
            
            
            

            // ------------------------------------------------------------
            // 3. Dependency Injection (Wiring)
            // ------------------------------------------------------------
            engine.setVehicleManager(vehicleMgr);
            engine.setTrafficLightManager(lightMgr);
            engine.setStatCollector(statsCollector);
            engine.setInfrastructureManager(infraMgr);
         
         
            // engine.setStatsCollector(statsCollector);

            // ------------------------------------------------------------
            // 4. Initialize SUMO Connection
            // ------------------------------------------------------------
            // This is a blocking call that ensures TraCI is connected before GUI starts
            engine.initialize();

            // ------------------------------------------------------------
            // 5. Start User Interface (View - Member 4)
            // ------------------------------------------------------------
            // Passes the engine and managers to the GUI so controls can work immediately
            GuiManager.startUI(engine, vehicleMgr,lightMgr,statsCollector,infraMgr);

            // ------------------------------------------------------------
            // 6. Start the Simulation Loop
            // ------------------------------------------------------------
            engine.start();
            
            LOGGER.info(">>> System running. Close the GUI window to exit.");

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "CRITICAL FAILURE: Could not start application.", e);
            System.exit(1); // Non-zero exit code indicates failure
        }
    }
}