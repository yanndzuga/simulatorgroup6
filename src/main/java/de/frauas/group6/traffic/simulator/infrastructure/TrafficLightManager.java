package de.frauas.group6.traffic.simulator.infrastructure;

import de.frauas.group6.traffic.simulator.core.ISimulationEngine;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TrafficLightManager implements ITrafficLightManager {

    private ISimulationEngine simulationEngine;
    private final Map<String, ITrafficLight> trafficLights = new HashMap<>();
    private boolean initialized = false;

    public TrafficLightManager(ISimulationEngine simulationEngine) {
        this.simulationEngine = simulationEngine;
    }

    @Override
    public void updateTrafficLights() {
        if (!initialized) {
            initializeMap();
            initialized = true;
        }

        for (ITrafficLight tl : trafficLights.values()) {
            try {
                int phase = simulationEngine.getTrafficLightPhase(tl.getId());
                String state = simulationEngine.getTrafficLightState(tl.getId());
                long time = simulationEngine.getTrafficLightRemainingTime(tl.getId());

                tl.setCurrentPhase(phase);
                tl.setCurrentState(state);
                tl.setRemainingTime(time);
            } catch (Exception e) {
                System.err.println("Error updating TrafficLight " + tl.getId());
            }
        }
    }

    private void initializeMap() {
        List<String> ids = simulationEngine.getTrafficLightIdList();
        if (ids == null) return;

        for (String id : ids) {
            TrafficLight tl = new TrafficLight(id, simulationEngine.getTrafficLightPosition(id));
            tl.setControlledLanes(simulationEngine.getControlledLanes(id));
            trafficLights.put(id, tl);
        }
    }

    @Override
    public List<ITrafficLight> getAllTrafficLights() {
        return new ArrayList<>(trafficLights.values());
    }

    @Override
    public ITrafficLight getTrafficLightById(String id) {
        return trafficLights.get(id);
    }

    @Override
    public void switchPhase(String tlId, int newPhase) {
        if (trafficLights.containsKey(tlId)) {
            simulationEngine.setTrafficLightPhase(tlId, newPhase);
        }
    }

    @Override
    public void setDuration(String tlId, int durationSeconds) {
        if (trafficLights.containsKey(tlId)) {
            simulationEngine.setTrafficLightDuration(tlId, durationSeconds);
        }
    }

    // =========================================================
    // SMART FEATURES (Force & Congestion Logic)
    // =========================================================

    @Override
    public void forceGreen(String tlId) {
        if (!trafficLights.containsKey(tlId)) return;
        
        System.out.println(">>> TrafficManager: Action FORCE GREEN on " + tlId);
        // Assuming Phase 0 is Green
        simulationEngine.setTrafficLightPhase(tlId, 0); 
        simulationEngine.setTrafficLightDuration(tlId, 60); 
    }

    @Override
    public void forceRed(String tlId) {
        if (!trafficLights.containsKey(tlId)) return;

        System.out.println(">>> TrafficManager: Action FORCE RED on " + tlId);
        // Assuming Phase 2 is Red/Yellow
        simulationEngine.setTrafficLightPhase(tlId, 2); 
        simulationEngine.setTrafficLightDuration(tlId, 60); 
    }

    @Override
    public void handleCongestion(List<IEdge> edges) {
        // List to track TLs modified in this loop to avoid conflicts
        List<String> processedTLs = new ArrayList<>();

        for (IEdge edge : edges) {
            // 1. Condition: More than 25 vehicles
            if (edge.getVehicleCount() > 25) {
                
                // 2. Find relevant Traffic Light
                String targetTlId = findTrafficLightForEdge(edge.getId());
                
                if (targetTlId != null) {
                    // 3. Check if TL was already modified in this cycle
                    if (!processedTLs.contains(targetTlId)) {
                        System.out.println("Congestion Detected: Edge " + edge.getId() + 
                                           " has " + edge.getVehicleCount() + " vehicles.");
                        
                        // 4. Action
                        forceGreen(targetTlId);
                        
                        // 5. Mark as processed
                        processedTLs.add(targetTlId);
                        
                    } else {
                        System.out.println("INFO: Edge " + edge.getId() + " is also congested, " +
                                           "but TL " + targetTlId + " is already being handled.");
                    }
                }
            }
        }
    }

    private String findTrafficLightForEdge(String edgeId) {
        String possibleLaneId = edgeId + "_0"; 
        
        for (ITrafficLight tl : trafficLights.values()) {
            List<String> lanes = tl.getControlledLanes();
            if (lanes != null && lanes.contains(possibleLaneId)) {
                return tl.getId();
            }
        }
        return null; 
    }
}