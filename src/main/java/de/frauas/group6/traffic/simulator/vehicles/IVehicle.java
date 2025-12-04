package de.frauas.group6.traffic.simulator.vehicles;

import java.awt.geom.Point2D;

public interface IVehicle {
	
	String getId();
	Point2D getPosition();
	String getColor();
	double getSpeed();

}
