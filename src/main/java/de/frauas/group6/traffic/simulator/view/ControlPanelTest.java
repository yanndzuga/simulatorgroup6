package de.frauas.group6.traffic.simulator.view;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.scene.control.Label;
import javafx.stage.Stage;

// --- IMPORTS DAROURIYIN ---
// (Ila tle3 lik error hna, dir Ctrl+Shift+O)
import de.frauas.group6.traffic.simulator.core.SimulationEngine;
import de.frauas.group6.traffic.simulator.infrastructure.InfrastructureManager;
import de.frauas.group6.traffic.simulator.infrastructure.TrafficLightManager;
import de.frauas.group6.traffic.simulator.vehicles.VehicleManager;

public class ControlPanelTest extends Application {

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        try {
            System.out.println("Lancement de l'application...");

            // 1. Création de l'Engine (Le Moteur SUMO)
            SimulationEngine realEngine = new SimulationEngine();
            
            // 2. Création des Managers (On leur donne l'engine)
            // NOTE: Ila kanu Managers dyal s7abk ma-kay-akhdouch 'engine' f Constructor,
            // dir: new VehicleManager(); w mn be3d vm.setEngine(realEngine);
            InfrastructureManager infraManager = new InfrastructureManager(realEngine);
            TrafficLightManager trafficLightManager = new TrafficLightManager(realEngine);
            VehicleManager vehicleManager = new VehicleManager(realEngine);
            
            // 3. Liaison Inverse (Engine khassu ya3ref Managers)
            realEngine.setInfrastructureManager(infraManager);
            realEngine.setTrafficLightManager(trafficLightManager);
            realEngine.setVehicleManager(vehicleManager);
            
            // 4. Initialisation du Controller (Le Cerveau de l'UI)
            SimulationController realController = new SimulationController(realEngine, vehicleManager, trafficLightManager);
            
            // 5. Création du Panel (La Vue)
            ControlPanel controlPanel = new ControlPanel(realController);

            // 6. Setup de la Fenêtre (Scene)
            BorderPane root = new BorderPane();
            root.setRight(controlPanel);
            
            // (Optionnel) Un texte au centre pour dire que c'est pret
            Label statusLabel = new Label("Simulation Ready. Click Start.");
            root.setCenter(statusLabel);

            Scene scene = new Scene(root, 900, 600);
            
            // 7. Important: Arrêter SUMO quand on ferme la fenêtre
            primaryStage.setOnCloseRequest(e -> {
                System.out.println("Fermeture...");
                if (realEngine != null) {
                    realEngine.stop();
                }
                System.exit(0);
            });

            primaryStage.setTitle("Traffic Simulator - Group 6");
            primaryStage.setScene(scene);
            primaryStage.show();
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}