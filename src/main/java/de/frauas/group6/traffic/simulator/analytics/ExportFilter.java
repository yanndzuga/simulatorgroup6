package de.frauas.group6.traffic.simulator.analytics;

import de.frauas.group6.traffic.simulator.vehicles.IVehicle;

public class ExportFilter {

	// -----------------
	// VEHICLE FILTER
	// -----------------
	private String vehicleColor;
	
	// -----------------
	// ROUTE FILTER
	// -----------------
	private Double minAverageTravelTime;
	private String onlyRouteId;
	
	// -----------------
	// EDGE FILTER
	// -----------------
	private String onlyEdgeId;
	private boolean onlyCongestedEdges;
	private Double minEdgeDensity;
	

	//--------------------
	// SETTER
	//--------------------
	public void setVehicleColor (String vehicleColor) { this.vehicleColor = vehicleColor; }
	
	public void setMinAverageTravelTime (Double minAverageTravelTime) { this.minAverageTravelTime = minAverageTravelTime; }
	
	public void setOnlyRouteId (String onlyRouteId) { this.onlyRouteId = onlyRouteId; }
	
	public void setOnlyCongestedEdges (boolean onlyCongestedEdges) { this.onlyCongestedEdges = onlyCongestedEdges; }
	
	public void setOnlyEdgeId (String onlyEdgeId) { this.onlyEdgeId = onlyEdgeId; }
	
	public void setMinEdgeDensity (Double minEdgeDensity) { this.minEdgeDensity = minEdgeDensity; }
	
	//--------------------
	// GETTER
	//--------------------
	
	public String getVehicleColor() { return vehicleColor; }
	
	public Double getMinAverageTravelTime() {return minAverageTravelTime; }
	
	public String getOnlyRouteId() { return onlyRouteId; }
	
	public String getOnlyEdgeId() { return onlyEdgeId; }
	
	public boolean getOnlyCongestedEdges() { return onlyCongestedEdges; }
	
	public Double getMinEdgeDensity() { return minEdgeDensity; }
	
	
	public boolean hasVehicleColorFilter() { return vehicleColor != null; }

	public boolean hasOnlyEdgeIdFilter() { return onlyEdgeId != null; }

	public boolean hasMinEdgeDensityFilter() { return minEdgeDensity != null; }

	public boolean hasOnlyCongestedEdgesFilter() { return onlyCongestedEdges; }

	public boolean hasOnlyRouteIdFilter() { return onlyRouteId != null; }

	public boolean hasMinAverageTravelTimeFilter() { return minAverageTravelTime != null; }

	
	// toString() for PDF export
	@Override
	public String toString() {
	    StringBuilder sb = new StringBuilder();

	    if (vehicleColor != null) {
	        sb.append("vehicleColor = ").append(vehicleColor).append("\n");
	    }
	    if (onlyRouteId != null) {
	        sb.append("onlyRouteId = ").append(onlyRouteId).append("\n");
	    }
	    if (minAverageTravelTime != null) {
	        sb.append("minAverageTravelTime = ").append(minAverageTravelTime).append("\n");
	    }
	    if (onlyEdgeId != null) {
	        sb.append("onlyEdgeId = ").append(onlyEdgeId).append("\n");
	    }
	    if (onlyCongestedEdges) {
	        sb.append("onlyCongestedEdges = true\n");
	    }
	    if (minEdgeDensity != null) {
	        sb.append("minEdgeDensity = ").append(minEdgeDensity).append("\n");
	    }

	    return sb.toString();
	}

	
}
