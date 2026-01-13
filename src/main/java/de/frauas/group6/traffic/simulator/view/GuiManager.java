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
import javafx.scene.SceneAntialiasing;
import javafx.scene.SubScene;
import javafx.scene.control.SplitPane; // Remplacement de VBox par SplitPane
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
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
    
    private MapView3D mapView3D; 
    private double mousePosX;
    private double mousePosY;
    private double zoomZ;

    //static method to launch IU from  JavaApp
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
        
        // (MapView)
               
      this.mapView3D = new MapView3D(engine,vehicleManager,controlPanel);
        
        
      SubScene subScene = new SubScene(mapView3D.getRoot(), 800, 800, true, SceneAntialiasing.BALANCED);
      subScene.setPickOnBounds(true);
      StackPane mapContainer= new StackPane(subScene);
      root.setCenter(mapContainer);
      
        subScene.setOnMousePressed(event->{
         mousePosX= event.getSceneX();
         mousePosY= event.getSceneY();
         
        });
       subScene.setOnMouseDragged(event->{
       if(event.isPrimaryButtonDown()) {
        double dx= event.getSceneX()-mousePosX;
        double dy= event.getSceneY()-mousePosY;
        mapView3D.updateRotation(dx*0.2,-dy*0.2);}
       
         else if(event.isShiftDown() && event.isSecondaryButtonDown() ) {
        
        
        double dy= event.getSceneY()-mousePosY;
        double dx= event.getSceneX()-mousePosX;
        mapView3D.moveHorizontale(dx,-dy);
       
       }
       else if( !(event.isShiftDown()) && event.isSecondaryButtonDown()) {
        double dy= event.getSceneY()-mousePosY;
              mapView3D. moveRoadsVertical(dy);
       }
       
       
       
       mousePosX = event.getSceneX();
       mousePosY = event.getSceneY();
       });
       
        subScene.setOnScroll(event->{
        zoomZ=event.getDeltaY();
        mapView3D.Zoom(zoomZ*2.0);
       });
        
        subScene.setCamera(mapView3D.getCamera());
        
        
        subScene.widthProperty().bind(root.widthProperty().subtract(350)); 
        subScene.heightProperty().bind(root.heightProperty());
        // 2. Right : Sidebar
        SplitPane sidebar = new SplitPane();
        sidebar.setOrientation(Orientation.VERTICAL); //  vertical Empilement
        sidebar.setPrefWidth(320);
        sidebar.setStyle("-fx-background-color: #f4f4f4; -fx-border-color: #cccccc; -fx-border-width: 0 0 0 1;");
        
        // A. ControlPanel
        this.controlPanel = new ControlPanel(engine, vehicleManager, trafficLightManager,infraMgr);
        
        // B. Dashboard
        this.dashboard = new DashBoard(statsCollector,infraMgr);

        // ajust components of  SplitPane
        sidebar.getItems().addAll(controlPanel, dashboard);
        
        
        sidebar.setDividerPositions(0.5);
        
        root.setRight(sidebar);

      
        //mapView.setOnVehicleSelected(id -> controlPanel.selectVehicle(id));

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
        	if (controlPanel != null) controlPanel.updateRealTimeData();
            if (dashboard != null)  dashboard.update();
            if (mapView3D != null) {
                mapView3D.renderRoads(); // Method ghadi n-redouha public
                mapView3D.updateVehicles(vehicleManager); 
                mapView3D.updateTrafficLights();
            }
            
            
        });
    }
}