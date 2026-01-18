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
import de.frauas.group6.traffic.simulator.analytics.IStatsCollector;
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
 * Refactored for Clean Code and Professional Error Handling.
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
    private IStatsCollector statsCollector;

    private volatile boolean isRunning = false;
    private volatile boolean isPaused = false;
    private Thread simulationThread;

    private final int SIMULATION_STEP_SIZE = 1000;
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
        int maxRetries = 5;
        int attempt = 0;
        boolean connected = false;

        while (attempt < maxRetries && !connected) {
            try {
                LOGGER.info("Initializing SUMO connection (Attempt " + (attempt + 1) + ")...");
                connection.addOption("start", "true");
                connection.runServer();
                connected = true;
                LOGGER.info("SUMO Connected successfully.");
            } catch (Exception e) {
                attempt++;
                LOGGER.log(Level.WARNING, "Connection attempt " + attempt + " failed: " + e.getMessage());

                if (attempt >= maxRetries) {
                    LOGGER.log(Level.SEVERE, "CRITICAL: Could not connect to SUMO after " + maxRetries + " attempts.", e);
                    throw new TraasCommunicationException("Failed to initialize SUMO connection after retries", e);
                }

                try {
                    Thread.sleep(2000); // Wait for ports to clear
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Initialization interrupted", ie);
                }
            }
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
    public void step() {
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
        // Safe execution of timestep
        executeTraasVoid(() -> {
            if (connection.isClosed()) return;
            connection.do_timestep();
        }, "Error in simulation step (TraCI)");

        // Update Managers securely
        try {
            if (vehicleManager != null) vehicleManager.updateVehicles();
            if (trafficLightManager != null) trafficLightManager.updateTrafficLights();
            if (mapObserver != null) mapObserver.refresh();
            if (statsCollector != null) statsCollector.collectData();
            if (infrastructureManager != null) infrastructureManager.refreshEdgeData();
                 
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error updating managers in simulation loop", e);
            stop(); // Emergency stop
        }
    }

    public Object getTraciLock() {
        return traciLock;
    }

    // =================================================================================
    // GENERIC WRAPPERS (THE "CLEAN CODE" SECRET SAUCE)
    // =================================================================================

    /**
     * Executes a TraaS command that returns a value safely.
     * Handles synchronization, logging, and default values.
     */
    private <T> T executeTraas(TraasCommand<T> command, T defaultValue, String errorMessage) {
        synchronized (traciLock) {
            try {
                return command.execute();
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, errorMessage, e);
                return defaultValue;
            }
        }
    }

    /**
     * Executes a TraaS command that does not return a value (void) safely.
     */
    private void executeTraasVoid(TraasVoidCommand command, String errorMessage) {
        synchronized (traciLock) {
            try {
                command.execute();
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, errorMessage, e);
            }
        }
    }

    // =================================================================================
    // READERS (using generic wrappers)
    // =================================================================================

    @Override
    public double getCurrentSimulationTime() {
        return executeTraas(
            () -> (double) connection.do_job_get(Simulation.getTime()),
            0.0,
            "Error getting simulation time"
        );
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<String> getVehicleIdList() {
        return executeTraas(
            () -> (List<String>) connection.do_job_get(Vehicle.getIDList()),
            Collections.emptyList(),
            "Error getting vehicle ID list"
        );
    }

    @Override
    public Point2D getVehiclePosition(String vehicleId) {
        return executeTraas(
            () -> {
                SumoPosition2D pos = (SumoPosition2D) connection.do_job_get(Vehicle.getPosition(vehicleId));
                return new Point2D.Double(pos.x, pos.y);
            },
            new Point2D.Double(0, 0),
            "Error getting position for vehicle: " + vehicleId
        );
    }

    @Override
    public double getVehicleSpeed(String vehicleId) {
        return executeTraas(
            () -> (double) connection.do_job_get(Vehicle.getSpeed(vehicleId)),
            0.0,
            "Error getting speed for vehicle: " + vehicleId
        );
    }

    @Override
    public String getVehicleRoadId(String vehicleId) {
        return executeTraas(
            () -> (String) connection.do_job_get(Vehicle.getRoadID(vehicleId)),
            "",
            "Error getting road ID for vehicle: " + vehicleId
        );
    }

    @Override
    public String getVehicleLaneId(String vehicleId) {
        return executeTraas(
            () -> (String) connection.do_job_get(Vehicle.getLaneID(vehicleId)),
            "",
            "Error getting lane ID for vehicle: " + vehicleId
        );
    }

    @Override
    public int[] getVehicleColor(String vehicleId) {
        return executeTraas(
            () -> {
                SumoColor c = (SumoColor) connection.do_job_get(Vehicle.getColor(vehicleId));
                return new int[]{c.r, c.g, c.b, c.a};
            },
            new int[]{255, 255, 255, 255},
            "Error getting color for vehicle: " + vehicleId
        );
    }

    @SuppressWarnings("unchecked")
    @Override
    public String getVehicleIdAtPosition(double x, double y, double radius) {
        return executeTraas(
            () -> {
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
            },
            null,
            "Error finding vehicle at position"
        );
    }

    public double getVehicleAngle(String vehID) {
        return executeTraas(
            () -> (double) connection.do_job_get(Vehicle.getAngle(vehID)),
            0.0,
            "Error getting angle for vehicle: " + vehID
        );
    }

    // =================================================================================
    // TRAFFIC LIGHTS
    // =================================================================================

    @SuppressWarnings("unchecked")
    @Override
    public List<String> getTrafficLightIdList() {
        return executeTraas(
            () -> (List<String>) connection.do_job_get(Trafficlight.getIDList()),
            Collections.emptyList(),
            "Error fetching Traffic Light IDs"
        );
    }

    @Override
    public int getTrafficLightPhase(String tlId) {
        return executeTraas(
            () -> (int) connection.do_job_get(Trafficlight.getPhase(tlId)),
            -1,
            "Error getting phase for TL: " + tlId
        );
    }

    @Override
    public long getTrafficLightRemainingTime(String tlId) {
        return executeTraas(
            () -> {
                double nextSwitch = (double) connection.do_job_get(Trafficlight.getNextSwitch(tlId));
                double current = (double) connection.do_job_get(Simulation.getTime());
                return (long) (nextSwitch - current);
            },
            0L,
            "Error getting remaining time for TL: " + tlId
        );
    }

    @Override
    public String getTrafficLightState(String tlId) {
        return executeTraas(
            () -> (String) connection.do_job_get(Trafficlight.getRedYellowGreenState(tlId)),
            "",
            "Error getting state for TL: " + tlId
        );
    }

    @Override
    public List<String> getControlledLanes(String tlId) {
        return executeTraas(
            () -> (SumoStringList) connection.do_job_get(Trafficlight.getControlledLanes(tlId)),
            Collections.emptyList(),
            "Error getting controlled lanes for TL: " + tlId
        );
    }

    @Override
    public int getLaneWaitingVehicleCount(String laneId) {
        return executeTraas(
            () -> (int) connection.do_job_get(Lane.getLastStepHaltingNumber(laneId)),
            0,
            "Error getting waiting count for lane: " + laneId
        );
    }

    @Override
    public Point2D getTrafficLightPosition(String tlId) {
        return executeTraas(
            () -> {
                SumoPosition2D pos = (SumoPosition2D) connection.do_job_get(Junction.getPosition(tlId));
                return new Point2D.Double(pos.x, pos.y);
            },
            new Point2D.Double(0, 0),
            "Error getting position for TL: " + tlId
        );
    }

    // =================================================================================
    // JUNCTIONS & EDGES
    // =================================================================================

    @SuppressWarnings("unchecked")
    public List<String> getJunctionIdList() {
        return executeTraas(
            () -> (List<String>) connection.do_job_get(Junction.getIDList()),
            Collections.emptyList(),
            "Error getting junction ID list"
        );
    }

    public List<Point2D> getJunctionShape(String junctionId) {
        return executeTraas(
            () -> {
                SumoGeometry geometry = (SumoGeometry) connection.do_job_get(Junction.getShape(junctionId));
                List<Point2D> points = new ArrayList<>();
                for (SumoPosition2D pos : geometry.coords) {
                    points.add(new Point2D.Double(pos.x, pos.y));
                }
                return points;
            },
            Collections.emptyList(),
            "Error getting shape for junction: " + junctionId
        );
    }

    public Point2D getJunctionPosition(String jId) {
        return executeTraas(
            () -> {
                SumoPosition2D sumoPos = (SumoPosition2D) connection.do_job_get(Junction.getPosition(jId));
                return new Point2D.Double(sumoPos.x, sumoPos.y);
            },
            new Point2D.Double(0, 0),
            "Error getting position for junction: " + jId
        );
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<String> getEdgeIdList() {
        return executeTraas(
            () -> (List<String>) connection.do_job_get(Edge.getIDList()),
            Collections.emptyList(),
            "Error getting edge ID list"
        );
    }

    @Override
    public List<Point2D> getEdgeShape(String edgeId) {
        return executeTraas(
            () -> {
                String laneId = edgeId + "_0";
                SumoGeometry geometry = (SumoGeometry) connection.do_job_get(Lane.getShape(laneId));
                List<Point2D> points = new ArrayList<>();
                for (SumoPosition2D pos : geometry.coords) {
                    points.add(new Point2D.Double(pos.x, pos.y));
                }
                return points;
            },
            Collections.emptyList(),
            "Error getting shape for edge: " + edgeId
        );
    }

    @Override
    public int getEdgeVehicleCount(String edgeId) {
        return executeTraas(
            () -> (int) connection.do_job_get(Edge.getLastStepVehicleNumber(edgeId)),
            0,
            "Error getting vehicle count for edge: " + edgeId
        );
    }

    @Override
    public double getEdgeLength(String edgeId) {
        return executeTraas(
            () -> {
                String laneId = edgeId + "_0";
                return (double) connection.do_job_get(Lane.getLength(laneId));
            },
            0.0,
            "Error getting length for edge: " + edgeId
        );
    }

    @Override
    public List<String> getLaneList(String edgeId) {
        return executeTraas(
            () -> {
                int numLanes = (int) connection.do_job_get(Edge.getLaneNumber(edgeId));
                List<String> lanes = new ArrayList<>();
                for (int i = 0; i < numLanes; i++) {
                    lanes.add(edgeId + "_" + i);
                }
                return lanes;
            },
            Collections.emptyList(),
            "Error getting lanes for edge: " + edgeId
        );
    }


    // =================================================================================
    // WRITERS (using generic void wrappers)
    // =================================================================================

    @Override
    public void setVehicleSpeed(String id, double speed) {
        executeTraasVoid(
            () -> connection.do_job_set(Vehicle.setSpeed(id, speed)),
            "Failed to set speed for vehicle: " + id
        );
    }

    @Override
    public void removeVehicle(String id) {
        executeTraasVoid(
            () -> connection.do_job_set(Vehicle.remove(id, (byte) 2)),
            "Failed to remove vehicle: " + id
        );
    }

    @Override
    public void spawnVehicle(String id, String routeId, byte edgeLane, String typeId, int r, int g, int b, double speedInMps) {
        executeTraasVoid(
            () -> {
                connection.do_job_set(Vehicle.add(id, typeId, routeId, 0, 0.0, speedInMps, edgeLane));
                SumoColor c = new SumoColor(r, g, b, 255);
                connection.do_job_set(Vehicle.setColor(id, c));
                LOGGER.info("Spawned vehicle: " + id);
            },
            "Failed to spawn vehicle: " + id
        );
    }

    @Override
    public void setVehicleColor(String id, int r, int g, int b) {
        executeTraasVoid(
            () -> {
                SumoColor c = new SumoColor(r, g, b, 255);
                connection.do_job_set(Vehicle.setColor(id, c));
            },
            "Failed to set color for vehicle: " + id
        );
    }

    @Override
    public void setTrafficLightPhase(String tlId, int phaseIndex) {
        executeTraasVoid(
            () -> connection.do_job_set(Trafficlight.setPhase(tlId, phaseIndex)),
            "Failed to set phase for TL: " + tlId
        );
    }

    @Override
    public void setTrafficLightDuration(String tlId, int durationSeconds) {
        executeTraasVoid(
            () -> connection.do_job_set(Trafficlight.setPhaseDuration(tlId, durationSeconds * 1000)),
            "Failed to set duration for TL: " + tlId
        );
    }

    // --- DEPENDENCY INJECTION ---
    public void setVehicleManager(IVehicleManager vm) { this.vehicleManager = vm; }
    public void setTrafficLightManager(ITrafficLightManager tlm) { this.trafficLightManager = tlm; }
    public void setInfrastructureManager(IInfrastructureManager infraManager) { this.infrastructureManager = infraManager; }
    public void setMapObserver(IMapObserver mo) { this.mapObserver = mo; }
    public void setStatCollector(IStatsCollector sc) { this.statsCollector = sc; }
}