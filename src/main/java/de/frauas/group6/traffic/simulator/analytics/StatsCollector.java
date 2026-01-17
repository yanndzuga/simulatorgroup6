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
// --- Chart generation and PDF image support (JFreeChart + OpenPDF) ---
import com.lowagie.text.Image;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.NumberTickUnit;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.chart.plot.PlotOrientation;
// --- Java image rendering and in-memory I/O ---
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.ByteArrayOutputStream;
// --- Number formatting for chart axes ---
import java.text.DecimalFormat;
import org.jfree.chart.renderer.category.BarRenderer;
import java.awt.Color;
import org.jfree.chart.renderer.category.StandardBarPainter;
import java.awt.BasicStroke;
// --- PDF Table ---
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.Phrase;
import com.lowagie.text.Font;
// --- Simulation core and domain interfaces ---
import de.frauas.group6.traffic.simulator.core.ISimulationEngine;
import de.frauas.group6.traffic.simulator.vehicles.IVehicle;
import de.frauas.group6.traffic.simulator.vehicles.IVehicleManager;
import de.frauas.group6.traffic.simulator.infrastructure.IEdge;
import de.frauas.group6.traffic.simulator.infrastructure.IInfrastructureManager;

public class StatsCollector implements IStatsCollector {
	
	private final IVehicleManager vehicleManager;
	private final IInfrastructureManager infrastructureManager;
	private final ISimulationEngine simulationEngine;
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
	
//	private void initRoutesFromInfrastructure(String resourceName) {
//		// Routes, Edges
//	   Map<String, List<String>> routes = infrastructureManager.loadRoutes(resourceName);
//	   routeEdges.putAll(routes);
//	}

