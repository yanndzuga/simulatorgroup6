package de.frauas.group6.traffic.simulator.vehicles;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import de.frauas.group6.traffic.simulator.core.ISimulationEngine;

/*------------------------------------------------------------------------------------------
  Manages the lifecycle of vehicles in the simulation.
  Handles injection, modification, deletion, and synchronization with the SUMO engine.
  ------------------------------------------------------------------------------------------
 */
public class VehicleManager implements IVehicleManager {

    private static final Logger LOGGER = Logger.getLogger(VehicleManager.class.getName());
    
    private ISimulationEngine SumolationEngine;
    private Map<String, IVehicle> Vehicles;
    
    //-- Tracks creation time to provide a "grace period" for new vehicles before deletion--
    private Map<String, Long> creationTimes; 
    static Long counter; 

    /*----------------------------------------------------------------------
      Constructor for VehicleManager.
        @param SumolationEngine The core simulation engine interface.
        ---------------------------------------------------------------------
     */
    public VehicleManager(ISimulationEngine SumolationEngine) {
        this.SumolationEngine = SumolationEngine;
        this.Vehicles = new ConcurrentHashMap<>();
        this.creationTimes = new ConcurrentHashMap<>();
        counter = (long) 0;
        LOGGER.info("ATTENTION: A new VehicleManager has been created! " + this);
    }

