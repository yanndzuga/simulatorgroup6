package de.frauas.group6.traffic.simulator.analytics;
// --- Java utility and concurrency classes (collections, maps, Logging) ---
import java.util.*;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.logging.Logger;
// --- Java I/O classes for CSV and in-memory text handling ---
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.FileOutputStream;
// --- PDF generation using OpenPDF ---
import com.lowagie.text.Document;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfWriter;
// --- Date and time utilities for report timestamps ---
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
// --- Simulation core and domain interfaces ---
import de.frauas.group6.traffic.simulator.core.ISimulationEngine;
import de.frauas.group6.traffic.simulator.vehicles.IVehicle;
import de.frauas.group6.traffic.simulator.vehicles.IVehicleManager;
import de.frauas.group6.traffic.simulator.infrastructure.IEdge;
import de.frauas.group6.traffic.simulator.infrastructure.IInfrastructureManager;
import de.frauas.group6.traffic.simulator.infrastructure.ITrafficLightManager;

public class StatsCollector implements IStatsCollector {
	
	private final Map<String, IVehicle> vehicleById = new HashMap<>();
	private final Map<String, List<Double>> speedPerVehiclePerStep = new HashMap<>();
	private final Map<String, List<String>> edgePerVehiclePerStep = new  HashMap<>();
	private final ConcurrentSkipListMap<Integer,Double> avgSpeedPerStep = new ConcurrentSkipListMap<>();
	private final Map<String, Double> enterTime = new HashMap<>();
	private final Map<String, Double> exitTime = new HashMap<>();
	private final Map<String, List<String>> routeEdges = new HashMap<>();
	private final Map<String, List<Double>> TravelTimeRouteList = new HashMap<>();
	private final Map<String, Double> avgTravelTimeRoute = new HashMap<>();
	private final Map<String, List<Double>> edgeDensityPerStepList = new HashMap<>();
	private final Map<String, Double> edgeDensity = new HashMap<>();
	private final Map<String, Integer> congestionList = new HashMap<>();
	private final Map<String, Integer> currentStepCongestion = new HashMap<>();
	
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
	        TravelTimeRouteList.put(routeId, new ArrayList<>());
	        avgTravelTimeRoute.put(routeId, 0.0);
	    }
	}

	private final IVehicleManager vehicleManager;
	private final IInfrastructureManager infrastructureManager;
	private final ITrafficLightManager trafficLightManager;
	private final ISimulationEngine simulationEngine;
	public StatsCollector(IVehicleManager vehicleManager, IInfrastructureManager infrastructureManager,ITrafficLightManager trafficLightManager, ISimulationEngine simulationEngine) {
		this.vehicleManager = vehicleManager;
	    this.infrastructureManager = infrastructureManager;
	    this.trafficLightManager = trafficLightManager;
	    this.simulationEngine = simulationEngine;
	    initRoutesManually();
	    initAvgTravelTimeRouteList();       
	}
	
	private void storeVehicleState (Collection<IVehicle> vehicles) {
		// Append the current speed of each vehicle to its speed history
	    for (IVehicle v : vehicles) {
	        speedPerVehiclePerStep.computeIfAbsent(v.getId(), id -> new ArrayList<>()).add(v.getSpeed());
	        edgePerVehiclePerStep.computeIfAbsent(v.getId(), id -> new ArrayList<>()).add(v.getEdgeId());
	    }
	}
	
	private void computeAverageSpeedForStep (int step, Collection<IVehicle> vehicles) {
	    double speedSum = 0.0;
	    int movingVehicleCount  = 0;
	    // Consider only moving vehicles
	    for (IVehicle v : vehicles) {
	        double speed = v.getSpeed();
	        if (speed > 0.0) {          
	        	speedSum += speed;
	        	movingVehicleCount++;
	        }
	    }
	    double avgSpeed = movingVehicleCount > 0 ? speedSum / movingVehicleCount : 0.0;
	    // Store average speed for this simulation step
	    avgSpeedPerStep.put(step, avgSpeed);
	}

	private void collectEdgeDensity() {
		
		for (IEdge edge : infrastructureManager.getAllEdges()) {
	    	String edgeId = edge.getId();
	    	// Number of vehicles on this edge in the current step
	    	int vehiclesOnEdge = simulationEngine.getEdgeVehicleCount(edgeId);
	    	// Length of the edge 
	    	double edgeLength = simulationEngine.getEdgeLength(edgeId);
	    	
	    	double densityThisStep = 0.0;
	    	if (edgeLength > 0) { densityThisStep = (double) vehiclesOnEdge / edgeLength; }
	    	// Store density per edge and per step
	    	edgeDensityPerStepList.computeIfAbsent(edgeId, k -> new ArrayList<>()).add(densityThisStep);
		}
	}
	
	

	// OPTIMISATION Yann  
    private void detectCongestion(Collection<IVehicle> vehicles) {
        // 1.Reset instant congestion for this step
        currentStepCongestion.clear();
        Map<String, Integer> stoppedCountPerEdge = new HashMap<>();

        // 2. A single loop on the vehicles
        for (IVehicle v : vehicles) {
            // If the vehicle is stationary and on a valid road
            if (v.getSpeed() <= 0.5 && v.getEdgeId() != null) {
                stoppedCountPerEdge.merge(v.getEdgeId(), 1, Integer::sum);
            }
        }

        // 3.Update the stats
        stoppedCountPerEdge.forEach((edgeId, count) -> {
            if (count >= MIN_STOPPED_VEHICLES) {
                //For the Real-Time Dashboard
                currentStepCongestion.put(edgeId, count);

                // For the Final Report (Historical Maximum)	
                int previousMax = congestionList.getOrDefault(edgeId, 0);
                if (count > previousMax) {
                    congestionList.put(edgeId, count);
                }
            }
        });
    }

    public Map<String, Integer> getCurrentCongestedEdgeIds() {
        return new HashMap<>(currentStepCongestion);
    }
	
	private static final Logger LOGGER =Logger.getLogger(StatsCollector.class.getName());
	private int currentStep = 0;
	private static int MIN_STOPPED_VEHICLES = 3;

	//===========================
	// COLLECT DATA PER STEP
	//===========================
	
