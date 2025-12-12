package de.frauas.group6.traffic.simulator.infrastructure;

import java.awt.geom.Point2D;

public interface ITrafficLight {
	
	String getId();
    
    // Where to draw the traffic light icon
    Point2D getPosition();
    
    // The current state string (e.g., "GrGr") or simplified state
    String getState();
    
    // The current phase index
    int getCurrentPhase();
    
    // Time remaining for current phase
    long getRemainingTime();

}
