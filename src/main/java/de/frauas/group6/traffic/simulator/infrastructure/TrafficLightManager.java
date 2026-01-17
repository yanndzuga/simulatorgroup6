package de.frauas.group6.traffic.simulator.infrastructure;

import de.frauas.group6.traffic.simulator.core.ISimulationEngine;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * TrafficLightManager
 * <p>
 * Implementation of the Traffic Light Manager interface.
 * Responsibilities include:
 * 1. Synchronizing state between the Simulation Engine and the View.
 * 2. Handling manual overrides (Force Green/Red).
 * 3. Implementing "Smart" congestion handling by analyzing queue lengths.
 * </p>
 */
public class TrafficLightManager implements ITrafficLightManager {

    private static final Logger LOGGER = Logger.getLogger(TrafficLightManager.class.getName());

    private ISimulationEngine simulationEngine;
    private IInfrastructureManager infrastructureManager;
    private final Map<String, ITrafficLight> trafficLights = new HashMap<>();
    
    // Cooldown timer to prevent rapid switching during congestion handling
    private final Map<String, Long> lastActionTime = new HashMap<>();
    private final long COOLDOWN_MS = 10000; // 10 seconds

    // Configuration for specific junctions (e.g., J55)
    private final Map<String, Integer> altGreenPhaseMap = new HashMap<>();
    private boolean initialized = false;

    public TrafficLightManager(ISimulationEngine simulationEngine, IInfrastructureManager infraMgr) {
        if (simulationEngine == null) {
            LOGGER.severe("SimulationEngine is NULL!"); 
            throw new InfrastructureException("Cannot start: Engine is null.");
        }

        this.simulationEngine = simulationEngine;
        this.infrastructureManager = infraMgr;
        
        // Default green phase mapping for J55
        altGreenPhaseMap.put("J55", 4); 
        
        LOGGER.info("TrafficLightManager Started.");
    }

