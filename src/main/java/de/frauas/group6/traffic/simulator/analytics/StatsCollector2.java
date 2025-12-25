package de.frauas.group6.traffic.simulator.analytics;

import java.io.PrintWriter;
import java.util.*;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.logging.Logger;

import com.lowagie.text.Document;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfWriter;


import de.frauas.group6.traffic.simulator.core.ISimulationEngine;
import de.frauas.group6.traffic.simulator.vehicles.IVehicle;
import de.frauas.group6.traffic.simulator.vehicles.IVehicleManager;
import de.frauas.group6.traffic.simulator.infrastructure.IEdge;
import de.frauas.group6.traffic.simulator.infrastructure.IInfrastructureManager;

public class StatsCollector2 implements IStatsCollector {
	private final ConcurrentSkipListMap<Integer,Double> avgSpeedPerStep = new ConcurrentSkipListMap<>();
	private final List<Integer> vehicleCountPerStep = new ArrayList<>();
	private final Map<String, Double> enterTime = new HashMap<>();
	private final Map<String, Double> exitTime = new HashMap<>();
	private final Map<String, List<String>> routeEdges = new HashMap<>();
	private final Map<String, List<Double>> avgTravelTimeRouteList = new HashMap<>();
	private final Map<String, Double> avgTravelTimeRoute = new HashMap<>();
	private final Map<String, List<Double>> edgeDensityPerStepList = new HashMap<>();
	private final Map<String, Double> edgeDensityTotal = new HashMap<>();
	private final List<String> congestionList = new ArrayList<>();
	
	private void initRoutesManually() {
		routeEdges.put("R0", List.of("-E48", "-E47", "-E49"));
		routeEdges.put("R1", List.of("-E48", "-E45"));
		routeEdges.put("R2", List.of("E46", "-E47", "-E49"));
		routeEdges.put("R3", List.of("E46", "E48"));
		routeEdges.put("R4", List.of("E46", "E45"));
		routeEdges.put("R5", List.of("-E51", "-E49"));
		routeEdges.put("R6", List.of("-E51", "-E50"));
		routeEdges.put("R7", List.of("E50", "E47", "E48"));
		routeEdges.put("R8", List.of("E50", "E51"));
		routeEdges.put("R9", List.of("E45", "E48"));
		routeEdges.put("R10", List.of("E45", "-E46"));
		routeEdges.put("R11", List.of("E45", "-E47", "E49"));
		routeEdges.put("R12", List.of("E49", "E47", "E48"));
		routeEdges.put("R13", List.of("E49", "-E50"));
	}
	private void initAvgTravelTimeRouteList() {
	    for (String routeId : routeEdges.keySet()) {
	        avgTravelTimeRouteList.put(routeId, new ArrayList<>());
	        avgTravelTimeRoute.put(routeId, null);
	    }
	    
	}

	private final IVehicle ivehicle;
	private final IVehicleManager vehicleManager;
	private final IInfrastructureManager infrastructureManager;
	private final ISimulationEngine simulationEngine;
	public StatsCollector2( IVehicle ivehicle, IVehicleManager vehicleManager, IInfrastructureManager infrastructureManager, ISimulationEngine simulationEngine) {
	    this.ivehicle = ivehicle;
		this.vehicleManager = vehicleManager;
	    this.infrastructureManager = infrastructureManager;
	    this.simulationEngine = simulationEngine;
	    initRoutesManually();
	    initAvgTravelTimeRouteList();
	}
	