    /*-------------------------------------------------------------------
      Injects vehicles into the simulation on a separate thread.
      Maps user-defined types and colors to SUMO-compatible parameters.
      -------------------------------------------------------------------
     */
    public void injectVehicle(String RouteId, String VehicleType, String color, int number, double speed, String Currentcolor, double Currentspeed) {
        new Thread(() -> {
            String vehicleId;
            String TypeId = "";
            List<String> successfullyAddedIds = new ArrayList<>();
            byte edgeLane = 0;
            int r = 0, g = 0, b = 0;
            boolean isvisible = false;
            final double SPEED_TOLERANCE = 0.1;

            // Mapping color names to RGB values
            switch (color) {
                case "Yellow": r = 255; g = 255; b = 0; break;
                case "Red":    r = 255; g = 0;   b = 0; break;
                case "Green":  r = 0;   g = 255; b = 0; break;
            }

            //-- Vehicle Type Mapping to SUMO internal type IDs--
            switch (VehicleType) {
                case "Standard-Car": TypeId = "DEFAULT_VEHTYPE"; break;
                case "Truck":        TypeId = "DEFAULT_CONTAINERTYPE"; break;
                case "Emergency-Vehicle": TypeId = "RESCUE_TYPE"; break;
                case "City-Bus":     TypeId = "BUS_TYPE"; break;
            }

            // --Visibility filter logic based on color and speed criteria--
            switch(Currentcolor) {
                case "All": { 
                    if(Math.abs(speed - Currentspeed) <= SPEED_TOLERANCE || Currentspeed <= SPEED_TOLERANCE) {
                        isvisible = true;
                    }  
                }
                break;
                default: { 
                    if(color.equals(Currentcolor) || Math.abs(speed - Currentspeed) < SPEED_TOLERANCE) { 
                        isvisible = true; 
                    }  
                }
                break;
            }
            
            try {
                // --Synchronize on the Vehicles map to ensure thread safety during injection--
                synchronized (Vehicles) {
                    for (int i = 0; i < number; i++) {
                        counter++;
                        vehicleId = "VEH_" + counter;
                        Vehicle newvehicle = new Vehicle(vehicleId, TypeId, speed, color, "", (byte) 0, isvisible, RouteId);

                        // Extended Protection: Set creation time BEFORE adding to main map.
                        // This gives SUMO time (grace period) to process the insertion queue.
                        creationTimes.put(vehicleId, System.currentTimeMillis());
                        Vehicles.put(vehicleId, newvehicle);
                        
                        LOGGER.info("DEBUG: Car " + vehicleId + " added to map (size: " + Vehicles.size() + ")");
                        
                        //-- Execute spawn command in the simulation engine--
                        SumolationEngine.spawnVehicle(vehicleId, RouteId, edgeLane, TypeId, r, g, b, speed);
                        
                        successfullyAddedIds.add(vehicleId);
                    }
                }
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "CRITICAL TRAFFIC INJECTION FAILURE: " + e.getMessage());
                //-- Rollback: Cleanup local tracking if engine injection fails--
                for (String id : successfullyAddedIds) {
                    Vehicles.remove(id);
                    creationTimes.remove(id);
                }
            }
        }).start();
    }

    /*----------------------------------------------------------------------------------------
      Modifies properties of an existing vehicle.
      Updates local state and synchronizes with the SUMO engine.
      @throws ModifyVehicleException if vehicle is not found or synchronization fails.
      ----------------------------------------------------------------------------------------
     */
    public void modifyVehicle(String vehicleId, String newcolor, double newspeed) throws Exception {
        IVehicle myvehicle = Vehicles.get(vehicleId);

        if (myvehicle == null) {
            throw new ModifyVehicleException("Target vehicle ID '" + vehicleId + "' not found. Update aborted.");
        }
        
        try {
            myvehicle.setColor(newcolor);
            myvehicle.setSpeed(newspeed);
            int r = 0, g = 0, b = 0;
            
            // Re-map color to RGB for the engine update
            switch (newcolor) {
                case "Yellow": r = 255; g = 255; b = 0; break;
                case "Red":    r = 255; g = 0;   b = 0; break;
                case "Green":  r = 0;   g = 222; b = 0; break;
            }

            //-- Update SUMO engine state--
            SumolationEngine.setVehicleColor(vehicleId, r, g, b);
            SumolationEngine.setVehicleSpeed(vehicleId, newspeed);
        } catch(Exception e) {
            //-- Wrap underlying exceptions into custom domain exception--
            throw new ModifyVehicleException("Failed to synchronize modifications with the Simulation Engine for vehicle: " + vehicleId, e);
        }
    }

    /*---------------------------------------------------------------------------------------------------------
      Deletes vehicles matching specific edge, color, and speed criteria.
      @throws DeleteVehicleException if the required number of vehicles cannot be found or deletion fails.
      ---------------------------------------------------------------------------------------------------------
     */
    public void deleteVehicle(String requestedEdgeId, String requestedColor, int requestnumber) {
        int count = 0;
        List<String> validVehicleIds = new ArrayList<>();
        final double SPEED_TOLERANCE = 0.001;

        //-- Identification phase: find vehicles matching the user request--
        for (IVehicle vehicle : Vehicles.values()) {
           

            if (vehicle.getRouteId().equals(requestedEdgeId) &&
                vehicle.getColor().equals(requestedColor)
                ) {
                count++;
                validVehicleIds.add(vehicle.getId());
            }

            if (requestnumber == count) { break; }
        }

        //-- Validation: Ensure enough vehicles were found before proceeding--
        if (validVehicleIds.size() < requestnumber) {
            throw new DeleteVehicleException("ERROR: Only found " + validVehicleIds.size() + 
                                               " vehicles, but " + requestnumber + 
                                               " required. No vehicles deleted.");
        }

        // --Execution phase: remove from engine and local tracking--
        for (String id : validVehicleIds) {
            try {
                SumolationEngine.removeVehicle(id);
                Vehicles.remove(id);
                creationTimes.remove(id); 
            } catch (Exception e) {
                throw new DeleteVehicleException("ERROR deleting vehicle " + id + ". Process stopped.", e);
            }
        }
    }

    /*--------------------------------------------------------------------------------------------
      Updates vehicle positions and removes vehicles that have exited the simulation.
      Implements a "Grace Period" logic to allow newly injected vehicles time to appear in SUMO.
      --------------------------------------------------------------------------------------------
     */
    public void updateVehicles() {
        List<String> activeSumoIds = SumolationEngine.getVehicleIdList();
        Set<String> activeSet = new HashSet<>(activeSumoIds);
        long now = System.currentTimeMillis();

        //-- Iterative cleanup of the vehicle map--
        Vehicles.entrySet().removeIf(entry -> {
            String id = entry.getKey();
            IVehicle vehicle = entry.getValue();

            // --Case 1: Vehicle is active in the SUMO engine--
            if (activeSet.contains(id)) {
                try {
                    // Update 3D position
                    Point2D newPos = SumolationEngine.getVehiclePosition(id);
                    vehicle.setPosition(newPos);
                    
                    //Update Speed
                   double newSpeed= SumolationEngine.getVehicleSpeed(id);
                    vehicle.setSpeed(newSpeed);
                  
                    // --Vehicle successfully appeared in engine, remove creation timestamp--
                    creationTimes.remove(id); 
                } catch (Exception e) { }
                return false; // --Retain in local map--
            } 
            
            //-- Case 2: Vehicle not in SUMO yet (check if within grace period)--
            else {
                Long createdAt = creationTimes.get(id);

                // Wait up to 100 seconds (grace period) for SUMO to process injection
                if (createdAt != null && (now - createdAt) < 100000) {
                    return false; // --Retain (Still waiting for engine arrival)--
                } else {
                    return true; //-- Remove (Timeout reached or vehicle exited)--
                }
            }
        });

        // --Sync the auxiliary creationTimes map with the main Vehicles map--
        creationTimes.keySet().retainAll(Vehicles.keySet());
    }

    /*--------------------------------------------------------------
      @return A collection of all currently managed vehicles.
      --------------------------------------------------------------
     */
    public Collection<IVehicle> getAllVehicles() {
        return this.Vehicles.values();
    }
    
    /*---------------------------------------------------------------------
      Filters visibility of vehicles based on color and speed.
      This is used to highlight specific vehicle groups in the 3D view.
      ---------------------------------------------------------------------
     */
    public void SelectVehicle(String Currentcolor) { 
    	
        synchronized (Vehicles) {
            if(!(Currentcolor.equals("All"))) {
                for(IVehicle V : Vehicles.values()) {
                    // Filter by specific color and speed match
                    if(V.getColor().equalsIgnoreCase(Currentcolor)) {
                        V.setIsvisible(true);
                    } else { 
                        V.setIsvisible(false); 
                    }
                }
            } else {
                // --Filter by speed match only ("All" colors)--
                for(IVehicle V : Vehicles.values()) {           
                        V.setIsvisible(true);               
                }
            }
        }
    }
          }