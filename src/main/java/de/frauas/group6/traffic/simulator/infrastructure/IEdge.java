package de.frauas.group6.traffic.simulator.infrastructure;

import java.awt.geom.Point2D;
import java.util.List;

public interface IEdge {
	
	String getId();
    
    // The shape of the road (List of X,Y points)
    List<Point2D> getShape();
    
    // Statistics for visualization (e.g., color road red if busy)
    int getVehicleCount();
    double getLength();

}
