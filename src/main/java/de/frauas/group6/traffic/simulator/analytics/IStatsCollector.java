package de.frauas.group6.traffic.simulator.analytics;

import java.util.List;

public interface IStatsCollector {
	
	List<Double> getSpeedHistory();
	
	double getAverageTravelTime();
	
	double getEdgeDensity(String edgeId);
	
	List<String> getCongestedEdgeIds();
	
	void exportToCsv(String filepath);
	
	void exportTopdf(String filepath);
	
	void collectData();
	
	double getAverageSpeed();
	
	
}
