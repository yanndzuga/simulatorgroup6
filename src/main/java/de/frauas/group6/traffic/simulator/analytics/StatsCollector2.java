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

public class StatsCollector2 implements IStatsCollector {
	private final ConcurrentSkipListMap<Integer,Double> avgSpeedPerStep = new ConcurrentSkipListMap<>();
	private final Map<String, Double> enterTime = new HashMap<>();
	private final Map<String, Double> exitTime = new HashMap<>();
	private final Map<String, List<String>> routeEdges = new HashMap<>();
	private final Map<String, List<Double>> avgTravelTimeRouteList = new HashMap<>();
	private final Map<String, Double> avgTravelTimeRoute = new HashMap<>();
	private final Map<String, List<Double>> edgeDensityPerStepList = new HashMap<>();
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
	    // MOVING VEHICLES COUNT & AVERAGE SPEED
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
	    	int vehiclesOnEdge = simulationEngine.getEdgeVehicleCount(edgeId);
	    	// Length of the edge 
	    	double edgeLength = simulationEngine.getEdgeLength(edgeId);
	    	
	    	double densityThisStep = 0.0;
	    	if (edgeLength > 0) { densityThisStep = (double) vehiclesOnEdge / edgeLength; }
	    	// Store density per edge and per step
	    	edgeDensityPerStepList.computeIfAbsent(edgeId, k -> new ArrayList<>()).add(densityThisStep);
	    	
	    	// CONGESTION DETECTION