public void collectData() {
	try {
		// STEP COUNTER
		// Increase the Simulation step counter (usually starts at 1)
	    currentStep++;
	    // Get all currently active vehicles
	    Collection<IVehicle> vehicles = vehicleManager.getAllVehicles();
	    
	    // VEHICLE ENTER TIME DETECTION
	    // Store the time when a vehicle first appears in the simulation
	    for (IVehicle v : vehicles) {
	        enterTime.putIfAbsent(v.getId(), simulationEngine.getCurrentSimulationTime());
	        vehicleById.put(v.getId(), v);
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

	    // Store raw speed values per vehicle
	    storeVehicleState(vehicles);
	    // Compute and store average speed for this step
	    computeAverageSpeedForStep(currentStep, vehicles);
	    // Compute and store edge density per step
	    collectEdgeDensity();
	    // Detect congested edges for this step
	    detectCongestion(vehicles);
	    
	    allAverageTravelTimesPerRoute();
	  
	} catch (Exception e) {
		LOGGER.warning("StatsCollector: error during collectData() at step " + currentStep + " : " + e.getMessage());
	}
	    
}

	//===========================
	// AVERAGE SPEED
	//===========================

 @Override
 public double getAverageSpeed() {
	 if (currentStep <= 0) return 0.0;
	 // Return the average speed of the current step. If for some reason the step is missing, return 0.0
     return avgSpeedPerStep.getOrDefault(currentStep, 0.0);
 }

 @Override
 public List<Double> getSpeedHistory() {
	 if (currentStep == 0) return Collections.emptyList();
	 // Return a copy of all average speed values
     return new ArrayList<>(avgSpeedPerStep.values());
 }
 
 private void exportAverageSpeedInternal(PrintWriter writer, ExportFilter filter, boolean forPdf) {

	    // --- Relevant filters for speed ---
	    boolean filterByColor = filter != null && filter.hasVehicleColorFilter();
	    boolean filterByEdge = filter != null && filter.hasOnlyEdgeIdFilter();

	    // --- Header / title ---
	    if (!forPdf) { writer.println("EdgeId,AverageSpeed(m/s)"); }
	    else { writer.println("Average Speed per Edge"); }

	    // --- Iterate over all edges ---
	    for (IEdge edge : infrastructureManager.getAllEdges()) {
	        String edgeId = edge.getId();

	        // If a specific edge is requested, skip others
	        if (filterByEdge && !edgeId.equals(filter.getOnlyEdgeId())) { continue; }

	        double sum = 0.0;
	        int count = 0;
	        // --- Iterate over all vehicles ---
	        for (String vehicleId : speedPerVehiclePerStep.keySet()) {
	            IVehicle vehicle = vehicleById.get(vehicleId);
	            if (vehicle == null) continue;
	            // Apply vehicle color filter (if any)
	            if (filterByColor && !vehicle.getColor().equals(filter.getVehicleColor())) { continue; }

	            List<Double> speeds = speedPerVehiclePerStep.get(vehicleId);
	            List<String> edges  = edgePerVehiclePerStep.get(vehicleId);

	            if (speeds == null || edges == null) continue;
	            // Iterate only while both lists have values
	            int steps = Math.min(speeds.size(), edges.size());
	            // --- Accumulate speeds for this edge ---
	            for (int i = 0; i < steps; i++) {
	                if (!edgeId.equals(edges.get(i))) continue;
	                double speed = speeds.get(i);
	                if (speed > 0.0) {
	                    sum += speed;
	                    count++;
	                }
	            }
	        }

	        double avgSpeed = count > 0 ? sum / count : 0.0;
	        // --- Output ---
	        if (!forPdf) {writer.println(edgeId + "," + String.format(Locale.US, "%.2f", avgSpeed));
	        } else { writer.println("Edge " + edgeId +" average speed: " + String.format(Locale.US, "%.2f m/s", avgSpeed)); }
	    }
	}


 
	//===============================
	// AVERAGE TRAVEL TIME PER ROUTE
	//===============================

 // Flag to ensure that average travel times are calculated only once
 //private boolean travelTimeCalculated = false;
 private void allAverageTravelTimesPerRoute() {
	//if (travelTimeCalculated) return;
	// Step 1: Collect individual travel times per route
	for (String vid : exitTime.keySet()) {
		IVehicle vehicle = vehicleById.get(vid);
		if (vehicle == null) continue;
		String routeId = vehicle.getRouteId();
		
	    Double enter = enterTime.get(vid);
	    Double exit = exitTime.get(vid);
	    if (enter != null && exit != null && exit > enter) {
	        double travelTime = (exit - enter);
	        TravelTimeRouteList.get(routeId).add(travelTime);
	    }
	}
	// Step 2: Compute average travel time per route
	for (String route : TravelTimeRouteList.keySet()) {
		List<Double> times = TravelTimeRouteList.get(route);
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
	//travelTimeCalculated = true;
 }
 
 @Override
 public Map<String, Double> getAverageTravelTime() {	
	 // Ensure averages are computed before accessing them
	 allAverageTravelTimesPerRoute();
	 // Return average travel time per route
     return new HashMap<>(avgTravelTimeRoute);
 }

 private void exportAverageTravelTimeInternal (PrintWriter writer, ExportFilter filter, boolean forPdf) {
	 
	 allAverageTravelTimesPerRoute();
	 if (!forPdf) {
	     writer.println("RouteId,RouteEdges,AverageTravelTime(s)");
	 } else { writer.println("Average Travel Time per Route"); }
	 
	 boolean filterByRouteId = filter != null && filter.hasOnlyRouteIdFilter();
	 boolean filterByMinAvgTime = filter != null && filter.hasMinAverageTravelTimeFilter();
	 boolean anyRoutePrinted = false;
	 for (String routeId : routeEdges.keySet()) {
		 double avgTime = avgTravelTimeRoute.get(routeId);
		 
		// -----------------Filter-----------------
    	 if (filterByRouteId && !routeId.equals(filter.getOnlyRouteId())) { continue; }
    	 if (filterByMinAvgTime && avgTime < filter.getMinAverageTravelTime()) { continue; }
    	 anyRoutePrinted = true;
    	 List<String> edges = routeEdges.get(routeId);
    	 String edgesFormatted = "(" + String.join(" ", edges) + ")";
    	 
    	 if (!forPdf) { writer.println(routeId + "," + edgesFormatted + "," +  String.format(Locale.US, "%.2f", avgTime)); }
    	 else { writer.println("Route " + routeId + "  " + edgesFormatted + "  Avg travel time: " + String.format(Locale.US, "%.2f s", avgTime)); }
    	 
	 }
	 if (forPdf && !anyRoutePrinted) { writer.println("No routes match the selected filters."); }
}
	 
 	//=================
	// EDGE DENSITY
	//=================
 
 @Override
 public Map<String, Double> getEdgeDensity () {
	 
	 edgeDensity.clear();
	 // Get the density history for each edge
     for (String edgeId : edgeDensityPerStepList.keySet()) {
    	 List<Double> history = edgeDensityPerStepList.get(edgeId);
    	 if (history == null || history.isEmpty()) {
    		 edgeDensity.put(edgeId, 0.0);
    		 continue;
    	 }
    	 double sum = 0.0;
    	 for (double density : history) { sum += density; }
    	 double edgeDen = sum / history.size();
    	 edgeDensity.put(edgeId, edgeDen);
     }
     
     return new HashMap<>(edgeDensity);
 }

 private void exportEdgeDensityInternal (PrintWriter writer, ExportFilter filter, boolean forPdf) {
	 
	 if (forPdf) { writer.println("Edge Density Summary"); } 
	 else { writer.println("EdgeId,AverageDensity(veh/m)"); }
	 
	 boolean filterByEdgeId = filter != null && filter.hasOnlyEdgeIdFilter();
	 boolean filterByMinDensity = filter != null && filter.hasMinEdgeDensityFilter();
	 boolean filterByCongested = filter != null && filter.hasOnlyCongestedEdgesFilter();
	 
	 // Use the precomputed average edge density
	 Map<String, Double> avgEdgeDensity = getEdgeDensity();
	 
	 boolean anyEdgePrinted = false;
	 for (Map.Entry<String, Double> entry : avgEdgeDensity.entrySet()) {
		 String edgeId = entry.getKey();
		 double avgDensity = entry.getValue();
		 
		 // onlyEdgeId filter
		 if (filterByEdgeId && !edgeId.equals(filter.getOnlyEdgeId())) { continue; }
		 // minEdgeDensity filter
		 if (filterByMinDensity && avgDensity < filter.getMinEdgeDensity()) { continue; }
		 
		 boolean congestedEver = congestionList.containsKey(edgeId);
		 // congestion filter
		 if (filterByCongested && !congestedEver) { continue; }
		 anyEdgePrinted = true;
		 if(forPdf) { 
			 writer.println("Edge " + edgeId + "  Average density: " + String.format(Locale.US, "%.2f veh/m", avgDensity) + (congestedEver ? " (congested)" : "")); }
		 else {writer.println(edgeId + "," + String.format(Locale.US, "%.2f", avgDensity)); }
	 }	 
	 if (forPdf && !anyEdgePrinted) { writer.println("No edges match the selected filters."); }
 }	 
 
 	//====================
	// CONGESTED EDGES
	//==================== 

 @Override// Map with edgeId and count of Vehicles 
 public Map<String, Integer> getCongestedEdgeIds() {
	 return new HashMap<>(congestionList);
 }

 private void exportCongestedEdgesInternal (PrintWriter writer, ExportFilter filter, boolean forPdf) {
	 
	 writer.println(forPdf ? "Congested Edges" : "EdgeId,StoppedVehicles");
	 
	 boolean filterByEdgeId = filter != null && filter.hasOnlyEdgeIdFilter();
	 boolean filterByCongested = filter != null && filter.hasOnlyCongestedEdgesFilter();
	 
	 boolean anyEdgePrinted = false;
	 for (Map.Entry<String, Integer> entry : congestionList.entrySet()) {
		 String edgeId = entry.getKey();
		 int stoppedVehicles = entry.getValue();
		 
		 // onlyEdgeId filter
		 if (filterByEdgeId && !edgeId.equals(filter.getOnlyEdgeId())) { continue; }
		 // onlyCongestedEdges
		 if (filterByCongested && stoppedVehicles < MIN_STOPPED_VEHICLES) { continue; }
		 anyEdgePrinted = true;
		 
		 if (forPdf) { writer.println("Edge " + edgeId + " is congested (stopped vehicles: " + stoppedVehicles + ")"); }
		 else { writer.println(edgeId + "," + stoppedVehicles); }
	 }
	 if (forPdf && !anyEdgePrinted) { writer.println("No congested edges match the selected filters."); }
 }
 
 	//=========================
	// VEHICLE TRAVEL TIME
	//=========================

 private void exportVehicleTravelTimesInternal (PrintWriter writer, ExportFilter filter, boolean forPdf) {

	if (forPdf) { writer.println("Vehicle Travel Times"); }
	else { writer.println("vehicleId,color,routeId,travelTime(s)"); }

	boolean filterByColor = filter != null && filter.hasVehicleColorFilter();
    boolean filterByRoute =filter != null && filter.hasOnlyRouteIdFilter();
	
    boolean anyVehiclePrinted = false;
    for (String vehicleId : exitTime.keySet()) {
        Double enter = enterTime.get(vehicleId);
        Double exit  = exitTime.get(vehicleId);

        if (enter == null || exit == null || exit <= enter) { continue; }
        double travelTime = exit - enter;
        
        IVehicle vehicle = vehicleById.get(vehicleId);
        if (vehicle == null) { continue; }
        
        //--------------------FILTERS------------------
        // vehicleColor filter
        if (filterByColor && !vehicle.getColor().equals(filter.getVehicleColor())) { continue; }
        // onlyRouteId filter
        if (filterByRoute && !vehicle.getRouteId().equals(filter.getOnlyRouteId())) { continue; }
        anyVehiclePrinted = true;

        if (forPdf) { writer.println("Vehicle " + vehicleId + " Color: " + vehicle.getColor() + " Route: " + vehicle.getRouteId() + " Travel time: " + String.format(Locale.US, "%.2f s", travelTime)); }
        else { writer.println(vehicleId + "," + vehicle.getColor() + "," + vehicle.getRouteId() + "," + String.format(Locale.US, "%.2f", travelTime)); }
    }
    if (forPdf && !anyVehiclePrinted) { writer.println("No vehicles match the selected filters."); } 
}

	//=============
	// SUMMARY
	//=============

 private void exportSummaryInternal (PrintWriter writer, boolean forPdf) {	// with no Filter
	
	writer.println("Simulation Summary");
	
	// --- Basic simulation information ---
	int totalSteps = currentStep;
	int totalVehicles = enterTime.size();
	int totalEdges = infrastructureManager != null ? infrastructureManager.getAllEdges().size() : 0;
	int totalTrafficLights = trafficLightManager != null ? trafficLightManager.getAllTrafficLights().size() : 0;
	
	writer.println("Total simulation steps: " + totalSteps);
	writer.println("Total vehicles: " + totalVehicles);
	writer.println("Total edges: " + totalEdges);
	writer.println("Total trafficLights: " + totalTrafficLights);

}

	//=================
	// EXPORT TO CSV
	//=================
 
 @Override
 public void exportToCsv (String filepath, ExportFilter filter, List<ExportType> types) {
	 
     try (PrintWriter writer = new PrintWriter(filepath, "UTF-8")) {
    	 
    	 for (ExportType type : types) {
    		 
    		 writer.println("# " + type);
	         
    		 switch(type) {
	        	 
	        	 case AVG_SPEED: exportAverageSpeedInternal(writer,filter, false);
	        	 break;
	        	 
	        	 case AVG_TRAVEL_TIME: exportAverageTravelTimeInternal(writer, filter, false);
	        	 break;
	        	 
	        	 case EDGE_DENSITY: exportEdgeDensityInternal(writer, filter, false);
	        	 break;
	        	 
	        	 case CONGESTED_EDGES: exportCongestedEdgesInternal(writer, filter, false);
	        	 break;
	        	 
	        	 case VEHICLE_TRAVEL_TIMES: exportVehicleTravelTimesInternal(writer, filter, false);
	        	 break;
	        	 
	        	 case SUMMARY: exportSummaryInternal(writer, false);
	        	 break;
	        	 
	         }
	         writer.println();	// empty Line between sections
    	 }
    	 
         
         
     } catch (Exception e) {
    	 LOGGER.warning("StatsCollector: failed to export CSV: " + e.getMessage());
	}
 }

	//=================
	// EXPORT TO PDF
	//=================
 
 private String getDescriptionForType(ExportType type) {

	    switch (type) {

	        case AVG_SPEED:
	            return "This report shows the average vehicle speed per edge, computed over the entire simulation. Results may be filtered by vehicle color and/or edge.";

	        case AVG_TRAVEL_TIME:
	            return "This report shows the average travel time per route, calculated from vehicles that completed their routes.";

	        case EDGE_DENSITY:
	            return "This report summarizes the average vehicle density per edge over the entire simulation. Results may be filtered by edge, congestion state, or minimum density.";

	        case CONGESTED_EDGES:
	            return "This report lists all edges that were identified as congested during the simulation, including the maximum number of stopped vehicles observed.";

	        case VEHICLE_TRAVEL_TIMES:
	            return "This report shows the individual travel times of vehicles that completed their routes. Results may be filtered by vehicle color and route.";

	        case SUMMARY:
	            return "This report provides an overall summary of the traffic simulation, including global statistics such as simulation steps, vehicles, edges, and traffic lights.";

	        default:
	            return "No description available for this report type.";
	    }
	}

 
 @Override
 public void exportToPdf (String filepath, ExportFilter filter, List<ExportType> types) {

     try {
    	 
    	 // Create the PDF document
    	 Document document = new Document();
    	 // Bind the document to a file
    	 PdfWriter.getInstance(document, new FileOutputStream(filepath));
    	 // Open the document
    	 document.open();
    	 
    	 // Formatter for a readable timestamp in the PDF report
    	 DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    	 // Timestamp representing the report generation time
    	 String timestamp = LocalDateTime.now().format(formatter);
         
    	 // --- Report title ---
    	 document.add(new Paragraph("Traffic Simulation Statistics Report"));
    	 // --- Report metadata: creation time ---
    	 document.add(new Paragraph("Generated on: " + timestamp));
    	 document.add(new Paragraph(" "));
    	 
    	 for (ExportType type : types) {
	    	 // Which Export Type
	    	 document.add(new Paragraph("Export Type: " + type));
	    	 // Type description
	    	 document.add(new Paragraph(getDescriptionForType(type)));
	    	 
	    	 // --- Applied Filters section ---
	    	 document.add(new Paragraph("Applied Filters"));
	    	 if (filter != null) {
	    		 document.add(new Paragraph(filter.toString()));
	    	 } else { document.add(new Paragraph("No filters applied")); }
	    	 
	    	 document.add(new Paragraph(" "));
	    	 document.add(new Paragraph("Filtered Results"));
	    	 document.add(new Paragraph(" "));
	    	 
	    	 // Collect output using existing export methods
	    	 StringWriter sw = new StringWriter();
	    	 PrintWriter pw = new PrintWriter(sw);
	    	 
	    	 switch(type) {
	    	 
	    	 	case AVG_SPEED: exportAverageSpeedInternal(pw, filter, true);
	    	 	break;
	    	 
	    	 	case AVG_TRAVEL_TIME: exportAverageTravelTimeInternal(pw, filter, true);
	    	 	break;
	    	 
	    	 	case EDGE_DENSITY: exportEdgeDensityInternal(pw, filter, true);
	    	 	break;
	    	 
	    	 	case CONGESTED_EDGES: exportCongestedEdgesInternal(pw, filter, true);
	    	 	break;
	    	 
	    	 	case VEHICLE_TRAVEL_TIMES: exportVehicleTravelTimesInternal(pw, filter, true);
	    	 	break;
	    	 
	    	 	case SUMMARY: exportSummaryInternal(pw, true);
	    	 	break;
	    	  
	    	 }
	    	 
	    	 pw.flush();
	    	 
	    	 // Write lines into PDF
	    	 for (String line : sw.toString().split("\n")) { document.add(new Paragraph(line)); }
	    	 document.add(new Paragraph(" "));
    	 }
    	 // Close the document
    	 document.close();

     } catch (Exception e) {
         LOGGER.warning("StatsCollector: failed to export PDF: " + e.getMessage());
     }
 }
 
}