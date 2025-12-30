package de.frauas.group6.traffic.simulator.core;

import de.tudresden.sumo.cmd.Edge;
import de.tudresden.sumo.cmd.Junction;
import de.tudresden.sumo.cmd.Lane;
import de.tudresden.sumo.cmd.Vehicle;
import de.tudresden.sumo.cmd.Trafficlight;
import de.tudresden.sumo.cmd.Simulation;
import de.tudresden.sumo.objects.SumoColor;
import de.tudresden.sumo.objects.SumoGeometry;
import de.tudresden.sumo.objects.SumoPosition2D;
import de.tudresden.sumo.objects.SumoStringList;
import it.polito.appeal.traci.SumoTraciConnection;

import de.frauas.group6.traffic.simulator.vehicles.IVehicleManager;
import de.frauas.group6.traffic.simulator.infrastructure.ITrafficLightManager;
import de.frauas.group6.traffic.simulator.infrastructure.IInfrastructureManager;
import de.frauas.group6.traffic.simulator.infrastructure.IEdge;
import de.frauas.group6.traffic.simulator.view.IMapObserver;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * CORE MODULE - IMPLEMENTATION
 * Handles raw communication with TraaS (SUMO) and synchronizes threads.
 */
public class SimulationEngine implements ISimulationEngine {

    private static final Logger LOGGER = Logger.getLogger(SimulationEngine.class.getName());

    private SumoTraciConnection connection;
    private final Object traciLock = new Object();

    // Dependencies
    private IVehicleManager vehicleManager;
    private ITrafficLightManager trafficLightManager;
    private IInfrastructureManager infrastructureManager; 
    private IMapObserver mapObserver;

    private volatile boolean isRunning = false;
    private volatile boolean isPaused = false;
    
    private Thread simulationThread;
    
    private final int SIMULATION_STEP_SIZE = 100; 
    private final String configFile = "src/main/resources/meine_sim.sumocfg"; 
    private String sumoBin; 

    public SimulationEngine() {
        String sumoHome = System.getenv("SUMO_HOME");
        if (sumoHome == null) {
            throw new RuntimeException("ERROR: SUMO_HOME environment variable is not set.");
        }

        sumoBin = sumoHome + "/bin/sumo-gui";
        if (System.getProperty("os.name").toLowerCase().contains("win")) {
            sumoBin += ".exe";
        }

        this.connection = new SumoTraciConnection(sumoBin, configFile);
    }

    // =================================================================================
    // LIFECYCLE
    // =================================================================================

