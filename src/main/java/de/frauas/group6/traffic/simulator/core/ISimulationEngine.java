package de.frauas.group6.traffic.simulator.core;

import java.awt.geom.Point2D;
import java.util.List;



public interface ISimulationEngine {

    // --- time control ---
    double getCurrentSimulationTime();

    // --- Vehicles (for member 2 & 4 & 5) ---
    List<String> getVehicleIdList();
    Point2D getVehiclePosition(String vehicleId);
    double getVehicleSpeed(String vehicleId);
    String getVehicleRoadId(String vehicleId);
    
    void spawnVehicle(String id, String routeId, String typeId);
    void setVehicleColor(String id, int r, int g, int b);

    // --- Trafficlight (member 3) ---
    List<String> getTrafficLightIdList();
    int getTrafficLightPhase(String tlId);
    long getTrafficLightRemainingTime(String tlId);
    
    void setTrafficLightPhase(String tlId, int phaseIndex);
    void setTrafficLightDuration(String tlId, int durationSeconds);

    // --- edge  ---
    List<String> getEdgeIdList();
    List<Point2D> getEdgeShape(String edgeId);
    int getEdgeVehicleCount(String edgeId);
    double getEdgeLength(String edgeId);
    
    // --- engine control ---
    void start ();
    void stop();
}
