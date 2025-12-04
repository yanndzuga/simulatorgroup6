package de.frauas.group6.traffic.simulator.infrastructure;

public interface ITrafficLightManager {
	

	int  getPhase(String junctionId);
	void switchToPhase(String junctionId, int phase);
	int  getRemainingTime();
	void setAutomaticControl(boolean enabled);
	void updateTrafficLights();
	

}
