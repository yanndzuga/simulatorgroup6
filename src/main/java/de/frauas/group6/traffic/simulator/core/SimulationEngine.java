package de.frauas.group6.traffic.simulator.core;


import de.tudresden.sumo.cmd.Edge; 
import de.tudresden.sumo.cmd.Lane; 
import de.tudresden.sumo.cmd.Vehicle;
import de.tudresden.sumo.cmd.Trafficlight;
import de.tudresden.sumo.cmd.Simulation;
import de.tudresden.sumo.objects.SumoColor;
import de.tudresden.sumo.objects.SumoGeometry;
import de.tudresden.sumo.objects.SumoPosition2D;
import it.polito.appeal.traci.SumoTraciConnection;

import de.frauas.group6.traffic.simulator.vehicles.IVehicleManager;
import de.frauas.group6.traffic.simulator.infrastructure.ITrafficLightManager;
import de.frauas.group6.traffic.simulator.view.IMapObserver;


import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * CORE MODULE - MEMBER 1
 */
public class SimulationEngine implements ISimulationEngine {

    private static final Logger LOGGER = Logger.getLogger(SimulationEngine.class.getName());

    private SumoTraciConnection connection;
    
    // Dependencies
    private IVehicleManager vehicleManager;
    private ITrafficLightManager trafficLightManager;
    private IMapObserver mapObserver;

    private volatile boolean isRunning = false;
    private Thread simulationThread;
    
    private final int SIMULATION_STEP_SIZE = 1000; 
    private  String sumoBin ; 
    private final String configFile = "src/main/resources/meine_sim.sumocfg"; 
    private String sumoHome ;
    
    
    
    
   
    
    
    public SimulationEngine() {
    	sumoHome = System.getenv("SUMO_HOME");
    	if (sumoHome == null)
    	{throw new RuntimeException("the environment Variable was not definied");}
    	
    sumoBin = sumoHome + "/bin/sumo-gui";
    if (System.getProperty("os.name").toLowerCase().contains("win"))
    sumoBin += ".exe";
    	
        this.connection = new SumoTraciConnection(sumoBin, configFile);
    }

