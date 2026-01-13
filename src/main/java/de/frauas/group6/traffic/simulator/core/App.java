package de.frauas.group6.traffic.simulator.core;

import de.frauas.group6.traffic.simulator.infrastructure.ITrafficLightManager;
import de.frauas.group6.traffic.simulator.infrastructure.TrafficLightManager;
import de.frauas.group6.traffic.simulator.infrastructure.IInfrastructureManager;
import de.frauas.group6.traffic.simulator.infrastructure.InfrastructureManager;
import de.frauas.group6.traffic.simulator.vehicles.IVehicleManager;
import de.frauas.group6.traffic.simulator.vehicles.VehicleManager;
import de.frauas.group6.traffic.simulator.view.GuiManager;

import java.util.logging.Level;
import java.util.logging.Logger;

public class App {

    private static final Logger LOGGER = Logger.getLogger(App.class.getName());

    public static void main(String[] args) {
        LOGGER.info(">>> Initializing Traffic Simulation System...");

        try {
            // 1. Create Engine
            SimulationEngine engine = new SimulationEngine();

            // 2. Create Managers
            IVehicleManager vehicleMgr = new VehicleManager(engine);
            IInfrastructureManager infraMgr = new InfrastructureManager(engine);
            
            // Pass dependencies
            ITrafficLightManager lightMgr = new TrafficLightManager(engine, infraMgr);

            // 3. Wiring
            engine.setVehicleManager(vehicleMgr);
            engine.setTrafficLightManager(lightMgr);
            engine.setInfrastructureManager(infraMgr);

            // 4. Initialize
            engine.initialize();

            // 5. Load Network
            infraMgr.loadNetwork(); 

            // 6. Start UI
            GuiManager.startUI(engine, vehicleMgr, lightMgr);

            // 7. Start Loop
            engine.start();
            
            LOGGER.info(">>> System running.");

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "CRITICAL FAILURE", e);
            System.exit(1);
        }
    }
}