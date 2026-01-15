package de.frauas.group6.traffic.simulator.view;

import java.util.logging.Level;
import java.util.logging.Logger;

import de.frauas.group6.traffic.simulator.analytics.IStatsCollector;
import de.frauas.group6.traffic.simulator.core.ISimulationEngine;
import de.frauas.group6.traffic.simulator.infrastructure.IInfrastructureManager;
import de.frauas.group6.traffic.simulator.infrastructure.ITrafficLightManager;
import de.frauas.group6.traffic.simulator.vehicles.IVehicleManager;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Orientation;
import javafx.scene.Scene;
import javafx.scene.SceneAntialiasing;
import javafx.scene.SubScene;
import javafx.scene.control.SplitPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

/**
 * GuiManager - Main Entry Point for the JavaFX User Interface.
 * Implements the MapObserver interface to receive updates from the simulation engine.
 * * Responsibilities:
 * - Initializes the JavaFX application window.
 * - Sets up the layout (Map, Sidebar, Dashboard).
 * - Manages user input on the 3D Map (Zoom, Pan, Rotate).
 * - Refreshes UI components upon simulation ticks.
 */
public class GuiManager extends Application implements IMapObserver {

    private static final Logger LOGGER = Logger.getLogger(GuiManager.class.getName());

    // Static references to pass dependencies from the main Application entry point (App.java) to JavaFX start()
    private static ISimulationEngine staticEngine;
    private static IVehicleManager staticVehicleManager;
    private static ITrafficLightManager staticTrafficLightManager;
    private static IStatsCollector staticStatsCollector;
    private static IInfrastructureManager staticInfrastructureManager;
    
    // Instance dependencies
    private ISimulationEngine engine;
    private IVehicleManager vehicleManager;
    private ITrafficLightManager trafficLightManager;
    private IStatsCollector statsCollector;
    private IInfrastructureManager infraMgr;
    
    // UI Components
    private ControlPanel controlPanel;
    private DashBoard dashboard; 
    private MapView3D mapView3D; 
    
    // Mouse Interaction State
    private double mousePosX;
    private double mousePosY;
    private double zoomZ;

    /**
     * Static method to launch the JavaFX UI from the main Java application.
     * Stores dependencies in static fields to be accessible in the start() method.
     * * @param engineInstance The simulation engine instance.
     * @param vmInstance The vehicle manager instance.
     * @param trafficLightManager The traffic light manager instance.
     * @param statsCollector The statistics collector instance.
     * @param infraMgr The infrastructure manager instance.
     */
    public static void startUI(ISimulationEngine engineInstance, IVehicleManager vmInstance, ITrafficLightManager trafficLightManager, IStatsCollector statsCollector, IInfrastructureManager infraMgr) {
        staticEngine = engineInstance;
        staticVehicleManager = vmInstance;
        staticTrafficLightManager = trafficLightManager;
        staticStatsCollector = statsCollector;
        staticInfrastructureManager = infraMgr;
        
        // Launch JavaFX in a separate thread to avoid blocking the main thread
        new Thread(() -> Application.launch(GuiManager.class)).start();
    }

    /**
     * Main entry point for the JavaFX Application thread.
     * Initializes the stage, scene, and all UI components.
     * * @param primaryStage The primary stage for this application.
     */
    @Override
    public void start(Stage primaryStage) {
        try {
            // Retrieve dependencies from static fields
            this.engine = staticEngine;
            this.vehicleManager = staticVehicleManager;
            this.trafficLightManager = staticTrafficLightManager;
            this.statsCollector = staticStatsCollector;
            this.infraMgr = staticInfrastructureManager;
            
            BorderPane root = new BorderPane();
            
            // 1. Setup the Sidebar (Control Panel + Dashboard)
            SplitPane sidebar = createSidebar();
            
            // 2. Setup the 3D Map View
            // We initialize ControlPanel first (inside createSidebar) so it can be passed to MapView3D if needed
            // NOTE: MapView3D constructor might need adjustment if ControlPanel is a dependency
            this.mapView3D = new MapView3D(engine, vehicleManager, controlPanel);
            
            SubScene subScene = createMapSubScene();
            StackPane mapContainer = new StackPane(subScene);
            root.setCenter(mapContainer);
            
            // Bind SubScene size to the center area
            subScene.widthProperty().bind(root.widthProperty().subtract(sidebar.widthProperty())); 
            subScene.heightProperty().bind(root.heightProperty());

            root.setRight(sidebar);

            // 3. Configure Scene and Stage
            Scene scene = new Scene(root, 1280, 800);
            primaryStage.setTitle("Traffic Simulator - Group 6");
            primaryStage.setScene(scene);
            
            setupCloseHandler(primaryStage);
            
            // Register this GUI as an observer to the engine
            if (engine != null) {
                engine.setMapObserver(this);
            }
            
            primaryStage.show();
            LOGGER.info("GUI started successfully.");

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to start GUI", e);
            Platform.exit();
        }
    }

