package de.frauas.group6.traffic.simulator.infrastructure;

import de.frauas.group6.traffic.simulator.core.ISimulationEngine;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.logging.Level;

public class TrafficLightManager implements ITrafficLightManager {

    private static final Logger LOGGER = Logger.getLogger(TrafficLightManager.class.getName());

    private ISimulationEngine simulationEngine;
    private IInfrastructureManager infrastructureManager;
    private final Map<String, ITrafficLight> trafficLights = new HashMap<>();
    
    // Cooldown timer
    private final Map<String, Long> lastActionTime = new HashMap<>();
    private final long COOLDOWN_MS = 10000; 

    // Config for J55
    private final Map<String, Integer> altGreenPhaseMap = new HashMap<>();
    private boolean initialized = false;

    public TrafficLightManager(ISimulationEngine simulationEngine, IInfrastructureManager infraMgr) {
        if (simulationEngine == null) {
            LOGGER.severe("SimulationEngine is NULL!"); 
            throw new InfrastructureException("Cannot start: Engine is null.");
        }

        this.simulationEngine = simulationEngine;
        this.infrastructureManager = infraMgr;
        
        altGreenPhaseMap.put("J55", 4); 
        
        LOGGER.info("TrafficLightManager Started.");
    }

    @Override
    public void updateTrafficLights() {
        if (!initialized) {
            initializeMap();
            initialized = true;
        }
        checkAndHandleCongestion();

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

    @Override
    public void setDuration(String tlId, int durationSeconds) {
        if (trafficLights.containsKey(tlId)) {
            simulationEngine.setTrafficLightDuration(tlId, durationSeconds);
        }
    }

    @Override
    public void forceGreen(String tlId) {
        if (!trafficLights.containsKey(tlId)) {
            throw new InfrastructureException("Error: TL " + tlId + " not found!");
        }

        int current = simulationEngine.getTrafficLightPhase(tlId);
        int secondaryPhase = altGreenPhaseMap.getOrDefault(tlId, 2); 
        
        int next = (current == 0) ? secondaryPhase : 0;
        
        LOGGER.info("Force Green: " + tlId + " (" + current + " -> " + next + ")");
        
        simulationEngine.setTrafficLightPhase(tlId, next);
        simulationEngine.setTrafficLightDuration(tlId, 45); 
        
        lastActionTime.put(tlId, System.currentTimeMillis());
    }

    @Override
    public void forceRed(String tlId) {
        if (!trafficLights.containsKey(tlId)) return;
        
        simulationEngine.setTrafficLightPhase(tlId, 1); 
        LOGGER.info("Force Red: " + tlId);
        lastActionTime.put(tlId, System.currentTimeMillis());
    }

    @Override
    public void handleCongestion(List<IEdge> edges) {
        long now = System.currentTimeMillis();

        for (IEdge edge : edges) {
            if (edge.getVehicleCount() > 7) { 
                
                String targetTlId = findTrafficLightForEdge(edge.getId());
                
                if (targetTlId != null) {
                    long lastTime = lastActionTime.getOrDefault(targetTlId, 0L);
                    
                    if (now - lastTime > COOLDOWN_MS) {
                        LOGGER.warning("Congestion detected (" + edge.getId() + ") -> Action on " + targetTlId);
                        
                        forceGreen(targetTlId);
                        lastActionTime.put(targetTlId, now);
                    } 
                }
            }
        }
    }
    
    public void checkAndHandleCongestion() {
        if (infrastructureManager == null) {
            System.err.println("Error: infrastructureManager is NULL!");
            return;
        }

        infrastructureManager.refreshEdgeData();
        List<IEdge> edges = infrastructureManager.getAllEdges();

        if (edges == null || edges.isEmpty()) {
            System.err.println("Warning: Edge list is empty!");
            return;
        }

        handleCongestion(edges);
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