package de.frauas.group6.traffic.simulator.view;

import de.frauas.group6.traffic.simulator.analytics.IStatsCollector;
import de.frauas.group6.traffic.simulator.core.ISimulationEngine;
import de.frauas.group6.traffic.simulator.infrastructure.IInfrastructureManager;
import de.frauas.group6.traffic.simulator.infrastructure.ITrafficLightManager;
import de.frauas.group6.traffic.simulator.vehicles.IVehicleManager;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Orientation; // Import pour l'orientation du SplitPane
import javafx.scene.Scene;
import javafx.scene.control.SplitPane; // Remplacement de VBox par SplitPane
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

/**
 * MEMBRE 4 - VIEW
 * Point d'entrée principal de l'interface graphique.
 */
public class GuiManager extends Application implements IMapObserver {

    private static ISimulationEngine staticEngine;
    private static IVehicleManager staticVehicleManager;
    private static ITrafficLightManager staticTrafficLightManager;
    private static IStatsCollector staticStatsCollector;
    private static IInfrastructureManager staticInfrastructureManager;
    
    private ISimulationEngine engine;
    private IVehicleManager vehicleManager;
    private ITrafficLightManager trafficLightManager;
    private IStatsCollector statsCollector;
    private IInfrastructureManager infraMgr;
    
    private MapView mapView;
    private ControlPanel controlPanel;
    private DashBoard dashboard; 

    // Méthode statique pour lancer l'UI depuis le Main
    public static void startUI(ISimulationEngine engineInstance, IVehicleManager vmInstance, ITrafficLightManager trafficLightManager,IStatsCollector statsCollector,IInfrastructureManager infraMgr) {
        staticEngine = engineInstance;
        staticVehicleManager = vmInstance;
        staticTrafficLightManager =  trafficLightManager;
        staticStatsCollector = statsCollector;
        staticInfrastructureManager = infraMgr;
        new Thread(() -> Application.launch(GuiManager.class)).start();
    }

    @Override
    public void start(Stage primaryStage) {
        this.engine = staticEngine;
        this.vehicleManager = staticVehicleManager;
        this.trafficLightManager = staticTrafficLightManager;
        this.statsCollector = staticStatsCollector;
        this.infraMgr = staticInfrastructureManager;
        
        BorderPane root = new BorderPane();
        
        // 1. CENTRE : La Carte (MapView)
        this.mapView = new MapView(engine, vehicleManager);
        root.setCenter(mapView);
        
        // 2. DROITE : Sidebar
        // Utilisation d'un SplitPane au lieu d'une VBox pour permettre le redimensionnement manuel
        SplitPane sidebar = new SplitPane();
        sidebar.setOrientation(Orientation.VERTICAL); // Empilement vertical
        sidebar.setPrefWidth(320);
        sidebar.setStyle("-fx-background-color: #f4f4f4; -fx-border-color: #cccccc; -fx-border-width: 0 0 0 1;");
        
        // A. ControlPanel
        this.controlPanel = new ControlPanel(engine, vehicleManager, trafficLightManager,infraMgr);
        
        // B. Dashboard
        this.dashboard = new DashBoard(statsCollector,infraMgr);

        // Ajout des composants au SplitPane
        sidebar.getItems().addAll(controlPanel, dashboard);
        
        // DÉFINITION DE LA TAILLE PAR DÉFAUT
        // 0.6 signifie que le ControlPanel (premier élément) prendra 60% de la hauteur au démarrage
        sidebar.setDividerPositions(0.5);
        
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
            if (dashboard != null)  dashboard.update(); 
            
        });
    }
}