    public void initialize() {
        try {
            LOGGER.info("Initializing SUMO connection...");
            connection.addOption("start", "true"); 
            
            // ADD THIS LINE: This tells SUMO to use 0.1s increments
            connection.addOption("step-length", "0.1"); 
            
            connection.runServer();
            LOGGER.info("SUMO Connected.");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "CRITICAL: Could not connect to SUMO.", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public void start() {
        if (isRunning) return;
        isRunning = true;
        isPaused = false;
        simulationThread = new Thread(this::runGameLoop, "Sim-Thread");
        simulationThread.start();
        LOGGER.info("Simulation started.");
    }

    @Override
    public void stop() {
        isRunning = false;
        try {
            if (connection != null && !connection.isClosed()) {
                synchronized (traciLock) {
                    connection.close();
                }
                LOGGER.info("Simulation stopped.");
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error closing connection", e);
        }
    }

    @Override
    public void pause() {
        isPaused = true;
        LOGGER.info("Simulation paused.");
    }

    @Override
    public void resume() {
        isPaused = false;
        LOGGER.info("Simulation resumed.");
    }

    @Override
    public boolean isPaused() {
        return isPaused;
    }

    @Override
    public void step(){
        doStepLogic();
    }

    private void runGameLoop() {
        while (isRunning) {
            if (isPaused) {
                try {
                    Thread.sleep(100); 
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
                continue; 
            }

            long startTime = System.currentTimeMillis();
            doStepLogic();
            long executionTime = System.currentTimeMillis() - startTime;
            long sleepTime = SIMULATION_STEP_SIZE - executionTime;

            if (sleepTime > 0) {
                try {
                    Thread.sleep(sleepTime);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
    }

    private void doStepLogic() {
        try {
            synchronized (traciLock) {
                if (connection.isClosed()) return;
                connection.do_timestep();
            }

            if (vehicleManager != null) vehicleManager.updateVehicles(); 
            if (trafficLightManager != null) trafficLightManager.updateTrafficLights();
            if (mapObserver != null) mapObserver.refresh();

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error in simulation step", e);
            stop(); 
        }
    }
    
    public Object getTraciLock() {
        return traciLock;
    }

    // =================================================================================
    // READERS
    // =================================================================================

    @Override
    public double getCurrentSimulationTime() {
        synchronized (traciLock) {
            try {
                return (double) connection.do_job_get(Simulation.getTime());
            } catch (Exception e) { return 0.0; }
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<String> getVehicleIdList() {
        synchronized (traciLock) {
            try {
                return (List<String>) connection.do_job_get(Vehicle.getIDList());
            } catch (Exception e) { return Collections.emptyList(); }
        }
    }

    @Override
    public Point2D getVehiclePosition(String vehicleId) {
        synchronized (traciLock) {
            try {
                SumoPosition2D pos = (SumoPosition2D) connection.do_job_get(Vehicle.getPosition(vehicleId));
                return new Point2D.Double(pos.x, pos.y);
            } catch (Exception e) { return new Point2D.Double(0, 0); }
        }
    }

    @Override
    public double getVehicleSpeed(String vehicleId) {
        synchronized (traciLock) {
            try {
                return (double) connection.do_job_get(Vehicle.getSpeed(vehicleId));
            } catch (Exception e) { return 0.0; }
        }
    }

    @Override
    public String getVehicleRoadId(String vehicleId) {
        synchronized (traciLock) {
            try {
                return (String) connection.do_job_get(Vehicle.getRoadID(vehicleId));
            } catch (Exception e) { return ""; }
        }
    }
    
    @Override
    public String getVehicleLaneId(String vehicleId) {
        synchronized (traciLock) {
            try {
                return (String) connection.do_job_get(Vehicle.getLaneID(vehicleId));
            } catch (Exception e) { return ""; }
        }
    }

    @Override
    public int[] getVehicleColor(String vehicleId) {
        synchronized (traciLock) {
            try {
                SumoColor c = (SumoColor) connection.do_job_get(Vehicle.getColor(vehicleId));
                return new int[]{c.r, c.g, c.b, c.a};
            } catch (Exception e) { return new int[]{255, 255, 255, 255}; }
        }
    }
    
    public String getVehicleIdAtPosition(double x, double y, double radius) {
        synchronized (traciLock) {
            try {
                List<String> ids = (List<String>) connection.do_job_get(Vehicle.getIDList());
                String closestId = null;
                double closestDistance = Double.MAX_VALUE;
                for (String id : ids) {
                    SumoPosition2D pos = (SumoPosition2D) connection.do_job_get(Vehicle.getPosition(id));
                    double dist = Math.sqrt(Math.pow(pos.x - x, 2) + Math.pow(pos.y - y, 2));
                    if (dist <= radius && dist < closestDistance) {
                        closestDistance = dist;
                        closestId = id;
                    }
                }
                return closestId;
            } catch (Exception e) { return null; }
        }
    }

    @Override
    public double getVehicleAngle(String vehicleId) {
        synchronized (traciLock) {
            try {
                // This pulls the real-time heading from SUMO
                return (double) connection.do_job_get(de.tudresden.sumo.cmd.Vehicle.getAngle(vehicleId));
            } catch (Exception e) { 
                return 0.0; 
            }
        }
    }

    /**
     * Retrieves the list of all Traffic Light IDs from SUMO.
     */
    @SuppressWarnings("unchecked")
    @Override
    public List<String> getTrafficLightIdList() {
        synchronized (traciLock) {
            try {
                // Real TraCI call to get IDs
                return (List<String>) connection.do_job_get(Trafficlight.getIDList());
            } catch (Exception e) {
                System.err.println("Error fetching Traffic Light IDs: " + e.getMessage());
                return Collections.emptyList();
            }
        }
    }
    @Override
    public int getTrafficLightPhase(String tlId) {
        synchronized (traciLock) {
            try {
                return (int) connection.do_job_get(Trafficlight.getPhase(tlId));
            } catch (Exception e) { return -1; }
        }
    }

    @Override
    public long getTrafficLightRemainingTime(String tlId) {
        synchronized (traciLock) {
            try {
                double nextSwitch = (double) connection.do_job_get(Trafficlight.getNextSwitch(tlId));
                double current = (double) connection.do_job_get(Simulation.getTime());
                return (long) (nextSwitch - current); 
            } catch (Exception e) { return 0; }
        }
    }
    
    @Override
    public String getTrafficLightState(String tlId) {
        synchronized (traciLock) {
            try {
                return (String) connection.do_job_get(Trafficlight.getRedYellowGreenState(tlId));
            } catch (Exception e) { return ""; }
        }
    }
    
    public String getTrafficLightDebugInfo(String tlId) {
        synchronized (traciLock) {
            try {
                // Get State
                String state = (String) connection.do_job_get(Trafficlight.getRedYellowGreenState(tlId));
                // Get Duration (convert ms to seconds)
                int durationMs = (int) connection.do_job_get(Trafficlight.getPhaseDuration(tlId));
                int durationSec = durationMs / 1000;
                
                return "State: " + state + " | Duration: " + durationSec + "s";
            } catch (Exception e) {
                return "Error: Could not fetch info";
            }
        }
    }
    @Override
    public List<String> getControlledLanes(String tlId) {
        synchronized (traciLock) {
            try {
                SumoStringList list = (SumoStringList) connection.do_job_get(Trafficlight.getControlledLanes(tlId));
                return list; 
            } catch (Exception e) { return Collections.emptyList(); }
        }
    }
    
    @Override
    public int getLaneWaitingVehicleCount(String laneId) {
        synchronized (traciLock) {
            try {
                return (int) connection.do_job_get(Lane.getLastStepHaltingNumber(laneId));
            } catch (Exception e) { return 0; }
        }
    }
    
    public Point2D getTrafficLightPosition(String tlId) {
        synchronized (traciLock) {
            try {
                SumoPosition2D pos = (SumoPosition2D) connection.do_job_get(Junction.getPosition(tlId));
                return new Point2D.Double(pos.x, pos.y);
            } catch (Exception e) { return new Point2D.Double(0, 0); }
        }
    }
    
    @SuppressWarnings("unchecked")
    public List<String> getJunctionIdList() {
        synchronized (traciLock) {
            try {
                return (List<String>) connection.do_job_get(Junction.getIDList());
            } catch (Exception e) { return Collections.emptyList(); }
        }
    }

    public List<Point2D> getJunctionShape(String junctionId) {
        synchronized (traciLock) {
            try {
                SumoGeometry geometry = (SumoGeometry) connection.do_job_get(Junction.getShape(junctionId));
                List<Point2D> points = new ArrayList<>();
                for (SumoPosition2D pos : geometry.coords) {
                    points.add(new Point2D.Double(pos.x, pos.y));
                }
                return points;
            } catch (Exception e) { return Collections.emptyList(); }
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<String> getEdgeIdList() {
        synchronized (traciLock) {
            try {
                return (List<String>) connection.do_job_get(Edge.getIDList());
            } catch (Exception e) { return Collections.emptyList(); }
        }
    }

    @Override
    public List<Point2D> getEdgeShape(String edgeId) {
        synchronized (traciLock) {
            try {
                String laneId = edgeId + "_0";
                SumoGeometry geometry = (SumoGeometry) connection.do_job_get(Lane.getShape(laneId));
                List<Point2D> points = new ArrayList<>();
                for (SumoPosition2D pos : geometry.coords) {
                    points.add(new Point2D.Double(pos.x, pos.y));
                }
                return points;
            } catch (Exception e) { return Collections.emptyList(); }
        }
    }

    @Override
    public int getEdgeVehicleCount(String edgeId) {
        synchronized (traciLock) {
            try {
                return (int) connection.do_job_get(Edge.getLastStepVehicleNumber(edgeId));
            } catch (Exception e) { return 0; }
        }
    }

    @Override
    public double getEdgeLength(String edgeId) {
        synchronized (traciLock) {
            try {
                String laneId = edgeId + "_0";
                return (double) connection.do_job_get(Lane.getLength(laneId));
            } catch (Exception e) { return 0.0; }
        }
    }
    
    @Override
    public List<String> getLaneList(String edgeId) {
        synchronized (traciLock) {
            try {
                int numLanes = (int) connection.do_job_get(Edge.getLaneNumber(edgeId));
                List<String> lanes = new ArrayList<>();
                for(int i=0; i<numLanes; i++) {
                    lanes.add(edgeId + "_" + i);
                }
                return lanes;
            } catch (Exception e) { return Collections.emptyList(); }
        }
    }
    
    public byte getVehicleLaneIndex(String vehicleId) {
        synchronized (traciLock) {
            try {
                return (byte) connection.do_job_get(Vehicle.getLaneIndex(vehicleId));
            } catch (Exception e) { return -1; }
        }
    }

    // =================================================================================
    // WRITERS
    // =================================================================================

    @Override
    public void setVehicleSpeed(String id, double speed) {
        synchronized (traciLock) {
            try {
                connection.do_job_set(Vehicle.setSpeed(id, speed));
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Failed to set speed for: " + id, e);
            }
        }
    }
    
    @Override
    public void removeVehicle(String id) {
        synchronized (traciLock) {
            try {
                connection.do_job_set(Vehicle.remove(id, (byte) 2)); 
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Failed to remove vehicle: " + id, e);
            }
        }
    }
    
    @Override
    public void spawnVehicle(String id, String routeId, byte edgeLane, String typeId, int r, int g, int b, double speedInMps) {
        synchronized (traciLock) {
            try {
                // Use speedInMps as departSpeed (6th argument)
                // -2 = NOW (Time)
                // 0.0 = POS (Start of lane)
                // speedInMps = Depart Speed
                // -2 = First Allowed Lane
                connection.do_job_set(Vehicle.add(id, typeId, routeId, 0, 0.0, speedInMps, edgeLane));
                
                // Apply color immediately

                SumoColor c = new SumoColor(r, g, b, 255);
                connection.do_job_set(Vehicle.setColor(id, c));
                LOGGER.info("Spawned vehicle: " + id);
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Failed to spawn vehicle: " + id, e);
            }
        }
    }

    
 
    
    
    @Override
    public void setVehicleColor(String id, int r, int g, int b) {
        synchronized (traciLock) {
            try {
                SumoColor c = new SumoColor(r, g, b, 255);
                connection.do_job_set(Vehicle.setColor(id, c));
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Failed to color vehicle: " + id, e);
            }
        }
    }

    @Override
    public void setTrafficLightPhase(String tlId, int phaseIndex) {
        synchronized (traciLock) {
            try {
                connection.do_job_set(Trafficlight.setPhase(tlId, phaseIndex));
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Failed to switch phase for " + tlId, e);
            }
        }
    }

    @Override
    public void setTrafficLightDuration(String tlId, int durationSeconds) {
        synchronized (traciLock) {
            try {
                connection.do_job_set(Trafficlight.setPhaseDuration(tlId, durationSeconds * 1000));
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Failed to set duration for " + tlId, e);
            }
        }
    }

 /*
  

    @Override
    public void checkAndHandleCongestion() {
        if (trafficLightManager != null && infrastructureManager != null) {
            // 1. Refresh Edge Data
            infrastructureManager.refreshEdgeData();
            // 2. Logic decision
            trafficLightManager.handleCongestion(infrastructureManager.getAllEdges());
        }
    } */ 

    // --- DEPENDENCY INJECTION ---
    public void setVehicleManager(IVehicleManager vm) { this.vehicleManager = vm; }
    public void setTrafficLightManager(ITrafficLightManager tlm) { this.trafficLightManager = tlm; }
    public void setInfrastructureManager(IInfrastructureManager infraManager) { this.infrastructureManager = infraManager; }
    public void setMapObserver(IMapObserver mo) { this.mapObserver = mo; }
}