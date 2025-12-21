package de.frauas.group6.traffic.simulator.infrastructure;

import de.frauas.group6.traffic.simulator.core.ISimulationEngine;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class InfrastructureManager implements IInfrastructureManager {
    
    private ISimulationEngine simulationEngine;
    
    private final Map<String, IEdge> edges = new HashMap<>();
    private final Map<String, IJunction> junctions = new HashMap<>();
    private boolean initialized = false;

    public InfrastructureManager(ISimulationEngine simulationEngine) {
        this.simulationEngine = simulationEngine;
    }

    @Override
    public void loadNetwork() {
        if (initialized) return;

        System.out.println("Loading network infrastructure from SUMO...");

        // 1. Load Edges
        List<String> edgeIds = simulationEngine.getEdgeIdList();
        if (edgeIds != null) {
            for (String id : edgeIds) {
                List<Point2D> shape = simulationEngine.getEdgeShape(id);
                double length = simulationEngine.getEdgeLength(id);
                
                edges.put(id, new Edge(id, shape, length));
            }
        }

        // 2. Load Junctions
        List<String> junctionIds = simulationEngine.getJunctionIdList();
        if (junctionIds != null) {
            for (String id : junctionIds) {
                List<Point2D> shape = simulationEngine.getJunctionShape(id);
                Point2D center = shape.isEmpty() ? new Point2D.Double(0,0) : shape.get(0); 
                
                junctions.put(id, new Junction(id, shape, center));
            }
        }

        initialized = true;
        System.out.println("Infrastructure Loaded: " + edges.size() + " edges, " + junctions.size() + " junctions.");
    }

    @Override
    public void refreshEdgeData() {
        // Iterate over all edges to fetch fresh vehicle counts
        for (IEdge edge : edges.values()) {
            try {
                int currentCount = simulationEngine.getEdgeVehicleCount(edge.getId());
                edge.setVehicleCount(currentCount);
            } catch (Exception e) {
                System.err.println("Error updating edge data: " + edge.getId());
            }
        }
    }

    @Override
    public List<IEdge> getAllEdges() {
        return new ArrayList<>(edges.values());
    }

    @Override
    public List<IJunction> getAllJunctions() {
        return new ArrayList<>(junctions.values());
    }
}