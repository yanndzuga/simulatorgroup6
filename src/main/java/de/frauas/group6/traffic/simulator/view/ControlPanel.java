package de.frauas.group6.traffic.simulator.view;

import java.util.List;
import java.util.Optional;

import de.frauas.group6.traffic.simulator.core.ISimulationEngine;
import de.frauas.group6.traffic.simulator.vehicles.IVehicleManager;
import de.frauas.group6.traffic.simulator.vehicles.IVehicle;
import de.frauas.group6.traffic.simulator.infrastructure.IInfrastructureManager;
import de.frauas.group6.traffic.simulator.infrastructure.ITrafficLightManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;



// Changed extension from VBox to ScrollPane to allow scrolling
public class ControlPanel extends ScrollPane {

    private ISimulationEngine engine;
    private IVehicleManager vehicleManager;
    private ITrafficLightManager trafficLightManager;
    private IInfrastructureManager infraMgr;
    private Runnable onRefreshRequest;
    
    // UI Components
    private TextField txtSelectedId;
    private ComboBox<String> cbRoute; 
    // private ComboBox<String> cbLane;     
    private ComboBox<String> cbType;    
    private ComboBox<String> cbColor;   
    private Slider sliderSpeed;
    private Spinner<Integer> spinnerCount;
    
    private ComboBox<String> cbTrafficLight;
    private Label lblTlState;
    private Circle indicatorLight;
    private Label lblPhaseTime;
    private Label lblTime;

    public ControlPanel(ISimulationEngine engine, IVehicleManager vm, ITrafficLightManager tm, IInfrastructureManager im) {
        this.engine = engine;
        this.vehicleManager = vm;
        this.trafficLightManager = tm;
        this.infraMgr= im;
        initializeUI();
    }
    
    public void setOnRefreshRequest(Runnable callback) {
        this.onRefreshRequest = callback;
    }

