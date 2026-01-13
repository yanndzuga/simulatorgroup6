package de.frauas.group6.traffic.simulator.view;

import de.frauas.group6.traffic.simulator.core.ISimulationEngine;
import de.frauas.group6.traffic.simulator.infrastructure.ITrafficLightManager;
import de.frauas.group6.traffic.simulator.vehicles.IVehicleManager;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

/**
 * MEMBER 4 - VIEW
 * Main entry point for the GUI.
 */
public class GuiManager extends Application implements IMapObserver {

    private static ISimulationEngine staticEngine;
    private static IVehicleManager staticVehicleManager; 
    private static ITrafficLightManager staticInfrastrutureManager;
    
    private ISimulationEngine engine;
    private IVehicleManager vehicleManager;
    private ITrafficLightManager InfrastrutureManager;
    
    private MapView mapView;
    private ControlPanel controlPanel;
    
    private VBox dashboardPlaceholder; 

    public static void startUI(ISimulationEngine engineInstance, IVehicleManager vmInstance, ITrafficLightManager InfrastrutureManager) {
        staticEngine = engineInstance;
        staticVehicleManager = vmInstance;
        staticInfrastrutureManager = InfrastrutureManager;
        new Thread(() -> Application.launch(GuiManager.class)).start();
    }

    @Override
    public void start(Stage primaryStage) {
        this.engine = staticEngine;
        this.vehicleManager = staticVehicleManager;
        this.InfrastrutureManager = staticInfrastrutureManager;
        
        BorderPane root = new BorderPane();
        
        // 1. Center: MapView
        this.mapView = new MapView(engine, vehicleManager);
        root.setCenter(mapView);
        
        // 2. Right: Sidebar
        VBox sidebar = new VBox();
        sidebar.setPrefWidth(320);
        sidebar.setStyle("-fx-background-color: #f4f4f4; -fx-border-color: #cccccc; -fx-border-width: 0 0 0 1;");
        
        // Control Panel
        this.controlPanel = new ControlPanel(engine, vehicleManager, InfrastrutureManager);
        
        // Dashboard Placeholder
        this.dashboardPlaceholder = new VBox();
        dashboardPlaceholder.setPrefHeight(300);
        
        sidebar.getChildren().addAll(controlPanel, dashboardPlaceholder);
        root.setRight(sidebar);

        // Vehicle selection binding
        mapView.setOnVehicleSelected(id -> controlPanel.selectVehicle(id));

        Scene scene = new Scene(root, 1280, 800);
        primaryStage.setTitle("Traffic Simulator - Group 6");
        primaryStage.setScene(scene);
        
        // Proper shutdown
        primaryStage.setOnCloseRequest(e -> {
            if(engine != null) engine.stop();
            Platform.exit();
            System.exit(0);
        });
        
        engine.setMapObserver(this);
        primaryStage.show();
    }

    @Override
    public void refresh() {
        Platform.runLater(() -> {
            if (mapView != null) mapView.render();
            if (controlPanel != null) controlPanel.updateRealTimeData();
        });
    }
}