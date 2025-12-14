package de.frauas.group6.traffic.simulator.view;

import de.frauas.group6.traffic.simulator.core.ISimulationEngine;
import de.frauas.group6.traffic.simulator.vehicles.IVehicleManager;
import de.frauas.group6.traffic.simulator.vehicles.IVehicle;
import de.frauas.group6.traffic.simulator.infrastructure.ITrafficLightManager; 
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import java.util.List;
import java.util.Optional;

public class ControlPanel extends VBox {

    private ISimulationEngine engine;
    private IVehicleManager vehicleManager;
    private ITrafficLightManager trafficLightManager;
    
    // Callback pour demander à la vue (MapView) de se rafraîchir immédiatement
    private Runnable onRefreshRequest;
    
    // --- UI Components ---
    
    // Vehicle Section
    private TextField txtSelectedId;
    private ComboBox<String> cbRoute; 
    private ComboBox<String> cbLane;    
    private ComboBox<String> cbType;    
    private ComboBox<String> cbColor;   
    private Slider sliderSpeed;
    private Spinner<Integer> spinnerCount;
    
    // Traffic Light Section
    private ComboBox<String> cbTrafficLight;
    private Label lblTlState;
    private Circle indicatorLight;
    private Label lblPhaseTime;
    
    // Global
    private Label lblTime;

    public ControlPanel(ISimulationEngine engine, IVehicleManager vm) {
        this.engine = engine;
        this.vehicleManager = vm;
        initializeUI();
    }
    
    // Injection des dépendances optionnelles
    public void setTrafficLightManager(ITrafficLightManager tm) {
        this.trafficLightManager = tm;
    }
    
    public void setOnRefreshRequest(Runnable callback) {
        this.onRefreshRequest = callback;
    }

    private void initializeUI() {
        // Style Global Dark Theme
        this.setStyle("-fx-background-color: linear-gradient(to bottom, #2b2b2b, #1a1a1a); -fx-border-color: #444; -fx-border-width: 0 2 0 0;");
        setPadding(new Insets(20));
        setSpacing(15);
        setAlignment(Pos.TOP_CENTER);
        setPrefWidth(350);
        
        // --- HEADER (Simulation Controls) ---
        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER);
        Button btnPlay = createIconBtn("▶", Color.LIGHTGREEN);
        Button btnPause = createIconBtn("⏸", Color.ORANGE);
        Button btnStep = createIconBtn("⏯", Color.LIGHTBLUE);
        
        btnPlay.setOnAction(e -> { if(engine!=null) { if(engine.isPaused()) engine.resume(); else engine.start(); }});
        btnPause.setOnAction(e -> { if(engine!=null) engine.pause(); });
        btnStep.setOnAction(e -> { if(engine!=null) engine.step(); });
        
        header.getChildren().addAll(btnPlay, btnPause, btnStep);
        
        lblTime = new Label("TIME: 0.00 s");
        lblTime.setTextFill(Color.WHITE);
        lblTime.setFont(Font.font("Consolas", FontWeight.BOLD, 18));
        
        // --- VEHICLE CONTROL CARD ---
        VBox vehicleCard = createCard("VEHICLE CONTROL");
        
        // Action Buttons
        HBox actions = new HBox(8);
        actions.setAlignment(Pos.CENTER);
        Button btnCreate = createFlatBtn("INJECT", "#4CAF50");
        Button btnModify = createFlatBtn("UPDATE", "#2196F3");
        Button btnDelete = createFlatBtn("DELETE", "#F44336");
        actions.getChildren().addAll(btnCreate, btnModify, btnDelete);
        
        // Inputs
        txtSelectedId = new TextField(); 
        txtSelectedId.setPromptText("Vehicle ID"); 
        txtSelectedId.setStyle("-fx-background-color: #444; -fx-text-fill: white; -fx-prompt-text-fill: #888;");
        txtSelectedId.setDisable(true); 
        
        cbRoute = createStyledCombo("Route");
        cbRoute.getItems().addAll("-E48","E45","E46","E50","E49","-E51"); 
        cbRoute.getSelectionModel().select(0);
        
        cbLane = createStyledCombo("Lane");
        cbLane.getItems().addAll("Right", "middle", "Left"); 
        cbLane.getSelectionModel().select(0);
        
        cbType = createStyledCombo("Type");
        cbType.getItems().addAll("Standard-Car", "Truck", "Emergency-Vehicle", "City-Bus");
        cbType.getSelectionModel().select(0);
        
        cbColor = createStyledCombo("Color");
        cbColor.getItems().addAll("Rot", "Green", "Yellow");
        cbColor.getSelectionModel().select(0);
        
        sliderSpeed = new Slider(0, 50, 15);
        Label lblSpeed = new Label("Speed: 15.0 m/s");
        lblSpeed.setTextFill(Color.LIGHTGRAY);
        sliderSpeed.valueProperty().addListener((o,old,val) -> lblSpeed.setText(String.format("Speed: %.1f m/s", val.doubleValue())));
        
