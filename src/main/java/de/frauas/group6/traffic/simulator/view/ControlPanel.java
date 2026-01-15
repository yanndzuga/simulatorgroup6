package de.frauas.group6.traffic.simulator.view;

import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

import de.frauas.group6.traffic.simulator.core.ISimulationEngine;
import de.frauas.group6.traffic.simulator.vehicles.IVehicleManager;
import de.frauas.group6.traffic.simulator.vehicles.IVehicle;
import de.frauas.group6.traffic.simulator.infrastructure.IInfrastructureManager;
import de.frauas.group6.traffic.simulator.infrastructure.ITrafficLightManager;

import javafx.application.Platform;
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

/**
 * ControlPanel - User Interface for managing simulation interaction.
 * Refactored for Clean Code, Error Handling, and Logging standards.
 */
public class ControlPanel extends ScrollPane {

    private static final Logger LOGGER = Logger.getLogger(ControlPanel.class.getName());

    // Dependencies
    private final ISimulationEngine engine;
    private final IVehicleManager vehicleManager;
    private final ITrafficLightManager trafficLightManager;
    private final IInfrastructureManager infraMgr;
    
    // Callbacks
    private Runnable onRefreshRequest;

    // UI Components
    private TextField txtSelectedId;
    private ComboBox<String> cbRoute;
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
        this.infraMgr = im;
        initializeUI();
    }

    public void setOnRefreshRequest(Runnable callback) {
        this.onRefreshRequest = callback;
    }

    private void initializeUI() {
        VBox content = new VBox();
        content.setStyle("-fx-background-color: linear-gradient(to bottom, #2b2b2b, #1a1a1a);");
        content.setPadding(new Insets(15));
        content.setSpacing(10);
        content.setAlignment(Pos.TOP_CENTER);

        // Organize content into clear sections
        content.getChildren().addAll(
            createHeaderSection(),
            createSimulationTimeLabel(),
            createVehicleControlCard(),
            createTrafficLightCard()
        );

        // Configure ScrollPane
        this.setContent(content);
        this.setFitToWidth(true);
        this.setHbarPolicy(ScrollBarPolicy.NEVER);
        this.setVbarPolicy(ScrollBarPolicy.AS_NEEDED);
        this.setPannable(true);
        this.setStyle("-fx-background: #1a1a1a; -fx-border-color: transparent; -fx-control-inner-background: #1a1a1a;");
    }

    // =================================================================================
    // UI CONSTRUCTION HELPERS (Clean Code: Split complex init methods)
    // =================================================================================

    private HBox createHeaderSection() {
        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER);
        
        // Use safer ASCII characters for icons to ensure compatibility across all OS/Fonts
        Button btnPlay = createIconBtn("▶", Color.LIGHTGREEN); // Play
        Button btnPause = createIconBtn("||", Color.ORANGE);   // Pause (ASCII safe replacement for ⏸)
        Button btnStep = createIconBtn("⏯", Color.LIGHTBLUE); // Step

        btnPlay.setOnAction(e -> { 
            if(engine != null) { 
                if(engine.isPaused()) engine.resume(); 
                else engine.start(); 
            }
        });
        btnPause.setOnAction(e -> { if(engine != null) engine.pause(); });
        btnStep.setOnAction(e -> { if(engine != null) engine.step(); });

        header.getChildren().addAll(btnPlay, btnPause, btnStep);
        return header;
    }

    private Label createSimulationTimeLabel() {
        lblTime = new Label("TIME: 0.00 s");
        lblTime.setTextFill(Color.WHITE);
        lblTime.setFont(Font.font("Consolas", FontWeight.BOLD, 16));
        return lblTime;
    }

    private VBox createVehicleControlCard() {
        VBox card = createCard("VEHICLE CONTROL");

        // --- Buttons Row 1 ---
        HBox actions = new HBox(5);
        actions.setAlignment(Pos.CENTER);
        Button btnCreate = createFlatBtn("INJECT", "#4CAF50");
        Button btnModify = createFlatBtn("UPDATE", "#2196F3");
        HBox.setHgrow(btnCreate, Priority.ALWAYS);
        HBox.setHgrow(btnModify, Priority.ALWAYS);
        actions.getChildren().addAll(btnCreate, btnModify);

        // --- Buttons Row 2 ---
        HBox actions2 = new HBox(5);
        actions2.setAlignment(Pos.CENTER);
        Button btnDelete = createFlatBtn("DELETE", "#F44336");
        Button btnSelect = createFlatBtn("SELECT", "#FFC107");
        //btnSelect.setStyle("-fx-background-color: #FFC107; -fx-text-fill: black; -fx-font-weight: bold; -fx-background-radius: 4; -fx-cursor: hand;");
        HBox.setHgrow(btnDelete, Priority.ALWAYS);
        HBox.setHgrow(btnSelect, Priority.ALWAYS);
        actions2.getChildren().addAll(btnDelete, btnSelect);

        // --- Inputs ---
        txtSelectedId = new TextField(); 
        txtSelectedId.setPromptText("Vehicle ID (Click Map)"); 
        txtSelectedId.setStyle("-fx-background-color: #444; -fx-text-fill: white;");
        txtSelectedId.setDisable(true); 

        cbRoute = createStyledCombo("Route");
        loadRoutesSafely(); // Extract logic to avoid clutter

        cbType = createStyledCombo("Type");
        cbType.getItems().addAll("Standard-Car", "Truck", "Emergency-Vehicle", "City-Bus");
        cbType.getSelectionModel().select(0);

        cbColor = createStyledCombo("Color");
        cbColor.getItems().addAll("Red", "Green", "Yellow", "All");
        cbColor.getSelectionModel().select(0);

        sliderSpeed = new Slider(0, 60, 15);
        Label lblSpeed = new Label("Speed: 15.0 m/s");
        lblSpeed.setTextFill(Color.LIGHTGRAY);
        sliderSpeed.valueProperty().addListener((o,old,val) -> 
            lblSpeed.setText(String.format("Speed: %.1f m/s", val.doubleValue())));

        spinnerCount = new Spinner<>(1, 100, 1);
        spinnerCount.setStyle("-fx-body-color: #444; -fx-text-fill: white;");
        spinnerCount.setMaxWidth(Double.MAX_VALUE);

        setupVehicleHandlers(btnCreate, btnModify, btnDelete, btnSelect);

        card.getChildren().addAll(
            actions, actions2, 
            new Separator(), 
            createLabel("ID:"), txtSelectedId,
            createLabel("Config:"), cbRoute, cbType, cbColor,
            lblSpeed, sliderSpeed,
            createLabel("Qty:"), spinnerCount
        );
        return card;
    }

    private VBox createTrafficLightCard() {
        VBox card = createCard("TRAFFIC LIGHTS");

        cbTrafficLight = createStyledCombo("Select Junction");
        cbTrafficLight.setOnAction(e -> updateTlInfo());

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

        setupTrafficLightHandlers(btnGreen, btnRed);

        card.getChildren().addAll(
            createLabel("Junction:"), cbTrafficLight,
            statusRow, lblPhaseTime,
            new Separator(),
            smartBtns
        );
        return card;
    }

    private void loadRoutesSafely() {
        try {
            if (infraMgr != null) {
                List<String> routes = infraMgr.loadRouteIds("minimal.rou.xml");
                cbRoute.getItems().addAll(routes);
                if (!routes.isEmpty()) cbRoute.getSelectionModel().selectFirst();
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Could not load routes from XML", e);
        }
    }

    // =================================================================================
    // LOGIC HANDLERS
    // =================================================================================

    public void selectVehicle(String id) {
        if (vehicleManager == null || id == null) return;
        txtSelectedId.setText(id);
        
        try {
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
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error selecting vehicle", e);
        }
    }

    public void updateRealTimeData() {
        if(engine == null) return;
        
        // Ensure UI updates run on FX thread
        Platform.runLater(() -> {
            try {
                lblTime.setText(String.format("TIME: %.2f s", engine.getCurrentSimulationTime()));
                updateTlInfo();
                
                if (cbTrafficLight.getItems().isEmpty() && engine.getTrafficLightIdList() != null) {
                    cbTrafficLight.getItems().setAll(engine.getTrafficLightIdList());
                }
            } catch (Exception e) {
                // Log sparingly to avoid flooding logs
            }
        });
    }

    private void setupVehicleHandlers(Button create, Button mod, Button del, Button select) {
        create.setOnAction(e -> {
            if (vehicleManager != null) {
                try {
                    vehicleManager.injectVehicle(
                        cbRoute.getValue(), cbType.getValue(), cbColor.getValue(), 
                        spinnerCount.getValue(), sliderSpeed.getValue(), 
                        cbColor.getValue(), sliderSpeed.getValue()
                    );
                    LOGGER.info("Inject vehicle requested.");
                } catch(Exception ex) {
                    LOGGER.log(Level.SEVERE, "Failed to inject vehicle", ex);
                    showAlert("Injection Error: " + ex.getMessage());
                }
            }
        });

        del.setOnAction(e -> {
            if (vehicleManager != null) {
                try {
                    vehicleManager.deleteVehicle(cbRoute.getValue(), cbColor.getValue(), sliderSpeed.getValue(), spinnerCount.getValue());
                    txtSelectedId.clear();
                    LOGGER.info("Delete vehicle requested.");
                } catch(Exception ex) {
                    LOGGER.log(Level.SEVERE, "Failed to delete vehicle", ex);
                }
            }
        });

        select.setOnAction(e -> {
            if (vehicleManager != null) {
                vehicleManager.SelectVehicle(cbColor.getValue(), sliderSpeed.getValue());
                if (onRefreshRequest != null) onRefreshRequest.run();
                LOGGER.info("Filter applied: " + cbColor.getValue() + " @ " + sliderSpeed.getValue() + "m/s");
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
                    LOGGER.info("Vehicle modified: " + id);
                }
            } catch(Exception ex) { 
                LOGGER.log(Level.WARNING, "Modify error", ex);
                showAlert(ex.getMessage()); 
            }
        });
    }

    private void setupTrafficLightHandlers(Button btnGreen, Button btnRed) {
        btnGreen.setOnAction(e -> handleTrafficLightAction(true));
        btnRed.setOnAction(e -> handleTrafficLightAction(false));
    }

    private void handleTrafficLightAction(boolean forceGreen) {
        String id = cbTrafficLight.getValue();
        if (id != null && trafficLightManager != null) {
            try {
                if (forceGreen) trafficLightManager.forceGreen(id);
                else trafficLightManager.forceRed(id);
                
                updateTlInfo();
                if (onRefreshRequest != null) onRefreshRequest.run();
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Traffic Light action failed", e);
            }
        }
    }

    private void updateTlInfo() {
        String id = cbTrafficLight.getValue();
        if (id == null || engine == null) return;
        try {
            String state = engine.getTrafficLightState(id);
            double timeLeft = engine.getTrafficLightRemainingTime(id);
            int phase = engine.getTrafficLightPhase(id);
            
            lblTlState.setText("P:" + phase + " (" + state + ")");
            lblPhaseTime.setText(String.format("%.1fs", timeLeft));
            
            if(phase == 0 || phase == 4 || state.toLowerCase().contains("g")) 
                indicatorLight.setFill(Color.LIME);
            else if(phase == 2 || state.toLowerCase().contains("r")) 
                indicatorLight.setFill(Color.RED);
            else 
                indicatorLight.setFill(Color.ORANGE);
        } catch(Exception e) {
            LOGGER.log(Level.FINE, "Error updating TL info (possibly transient)", e);
        }
    }

    private void showAlert(String msg) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Simulation Warning");
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }

    // =================================================================================
    // COMPONENT FACTORIES (Clean Code: Reusable UI builders)
    // =================================================================================

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