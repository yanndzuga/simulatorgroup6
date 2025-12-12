package de.frauas.group6.traffic.simulator.infrastructure;

import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

import it.polito.appeal.traci.SumoTraciConnection;
import de.tudresden.sumo.cmd.Trafficlight;
import de.tudresden.sumo.cmd.Simulation;

public class TrafficLightManager implements ITrafficLightManager {

    private SumoTraciConnection conn;
    
    // Map to track manual timer expiration: Junction ID -> Expiry Timestamp (ms)
    private Map<String, Long> manualExpiryTimes; 

    public TrafficLightManager(SumoTraciConnection conn) {
        this.conn = conn;
        this.manualExpiryTimes = new HashMap<>();
    }

    @Override
    public List<String> getJunctionIds() {
        try {
            return (List<String>) conn.do_job_get(Trafficlight.getIDList());
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    @Override
    public int getPhase(String junctionId) {
        try {
            return (int) conn.do_job_get(Trafficlight.getPhase(junctionId));
        } catch (Exception e) {
            return -1;
        }
    }

    // Calculates the remaining time for the manual timer (in seconds)
    public long getTimeUntilSwitch(String junctionId) {
        try {
            long currentTime = ((Number) conn.do_job_get(Simulation.getCurrentTime())).longValue();
            
            if (manualExpiryTimes.containsKey(junctionId)) {
                long expiryTime = manualExpiryTimes.get(junctionId);
                long timeLeftMs = expiryTime - currentTime;
                
                if (timeLeftMs > 0) {
                    return (timeLeftMs / 1000) + 1; // +1 for better display rounding
                } else {
                    return 0;
                }
            }
            return 0;
        } catch (Exception e) {
            return 0;
        }
    }

    @Override
    public void switchToPhase(String junctionId, int phase, int time) {
        try {
            // 1. Force the phase in SUMO
            conn.do_job_set(Trafficlight.setPhase(junctionId, phase));
            
            // 2. Set a very long duration in SUMO so it doesn't switch automatically.
            // We control the actual timing in Java.
            conn.do_job_set(Trafficlight.setPhaseDuration(junctionId, time * 2000));
            
            // 3. Register the timer in Java
            long now = ((Number) conn.do_job_get(Simulation.getCurrentTime())).longValue();
            long durationMs = time * 1000;
            
            manualExpiryTimes.put(junctionId, now + durationMs);

            System.out.println("MANUAL START: " + junctionId + " Forced to Phase " + phase + " for " + time + "s");
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void setAutomaticControl(boolean enabled) {
        try {
            if (enabled) {
                for(String id : getJunctionIds()) {
                    conn.do_job_set(Trafficlight.setProgram(id, "0"));
                    conn.do_job_set(Trafficlight.setPhase(id, 0)); // Reset to Green start
                }
                manualExpiryTimes.clear();
                System.out.println("USER ACTION: Restored Automatic Mode.");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // CRITICAL: Checks for expired timers and forces a reset to Green
    @Override
    public void updateTrafficLights() {
        try {
            long currentTime = ((Number) conn.do_job_get(Simulation.getCurrentTime())).longValue();
            
            List<String> expiredIds = new ArrayList<>();

            // 1. Check for expired timers
            for (String id : manualExpiryTimes.keySet()) {
                long expiryTime = manualExpiryTimes.get(id);
                if (currentTime >= expiryTime) {
                    expiredIds.add(id);
                }
            }

            // 2. Perform Hard Reset on expired lights
            for (String id : expiredIds) {
                System.out.println("TIMER EXPIRED: " + id + " -> Executing Hard Reset...");

                // A. Kill the remaining duration in SUMO (Force to 0)
                conn.do_job_set(Trafficlight.setPhaseDuration(id, 0));

                // B. Restore Automatic Program
                conn.do_job_set(Trafficlight.setProgram(id, "0"));
                
                // C. Force immediate switch to Phase 0 (Green)
                conn.do_job_set(Trafficlight.setPhase(id, 0));
                
                // D. Remove from tracking map
                manualExpiryTimes.remove(id);
                
                System.out.println("RESET COMPLETE: " + id + " is now GREEN & AUTO.");
            }
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public int getRemainingTime() { return 0; }

    public boolean isGreen(String junctionId, int phaseIndex) {
        return phaseIndex == 0; // Assuming Phase 0 is always Green
    }
}