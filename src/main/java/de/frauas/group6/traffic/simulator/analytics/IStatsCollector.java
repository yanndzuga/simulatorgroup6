package de.frauas.group6.traffic.simulator.analytics;

import java.util.List;

public interface IStatsCollector {
	
	void collectData();
	
	double getAverageSpeed();
	
	List<Double> getSpeedHistory();
	
	double getAverageTravelTime(String routeId);
	
	double getEdgeDensity(String edgeId);
	
	List<String> getCongestedEdgeIds();
	
	void exportToCsv(String filepath, ExportType type, ExportFilter filter);
	
	void exportToPdf(String filepath, ExportType type, ExportFilter filter);
	

}