        spinnerCount = new Spinner<>(1, 10, 1);
        spinnerCount.setStyle("-fx-body-color: #444; -fx-text-fill: white;");
        spinnerCount.setMaxWidth(Double.MAX_VALUE);
        
        vehicleCard.getChildren().addAll(
            actions, 
            new Separator(), 
            createLabel("Selected ID:"), txtSelectedId,
            createLabel("Parameters:"), cbRoute, cbLane, cbType, cbColor,
            lblSpeed, sliderSpeed,
            createLabel("Quantity:"), spinnerCount
        );
        
        // --- TRAFFIC LIGHT CONTROL CARD ---
        VBox tlCard = createCard("TRAFFIC LIGHTS");
        
        cbTrafficLight = createStyledCombo("Select Junction");
        
        HBox statusRow = new HBox(15);
        statusRow.setAlignment(Pos.CENTER_LEFT);
        indicatorLight = new Circle(10, Color.web("#333"));
        indicatorLight.setStroke(Color.GRAY);
        lblTlState = new Label("State: --");
        lblTlState.setTextFill(Color.WHITE);
        lblTlState.setFont(Font.font("System", FontWeight.BOLD, 13));
        statusRow.getChildren().addAll(indicatorLight, lblTlState);
        
        lblPhaseTime = new Label("Remaining: -- s");
        lblPhaseTime.setTextFill(Color.LIGHTGRAY);
        
        HBox smartBtns = new HBox(10);
        smartBtns.setAlignment(Pos.CENTER);
        Button btnGreen = createFlatBtn("FORCE GREEN", "#00E676");
        Button btnRed = createFlatBtn("FORCE RED", "#FF1744");
        btnGreen.setPrefWidth(120);
        btnRed.setPrefWidth(120);
        smartBtns.getChildren().addAll(btnGreen, btnRed);
        
        tlCard.getChildren().addAll(
            createLabel("Junction:"), cbTrafficLight,
            statusRow, lblPhaseTime,
            new Separator(),
            createLabel("Override Logic:"), smartBtns
        );
        
        getChildren().addAll(header, lblTime, vehicleCard, tlCard);
        
