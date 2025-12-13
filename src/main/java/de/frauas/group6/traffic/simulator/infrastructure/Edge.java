package de.frauas.group6.traffic.simulator.infrastructure;

import java.awt.geom.Point2D;
import java.util.List;

public class Edge implements IEdge {
    
    private final String id;
    private final List<Point2D> shape;
    private final double length;
    private int vehicleCount;

    public Edge(String id, List<Point2D> shape, double length) {
        this.id = id;
        this.shape = shape;
        this.length = length;
        this.vehicleCount = 0;
    }

    @Override
    public String getId() { return id; }

    @Override
    public List<Point2D> getShape() { return shape; }

    @Override
    public double getLength() { return length; }

    @Override
    public int getVehicleCount() { return vehicleCount; }

    @Override
    public void setVehicleCount(int vehicleCount) {
        this.vehicleCount = vehicleCount;
    }
}