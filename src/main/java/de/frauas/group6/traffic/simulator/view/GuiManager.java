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
import javafx.scene.control.SplitPane;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

/**
 * GuiManager - Main Entry Point for the JavaFX User Interface.
 * Refactored to use SimulationViewStack for switching between 2D and 3D.
 */
public class GuiManager extends Application implements IMapObserver {

    private static final Logger LOGGER = Logger.getLogger(GuiManager.class.getName());

    // Static references to pass dependencies from the main Application entry point
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
    
    // --- NEW: The Stack Container that holds 2D and 3D ---
    private SimulationViewStack viewStack; 

    public static void startUI(ISimulationEngine engineInstance, IVehicleManager vmInstance, ITrafficLightManager trafficLightManager, IStatsCollector statsCollector, IInfrastructureManager infraMgr) {
        staticEngine = engineInstance;
        staticVehicleManager = vmInstance;
        staticTrafficLightManager = trafficLightManager;
        staticStatsCollector = statsCollector;
        staticInfrastructureManager = infraMgr;
        
        new Thread(() -> Application.launch(GuiManager.class)).start();
    }

    @Override
    public void start(Stage primaryStage) {
        try {
            // Retrieve dependencies
            this.engine = staticEngine;
            this.vehicleManager = staticVehicleManager;
            this.trafficLightManager = staticTrafficLightManager;
            this.statsCollector = staticStatsCollector;
            this.infraMgr = staticInfrastructureManager;
            
            BorderPane root = new BorderPane();
            
            // 1. Setup the Sidebar (Control Panel + Dashboard)
            SplitPane sidebar = createSidebar();
            
            // 2. Setup the Main View Stack (Handles 2D/3D switching and Physics Loop)
            this.viewStack = new SimulationViewStack(engine, vehicleManager, controlPanel);
            
            // Set the Stack as the center of the application
            root.setCenter(viewStack);
            root.setRight(sidebar);

            // 3. Configure Scene and Stage
            Scene scene = new Scene(root, 1280, 800);
            primaryStage.setTitle("Traffic Simulator - Group 6 (2D/3D Edition)");
            primaryStage.setScene(scene);
            
            setupCloseHandler(primaryStage);
            
            // Register this GUI as an observer
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

    private SplitPane createSidebar() {
        SplitPane sidebar = new SplitPane();
        sidebar.setOrientation(Orientation.VERTICAL);
        sidebar.setPrefWidth(320);
        sidebar.setStyle("-fx-background-color: #f4f4f4; -fx-border-color: #cccccc; -fx-border-width: 0 0 0 1;");
        
        this.controlPanel = new ControlPanel(engine, vehicleManager, trafficLightManager, infraMgr);
        this.dashboard = new DashBoard(statsCollector, infraMgr);

        sidebar.getItems().addAll(controlPanel, dashboard);
        sidebar.setDividerPositions(0.5); 
        
        return sidebar;
    }

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
     * Callback method triggered by the SimulationEngine.
     * Note: The Map rendering is now handled by the internal timer in SimulationViewStack.
     * This method is mainly used to keep the Text UI (Dashboard/Panels) in sync.
     */
    @Override
    public void refresh() {
        Platform.runLater(() -> {
            try {
                if (controlPanel != null) controlPanel.updateRealTimeData();
                if (dashboard != null) dashboard.update();
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Error during UI refresh", e);
            }
        });
    }
}