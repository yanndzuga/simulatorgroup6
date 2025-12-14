package de.frauas.group6.traffic.simulator.analytics;

import java.io.PrintWriter;
import java.util.*;
import java.util.logging.Logger;

import de.frauas.group6.traffic.simulator.vehicles.IVehicle;
import de.frauas.group6.traffic.simulator.vehicles.IVehicleManager;
import de.frauas.group6.traffic.simulator.infrastructure.IEdge;
import de.frauas.group6.traffic.simulator.infrastructure.ITrafficLightManager;

public class StatsCollector2 implements IStatsCollector {

   
	private final IVehicleManager vehicleManager;
	private final ITrafficLightManager trafficLightManager;
	public StatsCollector2( IVehicleManager vehicleManager, ITrafficLightManager trafficLightManager) {
	    this.vehicleManager = vehicleManager;
	    this.trafficLightManager = trafficLightManager;
	}
	
	private static final Logger LOGGER =Logger.getLogger(StatsCollector2.class.getName());
	private int currentStep = 0;
	private static final double DENSITY_THRESHOLD = 0.3;
	    
	
	private final List<Double> avgSpeedPerStep = new ArrayList<>();
	private final List<Integer> vehicleCountPerStep = new ArrayList<>();
	private final Map<String, Integer> enterStep = new HashMap<>();
	private final Map<String, Integer> exitStep = new HashMap<>();
	private final List<Double> avgTravelTimePerStep = new ArrayList<>();
	private final Map<String, List<Double>> edgeDensityPerStep = new HashMap<>();
	private final Map<String, List<Boolean>> congestionPerStep = new HashMap<>();


public void collectData() {
	try {
	    currentStep++;
	    Collection<IVehicle> vehicles = vehicleManager.getAllVehicles();
	    // Vehicle Count 
	    int vehicleCountThisStep = vehicles.size();
	    vehicleCountPerStep.add(vehicleCountThisStep);

	    // Average Speed Step and History
	    double avgSpeedThisStep = 0.0;

	    if (!vehicles.isEmpty()) {
	        double speedSum = 0.0;
	        for (IVehicle v : vehicles) {
	            speedSum += v.getSpeed();
	        }
	        avgSpeedThisStep = speedSum / vehicles.size();
	    }

	    avgSpeedPerStep.add(avgSpeedThisStep);
	    
	    // ENTER detection
	    for (IVehicle v : vehicles) {
	        enterStep.putIfAbsent(v.getId(), currentStep);
	    }
	    
	    // EXIT detection (einfach)
	    for (String vid : new ArrayList<>(enterStep.keySet())) {
	        boolean stillActive = false;
	        for (IVehicle v : vehicles) {
	            if (v.getId().equals(vid)) {
	                stillActive = true;
	                break;
	            }
	        }
	        if (!stillActive && !exitStep.containsKey(vid)) { exitStep.put(vid, currentStep); }
	    }

	    
	    // Average Travel Time
	    int sumTravelSteps = 0;
	    int finishedVehicles = 0;

	    for (String vid : exitStep.keySet()) {
	        Integer enter = enterStep.get(vid);
	        Integer exit = exitStep.get(vid);
	        if (enter != null && exit != null && exit > enter) {
	            sumTravelSteps += (exit - enter);
	            finishedVehicles++;
	        }
	    }

	    double avgTravelTimeThisStep = 0.0;
	    if (finishedVehicles > 0) {
	        avgTravelTimeThisStep =(double) sumTravelSteps / finishedVehicles;
	    }

	    avgTravelTimePerStep.add(avgTravelTimeThisStep);

	    // EdgeDensity (Step)
	    for (IEdge edge : trafficLightManager.getEdges()) {
	        String edgeId = edge.getId();

	        // Vehicles on this Edge 
	        int vehiclesOnEdge = 0;
	        for (IVehicle v : vehicles) {
	            if (edgeId.equals(v.getEdgeId())) { vehiclesOnEdge++; }
	        }

	        // Density in this Step
	        double densityThisStep = 0.0;
	        if (vehicleCountThisStep > 0) {
	            densityThisStep = (double) vehiclesOnEdge / vehicleCountThisStep;
	        }

	        
	        List<Double> densitylist = edgeDensityPerStep.get(edgeId);
	        if (densitylist == null) {
	        	densitylist = new ArrayList<>();
	            edgeDensityPerStep.put(edgeId, densitylist);
	        }

	        densitylist.add(densityThisStep);
	    
	        
		    // Congestion
		    boolean congestedThisStep = densityThisStep > DENSITY_THRESHOLD;
	
		    List<Boolean> congestionList = congestionPerStep.get(edgeId);
		    if (congestionList == null) {
		        congestionList = new ArrayList<>();
		        congestionPerStep.put(edgeId, congestionList);
		    }
		    congestionList.add(congestedThisStep);
	    }
	    
	} catch (Exception e) {
		LOGGER.warning("StatsCollector: error during collectData() at step " + currentStep + " : " + e.getMessage());
	}
	    
}

	 
 @Override
 public double getAverageSpeed() {
	 if (currentStep == 0) return 0.0;
     if (avgSpeedPerStep.isEmpty()) return 0.0;
     return avgSpeedPerStep.get(currentStep - 1);
 }


