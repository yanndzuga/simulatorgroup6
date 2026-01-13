package de.frauas.group6.traffic.simulator.infrastructure;

import java.util.List;

public interface ITrafficLight {
    String getId();
    int getCurrentPhase();
    String getCurrentState();
    long getRemainingTime();
    
    List<String> getControlledLanes();

    void setCurrentPhase(int phase);
    void setCurrentState(String state);
    void setRemainingTime(long time);
    void setControlledLanes(List<String> lanes);
}