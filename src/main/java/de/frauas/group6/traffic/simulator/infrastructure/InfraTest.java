package de.frauas.group6.traffic.simulator.infrastructure;

import de.frauas.group6.traffic.simulator.core.SimulationEngine;

public class InfraTest {

    public static void main(String[] args) {
        System.out.println("=== FINAL TEST: INFRA & SMART LOGIC ===");

        if (System.getenv("SUMO_HOME") == null) {
            System.err.println("ERROR: SUMO_HOME is not set!");
            return;
        }

        try {
            // 1. Start Engine
            System.out.println("Starting SUMO...");
            SimulationEngine engine = new SimulationEngine();
            engine.initialize(); 
            engine.start();

            Thread.sleep(2000);

            // 2. Setup Managers
            InfrastructureManager infraManager = new InfrastructureManager(engine);
            TrafficLightManager tlManager = new TrafficLightManager(engine);
            
            // Injection
            engine.setInfrastructureManager(infraManager);
            engine.setTrafficLightManager(tlManager);

            // 3. Load Data
            infraManager.loadNetwork();
            tlManager.updateTrafficLights();
            
            System.out.println("Data Loaded: " + infraManager.getAllEdges().size() + " Edges.");

            // TEST: Simulation of Congestion
            System.out.println("\nTEST: Congestion Simulation");

            if (!infraManager.getAllEdges().isEmpty()) {
                IEdge fakeEdge = infraManager.getAllEdges().get(0);
                
                System.out.println("   -> Simulating 50 vehicles on " + fakeEdge.getId());
                fakeEdge.setVehicleCount(50);
                
                System.out.println("   -> Triggering logic...");
                tlManager.handleCongestion(infraManager.getAllEdges());
                
                System.out.println("   -> Check logs above for 'Congestion Detected'");
            }

            System.out.println("\nTEST COMPLETED");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
} 