    // =================================================================================
    // LIFECYCLE METHODS (From ISimulationController)
    // =================================================================================

   
    public void initialize() {
        try {
            LOGGER.info("Initializing SUMO connection...");
            connection.printSumoError(true); 
            connection.printSumoOutput(true);
            connection.runServer();
            LOGGER.info("SUMO Connected.");
        } catch (Exception e) {
            // Improved logging: Pass the exception object 'e' to preserve stack trace
            LOGGER.log(Level.SEVERE, "CRITICAL: Could not connect to SUMO.", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public void start() {
        if (isRunning) return;
        isRunning = true;
        simulationThread = new Thread(this::runGameLoop, "Sim-Thread");
        simulationThread.start();
        LOGGER.info("Simulation started.");
    }

    @Override
    public void stop() {
        isRunning = false;
        if (connection != null && !connection.isClosed()) {
            connection.close();
            LOGGER.info("Simulation stopped.");
        }
    }

    private void runGameLoop() {
        while (isRunning) {
            long startTime = System.currentTimeMillis();
            try {
                connection.do_timestep();

                if (vehicleManager != null) vehicleManager.updateVehicles(); 
                if (trafficLightManager != null) trafficLightManager.updateTrafficLights();
                if (mapObserver != null) mapObserver.refresh();

                long sleepTime = SIMULATION_STEP_SIZE - (System.currentTimeMillis() - startTime);
                if (sleepTime > 0) Thread.sleep(sleepTime);

            } catch (Exception e) {
                // Improved logging: Pass the exception object 'e'
                LOGGER.log(Level.SEVERE, "Error in simulation loop", e);
            }
        }
    }

    // =================================================================================
    // READER METHODS 
    // =================================================================================

    @Override
    public double getCurrentSimulationTime() {
        try {
            return (double) connection.do_job_get(Simulation.getTime());
        } catch (Exception e) { return 0.0; }
    }

    // --- VEHICLES ---

    @SuppressWarnings("unchecked")
	@Override
    public List<String> getVehicleIdList() {
        try {
            return (List<String>) connection.do_job_get(Vehicle.getIDList());
        } catch (Exception e) { return Collections.emptyList(); }
    }

    @Override
    public Point2D getVehiclePosition(String vehicleId) {
        try {
            SumoPosition2D pos = (SumoPosition2D) connection.do_job_get(Vehicle.getPosition(vehicleId));
            return new Point2D.Double(pos.x, pos.y);
        } catch (Exception e) { return new Point2D.Double(0, 0); }
    }

    @Override
    public double getVehicleSpeed(String vehicleId) {
        try {
            return (double) connection.do_job_get(Vehicle.getSpeed(vehicleId));
        } catch (Exception e) { return 0.0; }
    }

    @Override
    public String getVehicleRoadId(String vehicleId) {
        try {
            return (String) connection.do_job_get(Vehicle.getRoadID(vehicleId));
        } catch (Exception e) { return ""; }
    }

    // --- TrafficLight ---

    @SuppressWarnings("unchecked")
	@Override
    public List<String> getTrafficLightIdList() {
        try {
            return (List<String>) connection.do_job_get(Trafficlight.getIDList());
        } catch (Exception e) { return Collections.emptyList(); }
    }

    @Override
    public int getTrafficLightPhase(String tlId) {
        try {
            return (int) connection.do_job_get(Trafficlight.getPhase(tlId));
        } catch (Exception e) { return -1; }
    }

    @Override
    public long getTrafficLightRemainingTime(String tlId) {
        try {
            double nextSwitch = (double) connection.do_job_get(Trafficlight.getNextSwitch(tlId));
            double current = (double) connection.do_job_get(Simulation.getTime());
            return (long) (nextSwitch - current); 
        } catch (Exception e) { return 0; }
    }

    // --- ROUTES ---

    @SuppressWarnings("unchecked")
	@Override
    public List<String> getEdgeIdList() {
        try {
            return (List<String>) connection.do_job_get(Edge.getIDList());
        } catch (Exception e) { return Collections.emptyList(); }
    }

    @Override
    public List<Point2D> getEdgeShape(String edgeId) {
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

    @Override
    public int getEdgeVehicleCount(String edgeId) {
        try {
            return (int) connection.do_job_get(Edge.getLastStepVehicleNumber(edgeId));
        } catch (Exception e) { return 0; }
    }

    @Override
    public double getEdgeLength(String edgeId) {
        try {
            String laneId = edgeId + "_0";
            return (double) connection.do_job_get(Lane.getLength(laneId));
        } catch (Exception e) { return 100.0; }
    }

    // =================================================================================
    // CONTROLLER METHODS 
    // =================================================================================

   
    @Override
    public void setVehicleSpeed(String id, double speed) {
                    try {
                connection.do_job_set(Vehicle.setSpeed(id, speed));
                LOGGER.info("Set speed for " + id + " to " + speed);
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Failed to set speed for: " + id, e);
            }
        
    }
    
    
    @Override
    public void removeVehicle(String id) {
            try {
                // byte 1 = REMOVE_REASON_TELEPORT (standard removal)
                connection.do_job_set(Vehicle.remove(id, (byte) 1));
                LOGGER.info("Removed vehicle: " + id);
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Failed to remove vehicle: " + id, e);
            }
        
    }
    
    
    @Override
    public void spawnVehicle(String id, String routeId, String typeId) {
        try {
            connection.do_job_set(Vehicle.add(id, routeId, typeId, -2, 0.0, 0.0, (byte) -2));
            LOGGER.info("Spawned vehicle: " + id);
        } catch (Exception e) {
            // Improved logging: Include exception for debugging
            LOGGER.log(Level.SEVERE, "Failed to spawn vehicle: " + id, e);
        }
    }

    @Override
    public void setVehicleColor(String id, int r, int g, int b) {
        try {
            SumoColor c = new SumoColor(r, g, b, 255);
            connection.do_job_set(Vehicle.setColor(id, c));
        } catch (Exception e) {
            // Improved logging: Include exception as warning
            LOGGER.log(Level.WARNING, "Failed to color vehicle: " + id, e);
        }
    }

    @Override
    public void setTrafficLightPhase(String tlId, int phaseIndex) {
        try {
            connection.do_job_set(Trafficlight.setPhase(tlId, phaseIndex));
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to switch phase for " + tlId, e);
        }
    }

    @Override
    public void setTrafficLightDuration(String tlId, int durationSeconds) {
        try {
            connection.do_job_set(Trafficlight.setPhaseDuration(tlId, durationSeconds * 1000));
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to set duration for " + tlId, e);
        }
    }

    // --- DEPENDENCY INJECTION (Not in interface, used by Main) ---

    public void setVehicleManager(IVehicleManager vm) { this.vehicleManager = vm; }
    public void setTrafficLightManager(ITrafficLightManager tlm) { this.trafficLightManager = tlm; }
    public void setMapObserver(IMapObserver mo) { this.mapObserver = mo; }
}