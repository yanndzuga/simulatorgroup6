package de.frauas.group6.traffic.simulator.vehicles;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import de.frauas.group6.traffic.simulator.core.ISimulationEngine;

/**
 * Manages the lifecycle of vehicles in the simulation.
 * Handles injection, modification, deletion, and synchronization with the SUMO engine.
 */
public class VehicleManager implements IVehicleManager {

    private ISimulationEngine SumolationEngine;
    private Map<String, IVehicle> Vehicles;
    // Tracks creation time to provide a "grace period" for new vehicles before deletion
    private Map<String, Long> creationTimes; 
    static Long counter; 

    /**
     * Constructor for VehicleManager.
     * @param SumolationEngine The core simulation engine interface.
     */
    public VehicleManager(ISimulationEngine SumolationEngine) {
        this.SumolationEngine = SumolationEngine;
        this.Vehicles = new ConcurrentHashMap<>();
        this.creationTimes = new ConcurrentHashMap<>();
        counter = (long) 0;
        System.out.println("ATTENTION: A new VehicleManager has been created! " + this);
    }

    /**
     * Injects vehicles into the simulation on a separate thread.
     */
    public void injectVehicle(String edgeId, String lane, String VehicleType, String color, int number, double speed) {
        new Thread(() -> {
            String vehicleId;
            String TypeId = "";
            List<String> successfullyAddedIds = new ArrayList<>();
            byte edgeLane = 0;
            int r = 0, g = 0, b = 0;

            switch (color) {
                case "Yellow": r = 255; g = 255; b = 0; break;
                case "Rot":    r = 255; g = 0;   b = 0; break;
                case "Green":  r = 0;   g = 222; b = 0; break;
            }

            // Lane logic configuration
            if (edgeId.equals("E45")) {
                if (lane.equals("middle")) { edgeLane = 1; }
                else if (lane.equals("Left")) { edgeLane = 2; }
            } else {
                if (lane.equals("Left")) { edgeLane = 1; }
            }

            // Vehicle Type Mapping
            switch (VehicleType) {
                case "Standard-Car": TypeId = "DEFAULT_VEHTYPE"; break;
                case "Truck":        TypeId = "DEFAULT_CONTAINERTYPE"; break;
                case "Emergency-Vehicle": TypeId = "RESCUE_TYPE"; break;
                case "City-Bus":     TypeId = "BUS_TYPE"; break;
            }

            // Route Mapping Logic
            String routeid = "";
            switch (edgeId) {
                case "-E48": routeid = (edgeLane == 0) ? "R0" : "R1"; break;
                case "E46":  routeid = (edgeLane == 0) ? "R2" : "R3"; break;
                case "-E51": routeid = (edgeLane == 0) ? "R5" : "R6"; break;
                case "E50":  routeid = (edgeLane == 0) ? "R7" : "R8"; break;
                case "E45":  routeid = (edgeLane == 0) ? "R9" : (edgeLane == 1 ? "R10" : "R11"); break;
                case "E49":  routeid = (edgeLane == 0) ? "R13" : "R12"; break;
            }

            try {
                // Synchronize on the Vehicles map to ensure thread safety during injection
                synchronized (Vehicles) {
                	if (number <=100)
                    for (int i = 0; i < number; i++) {
                        counter++;
                        vehicleId = "VEH_" + counter;
                        Vehicle newvehicle = new Vehicle(vehicleId, TypeId, speed, color, edgeId, edgeLane);

                        // Extended Protection: Set creation time BEFORE adding to main map.
                        // This gives SUMO time (10s) to process the insertion queue.
                        creationTimes.put(vehicleId, System.currentTimeMillis());
                        Vehicles.put(vehicleId, newvehicle);
                        
                        System.out.println("DEBUG: Car " + vehicleId + " added to map (size: " + Vehicles.size() + ")");
                        
                        // Spawn in SUMO
                        SumolationEngine.spawnVehicle(vehicleId, routeid, edgeLane, TypeId, r, g, b, speed);
                        
                        successfullyAddedIds.add(vehicleId);
                        
                        // Small delay to space out TraCI requests
                        Thread.sleep(50); 
                    }
                }
            } catch (Exception e) {
                System.err.println("CRITICAL TRAFFIC INJECTION FAILURE: " + e.getMessage());
                // Cleanup on failure
                for (String id : successfullyAddedIds) {
                    Vehicles.remove(id);
                    creationTimes.remove(id);
                }
            }
        }).start();
    }