    @Override
    public void updateTrafficLights() {
        if (!initialized) {
            initializeMap();
            initialized = true;
        }

        // Run smart congestion logic
        checkAndHandleCongestion();

        // Sync state from Engine to Local Objects
        for (ITrafficLight tl : trafficLights.values()) {
            try {
                int phase = simulationEngine.getTrafficLightPhase(tl.getId());
                String state = simulationEngine.getTrafficLightState(tl.getId());
                long time = simulationEngine.getTrafficLightRemainingTime(tl.getId());

                tl.setCurrentPhase(phase);
                tl.setCurrentState(state);
                tl.setRemainingTime(time);
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Error updating TL: " + tl.getId(), e);
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
        LOGGER.info("Loaded " + trafficLights.size() + " traffic lights.");
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
            LOGGER.info("Switching " + tlId + " -> Phase " + newPhase);
        } else {
            LOGGER.warning("Unknown TL: " + tlId);
        }
    }

    public void setDuration(String tlId, int durationSeconds) {
        if (trafficLights.containsKey(tlId)) {
            simulationEngine.setTrafficLightDuration(tlId, durationSeconds);
        }
    }

    @Override
    public void forceGreen(String tlId) {
        if (!trafficLights.containsKey(tlId)) return;
        
       LOGGER.info(">>> TrafficManager: Action FORCE GREEN on " + tlId);
        
        int greenPhase = 0; 

        // Specific Logic for Junction J55
        if (tlId.equals("J55")) {
            try {
                int current = simulationEngine.getTrafficLightPhase(tlId);
                // Toggle between North/South (0-3) and East/West (4)
                if (current == 0 || current == 1 || current == 2 || current == 3) {
                    greenPhase = 4; 
                } else {
                    greenPhase = 0;
                }
            }  catch (Exception e) {
                greenPhase = 4; 
            }
        } 
        // Specific Logic for Junction J57
        else if (tlId.equals("J57")) {
             try {
                int current = simulationEngine.getTrafficLightPhase(tlId);
                if (current == 0) greenPhase = 2; // Toggle
                else greenPhase = 0;
            } catch (Exception e) {}
        }

        simulationEngine.setTrafficLightPhase(tlId, greenPhase); 
    }

    @Override
    public void forceRed(String tlId) {
        if (!trafficLights.containsKey(tlId)) return;

        LOGGER.info(">>> TrafficManager: Action FORCE RED on " + tlId);
        
        // Set to "All Red" phase or Yellow -> Red transition.
        // In the SUMO configuration, Phase 2 is often the red transition for the main axis.
        int redPhase = 2;
        
        simulationEngine.setTrafficLightPhase(tlId, redPhase); 
    }
    
    /**
     * INTELLIGENT PHASE SELECTION
     * Selects the correct Green phase based on WHICH road is congested.
     * * @param tlId The traffic light ID
     * @param edgeId The ID of the road that is congested
     */
    private void forceGreenForEdge(String tlId, String edgeId) {
        int targetPhase = 0; // Default

        if (tlId.equals("J55")) {
            // XML ANALYSIS for J55:
            // Phase 0: North/South Straight (E45 -> -E46)
            // Phase 2: North Left Turn (E45 -> -E47) -> This is ROUTE 4!
            // Phase 4: East/West (-E48 -> -E47)

            if (edgeId.equals("E45")) {
                targetPhase = 2; 
                LOGGER.info("Smart Logic: Route 4 Congestion detected -> Forcing LEFT TURN Phase (2)");
            } 
            else if (edgeId.equals("-E48") || edgeId.equals("E47")) {
                // If traffic originates from East or West
                targetPhase = 4;
            } 
            else {
                // Default handling (E46, etc.)
                targetPhase = 0;
            }
        } 
        else if (tlId.equals("J57")) {
            // Simple logic for J57
            if (edgeId.equals("E49") || edgeId.equals("-E47")) targetPhase = 2;
            else targetPhase = 0;
        }

        simulationEngine.setTrafficLightPhase(tlId, targetPhase);
    }

    /**
     * Iterates through edges to find heavy traffic.
     * @param edges List of all road edges
     */
   
   
    private void handleCongestion(List<IEdge> edges) {
        long now = System.currentTimeMillis();
        
        for (IEdge edge : edges) {
            // Threshold: If more than 7 vehicles are waiting/driving on this edge
            if (edge.getVehicleCount() > 7) { 
                
                String targetTlId = findTrafficLightForEdge(edge.getId());
                
                if (targetTlId != null) {
                    long lastTime = lastActionTime.getOrDefault(targetTlId, 0L);
                    
                    // Respect the cooldown timer (10s)
                    if (now - lastTime > COOLDOWN_MS) {
                        LOGGER.warning("Congestion detected (" + edge.getId() + ") -> Action on " + targetTlId);                        
                        forceGreenForEdge(targetTlId, edge.getId());
                        lastActionTime.put(targetTlId, now);
                    } 
                }
            }
        }
    }
    
    private void checkAndHandleCongestion() {
        if (infrastructureManager == null) {
        	 LOGGER.warning("Error: infrastructureManager is NULL!");
            return;
        }

        // Ensure we have fresh data from the simulation
        infrastructureManager.refreshEdgeData();
        List<IEdge> edges = infrastructureManager.getAllEdges();

        if (edges == null || edges.isEmpty()) {
        	 LOGGER.warning("Warning: Edge list is empty!");
            return;
        }

        handleCongestion(edges);
    }

    private String findTrafficLightForEdge(String edgeId) {
        String possibleLaneId = edgeId + "_0"; // Append lane index 0
        for (ITrafficLight tl : trafficLights.values()) {
            List<String> lanes = tl.getControlledLanes();
            if (lanes != null && lanes.contains(possibleLaneId)) {
                return tl.getId();
            }
        }
        return null; 
    }
}