	    	int stoppedVehiclesOnEdge = 0;
	    	// Count stopped vehicles on this edge
	    	for (IVehicle v : vehicles) {
	    		if (edge.getId().equals(v.getEdgeId()) && v.getSpeed() <= 0.1) { stoppedVehiclesOnEdge++; }
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
 
 private void exportAverageSpeedInternal (PrintWriter writer, boolean forPdf) {
	
	//----------------------------------------
	// CSV export: output all simulation steps
	//----------------------------------------
	if (!forPdf) {
	    writer.println("Step,AverageSpeed(m/s)");
	    
	    // Iterate over all recorded steps 
	    for (Map.Entry<Integer, Double> e : avgSpeedPerStep.entrySet()) {
	    	// Example output: Step 1, 1.20m/s
	    	writer.println(e.getKey() + "," + String.format(Locale.US, "%.2f", e.getValue()));
	    }
	    return;
	}   
	
	//----------------------------
	// PDF export: summary only 
	//----------------------------
	writer.println("Average Speed Summary");
	
	// No data available
	if (avgSpeedPerStep.isEmpty()) { 
		writer.println("No speed data available"); 
		return;
	}
	
	double sum = 0.0;
	double min = Double.MAX_VALUE;
	double max = Double.MIN_VALUE;
	
	// Compute summary statistics based on per-step average speeds
	for (double speed : avgSpeedPerStep.values()) {
		sum += speed;
		min = Math.min(min, speed);
		max = Math.max(max, speed);
	}
	double avg = sum / avgSpeedPerStep.size();
	writer.println("Average speed (mean of step averages): " + avg);
	writer.println("Minimum average speed per step: " + String.format(Locale.US, "%.2f m/s", min));
	writer.println("Maximum average speed per step: " + String.format(Locale.US, "%.2f m/s", max));
}

 
	//=========================
	// AVERAGE TRAVEL TIME
	//=========================

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

 private void exportAverageTravelTimeInternal (PrintWriter writer, ExportFilter filter, boolean forPdf) {

	 if (!forPdf) {
	     writer.println("RouteId,RouteEdges,AverageTravelTime(s)");
	 } else { writer.println("Average Travel Time per Route"); }
	 
	 boolean anyRoutePrinted = false;
	 for (String routeId : routeEdges.keySet()) {
		 double avgTime = getAverageTravelTime(routeId);
		 
		// -----------------Filter-----------------
    	 if (filter != null && !filter.matchesRoute(routeId, avgTime)) { continue; }
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
 public double getEdgeDensity (String edgeId) {
	 if (currentStep == 0) return 0.0;
	 // Get the density history for the given edge
     List<Double> history = edgeDensityPerStepList.get(edgeId);
     if (history == null || history.isEmpty()) { return 0.0; }
     // Return the density value for the current simulation step
     // (currentStep starts at 1, List index starts at 0)
     return history.get(currentStep - 1);
 }

 private void exportEdgeDensityInternal (PrintWriter writer, ExportFilter filter, boolean forPdf) {
	 
	 if (forPdf) { writer.println("Edge Density Summary"); } 
	 else { writer.println("EdgeId,AverageDensity(veh/m)"); }
	 
	 boolean anyEdgePrinted = false;
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
		 // Check if this edge was ever congested
		 boolean congestedEver = congestionList.contains(edgeId);
		 
		 // ------------------Filter--------------------
		 if (filter != null && !filter.matchesEdge(edgeId, avgDensity, congestedEver)) { continue; }
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

 @Override
 public List<String> getCongestedEdgeIds() {
	 return new ArrayList<>(congestionList);
 }

 private void exportCongestedEdgesInternal (PrintWriter writer, ExportFilter filter, boolean forPdf) {
	 
	 writer.println("Congested Edges");
	 
	 boolean anyEdgePrinted = false;
	 for (String edgeId : congestionList) { 
		 //-------------Filter-----------------
		 if (filter != null && !filter.matchesCongestedEdge(edgeId)) { continue; }
		 anyEdgePrinted = true;
		 
		 if (forPdf) { writer.println("Edge " + edgeId + " is congested"); }
		 else { writer.println(edgeId); } 
	 }
	 if (forPdf && !anyEdgePrinted) { writer.println("No congested edges match the selected filters."); }
 }
 
 	//=========================
	// VEHICLE TRAVEL TIME
	//=========================
 
 private IVehicle findVehicleById (String vehicleId) {
    for (IVehicle v : vehicleManager.getAllVehicles()) {
        if (v.getId().equals(vehicleId)) {
            return v;
        }
    }
    return null;
}

 private void exportVehicleTravelTimesInternal (PrintWriter writer, ExportFilter filter, boolean forPdf) {

	if (forPdf) { writer.println("Vehicle Travel Times"); }
	else { writer.println("vehicleId,color,travelTime(s)"); }

	boolean anyVehiclePrinted = false;
    for (String vehicleId : exitTime.keySet()) {
        Double enter = enterTime.get(vehicleId);
        Double exit  = exitTime.get(vehicleId);

        if (enter == null || exit == null || exit <= enter) { continue; }
        double travelTime = exit - enter;
        IVehicle vehicle = findVehicleById(vehicleId);
        if (vehicle == null) { continue; }
        
        //--------------------FILTER---------------------
        if (filter != null && !filter.matchesVehicleColor(vehicle)) { continue; }
        anyVehiclePrinted = true;
        
        String color = vehicle.getColor();
        if (forPdf) { writer.println("Vehicle " + vehicleId + "  Color: " + color + "  Travel time: " + String.format(Locale.US, "%.2f s", travelTime)); }
        else { writer.println(vehicleId + "," + color + "," + String.format(Locale.US, "%.2f", travelTime)); }
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
	writer.println("Overall average speed (mean of step averages): " + String.format(Locale.US, "%.2f", overAllAvgSpeed) + "m/s");
	
	// --- Congestion information ---
	writer.println("Number of congested edges: " + congestionList.size());
}

	//=================
	// EXPORT TO CSV
	//=================
 
 @Override
 public void exportToCsv (String filepath, ExportType type, ExportFilter filter) {
     try (PrintWriter writer = new PrintWriter(filepath, "UTF-8")) {

         switch(type) {
        	 
        	 case AVG_SPEED: exportAverageSpeedInternal(writer, false);
        	 break;
        	 
        	 case AVG_TRAVEL_TIME: exportAverageTravelTimeInternal(writer, filter, false);
        	 break;
        	 
        	 case EDGE_DENSITY: exportEdgeDensityInternal(writer, filter, false);
        	 break;
        	 
        	 case CONGESTED_EDGES: exportCongestedEdgesInternal(writer, filter, false);
        	 break;
        	 
        	 case VEHICLE_TRAVEL_TIMES: exportVehicleTravelTimesInternal(writer, filter, false);
        	 break;
        	 
        	 case SUMMARY: 
        		 throw new UnsupportedOperationException("SUMMARY export is only supported for PDF, not for CSV.");
        	 
         }
     } catch (Exception e) {
    	 LOGGER.warning("StatsCollector: failed to export CSV: " + e.getMessage());
	}
 }

	//=================
	// EXPORT TO PDF
	//=================
 
 private String getDescriptionForType (ExportType type) {
	 
	 switch (type) {
	 
	 	case AVG_SPEED:
	 		return "This report shows the average vehicle speed computed as the mean of all per-step average speeds.";
	 	case AVG_TRAVEL_TIME:
	 		return "This report shows the average travel time per route based on vehicles that completed the route.";
	 	case EDGE_DENSITY:
	 		return "This report summarizes the average vehicle density per edge over the entire simulation.";
	 	case CONGESTED_EDGES:
	 		return "This report lists all edges that were congested at least once during the simulation.";
	 	case VEHICLE_TRAVEL_TIMES:
	 		return "This report shows the individual travel times of vehicles that completed their routes.";
	 	case SUMMARY:	
	 		return "This report provides a summary of the entire traffic simulation.";
	 		
	 	default:
	 		return "No description available for this report type.";
	 }
 }
 
 @Override
 public void exportToPdf (String filepath, ExportType type, ExportFilter filter) {

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
    	 
    	 	case AVG_SPEED: exportAverageSpeedInternal(pw, true);
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
    	 
    	 // Close the document
    	 document.close();

     } catch (Exception e) {
         LOGGER.warning("StatsCollector: failed to export PDF: " + e.getMessage());
     }
 }
 
}