 @Override
 public List<Double> getSpeedHistory() {
	 if (currentStep == 0) return Collections.emptyList();
     return new ArrayList<>(avgSpeedPerStep);
 }


 @Override
 public double getAverageTravelTime() {
	 if (currentStep == 0) return 0.0;
     if (avgTravelTimePerStep.isEmpty()) return 0.0;
     return avgTravelTimePerStep.get(currentStep - 1);
 }


    
 @Override
 public double getEdgeDensity (String edgeId) {
	 if (currentStep == 0) return 0.0;
     List<Double> history = edgeDensityPerStep.get(edgeId);
     if (history == null || history.isEmpty()) { return 0.0; }
     return history.get(currentStep - 1);
 }


 @Override
 public List<String> getCongestedEdgeIds() {
	 if (currentStep == 0) return Collections.emptyList();
     List<String> result = new ArrayList<>();
     for (String edgeId : congestionPerStep.keySet()) {
         List<Boolean> history = congestionPerStep.get(edgeId);

         if (history != null && !history.isEmpty()) {
             if (history.get(currentStep - 1)) { result.add(edgeId); }
         }
     }
     return result;
 }


 @Override
 public void exportToCsv (String filepath) {
     try (PrintWriter writer = new PrintWriter(filepath, "UTF-8")) {

         int totalVehicles = enterStep.size();
         int totalEdges = trafficLightManager.getEdges().size();
         int totalSteps = currentStep;

         writer.println("=== Simulation Summary ===");
         writer.println(" TotalVehicles ; " + totalVehicles);
         writer.println(" TotalEdges ; " + totalEdges);
         writer.println(" TotalSteps ; " + totalSteps);

         writer.println();
         
         double avgSpeedSum = 0.0;
         for (double s : avgSpeedPerStep) avgSpeedSum += s;
         double avgSpeedSimulation = avgSpeedPerStep.isEmpty() ? 0.0 : avgSpeedSum / avgSpeedPerStep.size();
         writer.println("=== Average Speed (Simulation) ===");
         writer.println(" AverageSpeed ; " + avgSpeedSimulation);

         writer.println();
         
         int travelSum = 0;
         int finishedVehicles = 0;
         for (String vid : exitStep.keySet()) {
             Integer enter = enterStep.get(vid);
             Integer exit = exitStep.get(vid);
             if (enter != null && exit != null && exit > enter) {
                 travelSum += (exit - enter);
                 finishedVehicles++;
             }
         }
         double avgTravelTime = finishedVehicles > 0 ? (double) travelSum / finishedVehicles : 0.0;
         writer.println("=== Average Travel Time (Simulation) ===");
         writer.println(" AverageTravelTime ; " + avgTravelTime);

         writer.println();
         
         writer.println("=== Edge Density (Simulation Average) ===");
         writer.println(" EdgeId ; AverageDensity");
         for (String edgeId : edgeDensityPerStep.keySet()) {
             List<Double> list = edgeDensityPerStep.get(edgeId);
             double sum = 0.0;
             for (double d : list) sum += d;
             double avgDensity = list.isEmpty() ? 0.0 : sum / list.size();
             writer.println(edgeId + " ; " + avgDensity);
         }

         writer.println();

         writer.println("=== Congested Edges (Ever) ===");
         writer.println(" EdgeId");
         for (String edgeId : congestionPerStep.keySet()) {
             List<Boolean> list = congestionPerStep.get(edgeId);
             boolean everCongested = false;
             for (boolean b : list) {
                 if (b) {
                     everCongested = true;
                     break;
                 }
             }
             if (everCongested) writer.println(" " + edgeId);
         }

         writer.println();

         writer.println("=== Travel Time per Vehicle ===");
         writer.println(" VehicleId ; EnterStep ; ExitStep ; TravelTime");
         for (String vid : exitStep.keySet()) {
             Integer enter = enterStep.get(vid);
             Integer exit = exitStep.get(vid);
             if (enter != null && exit != null && exit > enter) {
                 writer.println(vid + " ; " + enter + " ; " + exit + " ; " + (exit - enter));
             }
         }    
         
     } catch (Exception e) {
    	 LOGGER.warning("StatsCollector: failed to export CSV: " + e.getMessage());
	}
 }
 