	private void initRoutesFromXml(String filePath) { 
		LOGGER.info("Loading routes from XML: " + filePath);
		try { // Create a File object pointing to the XML file 
			File xmlFile = new File(filePath); // Create a factory for building DOM parsers 
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
			LOGGER.info("Loaded " + routeEdges.size() + " routes");
			} catch (Exception e) { throw new AnalyticsException("Failed to load routes from XML file" + filePath, e);
			}
		}
	private void initAvgTravelTimeRouteList() {
	    for (String routeId : routeEdges.keySet()) {
	        TravelTimeRouteList.put(routeId, new ArrayList<>());
	        avgTravelTimeRoute.put(routeId, 0.0);
	    }
	}

	
	public StatsCollector(IVehicleManager vehicleManager, IInfrastructureManager infrastructureManager, ISimulationEngine simulationEngine) {
		this.vehicleManager = vehicleManager;
	    this.infrastructureManager = infrastructureManager;
	    this.simulationEngine = simulationEngine;
	   // initRoutesFromInfrastructure("minimal.rou.xml");
	    initRoutesFromXml("src/main/resources/minimal.rou.xml");
	    initAvgTravelTimeRouteList();  
	    LOGGER.info("StatsCollector initialized with " + routeEdges.size() + " routes");
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

	// Identifies congested edges by counting stopped vehicles per edge
	// and updates current and historical congestion statistics.
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
            	LOGGER.fine("Congestion detected on edge " + edgeId + " with " + count + " stopped vehicles");
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
	private static final int MIN_STOPPED_VEHICLES = 3;

	//===========================
	// COLLECT DATA PER STEP
	//===========================
	
public void collectData() {
	LOGGER.fine("Collecting data for simulation step " + simulationEngine.getCurrentSimulationTime());
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
	  
	} catch (Exception e) {
		throw new AnalyticsException("Error collecting statistics at simulation step" + currentStep, e);
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
 
 private Map<String, Double> calculateAverageSpeedPerEdge (ExportFilter filter) {
	 
	 Map<String, Double> result = new LinkedHashMap<>();
	 // --- Relevant filters for speed ---
	 boolean filterByColor = filter != null && filter.hasVehicleColorFilter();
	 boolean filterByEdge = filter != null && filter.hasOnlyEdgeIdFilter();
	 boolean filterByCongested = filter != null && filter.hasOnlyCongestedEdgesFilter();
	// --- Iterate over all edges ---
    for (IEdge edge : infrastructureManager.getAllEdges()) {
        String edgeId = edge.getId();

        // If a specific edge is requested, skip others
        if (filterByEdge && !edgeId.equals(filter.getOnlyEdgeId())) continue;
        if (filterByCongested && !congestionList.containsKey(edgeId)) continue;
        
        double sum = 0.0;
        int count = 0;
        // --- Iterate over all vehicles ---
        for (String vehicleId : speedPerVehiclePerStep.keySet()) {
            IVehicle vehicle = vehicleById.get(vehicleId);
            if (vehicle == null) continue;
         
            // Apply vehicle color filter (if any)
            if (filterByColor && !vehicle.getColor().trim().equalsIgnoreCase(filter.getVehicleColor().trim())) { continue; }

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
	        result.put(edgeId, avgSpeed);
     }
    	return result;
 }
 
 private void exportAverageSpeedInternal(PrintWriter writer, Map<String, Double> data, boolean forPdf) {
	// --- Header / title ---
	if (!forPdf) { writer.println("EdgeId,AverageSpeed(m/s)"); }
	else { writer.println("Average Speed per Edge"); }
	
	data.forEach((edgeId,avgSpeed) -> {
	    // --- Output ---
	    if (!forPdf) {writer.println(edgeId + "," + String.format(Locale.US, "%.2f", avgSpeed));
	    } else if (avgSpeed != 0){ writer.println("Edge " + edgeId +" average speed: " + String.format(Locale.US, "%.2f m/s", avgSpeed)); }
	});
}
 
//Builds a bar chart image for the average speed per edge.
//The data is assumed to be already calculated and filtered.
 private Image buildAverageSpeedBarChart(Map<String, Double> data) {
	 if (data == null || data.isEmpty()) {
		 throw new AnalyticsException("No data available for average speed chart");
	 }
	 try {
     // Create dataset for a bar chart (category-based)
     DefaultCategoryDataset dataset = new DefaultCategoryDataset();

     // Fill dataset: edgeId -> avgSpeed
     data.forEach((edgeId, avgSpeed) -> dataset.addValue(avgSpeed, "Average Speed", edgeId));

     // Create the bar chart
     JFreeChart chart = ChartFactory.createBarChart(
             "Average Speed per Edge",     // chart title
             "Edge ID",                    // X-axis label
             "Average Speed (m/s)",        // Y-axis label
             dataset
     );

     // Access the plot area (contains axes and bars)
     CategoryPlot plot = chart.getCategoryPlot();
     // Configure Y-axis (range axis)
     NumberAxis yAxis = (NumberAxis) plot.getRangeAxis();
     // Set lower bound to zero (speed cannot be negative)
     yAxis.setLowerBound(0.0);
     yAxis.setMinorTickMarksVisible(true);
	 yAxis.setMinorTickCount(5);
	 plot.setRangeMinorGridlinesVisible(true);
	 plot.setRangeMinorGridlinePaint(new Color(220, 220, 220));
	 plot.setRangeGridlinePaint(Color.WHITE);
	 plot.setRangeGridlineStroke(new BasicStroke(1f));
     // Define tick interval: 0.0, 1.0, 2.0, 3.0...
     yAxis.setTickUnit(new NumberTickUnit(1.0));
     // Format tick labels to always show one decimal place
     yAxis.setNumberFormatOverride(new DecimalFormat("0.0"));
     // Render chart to an in-memory image
     BufferedImage image = chart.createBufferedImage(550,380);
     // Convert BufferedImage to PNG stored in memory
     ByteArrayOutputStream baos = new ByteArrayOutputStream();
     ImageIO.write(image, "png", baos);

     // Convert PNG bytes to OpenPDF Image and return
     return Image.getInstance(baos.toByteArray());
	 } catch (Exception e) {
		 throw new AnalyticsException("Failed to build average speed bar chart", e);
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
		if (routeId == null) {
			LOGGER.warning("Vehicle '" + vid + "' has no routeId assigned - skipped");
			continue;
		}
		List<Double> list = TravelTimeRouteList.get(routeId);
	    Double enter = enterTime.get(vid);
	    Double exit = exitTime.get(vid);
	    if (enter != null && exit != null && exit > enter) {
	        double travelTime = (exit - enter);
	        if (list == null) { throw new AnalyticsException("Vehicle '" + vid + "' references unknown routeId '" + routeId + "'"); }
	        list.add(travelTime);
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
 private Map<String, Double> calculateAverageTravelTimePerRoute(ExportFilter filter) {
	 allAverageTravelTimesPerRoute();
	 Map<String, Double> result = new LinkedHashMap<>();
	 
	 boolean filterByRouteId = filter != null && filter.hasOnlyRouteIdFilter();
	 boolean filterByMinAvgTime = filter != null && filter.hasMinAverageTravelTimeFilter();
	 for (String routeId : routeEdges.keySet()) {
		 double avgTime = avgTravelTimeRoute.get(routeId);
		 
		// -----------------Filter-----------------
    	 if (filterByRouteId && !routeId.equals(filter.getOnlyRouteId())) { continue; }
    	 if (filterByMinAvgTime && avgTime < filter.getMinAverageTravelTime()) { continue; }
    	 
    	 result.put(routeId, avgTime);
	 }
	 return result;
 }
 // Returns a rounded upper bound for chart axes to avoid awkward max values like 17.3 or 22.7...
 private double calculateNiceUpperBound(double maxValue) {

	    if (maxValue <= 10) return 10;
	    if (maxValue <= 20) return 20;
	    if (maxValue <= 30) return 30;
	    if (maxValue <= 40) return 40;
	    if (maxValue <= 50) return 50;
	    if (maxValue <= 60) return 60;
	    if (maxValue <= 70) return 70;
	    if (maxValue <= 80) return 80;
	    if (maxValue <= 90) return 90;

	    return Math.ceil(maxValue / 10.0) * 10;
 }
 private void exportAverageTravelTimeInternal (PrintWriter writer,  Map<String, Double> data, boolean forPdf) {
	 
	 if (!forPdf) {
	     writer.println("RouteId,RouteEdges,AverageTravelTime(s)");
	 } else { writer.println("Average Travel Time per Route"); }
	 
	 boolean anyRoutePrinted = false;
	 for (Map.Entry<String, Double> entry : data.entrySet()) {
		 String routeId = entry.getKey();
		 double avgTime = entry.getValue();
		 
    	 anyRoutePrinted = true;
    	 List<String> edges = routeEdges.get(routeId);
    	 String edgesFormatted = "(" + String.join(" ", edges) + ")";
    	 
    	 if (!forPdf) { writer.println(routeId + "," + edgesFormatted + "," +  String.format(Locale.US, "%.2f", avgTime)); }
    	 else if (avgTime != 0){ writer.println("Route " + routeId + "  " + edgesFormatted + "  Avg travel time: " + String.format(Locale.US, "%.2f s", avgTime)); }
    	 
	 }
	 if (forPdf && !anyRoutePrinted) { writer.println("No routes match the selected filters."); }
}
	
 private Image buildAverageTravelTimeHorizontalBarChart(Map<String, Double> data) {
	 if (data == null || data.isEmpty()) {
		 throw new AnalyticsException("No data available for average travel time chart");
	 }
	 try {
	 // Create dataset for horizontal bar chart
	 DefaultCategoryDataset dataset = new DefaultCategoryDataset();
	 data.forEach((routeId, avgTime) -> {
		// Build label: R0 (E45 E48 E49)
		 List<String> edges = routeEdges.get(routeId);
		 String label = routeId + " (" + String.join(" ", edges) + ")";
		 dataset.addValue(avgTime, "Average Travel Time", label);
	 });
	 
	// Create horizontal bar chart
	 JFreeChart chart = ChartFactory.createBarChart(
	            "Average Travel Time per Route",   // chart title
	            "Route",         // X-axis label
	            "Average Travel Time (s)",                           // Y-axis label
	            dataset,
	            PlotOrientation.HORIZONTAL,        // horizontal bars
	            false,                             // legend
	            true,
	            false
	    );
	// Access plot
	 CategoryPlot plot = chart.getCategoryPlot();
	 BarRenderer renderer = (BarRenderer) plot.getRenderer();
	 renderer.setSeriesPaint(0, Color.BLUE); // Blue
	 renderer.setBarPainter(new StandardBarPainter());
	 renderer.setDrawBarOutline(false);

	// Configure X-axis (numeric axis)
	 NumberAxis xAxis = (NumberAxis) plot.getRangeAxis();
	// Ensure axis starts at zero
	 xAxis.setLowerBound(0.0);
	 xAxis.setMinorTickMarksVisible(true);
	 xAxis.setMinorTickCount(5);
	 plot.setRangeMinorGridlinesVisible(true);
	 plot.setRangeMinorGridlinePaint(new Color(220, 220, 220));
	 plot.setRangeGridlinePaint(Color.WHITE);
	 plot.setRangeGridlineStroke(new BasicStroke(1f));
	// Determine a nice upper bound for the axis
	 double maxValue = data.values().stream().mapToDouble(Double::doubleValue).max().orElse(0.0);
	 double upperBound = calculateNiceUpperBound(maxValue);
	 xAxis.setUpperBound(upperBound);   
	// Format tick labels
	 xAxis.setTickUnit(new NumberTickUnit(upperBound / 5.0));
	 xAxis.setNumberFormatOverride(new DecimalFormat("0"));
	// Render chart to image
	 BufferedImage image = chart.createBufferedImage(400, 400);
	 ByteArrayOutputStream baos = new ByteArrayOutputStream();
	 ImageIO.write(image, "png", baos); 
	 
	 return Image.getInstance(baos.toByteArray());
	 } catch (Exception e) {
		 throw new AnalyticsException("Failed to build average travel time per route chart", e);
	 }
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
    		 LOGGER.fine("No density data available for edge " + edgeId);
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
 private Map<String, Double> calculateAverageEdgeDensity(ExportFilter filter) {
	// Use the precomputed average edge density
	 Map<String, Double> avgEdgeDensity = getEdgeDensity();
	 Map<String, Double> result = new LinkedHashMap<>();
	 
	 boolean filterByEdgeId = filter != null && filter.hasOnlyEdgeIdFilter();
	 boolean filterByMinDensity = filter != null && filter.hasMinEdgeDensityFilter();
	 boolean filterByCongested = filter != null && filter.hasOnlyCongestedEdgesFilter();
	 
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
		 result.put(edgeId, avgDensity);
	 }
	 
	 return result;
 }
 private void exportEdgeDensityInternal (PrintWriter writer, Map<String, Double> data, boolean forPdf) {
	 
	 if (forPdf) { writer.println("Edge Density Summary"); } 
	 else { writer.println("EdgeId,AverageDensity(veh/m)"); }
	 
	 boolean anyEdgePrinted = false;
	 for (Map.Entry<String, Double> entry : data.entrySet()) {
		 String edgeId = entry.getKey();
		 double avgDensity = entry.getValue();
		 boolean congestedEver = congestionList.containsKey(edgeId);
		 anyEdgePrinted = true;
		 if(!forPdf) { 
			 writer.println(edgeId + "," + String.format(Locale.US, "%.4f", avgDensity)); }
		 else if (avgDensity > 0){writer.println("Edge " + edgeId + "  Average density: " + String.format(Locale.US, "%.4f veh/m", avgDensity) + (congestedEver ? " (congested)" : "")); }
	 }
	 if (forPdf && !anyEdgePrinted) { writer.println("No edges match the selected filters."); }
 }	 
 
 private Image buildEdgeDensityBarChart(Map<String, Double> data) {
	 if (data == null || data.isEmpty()) {
		 throw new AnalyticsException("No data available for edge density chart");
	 }
	 try {
	 DefaultCategoryDataset dataset = new DefaultCategoryDataset();
	 data.forEach((edgeId, avgDensity) -> dataset.addValue(avgDensity, "Edge Density", edgeId));
	 
	 JFreeChart chart = ChartFactory.createBarChart(
	            "Average Edge Density",
	            "Edge ID",
	            "Average Density (veh/m)",
	            dataset
	    );
	 
	 CategoryPlot plot = chart.getCategoryPlot();
	 BarRenderer renderer = (BarRenderer) plot.getRenderer();
	 renderer.setSeriesPaint(0, new Color(140, 100, 60)); 
	 NumberAxis yAxis = (NumberAxis) plot.getRangeAxis();
	 yAxis.setLowerBound(0.0);
	 yAxis.setTickUnit(new NumberTickUnit(0.01));
	 yAxis.setMinorTickMarksVisible(true);
	 yAxis.setMinorTickCount(5);
	 plot.setRangeMinorGridlinesVisible(true);
	 plot.setRangeMinorGridlinePaint(new Color(220, 220, 220));
	 plot.setRangeGridlinePaint(Color.WHITE);
	 plot.setRangeGridlineStroke(new BasicStroke(1f));

	 yAxis.setNumberFormatOverride(new DecimalFormat("0.00"));
	 double max = data.values().stream().mapToDouble(Double::doubleValue).max().orElse(0.0);
	 yAxis.setUpperBound(Math.ceil(max / 0.01) * 0.01);
	 
	 BufferedImage image = chart.createBufferedImage(550, 380);
	 ByteArrayOutputStream baos = new ByteArrayOutputStream();
	 ImageIO.write(image, "png", baos);
	 
	 return Image.getInstance(baos.toByteArray());
	 } catch (Exception e) {
		 throw new AnalyticsException("Failed to build average edge density chart", e); 
	 }
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
	 
	 boolean anyEdgePrinted = false;
	 for (Map.Entry<String, Integer> entry : congestionList.entrySet()) {
		 String edgeId = entry.getKey();
		 int stoppedVehicles = entry.getValue();
		 
		 // onlyEdgeId filter
		 if (filterByEdgeId && !edgeId.equals(filter.getOnlyEdgeId())) { continue; }
		 anyEdgePrinted = true;
		 
		 if (forPdf) { writer.println("Edge " + edgeId + " congested (stopped vehicles: " + stoppedVehicles + ")"); }
		 else { writer.println(edgeId + "," + stoppedVehicles); }
	 }
	 if (forPdf && !anyEdgePrinted) { writer.println("No congested edges match the selected filters."); }
 }
 
 	//=========================
	// VEHICLE TRAVEL TIME
	//=========================
 
 private static class VehicleTravelRow {
    String vehicleId;
    String color;
    String routeId;
    double travelTime;

    VehicleTravelRow(String vehicleId, String color, String routeId, double travelTime) {
        this.vehicleId = vehicleId;
        this.color = color;
        this.routeId = routeId;
        this.travelTime = travelTime;
    }
}

 private void exportVehicleTravelTimesInternal (PrintWriter writer,Document document, ExportFilter filter, boolean forPdf) throws Exception {

	 boolean filterByColor = filter != null && filter.hasVehicleColorFilter();
	 boolean filterByRoute =filter != null && filter.hasOnlyRouteIdFilter();
	 
	 List<VehicleTravelRow> rows = new ArrayList<>();
	 
	 // ONE SINGLE LOOP		
	for (String vehicleId : exitTime.keySet()) {
	    Double enter = enterTime.get(vehicleId);
	    Double exit  = exitTime.get(vehicleId);
	    if (enter == null || exit == null || exit <= enter) continue;
	    IVehicle vehicle = vehicleById.get(vehicleId);
	    if (vehicle == null) continue;
	  //--------------------FILTERS------------------
		// vehicleColor filter
		if (filterByColor && !vehicle.getColor().equalsIgnoreCase(filter.getVehicleColor())) { continue; }
		// onlyRouteId filter
		if (filterByRoute && !vehicle.getRouteId().equals(filter.getOnlyRouteId())) { continue; }
	    double travelTime = exit - enter;
	    
	    rows.add(new VehicleTravelRow(vehicleId, vehicle.getColor(), vehicle.getRouteId(), travelTime));
	}
	
	// ---------- CSV ------------
	if (!forPdf) { 
		writer.println("vehicleId,color,routeId,travelTime(s)");
		for (VehicleTravelRow row : rows) {
			writer.println(row.vehicleId + "," + row.color + "," + row.routeId + "," + String.format(Locale.US, "%.2f", row.travelTime));
		}
		return;
	}
	
	if (document == null) {throw new AnalyticsException("PDF document is required for VEHICLE_TRAVEL_TIMES export");}
	
	// ---------- PDF (TABLE) -----------
	document.add(new Paragraph("Vehicle Travel Times"));
	document.add(new Paragraph(" "));
	
	if (rows.isEmpty()) {
		document.add(new Paragraph("No vehicles match the selected filters."));
		return;
	}
	PdfPTable table = new PdfPTable(4);
	table.setWidthPercentage(70);
	table.setWidths(new float[] {1f, 0.6f, 0.6f, 1f} );
	
	Font headerFont = new Font(Font.HELVETICA, 10, Font.BOLD, new Color(60, 60, 60));
	table.addCell(new PdfPCell(new Phrase("VehicleId", headerFont)));
	table.addCell(new PdfPCell(new Phrase("Color", headerFont)));
	table.addCell(new PdfPCell(new Phrase("RouteId",headerFont)));
	table.addCell(new PdfPCell(new Phrase("Travel Time (s)", headerFont)));
	
	for (VehicleTravelRow row : rows) {
		table.addCell(row.vehicleId);
		table.addCell(row.color);
		table.addCell(row.routeId);
		table.addCell(String.format(Locale.US, "%.2f", row.travelTime));
	}

	document.add(table);
 }

	//=============
	// SUMMARY
	//=============

 private List<ExportType> expandSummary (List<ExportType> types) {
	 if (!types.contains(ExportType.SUMMARY)) return types;
	 
	 List<ExportType> expanded = new ArrayList<>();
	 for (ExportType t : ExportType.values()) {
		 if (t != ExportType.SUMMARY) expanded.add(t);
	 }
	 
	 return expanded;
 }

	//=================
	// EXPORT TO CSV
	//=================
 
 @Override
 public void exportToCsv (String filePath, ExportFilter filter, List<ExportType> types) {
	 LOGGER.info("Exporting statistics to CSV: " + filePath);
	 List<ExportType> effectiveTypes = expandSummary(types);
     try (PrintWriter writer = new PrintWriter(filePath, "UTF-8")) {
    	 
    	 for (ExportType type : effectiveTypes) {
	         
    		 switch(type) {
	        	 
	        	 case AVG_SPEED -> { 
	        		 // calculate data
	        		 Map<String, Double> data = calculateAverageSpeedPerEdge(filter);
	        		 if (data.isEmpty()) {
	        			    LOGGER.warning("No AVG_SPEED data available for export");
	        			}
	        		 exportAverageSpeedInternal(writer,data, false);
	        	 }
	        	 
	        	 case AVG_TRAVEL_TIME -> {
	        		 Map<String, Double> data = calculateAverageTravelTimePerRoute(filter);
	        		 if (data.isEmpty()) {
	        			    LOGGER.warning("No AVG_TRAVEL_TIME data available for export");
	        			}
	        		 exportAverageTravelTimeInternal(writer, data, false);
	        	 }
	        	 
	        	 case EDGE_DENSITY -> {
	        		 Map<String, Double> data = calculateAverageEdgeDensity(filter);
	        		 if (data.isEmpty()) {
	        			    LOGGER.warning("No EDGE_DENSITY data available for export");
	        			}
	        		 exportEdgeDensityInternal(writer, data, false);
	        	 }
	        	 
	        	 case CONGESTED_EDGES -> exportCongestedEdgesInternal(writer, filter, false);
	        	 
	        	 case VEHICLE_TRAVEL_TIMES -> exportVehicleTravelTimesInternal(writer,null, filter, false);
	        	 
	         }
	         writer.println();	// empty Line between sections
    	 }
    	 
         
         
     } catch (Exception e) {
    	 LOGGER.severe("CSV export failed: " + filePath + " - " + e.getMessage());
    	 throw new AnalyticsException("CSV export failed: " + filePath, e);
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
	            return "SUMMARY export selected.\n"
	            		+ "This report includes all available export sections (Average Speed, Average Travel Time, Edge Density, Congested Edges, Vehicle Travel Times).\n"
	            		+ "Any filters applied are evaluated individually for each export type.";
	        default:
	            return "No description available for this report type.";
	    }
	}
 
 @Override
 public void exportToPdf (String filePath, ExportFilter filter, List<ExportType> types) {
	 LOGGER.info("Exporting statistics to PDF: " + filePath);
	 List<ExportType> effectiveTypes = expandSummary(types);
     try {
    	 
    	 // Create the PDF document
    	 Document document = new Document();
    	 // Bind the document to a file
    	 PdfWriter.getInstance(document, new FileOutputStream(filePath));
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
    	 boolean summarySelected = types.contains(ExportType.SUMMARY);
    	 if (summarySelected) {
    		 document.add(new Paragraph(getDescriptionForType(ExportType.SUMMARY)));
    		 document.add(new Paragraph(" "));
    	 }
    	 
    	 for (ExportType type : effectiveTypes) {
	    	 // Which Export Type
	    	 document.add(new Paragraph("Export Type: " + type));
	    	 // Type description
	    	 document.add(new Paragraph(getDescriptionForType(type)));
	    	 
	    	 // --- Applied Filters section ---
	    	 document.add(new Paragraph("Applied Filters"));
	    	 boolean hasFilters = (filter != null && !filter.isEmpty());
	    	 if (hasFilters) {
	    		 document.add(new Paragraph(filter.toString(type)));
	    	 } else { document.add(new Paragraph("No filters applied")); }
	    	 document.add(new Paragraph( hasFilters ? "Filtered Results" : "Results (no filters applied)"));
	    	 
	    	 // Collect output using existing export methods
	    	 StringWriter sw = new StringWriter();
	    	 PrintWriter pw = new PrintWriter(sw);
	    	 Image chartImage = null;
	    	 
	    	 switch(type) {
	    	 
	    	 	case AVG_SPEED -> {
	    	 		// Calculate data once
	    	 		Map<String, Double> data = calculateAverageSpeedPerEdge(filter);
	    	 		if (data.isEmpty()) {
        			    LOGGER.warning("No AVG_SPEED data available for export");
        			}
	    	 		// Export textual results to PDF 
	    	 		exportAverageSpeedInternal(pw, data, true);
	    	 		chartImage = buildAverageSpeedBarChart(data);
	    	 	}
	    	 	
	    	 	case AVG_TRAVEL_TIME -> {
	    	 		Map<String, Double> data = calculateAverageTravelTimePerRoute(filter);
	    	 		if (data.isEmpty()) {
        			    LOGGER.warning("No AVG_TRAVEL_TIME data available for export");
        			}
	    	 		exportAverageTravelTimeInternal(pw, data, true);
	    	 		chartImage = buildAverageTravelTimeHorizontalBarChart(data);
	    	 	}
	    	 
	    	 	case EDGE_DENSITY -> {
	    	 		Map<String, Double> data = calculateAverageEdgeDensity(filter);
	    	 		if (data.isEmpty()) {
        			    LOGGER.warning("No EDGE_DENSITY data available for export");
        			}
	    	 		exportEdgeDensityInternal(pw, data, true);
	    	 		chartImage = buildEdgeDensityBarChart(data);
	    	 	}
	    	 
	    	 	case CONGESTED_EDGES -> exportCongestedEdgesInternal(pw, filter, true);
	    	 
	    	 	case VEHICLE_TRAVEL_TIMES -> exportVehicleTravelTimesInternal(pw, document, filter, true);
	    	 
	    	 }
	    	 
	    	 pw.flush();
    	 	// Write lines into PDF
	    	if (type != ExportType.VEHICLE_TRAVEL_TIMES) {
   	    	for (String line : sw.toString().split("\n")) { document.add(new Paragraph(line)); }
   	    	document.add(new Paragraph(" "));
	    	}
	    	
   	    	if (chartImage != null) {
   	    		document.add(chartImage);
   	    		document.add(new Paragraph(" "));
   	    	}
	   	     
    	 }
    	 // Close the document
    	 document.close();

     } catch (Exception e) {
         throw new AnalyticsException("PDF export failed: " + filePath, e);
     }
 }
 
}