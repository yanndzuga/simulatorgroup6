package de.frauas.group6.traffic.simulator.infrastructure;

import de.frauas.group6.traffic.simulator.core.ISimulationEngine;
import java.awt.geom.Point2D;
import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class InfrastructureManager implements IInfrastructureManager {
    
    private static final Logger LOGGER = Logger.getLogger(InfrastructureManager.class.getName());
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

        LOGGER.info("Loading network infrastructure from SUMO...");

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
        LOGGER.info("Infrastructure Loaded: " + edges.size() + " edges, " + junctions.size() + " junctions.");
    }
    
    public List<String> loadRouteIds(String resourceName) {
        List<String> routeIds = new ArrayList<>();
        try {
            // Attempt to load the file from the classpath resources
            InputStream is = getClass().getClassLoader().getResourceAsStream(resourceName);
            if (is == null) {
                // Fallback for demo/testing purposes if file is missing
            	LOGGER.info("Resource not found: " + resourceName + ". Using mock routes.");
                for(int i=0; i<=5; i++) routeIds.add("route_" + i);
                return routeIds;
            }

            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(is);
            doc.getDocumentElement().normalize();

            // Look for <route> tags and extract 'id' attribute
            NodeList nList = doc.getElementsByTagName("route");
            for (int temp = 0; temp < nList.getLength(); temp++) {
                org.w3c.dom.Node nNode = nList.item(temp);
                if (nNode.getNodeType() == org.w3c.dom.Node.ELEMENT_NODE) {
                    Element eElement = (Element) nNode;
                    String id = eElement.getAttribute("id");
                    if (id != null && !id.isEmpty()) {
                        routeIds.add(id);
                    }
                }
            }
            Collections.sort(routeIds);
        } catch (Exception e) {
            e.printStackTrace();
            // Graceful degradation
            routeIds.add("Error Loading Routes");
        }
        return routeIds;
    }
    
    
    
    
    public Map<String, List<String>> loadRoutes(String resourceName) {
        Map<String, List<String>> routeEdges = new HashMap<>();
        try {
            // Attempt to load the file from the classpath resources 
            InputStream is = getClass().getClassLoader().getResourceAsStream(resourceName);
            
            if (is == null) {
                // Fallback or Error handling if file is missing
                LOGGER.severe("Error: Resource not found: " + resourceName);
                return routeEdges; // Return empty map
            }

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            // Parse the InputStream directly instead of a File object
            org.w3c.dom.Document doc = builder.parse(is);
            doc.getDocumentElement().normalize();

            NodeList routeNodes = doc.getElementsByTagName("route");

            for (int i = 0; i < routeNodes.getLength(); i++) {
                if (routeNodes.item(i).getNodeType() == org.w3c.dom.Node.ELEMENT_NODE) {
                    Element routeElement = (Element) routeNodes.item(i);
                    String routeId = routeElement.getAttribute("id");
                    String edgesAttr = routeElement.getAttribute("edges");
                    
                    if (routeId != null && edgesAttr != null) {
                        List<String> edges = List.of(edgesAttr.trim().split("\\s+"));
                        routeEdges.put(routeId, edges);
                    }
                }
            }
        } catch (Exception e) {
            // Using RuntimeException for unchecked propagation as in your original code, 
            // but wrapped for context.
            throw new RuntimeException("Failed to load routes from XML resource: " + resourceName, e);
        }
        return routeEdges;
    }
    
    
    
    
    
    
    
    

    @Override
    public void refreshEdgeData() {
    	loadNetwork();
        // Iterate over all edges to fetch fresh vehicle counts
        for (IEdge edge : edges.values()) {
            try {
                int currentCount = simulationEngine.getEdgeVehicleCount(edge.getId());
                edge.setVehicleCount(currentCount);
            } catch (Exception e) {
            	LOGGER.severe("Error updating edge data: " + edge.getId());
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