	private static final Logger LOGGER =Logger.getLogger(StatsCollector2.class.getName());
	private int currentStep = 0;
	private static int MIN_STOPPED_VEHICLES = 3;
	
public void collectData() {
	try {
		// STEP COUNTER
		// Increase the Simulation step counter (usually starts at 1)
	    currentStep++;
	    // Get all currently active vehicles
	    Collection<IVehicle> vehicles = vehicleManager.getAllVehicles();
	    // VEHICLE COUNT & AVERAGE SPEED
	    // Number of Vehicles in this Simulation step
	    int vehicleCountPerStep = vehicles.size();
	    double speedSum = 0.0;
	    int movingVehicleCount = 0;
	    
	    // Sum up speeds of moving vehicles only
	    for (IVehicle v : vehicles) {
	    	double speed = v.getSpeed();
	    	if (speed > 0.0) {
	    		speedSum += speed;
	    		movingVehicleCount++;
	    	}
	    }
	    
	    // Average speed of moving vehicles in this step
	    double avgSpeedThisStep = 0.0;
	    if (movingVehicleCount > 0) { avgSpeedThisStep = (double) speedSum / movingVehicleCount; }
	    // Store average speed per step
	    avgSpeedPerStep.put(currentStep, avgSpeedThisStep);
	    
	    // VEHICLE ENTER TIME DETECTION
	    // Store the time when a vehicle first appears in the simulation
	    for (IVehicle v : vehicles) {
	        enterTime.putIfAbsent(v.getId(), simulationEngine.getCurrentSimulationTime());
	    }
	    
	    // VEHICLE EXIT TIME DETECTION
	    // Check vehicles that were present before but are no Longer active
	    for (String vid : new ArrayList<>(enterTime.keySet())) {
	        boolean stillActive = false;
	        for (IVehicle v : vehicles) {
	            if (v.getId().equals(vid)) {
	                stillActive = true;
	                break;
	            }
	        }
	        // If the vehicle disappeared and no exit time was recorded yet
	        if (!stillActive && !exitTime.containsKey(vid)) { exitTime.put(vid, simulationEngine.getCurrentSimulationTime()); }
	    }

	    // EDGE DENSITY & CONGESTION DETECTION
	    for (IEdge edge : infrastructureManager.getAllEdges()) {
	    	String edgeId = edge.getId();
	    	
	    	// EDGE DENSITY
	    	
	    	// Number of vehicles on this edge in the current step
	    	int vehiclesOnEndge = simulationEngine.getEdgeVehicleCount(edgeId);
	    	// Length of the edge 
	    	double edgeLength = simulationEngine.getEdgeLength(edgeId);
	    	
	    	double densityThisStep = 0.0;
	    	if (edgeLength > 0) { densityThisStep = (double) vehiclesOnEndge / edgeLength; }
	    	// Store density per edge and per step
	    	edgeDensityPerStepList.computeIfAbsent(edgeId, k -> new ArrayList<>()).add(densityThisStep);
	    	
	    	// CONGESTION DETECTION

	    	int stoppedVehiclesOnEdge = 0;
	    	// Count stopped vehicles on this edge
	    	for (IVehicle v : vehicles) {
	    		if (edge.equals(v.getEdgeId()) && v.getSpeed() <= 0.1) { stoppedVehiclesOnEdge++; }
	    	}
	    	// Edge is considered congested if enough vehicles are stopped
	    	boolean congestedThisStep = stoppedVehiclesOnEdge >= MIN_STOPPED_VEHICLES;
	    	// Store the edge only once if it was ever congested
	    	if (congestedThisStep && !congestionList.contains(edgeId)) {
	    		congestionList.add(edgeId);
	    	}

	    }
	  
	} catch (Exception e) {
		LOGGER.warning("StatsCollector: error during collectData() at step " + currentStep + " : " + e.getMessage());
	}
	    
}

//-------------------------------------------------------------------------------------------------------------------------------

 @Override
 public double getAverageSpeed() {
	 if (currentStep <= 0) return 0.0;
	 // Return the average speed of the current step. If for some reason the step is missing, return 0.0
     return avgSpeedPerStep.getOrDefault(currentStep, 0.0);
 }

private void exportAverageSpeed(PrintWriter writer) {
	
    writer.println("=== Average Speed Per Step ===");
    
    // Iterate over all recorded steps 
    for (Map.Entry<Integer, Double> e : avgSpeedPerStep.entrySet()) {
    	// Example output: Step 1, 1.20m/s
    	writer.println("Step " + e.getKey() + ", " + String.format(Locale.US, "%.2f", e.getValue()) + "m/s");
    }
}

