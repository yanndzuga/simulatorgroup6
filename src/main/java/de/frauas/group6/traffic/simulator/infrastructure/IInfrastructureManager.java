package de.frauas.group6.traffic.simulator.infrastructure;

import java.util.List;
import java.util.Map;

public interface IInfrastructureManager {
    void loadNetwork();
    List<IEdge> getAllEdges();
    List<IJunction> getAllJunctions();
    List<String> loadRouteIds(String resource);
    void refreshEdgeData();
    Map<String, List<String>> loadRoutes (String filePath);
}