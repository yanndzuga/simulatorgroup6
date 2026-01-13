package de.frauas.group6.traffic.simulator.infrastructure;

import java.awt.geom.Point2D;
import java.util.List;

public class TrafficLight implements ITrafficLight {

    private final String id;
    private final Point2D position;
    
    private int currentPhase;
    private String currentState;
    private long remainingTime;
    private List<String> controlledLanes;

    public TrafficLight(String id, Point2D position) {
        this.id = id;
        this.position = position;
    }

    @Override
    public String getId() { return id; }
    
    public Point2D getPosition() { return position; }

    @Override
    public int getCurrentPhase() { return currentPhase; }

    @Override
    public void setCurrentPhase(int currentPhase) { this.currentPhase = currentPhase; }

    @Override
    public String getCurrentState() { return currentState; }

    @Override
    public void setCurrentState(String currentState) { this.currentState = currentState; }

    @Override
    public long getRemainingTime() { return remainingTime; }

    @Override
    public void setRemainingTime(long remainingTime) { this.remainingTime = remainingTime; }

    @Override
    public List<String> getControlledLanes() { return controlledLanes; }

    @Override
    public void setControlledLanes(List<String> controlledLanes) { this.controlledLanes = controlledLanes; }
}