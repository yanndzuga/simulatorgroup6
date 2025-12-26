package de.frauas.group6.traffic.simulator.vehicles;

import java.awt.geom.Point2D;

public interface IVehicle {
	
	
	String getId();
	Point2D getPosition();
	String getColor();
    String getTypeId();
	double getSpeed();
	int getEdgeLane();
	String getEdgeId();
	boolean isIsVisible();
	String  getRouteId();
	
	void setColor(String newColor);
	void setSpeed(double newSpeed);
	void setPosition(Point2D newpos);
	void setEdgeId(String newEdgeId);
	void setEdgeLane(byte newLane);
	void setIsvisible(boolean isvisible);
	void setRouteId(String routeid);
	

}
