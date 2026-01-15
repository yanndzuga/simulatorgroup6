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
                System.out.println("Resource not found: " + resourceName + ". Using mock routes.");
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
    
    
    
    
    public Map<String, List<String>> loadRoutes (String filePath) { 
    	  try { // Create a File object pointing to the XML file 
    	   File xmlFile = new File(filePath); // Create a factory for building DOM parsers 
    	   Map<String, List<String>>  routeEdges= new HashMap<>(); 
    	   DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance(); // Create a DocumentBuilder from the factory 
    	   DocumentBuilder builder = factory.newDocumentBuilder(); // Parse the XML file into a DOM Document 
    	   org.w3c.dom.Document doc = builder.parse(xmlFile); // Get a List of all <route> elements in the XML 
    	   NodeList routeNodes = doc.getElementsByTagName("route"); // Loop over each <route> element 
    	   
    	   
    	   for (int i = 0; i < routeNodes.getLength(); i++) { // Cast the current node to an Element 
    	    Element routeElement = (Element) routeNodes.item(i); // Read the value of the "id" attribute 
    	    String routeId = routeElement.getAttribute("id"); // Read the value of the "edges" attribute 
    	    String edgesAttr = routeElement.getAttribute("edges"); 
    	    List<String> edges = List.of(edgesAttr.trim().split("\\s+")); 
    	    routeEdges.put(routeId, edges); } 
    	   return routeEdges;
    	   } catch (Exception e) { throw new RuntimeException("Failed to load routes from XML", e);
    	   }
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