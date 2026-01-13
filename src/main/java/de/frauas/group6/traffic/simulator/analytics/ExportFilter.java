package de.frauas.group6.traffic.simulator.analytics;

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

	public boolean isEmpty() {
	    return !hasVehicleColorFilter()
	        && !hasOnlyRouteIdFilter()
	        && !hasOnlyEdgeIdFilter()
	        && !hasMinAverageTravelTimeFilter()
	        && !hasMinEdgeDensityFilter()
	        && !hasOnlyCongestedEdgesFilter();
	}

	// toString() for PDF export
	public String toString (ExportType type) {
	    StringBuilder sb = new StringBuilder();
	    switch (type) {
	    
	    	case AVG_SPEED:
			    if (vehicleColor != null) sb.append("vehicleColor = ").append(vehicleColor).append("\n");
			    if (onlyEdgeId != null) sb.append("onlyEdgeId = ").append(onlyEdgeId).append("\n");
			    if (onlyCongestedEdges) sb.append("onlyCongestedEdges = true\n");
			    break;
			    
	    	case AVG_TRAVEL_TIME:
	    		if (onlyRouteId != null) sb.append("onlyRouteId = ").append(onlyRouteId).append("\n");
	    		if (minAverageTravelTime != null) sb.append("minAverageTravelTime = ").append(minAverageTravelTime).append("\n");
	    		break;
	    		
	    	case EDGE_DENSITY:
	    		if (onlyEdgeId != null) sb.append("onlyEdgeId = ").append(onlyEdgeId).append("\n");
	    		if (minEdgeDensity != null) sb.append("minEdgeDensity = ").append(minEdgeDensity).append("\n");
	    		if (onlyCongestedEdges) sb.append("onlyCongestedEdges = true\n");
	    		break;
	    		
	    	case CONGESTED_EDGES:
	    		if (onlyEdgeId != null) sb.append("onlyEdgeId = ").append(onlyEdgeId).append("\n");
	    	
	    	case VEHICLE_TRAVEL_TIMES:	
	    		if (vehicleColor != null) sb.append("vehicleColor = ").append(vehicleColor).append("\n");
	    		if (onlyRouteId != null) sb.append("onlyRouteId = ").append(onlyRouteId).append("\n");
	    		break;
	    		
	    	case SUMMARY: break;
		    
	    }    
	    return sb.toString();
	}

	
}