 @Override
 public List<Double> getSpeedHistory() {
	 if (currentStep == 0) return Collections.emptyList();
	 // Return a copy of all average speed values
     return new ArrayList<>(avgSpeedPerStep.values());
 }
 
//------------------------------------------------------------------------------------------------------------------------------- 

 // Flag to ensure that average travel times are calculated only once
 private boolean travelTimeCalculated = false;
 private void allAverageTravelTimesPerRoute() {
	if (travelTimeCalculated) return;
	// Step 1: Collect individual travel times per route
	for (String vid : exitTime.keySet()) {
		String routeId = ivehicle.getRouteId(vid);
	    Double enter = enterTime.get(vid);
	    Double exit = exitTime.get(vid);
	    if (enter != null && exit != null && exit > enter) {
	        double travelTime = (exit - enter);
	        avgTravelTimeRouteList.get(routeId).add(travelTime);
	    }
	}
	// Step 2: Compute average travel time per route
	for (String route : avgTravelTimeRouteList.keySet()) {
		List<Double> times = avgTravelTimeRouteList.get(route);
		if (times.isEmpty()) {
			avgTravelTimeRoute.put(route, 0.0);
			continue;
		}
		double sum = 0.0;
		for (double t : times) {
			sum += t;
		}
		double avg = (double) sum / times.size();
		avgTravelTimeRoute.put(route, avg);
	}
	// Mark calculations as completed
	travelTimeCalculated = true;
 }
 
 @Override
 public double getAverageTravelTime(String routeId) {	
	 // Ensure averages are computed before accessing them
	 allAverageTravelTimesPerRoute();
	 // Return average travel time for the given route or 0.0 if no value is available
     return avgTravelTimeRoute.getOrDefault(routeId, 0.0);
 }

 private void exportAverageTravelTime(PrintWriter writer, ExportFilter filter) {
	 
     writer.println("=== Average Travel Time per Route ===");
     
     for (String routeId : routeEdges.keySet()) {
    	 double avgTime = getAverageTravelTime(routeId);
    	 // -----------------Filter-----------------
    	 if (filter != null && !filter.matchesRoute(routeId, avgTime)) {
    		 continue;
    	 }
    	 List<String> edges = routeEdges.get(routeId);
    	 // e.g. (-E48 -E47 -E49)
    	 String edgesFormatted = "(" + String.join(" ", edges) + ")";
    	 
    	 // e.g. R0, (-E48 -E47 -E49), 6.40s
    	 writer.println(routeId + ", " + edgesFormatted + ", " + String.format(Locale.US, "%.2f", avgTime) + "s");
     }	
 }
 
//------------------------------------------------------------------------------------------------------------------------------- 
 
 @Override
 public double getEdgeDensity (String edgeId) {
	 if (currentStep == 0) return 0.0;
	 // Get the density history for the given edge
     List<Double> history = edgeDensityPerStepList.get(edgeId);
     if (history == null || history.isEmpty()) { return 0.0; }
     // Return the density value for the current simulation step
     // (currentStep starts at 1, List index starts at 0)
     return history.get(currentStep - 1);
 }

 private void exportEdgeDensity (PrintWriter writer, ExportFilter filter) {
	 
	 writer.println("=== EdgeId and TotalDensity ===");
	 
	 // Iterate over all edges that have density data
	 for (String edgeId : edgeDensityPerStepList.keySet()) {
		 // Density history for this edge (one value per step)
		 List<Double> history = edgeDensityPerStepList.get(edgeId);
		 // Skip edges without data
		 if (history == null || history.isEmpty()) { continue; } 
		 // Sum up density values over all steps
		 double sum = 0.0;
		 for (double density : history) { sum += density; }		 
		 double avgDensity = sum / history.size();
		 // was this Edge congested ?
		 boolean congestedEver = congestionList.contains(edgeId);
		 // ------------------Filter--------------------
		 if (filter != null && !filter.matchesEdge(edgeId, avgDensity, congestedEver)) {
			 continue;
		 }
		 
		 writer.println(edgeId + ", " + String.format(Locale.US, "%.2f", avgDensity) + "(veh/m)");
	 }	 
 }	 
 
//------------------------------------------------------------------------------------------------------------------------------- 

