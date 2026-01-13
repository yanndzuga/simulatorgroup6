package de.frauas.group6.traffic.simulator.infrastructure;

import java.awt.geom.Point2D;
import java.util.List;

public class Junction implements IJunction {
    
    private final String id;
    private final List<Point2D> shape;
    private final Point2D position; 

    public Junction(String id, List<Point2D> shape, Point2D position) {
        this.id = id;
        this.shape = shape;
        this.position = position;
    }

    @Override
    public String getId() { return id; }

    @Override
    public List<Point2D> getShape() { return shape; }

    @Override
    public Point2D getPosition() { return position; }
    
    @Override
    public String toString() {
        return "Junction " + id;
    }
}