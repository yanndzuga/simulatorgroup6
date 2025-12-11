package de.frauas.group6.traffic.simulator.infrastructure;

import java.util.List;

public interface ITrafficLightManager {
	

void switchToPhase(String edgeId, int phase,int time);
	
	List<String> getJunctionIds();
	
	int  getRemainingTime();
	
	void setAutomaticControl(boolean enabled);
	
	void updateTrafficLights();
	
	int getPhase(String junctionId);
	

}
