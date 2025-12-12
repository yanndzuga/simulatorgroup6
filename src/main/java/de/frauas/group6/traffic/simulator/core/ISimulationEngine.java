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
    String getVehicleLaneId(String vehicleId);
    int[] getVehicleColor(String vehicleId);
    String getVehicleIdAtPosition(double x, double y, double radius);
    byte getVehicleLaneIndex(String vehicleId);
    
    void spawnVehicle(String id, String routeId,byte edgeLane, String typeId, int r, int g, int b, double speedInMps);
    void setVehicleColor(String id, int r, int g, int b);
    void setVehicleSpeed(String id, double speed);
    void removeVehicle(String id);

    // --- Trafficlight (member 3) ---
    List<String> getTrafficLightIdList();
    int getTrafficLightPhase(String tlId);
    long getTrafficLightRemainingTime(String tlId);
    String getTrafficLightState(String tlId);
    List<String> getControlledLanes(String tlId);
    int getLaneWaitingVehicleCount(String laneId);
    Point2D getTrafficLightPosition(String tlId);
    
    // --- junction (member 3 && 4) ---
    List<Point2D> getJunctionShape(String junctionId);
    List<String> getJunctionIdList();
    
    void setTrafficLightPhase(String tlId, int phaseIndex);
    void setTrafficLightDuration(String tlId, int durationSeconds);

    // --- edge  ---
    List<String> getEdgeIdList();
    List<Point2D> getEdgeShape(String edgeId);
    int getEdgeVehicleCount(String edgeId);
    List<String> getLaneList(String edgeId);
    double getEdgeLength(String edgeId);
    
    // --- engine control ---
    void start();
    void stop();
    void step();
    void pause();
    void resume();
    boolean isPaused();
}