        // Setup Logic
        setupVehicleHandlers(btnCreate, btnModify, btnDelete);
        setupTrafficLightHandlers(btnGreen, btnRed);
    }
    
    // --- PUBLIC INTERACTION METHODS ---
    
    /**
     * Appelé quand on clique sur un véhicule dans la MapView.
     * Remplit le formulaire avec les données du véhicule.
     */
    public void selectVehicle(String id) {
        if (vehicleManager == null || id == null) return;
        
        txtSelectedId.setText(id);
        
        Optional<IVehicle> vOpt = vehicleManager.getAllVehicles().stream()
                .filter(v -> v.getId().equals(id))
                .findFirst();
                
        if (vOpt.isPresent()) {
            IVehicle v = vOpt.get();
            sliderSpeed.setValue(v.getSpeed());
            
            String c = v.getColor(); 
            if (c != null && cbColor.getItems().contains(c)) {
                cbColor.setValue(c);
            }
            
            String edge = v.getEdgeId();
            if (edge != null && cbRoute.getItems().contains(edge)) {
                cbRoute.setValue(edge);
            }
        }
    }
    
    /**
     * Appelé par la boucle de jeu principale pour mettre à jour les infos temps réel (Temps, Feux).
     */
    public void updateRealTimeData() {
        if(engine == null) return;
        lblTime.setText(String.format("TIME: %.2f s", engine.getCurrentSimulationTime()));
        updateInfo(); // Mise à jour des infos Traffic Light sélectionné
    }
    
    // --- INTERNAL HANDLERS ---
    
    private void setupVehicleHandlers(Button create, Button mod, Button del) {
        // INJECT (Création)
        create.setOnAction(e -> {
            try {
                if (vehicleManager != null) {
                    vehicleManager.injectVehicle(cbRoute.getValue(), cbLane.getValue(), cbType.getValue(), cbColor.getValue(), spinnerCount.getValue(), sliderSpeed.getValue());
                }
            } catch(Exception ex) { ex.printStackTrace(); }
        });
        
        // DELETE (Suppression)
        del.setOnAction(e -> {
            try { if (vehicleManager!=null) {
                // Utilisation correcte de la quantité du spinner
                vehicleManager.deleteVehicle(cbRoute.getValue(), cbColor.getValue(), sliderSpeed.getValue(), spinnerCount.getValue());
                txtSelectedId.clear();
            }} catch(Exception ex) {}
        });
        
        // UPDATE (Modification)
        mod.setOnAction(e -> {
            // Sécurité : Simulation en PAUSE requise
            if (engine != null && !engine.isPaused()) {
                System.out.println("Action refusée : Veuillez mettre la simulation en PAUSE.");
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("Simulation en cours");
                alert.setHeaderText(null);
                alert.setContentText("Vous devez mettre la simulation en PAUSE pour modifier un véhicule !");
                alert.showAndWait();
                return;
            }

            String id = txtSelectedId.getText();
            if (id == null || id.isEmpty()) return;

            try { 
                if(vehicleManager != null) {
                    vehicleManager.modifyVehicle(id, cbColor.getValue(), sliderSpeed.getValue());
                    System.out.println("Véhicule " + id + " modifié.");
                    
                    // Rafraîchissement immédiat de la vue
                    if (onRefreshRequest != null) onRefreshRequest.run();
                }
            } catch(Exception ex) {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Erreur");
                alert.setContentText(ex.getMessage());
                alert.showAndWait();
            }
        });
    }
    
    private void setupTrafficLightHandlers(Button btnGreen, Button btnRed) {
        // Rafraîchir la liste des feux si on clique sur la combobox
        cbTrafficLight.setOnMouseClicked(e -> {
            if(engine==null) return;
            List<String> ids = engine.getTrafficLightIdList();
            if(ids!=null && !ids.equals(cbTrafficLight.getItems())) cbTrafficLight.getItems().setAll(ids);
        });
        
        cbTrafficLight.setOnAction(e -> updateInfo());
        
        // FORCE GREEN
        btnGreen.setOnAction(e -> {
            String id = cbTrafficLight.getValue();
            if (id == null) return;
            
            // Logique intelligente ou fallback
            if (trafficLightManager != null) {
                trafficLightManager.forceGreen(id);
            } else if (engine != null) {
                try { engine.setTrafficLightPhase(id, 0); } catch(Exception ex) {}
            }
            
            updateInfo();
            // Rafraîchissement immédiat de la vue
            if (onRefreshRequest != null) onRefreshRequest.run();
        });
        
        // FORCE RED
        btnRed.setOnAction(e -> {
            String id = cbTrafficLight.getValue();
            if (id == null) return;
            
            if (trafficLightManager != null) {
                trafficLightManager.forceRed(id);
            } else if (engine != null) {
                try { engine.setTrafficLightPhase(id, 2); } catch(Exception ex) {}
            }
            
            updateInfo();
            // Rafraîchissement immédiat de la vue
            if (onRefreshRequest != null) onRefreshRequest.run();
        });
    }
    
    private void updateInfo() {
        String id = cbTrafficLight.getValue();
        if (id == null || engine == null) return;
        
        try {
            String state = engine.getTrafficLightState(id);
            // Conversion double pour les décimales
            double timeLeft = engine.getTrafficLightRemainingTime(id) / 1000.0;
            
            lblTlState.setText("Phase: " + engine.getTrafficLightPhase(id) + " (" + state + ")");
            lblPhaseTime.setText(String.format("Remaining: %.1f s", timeLeft));
            
            if(state.toLowerCase().contains("g")) indicatorLight.setFill(Color.LIME);
            else if(state.toLowerCase().contains("y")) indicatorLight.setFill(Color.ORANGE);
            else indicatorLight.setFill(Color.RED);
            
        } catch(Exception e) {}
    }
    
    // --- STYLE HELPERS ---
    
    private VBox createCard(String title) {
        VBox v = new VBox(10);
        v.setStyle("-fx-background-color: #383838; -fx-background-radius: 8; -fx-padding: 15; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.4), 10, 0, 0, 2);");
        Label l = new Label(title);
        l.setTextFill(Color.web("#aaa"));
        l.setFont(Font.font("System", FontWeight.BOLD, 11));
        v.getChildren().add(l);
        return v;
    }
    
    private Button createFlatBtn(String text, String color) {
        Button b = new Button(text);
        b.setStyle("-fx-background-color: " + color + "; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 4; -fx-cursor: hand;");
        b.setMaxWidth(Double.MAX_VALUE);
        return b;
    }
    
    private Button createIconBtn(String icon, Color c) {
        Button b = new Button(icon);
        b.setStyle("-fx-background-color: #" + c.toString().substring(2,8) + "; -fx-text-fill: #222; -fx-font-weight: bold; -fx-font-size: 14; -fx-background-radius: 20;");
        b.setPrefSize(40, 40);
        return b;
    }
    
    private ComboBox<String> createStyledCombo(String prompt) {
        ComboBox<String> cb = new ComboBox<>();
        cb.setPromptText(prompt);
        cb.setMaxWidth(Double.MAX_VALUE);
        cb.setStyle("-fx-background-color: #555; -fx-text-fill: white; -fx-mark-color: white;");
        return cb;
    }
    
    private Label createLabel(String t) {
        Label l = new Label(t);
        l.setTextFill(Color.WHITE);
        l.setFont(Font.font(10));
        return l;
    }
}