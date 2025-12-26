package de.frauas.group6.traffic.simulator.infrastructure;

import java.awt.geom.Point2D;
import java.util.List;

public interface IEdge {
    String getId();
    List<Point2D> getShape();
    double getLength();
    
    // For Congestion Management
    int getVehicleCount();
    void setVehicleCount(int count);
}