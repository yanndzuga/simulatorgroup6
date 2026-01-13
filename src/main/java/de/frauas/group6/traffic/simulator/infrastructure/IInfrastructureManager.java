package de.frauas.group6.traffic.simulator.infrastructure;

import java.util.List;

public interface IInfrastructureManager {
    void loadNetwork();
    List<IEdge> getAllEdges();
    List<IJunction> getAllJunctions();
    List<String> loadRouteIds(String resource);
    void refreshEdgeData();
}