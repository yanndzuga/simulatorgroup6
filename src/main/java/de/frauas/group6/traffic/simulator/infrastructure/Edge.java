package de.frauas.group6.traffic.simulator.infrastructure;

public class Edge {
    private String id;
    private double length;
    private int vehicleCount;

    public Edge(String id, double length) {
        this.id = id;
        this.length = length;
        this.vehicleCount = 0;
    }

    // Getters
    public String getId() { return id; }
    public double getLength() { return length; }
    public int getVehicleCount() { return vehicleCount; }
    
    // Setters
    public void setVehicleCount(int vehicleCount) {
        this.vehicleCount = vehicleCount;
    }
    
    @Override
    public String toString() {
        return "Edge [id=" + id + ", cars=" + vehicleCount + "]";
    }
}	