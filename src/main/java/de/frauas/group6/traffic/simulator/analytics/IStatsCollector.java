package de.frauas.group6.traffic.simulator.analytics;

import java.util.List;
import java.util.Map;

public interface IStatsCollector {
	
	void collectData();
	
	double getAverageSpeed();
	
	List<Double> getSpeedHistory();
	
	Map<String, Double> getAverageTravelTime();
	
	Map<String, Double> getEdgeDensity();
	
	Map<String, Integer> getCongestedEdgeIds();
	
	void exportToCsv(String filepath, ExportFilter filter, List<ExportType> types);
	
	void exportToPdf(String filepath, ExportFilter filter, List<ExportType> types);
	

}
