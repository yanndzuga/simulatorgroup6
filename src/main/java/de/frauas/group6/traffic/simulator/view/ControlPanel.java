package de.frauas.group6.traffic.simulator.view;

import de.frauas.group6.traffic.simulator.core.ISimulationEngine;
import de.frauas.group6.traffic.simulator.vehicles.IVehicleManager;
import de.frauas.group6.traffic.simulator.vehicles.IVehicle;
import de.frauas.group6.traffic.simulator.infrastructure.ITrafficLightManager;
import javafx.animation.AnimationTimer;
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

/**
 * ControlPanel class providing UI controls for simulation management.
 * Allows users to control simulation flow, inject/modify vehicles, and control traffic lights.
 */
public class ControlPanel extends VBox {

    private ISimulationEngine engine;
    private IVehicleManager vehicleManager;
    private ITrafficLightManager trafficLightManager;
    
    // Callback to request immediate refresh of the view (MapView)
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
    
    

    /**
     * Constructor for ControlPanel.
     * Initializes the UI layout and binds event handlers.
     */
    public ControlPanel(ISimulationEngine engine, IVehicleManager vm, ITrafficLightManager tm) {
        this.engine = engine;
        this.vehicleManager = vm;
        this.trafficLightManager = tm;
        initializeUI();
    }
    
 
    
    /**
     * Sets the callback to trigger a view refresh.
     * @param callback Runnable to execute.
     */
    public void setOnRefreshRequest(Runnable callback) {
        this.onRefreshRequest = callback;
    }

    private void initializeUI() {
        // Global Dark Theme Style
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
        txtSelectedId.setDisable(true); // Read-only, filled by map click
        
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
        
        sliderSpeed = new Slider(0, 60, 15);
        Label lblSpeed = new Label("Speed: 15.0 m/s");
        lblSpeed.setTextFill(Color.LIGHTGRAY);
        sliderSpeed.valueProperty().addListener((o,old,val) -> lblSpeed.setText(String.format("Speed: %.1f m/s", val.doubleValue())));
        
        spinnerCount = new Spinner<>(1, 100, 1);
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
        
        // Setup Logic Handlers
        setupVehicleHandlers(btnCreate, btnModify, btnDelete);
        setupTrafficLightHandlers(btnGreen, btnRed);
    }
    
    // --- PUBLIC INTERACTION METHODS ---
    
    /**
     * Called when a vehicle is clicked in the MapView.
     * Populates the form fields with the vehicle's data.
     * @param id The ID of the selected vehicle.
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
     * Called by the main game loop to update real-time info (Time, Light status).
     */
    public void updateRealTimeData() {
        if(engine == null) return;
        lblTime.setText(String.format("TIME: %.2f s", engine.getCurrentSimulationTime()));
        updateInfo(); // Update Traffic Light info
        
        if (cbTrafficLight.getItems().isEmpty()) {
            List<String> ids = engine.getTrafficLightIdList();
            if(ids != null) cbTrafficLight.getItems().setAll(ids);
       }
    }
    
    // --- INTERNAL HANDLERS ---
    
    private void setupVehicleHandlers(Button create, Button mod, Button del) {
        // INJECT Handler
        create.setOnAction(e -> {
            try {
                if (vehicleManager != null) {
                    vehicleManager.injectVehicle(cbRoute.getValue(), cbLane.getValue(), cbType.getValue(), cbColor.getValue(), spinnerCount.getValue(), sliderSpeed.getValue());
                }
            } catch(Exception ex) { ex.printStackTrace(); }
        });
        
        // DELETE Handler
        del.setOnAction(e -> {
            try { if (vehicleManager!=null) {
                // Correctly use the spinner value for quantity
                vehicleManager.deleteVehicle(cbRoute.getValue(), cbColor.getValue(), sliderSpeed.getValue(), spinnerCount.getValue());
                txtSelectedId.clear();
            }} catch(Exception ex) {}
        });
        
        // UPDATE Handler
        mod.setOnAction(e -> {
            // Safety Check: Simulation must be PAUSED
            if (engine != null && !engine.isPaused()) {
                System.out.println("Action denied: Please PAUSE the simulation.");
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("Simulation Running");
                alert.setHeaderText(null);
                alert.setContentText("You must PAUSE the simulation to modify a vehicle!");
                alert.showAndWait();
                return;
            }

            String id = txtSelectedId.getText();
            if (id == null || id.isEmpty()) return;

            try { 
                if(vehicleManager != null) {
                    vehicleManager.modifyVehicle(id, cbColor.getValue(), sliderSpeed.getValue());
                    System.out.println("Vehicle " + id + " modified.");
                    
                    // Immediate view refresh
                    if (onRefreshRequest != null) onRefreshRequest.run();
                }
            } catch(Exception ex) {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Error");
                alert.setContentText(ex.getMessage());
                alert.showAndWait();
            }
        });
    }
    
    private void setupTrafficLightHandlers(Button btnGreen, Button btnRed) {
        // Refresh traffic light list on click
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
            
            // Smart Logic or Fallback
            if (trafficLightManager != null) {
                trafficLightManager.forceGreen(id);
            } else if (engine != null) {
                try { engine.setTrafficLightPhase(id, 0); } catch(Exception ex) {}
            }
            
            updateInfo();
            // Immediate view refresh
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
            // Immediate view refresh
            if (onRefreshRequest != null) onRefreshRequest.run();
        });
    }
    
    private void updateInfo() {
        String id = cbTrafficLight.getValue();
        if (id == null || engine == null) return;
        
        try {
            String state = engine.getTrafficLightState(id);
            // Double conversion for decimals
            double timeLeft = engine.getTrafficLightRemainingTime(id) / 1000.0;
            
            lblTlState.setText("Phase: " + engine.getTrafficLightPhase(id) + " (" + state + ")");
            lblPhaseTime.setText(String.format("Remaining: %.1f s", timeLeft));
            
            if(engine.getTrafficLightPhase(id)== 0 || engine.getTrafficLightPhase(id)==4 ) indicatorLight.setFill(Color.LIME);
            else if(engine.getTrafficLightPhase(id)==2) indicatorLight.setFill(Color.RED);
            else indicatorLight.setFill(Color.ORANGE);
            
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