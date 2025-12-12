package de.frauas.group6.traffic.simulator.infrastructure;

import it.polito.appeal.traci.SumoTraciConnection;
// Hada Edge dyal TraaS (Commande)
import de.tudresden.sumo.cmd.Edge; 
import java.util.ArrayList;
import java.util.List;

public class Edgemanager {

    private SumoTraciConnection conn;

    public Edgemanager(SumoTraciConnection conn) {
        this.conn = conn;
    }

   
    public List<String> getEdgeIds() {
        try {
            return (List<String>) conn.do_job_get(Edge.getIDList());
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

   
    public int getVehicleCount(String edgeId) {
        try {
            
            return (int) conn.do_job_get(Edge.getLastStepVehicleNumber(edgeId));
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }
}