    /**
     * Modifies properties of an existing vehicle.
     */
    public void modifyVehicle(String vehicleId, String newcolor, double newspeed) throws Exception {
        IVehicle myvehicle = Vehicles.get(vehicleId);

        if (myvehicle == null) {
            throw new IllegalArgumentException("ERROR: Vehicle ID '" + vehicleId + 
                                               "' not found in Manager. Modification aborted.");
        }

        myvehicle.setColor(newcolor);
        myvehicle.setSpeed(newspeed);
        int r = 0, g = 0, b = 0;
        switch (newcolor) {
            case "Yellow": r = 255; g = 255; b = 0; break;
            case "Rot":    r = 255; g = 0;   b = 0; break;
            case "Green":  r = 0;   g = 222; b = 0; break;
        }

        SumolationEngine.setVehicleColor(vehicleId, r, g, b);
        SumolationEngine.setVehicleSpeed(vehicleId, newspeed);
    }

    /**
     * Deletes vehicles matching criteria from a specific edge.
     */
    public void deleteVehicle(String requestedEdgeId, String requestedColor, double requestedSpeed, int requestnumber) {
        int count = 0;
        List<String> validVehicleIds = new ArrayList<>();
        final double SPEED_TOLERANCE = 0.001;

        for (IVehicle vehicle : Vehicles.values()) {
            boolean speedMatches = Math.abs(vehicle.getSpeed() - requestedSpeed) < SPEED_TOLERANCE;

            if (vehicle.getEdgeId().equals(requestedEdgeId) &&
                vehicle.getColor().equals(requestedColor) &&
                speedMatches) {
                count++;
                validVehicleIds.add(vehicle.getId());
            }

            if (requestnumber == count) { break; }
        }

        if (validVehicleIds.size() < requestnumber) {
            throw new IllegalArgumentException("ERROR: Only found " + validVehicleIds.size() + 
                                               " vehicles, but " + requestnumber + 
                                               " required. No vehicles deleted.");
        }

        for (String id : validVehicleIds) {
            try {
                SumolationEngine.removeVehicle(id);
                Vehicles.remove(id);
                creationTimes.remove(id); 
            } catch (Exception e) {
                throw new RuntimeException("ERROR deleting vehicle " + id + ". Process stopped.", e);
            }
        }
    }

    /**
     * Updates vehicle positions and removes vehicles that have exited the simulation.
     * Uses a 'grace period' logic to prevent premature deletion of newly injected vehicles.
     */
    public void updateVehicles() {
        List<String> activeSumoIds = SumolationEngine.getVehicleIdList();
        Set<String> activeSet = new HashSet<>(activeSumoIds);
        long now = System.currentTimeMillis();

        Vehicles.entrySet().removeIf(entry -> {
            String id = entry.getKey();
            IVehicle vehicle = entry.getValue();

            // CASE A: Vehicle is active in SUMO -> Update position
            if (activeSet.contains(id)) {
                try {
                    Point2D newPos = SumolationEngine.getVehiclePosition(id);
                    String newEdgeId = SumolationEngine.getVehicleRoadId(id);
                    int newLane = SumolationEngine.getVehicleLaneIndex(id);

                    vehicle.setPosition(newPos);
                    vehicle.setEdgeId(newEdgeId);
                    vehicle.setEdgeLane((byte) newLane);

                    // If active, we can remove it from the creation grace period list.
                    // Note: If removed too early and SUMO has a momentary glitch, it might be deleted.
                    // However, 'creationTimes' is mainly for the initial spawn delay.
                    creationTimes.remove(id);

                } catch (Exception e) {
                    // Ignore occasional read errors
                }
                return false; // KEEP in map
            }
            // CASE B: Vehicle is NOT in SUMO (Finished or not yet spawned)
            else {
                Long createdAt = creationTimes.get(id);

                // Grace period: 100 seconds (100000ms)
                // Allows SUMO to queue insertions if the entry lane is blocked.
                if (createdAt != null && (now - createdAt) < 100000) {
                    return false; // KEEP (Waiting for SUMO insertion)
                } else {
                    // Truly gone (Finished route or timed out from queue)
                    // System.out.println("Cleaning up: " + id);
                    return true; // DELETE from map
                }
            }
        });

        // Cleanup auxiliary map
        creationTimes.keySet().retainAll(Vehicles.keySet());
    }

    public Collection<IVehicle> getAllVehicles() {
        return this.Vehicles.values();
    }
}