    /**
     * Creates the sidebar containing the Control Panel and Dashboard.
     * * @return Configured SplitPane for the sidebar.
     */
    private SplitPane createSidebar() {
        SplitPane sidebar = new SplitPane();
        sidebar.setOrientation(Orientation.VERTICAL);
        sidebar.setPrefWidth(320);
        sidebar.setStyle("-fx-background-color: #f4f4f4; -fx-border-color: #cccccc; -fx-border-width: 0 0 0 1;");
        
        // Initialize ControlPanel
        this.controlPanel = new ControlPanel(engine, vehicleManager, trafficLightManager, infraMgr);
        
        // Initialize Dashboard
        this.dashboard = new DashBoard(statsCollector, infraMgr);

        sidebar.getItems().addAll(controlPanel, dashboard);
        sidebar.setDividerPositions(0.5); // Split equally
        
        return sidebar;
    }

    /**
     * Creates and configures the 3D SubScene for the map.
     * Sets up mouse event handlers for navigation.
     * * @return Configured SubScene.
     */
    private SubScene createMapSubScene() {
        SubScene subScene = new SubScene(mapView3D.getRoot(), 800, 800, true, SceneAntialiasing.BALANCED);
        subScene.setPickOnBounds(true);
        subScene.setCamera(mapView3D.getCamera());
        
        setupMouseHandlers(subScene);
        
        return subScene;
    }

    /**
     * Sets up mouse interaction handlers for the map (Pan, Rotate, Zoom).
     * * @param subScene The SubScene to attach handlers to.
     */
    private void setupMouseHandlers(SubScene subScene) {
        subScene.setOnMousePressed(event -> {
            mousePosX = event.getSceneX();
            mousePosY = event.getSceneY();
        });

        subScene.setOnMouseDragged(event -> {
            double dx = event.getSceneX() - mousePosX;
            double dy = event.getSceneY() - mousePosY;

            if (event.isPrimaryButtonDown()) {
                // Rotate
                mapView3D.updateRotation(dx * 0.2, -dy * 0.2);
            } else if (event.isShiftDown() && event.isSecondaryButtonDown()) {
                // Pan / Move Horizontally
                mapView3D.moveHorizontale(dx, -dy);
            } else if (!event.isShiftDown() && event.isSecondaryButtonDown()) {
                // Move Vertically / Tilt
                mapView3D.moveRoadsVertical(dy);
            }

            mousePosX = event.getSceneX();
            mousePosY = event.getSceneY();
        });
        
        subScene.setOnScroll(event -> {
            zoomZ = event.getDeltaY();
            mapView3D.Zoom(zoomZ * 2.0);
        });
    }

    /**
     * Configures the application close behavior.
     * Ensures the simulation engine stops properly.
     * * @param stage The primary stage.
     */
    private void setupCloseHandler(Stage stage) {
        stage.setOnCloseRequest(e -> {
            LOGGER.info("Application closing...");
            if (engine != null) {
                engine.stop();
            }
            Platform.exit();
            System.exit(0);
        });
    }

    /**
     * Callback method triggered by the SimulationEngine to update the UI.
     * Executes UI updates on the JavaFX Application Thread.
     */
    @Override
    public void refresh() {
        Platform.runLater(() -> {
            try {
                if (controlPanel != null) controlPanel.updateRealTimeData();
                if (dashboard != null) dashboard.update();
                if (mapView3D != null) {
                    mapView3D.renderRoads(); 
                    mapView3D.updateVehicles(vehicleManager); 
                    mapView3D.updateTrafficLights();
                }
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Error during UI refresh", e);
            }
        });
    }
}