    private void initializeUI() {
        // Create an inner VBox to hold the content
        VBox content = new VBox();
        
        // Apply styles to the inner content VBox instead of 'this'
        content.setStyle("-fx-background-color: linear-gradient(to bottom, #2b2b2b, #1a1a1a);");
        content.setPadding(new Insets(15));
        content.setSpacing(10);
        content.setAlignment(Pos.TOP_CENTER);
        
        // --- HEADER ---
        HBox header = new HBox(10);
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
        lblTime.setFont(Font.font("Consolas", FontWeight.BOLD, 16));
        
        // --- VEHICLE CONTROL CARD ---
        VBox vehicleCard = createCard("VEHICLE CONTROL");
        
        // Action Buttons Row 1
        HBox actions = new HBox(5);
        actions.setAlignment(Pos.CENTER);
        Button btnCreate = createFlatBtn("INJECT", "#4CAF50");
        Button btnModify = createFlatBtn("UPDATE", "#2196F3");
        HBox.setHgrow(btnCreate, Priority.ALWAYS);
        HBox.setHgrow(btnModify, Priority.ALWAYS);
        actions.getChildren().addAll(btnCreate, btnModify);

        // Action Buttons Row 2 (Delete & Select)
        HBox actions2 = new HBox(5);
        actions2.setAlignment(Pos.CENTER);
        Button btnDelete = createFlatBtn("DELETE", "#F44336");
        Button btnSelect = createFlatBtn("SELECT", "#FFC107"); // Bouton demandé
        btnSelect.setStyle("-fx-background-color: #FFC107; -fx-text-fill: black; -fx-font-weight: bold; -fx-background-radius: 4; -fx-cursor: hand;");
        
        HBox.setHgrow(btnDelete, Priority.ALWAYS);
        HBox.setHgrow(btnSelect, Priority.ALWAYS);
        actions2.getChildren().addAll(btnDelete, btnSelect);
        
        // Inputs
        txtSelectedId = new TextField(); 
        txtSelectedId.setPromptText("Vehicle ID (Click Map)"); 
        txtSelectedId.setStyle("-fx-background-color: #444; -fx-text-fill: white;");
        txtSelectedId.setDisable(true); 
        
        cbRoute = createStyledCombo("Route");
        List<String> routes = infraMgr.loadRouteIds("minimal.rou.xml");
        cbRoute.getItems().addAll(routes);
        cbRoute.getSelectionModel().selectFirst();
        
        
       
        
        /*cbLane = createStyledCombo("Lane");
        cbLane.getItems().addAll("Right", "middle", "Left"); 
        cbLane.getSelectionModel().select(0);*/
        
        cbType = createStyledCombo("Type");
        cbType.getItems().addAll("Standard-Car", "Truck", "Emergency-Vehicle", "City-Bus");
        cbType.getSelectionModel().select(0);
        
        cbColor = createStyledCombo("Color");
        // Mise à jour pour correspondre aux Switch Case de VehicleManager + Option "All"
        cbColor.getItems().addAll("Red", "Green", "Yellow", "All");
        cbColor.getSelectionModel().select(0);
        
        sliderSpeed = new Slider(0, 60, 15);
        Label lblSpeed = new Label("Speed: 15.0 m/s");
        lblSpeed.setTextFill(Color.LIGHTGRAY);
        sliderSpeed.valueProperty().addListener((o,old,val) -> lblSpeed.setText(String.format("Speed: %.1f m/s", val.doubleValue())));
        
        spinnerCount = new Spinner<>(1, 100, 1);
        spinnerCount.setStyle("-fx-body-color: #444; -fx-text-fill: white;");
        spinnerCount.setMaxWidth(Double.MAX_VALUE);
        
        vehicleCard.getChildren().addAll(
            actions, actions2, // Ajout de la ligne avec SELECT
            new Separator(), 
            createLabel("ID:"), txtSelectedId,
            createLabel("Config:"), cbRoute, cbType, cbColor,
            lblSpeed, sliderSpeed,
            createLabel("Qty:"), spinnerCount
        );
        
        // --- TRAFFIC LIGHT CONTROL CARD ---
        VBox tlCard = createCard("TRAFFIC LIGHTS");
        
        cbTrafficLight = createStyledCombo("Select Junction");
        
        HBox statusRow = new HBox(10);
        statusRow.setAlignment(Pos.CENTER_LEFT);
        indicatorLight = new Circle(8, Color.web("#333"));
        lblTlState = new Label("State: --");
        lblTlState.setTextFill(Color.WHITE);
        statusRow.getChildren().addAll(indicatorLight, lblTlState);
        
        lblPhaseTime = new Label("Time: -- s");
        lblPhaseTime.setTextFill(Color.LIGHTGRAY);
        
        HBox smartBtns = new HBox(5);
        smartBtns.setAlignment(Pos.CENTER);
        Button btnGreen = createFlatBtn("GREEN", "#00E676");
        Button btnRed = createFlatBtn("RED", "#FF1744");
        HBox.setHgrow(btnGreen, Priority.ALWAYS);
        HBox.setHgrow(btnRed, Priority.ALWAYS);
        smartBtns.getChildren().addAll(btnGreen, btnRed);
        
        tlCard.getChildren().addAll(
            createLabel("Junction:"), cbTrafficLight,
            statusRow, lblPhaseTime,
            new Separator(),
            smartBtns
        );
        
        // Add components to the inner VBox
        content.getChildren().addAll(header, lblTime, vehicleCard, tlCard);
        
        // Configure the ScrollPane (this)
        this.setContent(content);
        this.setFitToWidth(true); // Content stretches to ScrollPane width
        this.setHbarPolicy(ScrollBarPolicy.NEVER); // No horizontal scrollbar
        this.setVbarPolicy(ScrollBarPolicy.AS_NEEDED); // Vertical scrollbar as needed
        this.setPannable(true); // Allow mouse drag scrolling
        this.setStyle("-fx-background: #1a1a1a; -fx-border-color: transparent; -fx-control-inner-background: #1a1a1a;"); // Match background
        
        // Setup Logic Handlers
        setupVehicleHandlers(btnCreate, btnModify, btnDelete, btnSelect);
        setupTrafficLightHandlers(btnGreen, btnRed);
    }
    
    // --- HANDLERS ---
    
    public void selectVehicle(String id) {
        if (vehicleManager == null || id == null) return;
        txtSelectedId.setText(id);
        Optional<IVehicle> vOpt = vehicleManager.getAllVehicles().stream()
                .filter(v -> v.getId().equals(id)).findFirst();
        if (vOpt.isPresent()) {
            IVehicle v = vOpt.get();
            sliderSpeed.setValue(v.getSpeed());
            String c = v.getColor(); 
            if (c != null && cbColor.getItems().contains(c)) cbColor.setValue(c);
            String edge = v.getEdgeId();
            if (edge != null && cbRoute.getItems().contains(edge)) cbRoute.setValue(edge);
        }
    }
    
    public void updateRealTimeData() {
        if(engine == null) return;
        lblTime.setText(String.format("TIME: %.2f s", engine.getCurrentSimulationTime()));
        updateTlInfo();
        if (cbTrafficLight.getItems().isEmpty() && engine.getTrafficLightIdList() != null) {
            cbTrafficLight.getItems().setAll(engine.getTrafficLightIdList());
        }
    }
    
