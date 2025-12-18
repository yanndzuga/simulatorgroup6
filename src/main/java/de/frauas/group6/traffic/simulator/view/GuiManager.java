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
	 * MEMBRE 4 - VIEW
	 * Point d'entrée principal de l'interface graphique.
	 */
	public class GuiManager extends Application implements IMapObserver {

	    private static ISimulationEngine staticEngine;
	    private static IVehicleManager staticVehicleManager; // Nécessaire pour le ControlPanel
	    private static ITrafficLightManager staticTrafficLightManage;
	    
	    private ISimulationEngine engine;
	    private IVehicleManager vehicleManager;
	    private ITrafficLightManager TrafficLightManage;
	    
	    private MapView mapView;
	    private ControlPanel controlPanel;
	    
	    // Placeholder pour le travail de ton collègue
	    private VBox dashboardPlaceholder; 

	    // Méthode statique pour lancer l'UI depuis le Main
	    public static void startUI(ISimulationEngine engineInstance, IVehicleManager vmInstance, ITrafficLightManager TrafficLightManager) {
	        staticEngine = engineInstance;
	        staticVehicleManager = vmInstance;
	        staticTrafficLightManage =  TrafficLightManager;
	        new Thread(() -> Application.launch(GuiManager.class)).start();
	    }

	    @Override
	    public void start(Stage primaryStage) {
	        this.engine = staticEngine;
	        this.vehicleManager = staticVehicleManager;
	        this.TrafficLightManage = staticTrafficLightManage;
	        
	        BorderPane root = new BorderPane();
	        
	        // 1. CENTRE : La Carte (MapView)
	        this.mapView = new MapView(engine, vehicleManager);
	        // On lie le MapView au ControlPanel pour la sélection (voir plus bas)
	        root.setCenter(mapView);
	        
	        // 2. DROITE : Sidebar
	        VBox sidebar = new VBox();
	        sidebar.setPrefWidth(320);
	        sidebar.setStyle("-fx-background-color: #f4f4f4; -fx-border-color: #cccccc; -fx-border-width: 0 0 0 1;");
	        
	        // A. Ton ControlPanel
	        this.controlPanel = new ControlPanel(engine, vehicleManager,TrafficLightManage);
	        
	        // B. Le Dashboard de ton collègue (Placeholder)
	        this.dashboardPlaceholder = new VBox();
	        dashboardPlaceholder.setPrefHeight(300);
	        // dashboardPlaceholder.getChildren().add(new Label("Espace réservé au Dashboard"));
	        
	        sidebar.getChildren().addAll(controlPanel, dashboardPlaceholder);
	        root.setRight(sidebar);

	        // Connexion : Quand on clique sur une voiture, le ControlPanel se remplit
	        mapView.setOnVehicleSelected(id -> controlPanel.selectVehicle(id));

	        // Configuration Scène
	        Scene scene = new Scene(root, 1280, 800);
	        primaryStage.setTitle("Traffic Simulator - Group 6");
	        primaryStage.setScene(scene);
	        
	        // Arrêt propre
	        primaryStage.setOnCloseRequest(e -> {
	            if(engine != null) engine.stop();
	            Platform.exit();
	            System.exit(0);
	        });
	        
	        // Enregistrement de l'observer
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


