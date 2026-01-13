package de.frauas.group6.traffic.simulator.infrastructure;

import java.util.List;

public interface ITrafficLightManager {
    void updateTrafficLights();
    List<ITrafficLight> getAllTrafficLights();
    ITrafficLight getTrafficLightById(String id);
    void switchPhase(String tlId, int newPhase);
    void setDuration(String tlId, int durationSeconds);
    
    // --- Logic Control ---
    void forceGreen(String tlId);
    void forceRed(String tlId);
    void handleCongestion(List<IEdge> edges);
}