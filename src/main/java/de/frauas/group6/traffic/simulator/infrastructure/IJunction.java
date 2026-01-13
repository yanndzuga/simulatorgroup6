package de.frauas.group6.traffic.simulator.infrastructure;

import java.awt.geom.Point2D;
import java.util.List;

public interface IJunction {
    String getId();
    List<Point2D> getShape();
    Point2D getPosition();
}