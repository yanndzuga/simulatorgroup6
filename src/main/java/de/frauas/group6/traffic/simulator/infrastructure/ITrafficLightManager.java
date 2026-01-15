package de.frauas.group6.traffic.simulator.infrastructure;

import java.util.List;

public interface ITrafficLightManager {
    void updateTrafficLights();
    List<ITrafficLight> getAllTrafficLights();
    ITrafficLight getTrafficLightById(String id);
    void switchPhase(String tlId, int newPhase);
   
    
    // --- Logic Control ---
    void forceGreen(String tlId);
    void forceRed(String tlId);
    
}