 @Override
 public void exportTopdf (String filepath) {

     try {

         com.lowagie.text.Document document = new com.lowagie.text.Document();
         com.lowagie.text.pdf.PdfWriter.getInstance(document, new java.io.FileOutputStream(filepath));

         document.open();
         document.add(new com.lowagie.text.Paragraph("Traffic Simulation Statistics Report\n"));
         document.add(new com.lowagie.text.Paragraph("This document summarizes the results of a traffic simulation."
          		+ " All statistics are aggregated over the entire simulation runtime."));

         int totalVehicles = enterStep.size();
         int totalEdges = trafficLightManager.getEdges().size();
         int totalSteps = currentStep;

         document.add(new com.lowagie.text.Paragraph("The following values describe the overall size and duration of the simulation.\n"));
         document.add(new com.lowagie.text.Paragraph(
                 "Simulation Overview\n"
         + "Total Vehicles: " + totalVehicles + "\n"
         + "Total Edges: " + totalEdges + "\n"
         + "Total Simulation Steps: " + totalSteps + "\n\n"));

         double speedSum = 0.0;
         for (double s : avgSpeedPerStep) { speedSum += s; }
         double avgSpeed = avgSpeedPerStep.isEmpty() ? 0.0 : speedSum / avgSpeedPerStep.size();

         int travelSum = 0;
         int finishedVehicles = 0;
         for (String vid : exitStep.keySet()) {
             Integer enter = enterStep.get(vid);
             Integer exit = exitStep.get(vid);
             if (enter != null && exit != null && exit > enter) {
                 travelSum += (exit - enter);
                 finishedVehicles++;
             }
         }

         double avgTravelTime = finishedVehicles > 0 ? (double) travelSum / finishedVehicles : 0.0;
         
         document.add(new com.lowagie.text.Paragraph("Global averages are computed over all simulation steps and represent the overall traffic behavior.\n"));
         document.add(new com.lowagie.text.Paragraph(
                 "Global Averages\n"
         + "Average Speed: " + avgSpeed + "\n"
         + "Average Travel Time: " + avgTravelTime + "\n\n"));
         
         document.add(new com.lowagie.text.Paragraph("The average edge density describes how heavily a road segment was used relative to the total number of vehicles."));
         document.add(new com.lowagie.text.Paragraph("Average Edge Density (Simulation)"));
         for (String edgeId : edgeDensityPerStep.keySet()) {
             java.util.List<Double> list = edgeDensityPerStep.get(edgeId);
             double sum = 0.0;
             for (double d : list) { sum += d; }
             double avgDensity = list.isEmpty() ? 0.0 : sum / list.size();

             document.add(new com.lowagie.text.Paragraph(edgeId + ": " + avgDensity));
         }
         
         document.add(new com.lowagie.text.Paragraph("\nAn edge is considered congested if it exceeded the defined density threshold at least once during the simulation."));
         document.add(new com.lowagie.text.Paragraph("Congested Edges (Ever)"));
         for (String edgeId : congestionPerStep.keySet()) {
             java.util.List<Boolean> list = congestionPerStep.get(edgeId);
             boolean everCongested = false;
             for (boolean b : list) {
                 if (b) {
                     everCongested = true;
                     break;
                 }
             }
             if (everCongested) {
                 document.add(new com.lowagie.text.Paragraph(edgeId));
             }
         }

         document.add(new com.lowagie.text.Paragraph("\nThe travel time per vehicle is measured as the difference between entry and exit step."));
         document.add(new com.lowagie.text.Paragraph("Travel Time per Vehicle"));
         for (String vid : exitStep.keySet()) {
             Integer enter = enterStep.get(vid);
             Integer exit = exitStep.get(vid);
             if (enter != null && exit != null && exit > enter) {
                 document.add(new com.lowagie.text.Paragraph(
                     vid + " | Enter: " + enter
                         + " | Exit: " + exit
                         + " | Travel Time: " + (exit - enter)
                 ));
             }
         }

         document.close();

     } catch (Exception e) {
         LOGGER.warning("StatsCollector: failed to export PDF: " + e.getMessage());
     }
 }
 
}







