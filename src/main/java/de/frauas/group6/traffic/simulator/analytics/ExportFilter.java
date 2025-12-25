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
	
	// ------------------------------
	// VEHICLE MATCH (VEHICLE COLOR)
	// ------------------------------
	public boolean matchesVehicleColor (IVehicle v) {
		if (vehicleColor != null && !vehicleColor.equals(v.getColor())) { return false; }
		return true;
	}
	
	// ------------------------------------------
	// ROUTE MATCH (ROUTEID & AVERAGETRAVELTIME)
	// ------------------------------------------
	public boolean matchesRoute (String routeId, double avgTravelTime) {
		if (onlyRouteId != null && !onlyRouteId.equals(routeId)) { return false; }
		if (minAverageTravelTime != null && avgTravelTime < minAverageTravelTime ) { return false; }
		return true;
	}
	
	// ---------------------------------------------------
	// EDGE MATCH (EDGEID & EDGEDENSITY & CONGESTIONLIST)
	// ---------------------------------------------------
	public boolean matchesEdge (String edgeId, double avgDensity, boolean congestedEver) {
		if (onlyEdgeId != null && !onlyEdgeId.equals(edgeId)) { return false; }
		if (onlyCongestedEdges && !congestedEver) { return false; }
		if (minEdgeDensity != null && avgDensity < minEdgeDensity) { return false; }
		return true;
	}
	public boolean matchesCongestedEdge(String edgeId) {
	    if (onlyEdgeId != null && !onlyEdgeId.equals(edgeId)) return false;
	    return true;
	}

	//--------------------
	// SETTER
	//--------------------
	public void setVehicleColor (String vehicleColor) { this.vehicleColor = vehicleColor; }
	
	public void setMinAverageTravelTime (Double minAverageTravelTime) { this.minAverageTravelTime = minAverageTravelTime; }
	
	public void setOnlyRouteId (String onlyRouteId) { this.onlyRouteId = onlyRouteId; }
	
	public void setOnlyCongeytedEdges (boolean onlyCongestedEdges) { this.onlyCongestedEdges = onlyCongestedEdges; }
	
	public void setOnlyEdgeId (String onlyEdgeId) { this.onlyEdgeId = onlyEdgeId; }
	
	public void setMinEdgeDensity (Double minEdgeDensity) { this.minEdgeDensity = minEdgeDensity; }
	
	
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

	    if (sb.length() == 0) {
	        return "No filters applied";
	    }

	    return sb.toString();
	}

	
}
