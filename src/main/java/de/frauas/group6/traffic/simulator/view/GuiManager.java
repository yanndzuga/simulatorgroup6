package de.frauas.group6.traffic.simulator.view;

import java.util.Optional;
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
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.SceneAntialiasing;
import javafx.scene.SubScene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.ButtonType;
import javafx.scene.control.SplitPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

/**
 * GuiManager - Main Entry Point for the JavaFX User Interface.
 * Implements the MapObserver interface to receive updates from the simulation engine.
 * * Responsibilities:
 * - Initializes the JavaFX application window.
 * - Prompts user for Map Mode (2D or 3D).
 * - Sets up the layout (Map, Sidebar, Dashboard).
 * - Refreshes UI components upon simulation ticks.
 */
public class GuiManager extends Application implements IMapObserver {

    private static final Logger LOGGER = Logger.getLogger(GuiManager.class.getName());

    // Static references to pass dependencies from App.java
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
    
    // Map Components (Only one will be active)
    private MapView3D mapView3D; 
    private MapView mapView2D;
    private boolean is3DMode = true; // Default
    
    // Mouse Interaction State (For 3D)
    private double mousePosX;
    private double mousePosY;
    private double zoomZ;

    /**
     * Static method to launch the JavaFX UI from the main Java application.
     */
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
            
            // 1. Prompt User for Map Mode (2D vs 3D)
            promptForMapMode();

            // 2. Initialize Common Components (ControlPanel, Dashboard)
            initializeCommonComponents();

            // 3. Setup Layout
            BorderPane root = new BorderPane();
            SplitPane sidebar = createSidebar();
            
            // 4. Create Map View based on selection
            Node mapNode = createMapNode(root, sidebar);
            
            root.setCenter(mapNode);
            root.setRight(sidebar);

            // 5. Configure Scene
            Scene scene = new Scene(root, 1280, 800);
            primaryStage.setTitle("Traffic Simulator - Group 6 (" + (is3DMode ? "3D Mode" : "2D Mode") + ")");
            primaryStage.setScene(scene);
            
            setupCloseHandler(primaryStage);
            
            // Register Observer
            if (engine != null) {
                engine.setMapObserver(this);
            }
            
            primaryStage.show();
            LOGGER.info("GUI started successfully in " + (is3DMode ? "3D" : "2D") + " mode.");

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to start GUI", e);
            Platform.exit();
        }
    }

    /**
     * Shows a dialog to let the user choose the visualization mode.
     * Blocks until a choice is made.
     */
    private void promptForMapMode() {
        Alert alert = new Alert(AlertType.CONFIRMATION);
        alert.setTitle("Select Visualization Mode");
        alert.setHeaderText("Choose Map Type");
        alert.setContentText("How would you like to view the simulation?");

        ButtonType buttonType3D = new ButtonType("3D View");
        ButtonType buttonType2D = new ButtonType("2D View");
        ButtonType buttonTypeCancel = new ButtonType("Cancel", ButtonData.CANCEL_CLOSE);

        alert.getButtonTypes().setAll(buttonType3D, buttonType2D, buttonTypeCancel);

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent()) {
            if (result.get() == buttonType3D) {
                is3DMode = true;
            } else if (result.get() == buttonType2D) {
                is3DMode = false;
            } else {
                // Cancelled
                Platform.exit();
                System.exit(0);
            }
        }
    }

    private void initializeCommonComponents() {
        this.controlPanel = new ControlPanel(engine, vehicleManager, trafficLightManager, infraMgr);
        this.dashboard = new DashBoard(statsCollector, infraMgr);
    }

    private SplitPane createSidebar() {
        SplitPane sidebar = new SplitPane();
        sidebar.setOrientation(Orientation.VERTICAL);
        sidebar.setPrefWidth(320);
        sidebar.setStyle("-fx-background-color: #f4f4f4; -fx-border-color: #cccccc; -fx-border-width: 0 0 0 1;");
        sidebar.getItems().addAll(controlPanel, dashboard);
        sidebar.setDividerPositions(0.5); 
        return sidebar;
    }

    /**
     * Creates either the 3D SubScene or the 2D Canvas based on user choice.
     */
    private Node createMapNode(BorderPane root, SplitPane sidebar) {
        if (is3DMode) {
            // --- 3D MODE ---
            this.mapView3D = new MapView3D(engine, vehicleManager, controlPanel);
            SubScene subScene = new SubScene(mapView3D.getRoot(), 800, 800, true, SceneAntialiasing.BALANCED);
            subScene.setPickOnBounds(true);
            subScene.setCamera(mapView3D.getCamera());
            
            // Setup interactions
            setup3DMouseHandlers(subScene);
            
            // Wrap in StackPane for layout binding
            StackPane container = new StackPane(subScene);
            subScene.widthProperty().bind(container.widthProperty());
            subScene.heightProperty().bind(container.heightProperty());
            return container;
            
        } else {
            // --- 2D MODE ---
            this.mapView2D = new MapView(engine, vehicleManager);
            
            // Link 2D selection to ControlPanel
            this.mapView2D.setOnVehicleSelected(id -> {
                if (controlPanel != null) controlPanel.selectVehicle(id);
            });
            
            return mapView2D; // MapView extends Canvas or Pane usually
        }
    }

    private void setup3DMouseHandlers(SubScene subScene) {
        subScene.setOnMousePressed(event -> {
            mousePosX = event.getSceneX();
            mousePosY = event.getSceneY();
        });

        subScene.setOnMouseDragged(event -> {
            double dx = event.getSceneX() - mousePosX;
            double dy = event.getSceneY() - mousePosY;

            if (event.isPrimaryButtonDown()) {
                mapView3D.updateRotation(dx * 0.2, -dy * 0.2);
            } else if (event.isShiftDown() && event.isSecondaryButtonDown()) {
                mapView3D.moveHorizontale(dx, -dy);
            } else if (!event.isShiftDown() && event.isSecondaryButtonDown()) {
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

    private void setupCloseHandler(Stage stage) {
        stage.setOnCloseRequest(e -> {
            LOGGER.info("Application closing...");
            if (engine != null) engine.stop();
            Platform.exit();
            System.exit(0);
        });
    }

    @Override
    public void refresh() {
        Platform.runLater(() -> {
            try {
                // Update Sidebar
                if (controlPanel != null) controlPanel.updateRealTimeData();
                if (dashboard != null) dashboard.update();
                
                // Update Map based on active mode
                if (is3DMode && mapView3D != null) {
                    mapView3D.renderRoads(); 
                    mapView3D.updateVehicles(vehicleManager); 
                    mapView3D.updateTrafficLights();
                } else if (!is3DMode && mapView2D != null) {
                    mapView2D.render(); // Ensure MapView has a render() method
                }
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Error during UI refresh", e);
            }
        });
    }
}