 @Override
 public List<String> getCongestedEdgeIds() {
	 return new ArrayList<>(congestionList);
 }

 private void exportCongestedEdges (PrintWriter writer, ExportFilter filter) {
	 
	 writer.println("=== CongestedEdgeIDs ===");
	 
	 for (String edgeId : congestionList) { 
		 //-------------Filter-----------------
		 if (filter != null) {
			 // onlyEdgeID
			 if (!filter.matchesCongestedEdge(edgeId)) continue;
		 }
		 
		 writer.println(edgeId); 
	 }
 }
 
//-------------------------------------------------------------------------------------------------------------------------------
 
private IVehicle findVehicleById(String vehicleId) {
    for (IVehicle v : vehicleManager.getAllVehicles()) {
        if (v.getId().equals(vehicleId)) {
            return v;
        }
    }
    return null;
}

private void exportVehicleTravelTimes(PrintWriter writer, ExportFilter filter) {

    writer.println("=== vehicleId, color, travelTime ===");

    for (String vehicleId : exitTime.keySet()) {
        Double enter = enterTime.get(vehicleId);
        Double exit  = exitTime.get(vehicleId);

        if (enter == null || exit == null || exit <= enter) { continue; }
        double travelTime = exit - enter;

        IVehicle vehicle = findVehicleById(vehicleId);
        if (vehicle == null) { continue; }
        //--------------------FILTER---------------------
        if (filter != null && !filter.matchesVehicleColor(vehicle)) { continue; }

        String color = vehicle.getColor();
        writer.println(vehicleId + ", " + color + ", " + String.format(Locale.US, "%.2f", travelTime));
    }
}

private void exportSummary (PrintWriter writer) {	// with no Filter
	
	writer.println("=== Simulation Summary ==)");
	
	// --- Basic simulation information ---
	int totalSteps = currentStep;
	int totalVehicles = enterTime.size();
	int totalEdges = infrastructureManager.getAllEdges().size();
	
	writer.println("Total simulation steps: " + totalSteps);
	writer.println("Total vehicles: " + totalVehicles);
	writer.println("Total edges: " + totalEdges);
	
	// --- Overall average speed calculation ---
	double speedSum = 0.0;
	int speedCount = 0;
	for (double s : avgSpeedPerStep.values()) {
		speedSum += s;
		speedCount++;
	}
	double overAllAvgSpeed = speedCount > 0 ? speedSum / speedCount : 0.0;
	writer.println("Overall average speed: " + String.format(Locale.US, "%.2f", overAllAvgSpeed) + "m/s");
	
	// --- Congestion information ---
	writer.println("Congested edges: " + congestionList.size());
}

 @Override
 public void exportToCsv (String filepath, ExportType type, ExportFilter filter) {
     try (PrintWriter writer = new PrintWriter(filepath, "UTF-8")) {

         switch(type) {
        	 
        	 case AVG_SPEED: exportAverageSpeed(writer);
        	 break;
        	 
        	 case AVG_TRAVEL_TIME: exportAverageTravelTime(writer, filter);
        	 break;
        	 
        	 case EDGE_DENSITY: exportEdgeDensity(writer, filter);
        	 break;
        	 
        	 case CONGESTED_EDGES: exportCongestedEdges(writer, filter);
        	 break;
        	 
        	 case VEHICLE_TRAVEL_TIMES: exportVehicleTravelTimes(writer, filter);
        	 break;
        	 
        	 case SUMMARY: exportSummary(writer);
        	 break;
        	 
         }
     } catch (Exception e) {
    	 LOGGER.warning("StatsCollector: failed to export CSV: " + e.getMessage());
	}
 }
 
 @Override
 public void exportTopdf (String filepath) {

     try {
    	 com.lowagie.text.Document document = new com.lowagie.text.Document();
    	 

         

     } catch (Exception e) {
         LOGGER.warning("StatsCollector: failed to export PDF: " + e.getMessage());
     }
 }
 
}