    private void setupVehicleHandlers(Button create, Button mod, Button del, Button select) {
        create.setOnAction(e -> {
            if (vehicleManager != null) 
                vehicleManager.injectVehicle(cbRoute.getValue(), cbType.getValue(), cbColor.getValue(), spinnerCount.getValue(), sliderSpeed.getValue(), cbColor.getValue(), sliderSpeed.getValue());
        });
        
        del.setOnAction(e -> {
            if (vehicleManager!=null) {
                vehicleManager.deleteVehicle(cbRoute.getValue(), cbColor.getValue(), sliderSpeed.getValue(), spinnerCount.getValue());
                txtSelectedId.clear();
            }
        });
        
        // IMPLEMENTATION DU SELECT
        select.setOnAction(e -> {
            if (vehicleManager != null) {
                // Appel de la méthode SelectVehicle demandée avec les paramètres actuels
                vehicleManager.SelectVehicle(cbColor.getValue(), sliderSpeed.getValue());
                if (onRefreshRequest != null) onRefreshRequest.run();
                System.out.println("Filter applied: " + cbColor.getValue() + " @ " + sliderSpeed.getValue() + "m/s");
            }
        });

        mod.setOnAction(e -> {
            if (engine != null && !engine.isPaused()) {
                showAlert("Please PAUSE the simulation to modify a vehicle!");
                return;
            }
            String id = txtSelectedId.getText();
            if (id == null || id.isEmpty()) return;
            try { 
                if(vehicleManager != null) {
                    vehicleManager.modifyVehicle(id, cbColor.getValue(), sliderSpeed.getValue());
                    if (onRefreshRequest != null) onRefreshRequest.run();
                }
            } catch(Exception ex) { showAlert(ex.getMessage()); }
        });
    }
    
    private void setupTrafficLightHandlers(Button btnGreen, Button btnRed) {
        cbTrafficLight.setOnAction(e -> updateTlInfo());
        
        btnGreen.setOnAction(e -> {
            String id = cbTrafficLight.getValue();
            if (id != null && trafficLightManager != null) {
                trafficLightManager.forceGreen(id);
                updateTlInfo();
                if (onRefreshRequest != null) onRefreshRequest.run();
            }
        });
        
        btnRed.setOnAction(e -> {
            String id = cbTrafficLight.getValue();
            if (id != null && trafficLightManager != null) {
                trafficLightManager.forceRed(id);
                updateTlInfo();
                if (onRefreshRequest != null) onRefreshRequest.run();
            }
        });
    }
    
    private void updateTlInfo() {
        String id = cbTrafficLight.getValue();
        if (id == null || engine == null) return;
        try {
            String state = engine.getTrafficLightState(id);
            double timeLeft = engine.getTrafficLightRemainingTime(id) / 1000.0;
            lblTlState.setText("P:" + engine.getTrafficLightPhase(id) + " (" + state + ")");
            lblPhaseTime.setText(String.format("%.1fs", timeLeft));
            
            int phase = engine.getTrafficLightPhase(id);
            if(phase== 0 || phase==4 ) indicatorLight.setFill(Color.LIME);
            else if(phase==2) indicatorLight.setFill(Color.RED);
            else indicatorLight.setFill(Color.ORANGE);
        } catch(Exception e) {}
    }
    
    private void showAlert(String msg) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setContentText(msg);
        alert.showAndWait();
    }
    
    // Helpers
    private VBox createCard(String title) {
        VBox v = new VBox(8);
        v.setStyle("-fx-background-color: #383838; -fx-background-radius: 8; -fx-padding: 10; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.4), 5, 0, 0, 1);");
        Label l = new Label(title); l.setTextFill(Color.web("#aaa")); l.setFont(Font.font("System", FontWeight.BOLD, 10));
        v.getChildren().add(l);
        return v;
    }
    private Button createFlatBtn(String t, String c) {
        Button b = new Button(t); b.setStyle("-fx-background-color: "+c+"; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 10px;");
        b.setMaxWidth(Double.MAX_VALUE); return b;
    }
    private Button createIconBtn(String i, Color c) {
        Button b = new Button(i); b.setStyle("-fx-background-color: #"+c.toString().substring(2,8)+"; -fx-text-fill: #222; -fx-font-weight: bold; -fx-background-radius: 15;");
        b.setPrefSize(30, 30); return b;
    }
    private ComboBox<String> createStyledCombo(String p) {
        ComboBox<String> c = new ComboBox<>(); c.setPromptText(p); c.setMaxWidth(Double.MAX_VALUE);
        c.setStyle("-fx-background-color: #555; -fx-text-fill: white; -fx-mark-color: white;"); return c;
    }
    private Label createLabel(String t) { Label l = new Label(t); l.setTextFill(Color.WHITE); l.setFont(Font.font(10)); return l; }



}