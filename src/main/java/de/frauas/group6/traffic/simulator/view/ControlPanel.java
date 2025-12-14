package de.frauas.group6.traffic.simulator.view;

import de.frauas.group6.traffic.simulator.core.ISimulationEngine;
import de.frauas.group6.traffic.simulator.vehicles.IVehicleManager;
import de.frauas.group6.traffic.simulator.vehicles.IVehicle;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import java.util.Optional;
import java.util.List;


public class ControlPanel extends VBox {

    private ISimulationEngine engine;
    private IVehicleManager vehicleManager;
    
    // UI Elements - Vehicle
    private TextField txtSelectedId;
    private ComboBox<String> cbRoute; // Renommé de cbEdge à cbRoute pour la clarté
    private ComboBox<String> cbLane;    
    private ComboBox<String> cbType;    
    private ComboBox<String> cbColor;   
    private Slider sliderSpeed;
    private Spinner<Integer> spinnerCount;
    
    // UI Elements - Traffic Light
    private ComboBox<String> cbTrafficLight;
    private Label lblTlState;
    private Circle indicatorLight;
    private Label lblPhaseTime;
    
    private Label lblTime;

    public ControlPanel(ISimulationEngine engine, IVehicleManager vm) {
        this.engine = engine;
        this.vehicleManager = vm;
        
        setPadding(new Insets(15));
        setSpacing(15);
        setAlignment(Pos.TOP_CENTER);
        setPrefWidth(320);
        
        // --- HEADER ---
        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER);
        header.getChildren().addAll(
            createBtn("▶", e -> { if(engine != null) { if(engine.isPaused()) engine.resume(); else engine.start(); } }),
            createBtn("⏸", e -> { if(engine != null) engine.pause(); }),
            createBtn("⏯", e -> { if(engine != null) engine.step(); })
        );
        
        lblTime = new Label("TIME: 0.00 s");
        lblTime.setFont(Font.font("System", FontWeight.BOLD, 14));
        
        // --- SECTION 1: VEHICULE ---
        VBox vehicleCard = createSection("VEHICLE COMMANDS");
        
        // Actions
        HBox actions = new HBox(5);
        actions.setAlignment(Pos.CENTER);
        Button btnCreate = new Button("Inject");
        Button btnDelete = new Button("Delete");
        Button btnModify = new Button("Modify");
        styleBtn(btnCreate); styleBtn(btnDelete); styleBtn(btnModify);
        actions.getChildren().addAll(btnCreate, btnDelete, btnModify);
        
        // Inputs
        txtSelectedId = new TextField(); 
        txtSelectedId.setPromptText("Selected ID"); 
        txtSelectedId.setDisable(true);
        
        // --- MODIFICATION SIMPLIFIÉE ---
        // Initialisation directe des routes ici, comme demandé
        cbRoute = new ComboBox<>();
        cbRoute.setPromptText("Select Route"); 
        cbRoute.setMaxWidth(Double.MAX_VALUE);
        cbRoute.getItems().addAll("-E48","E45","E46","E50","E49","-E51");
        cbRoute.getSelectionModel().select(0);
        
        cbLane = new ComboBox<>();
        cbLane.getItems().addAll("Right", "middle", "Left"); 
        cbLane.getSelectionModel().select(0);
        cbLane.setMaxWidth(Double.MAX_VALUE);
        
        cbType = new ComboBox<>();
        cbType.getItems().addAll("Standard-Car", "Truck", "Emergency-Vehicle", "City-Bus");
        cbType.getSelectionModel().select(0);
        cbType.setMaxWidth(Double.MAX_VALUE);
        
        cbColor = new ComboBox<>();
        cbColor.getItems().addAll("Rot", "Green", "Yellow");
        cbColor.getSelectionModel().select(0);
        cbColor.setMaxWidth(Double.MAX_VALUE);
        
        sliderSpeed = new Slider(0, 50, 15);
        Label lblSpeed = new Label("Speed: 15.0 m/s");
        sliderSpeed.valueProperty().addListener((o,old,val) -> 
             lblSpeed.setText(String.format("Speed: %.1f m/s", val.doubleValue()))
        );
        
        spinnerCount = new Spinner<>(1, 10, 1);
        spinnerCount.setMaxWidth(Double.MAX_VALUE);
        
        vehicleCard.getChildren().addAll(actions, txtSelectedId, 
            new Label("Route"), cbRoute, // Utilisation de cbRoute
            new Label("Lane"), cbLane,
            new Label("Type"), cbType,
            new Label("Color"), cbColor,
            lblSpeed, sliderSpeed,
            new Label("Count"), spinnerCount
        );
        
        // --- SECTION 2: TRAFFIC LIGHT ---
        VBox tlCard = createSection("TRAFFIC LIGHT");
        
        cbTrafficLight = new ComboBox<>();
        cbTrafficLight.setPromptText("Select Junction");
        cbTrafficLight.setMaxWidth(Double.MAX_VALUE);
        
        HBox statusBox = new HBox(10);
        statusBox.setAlignment(Pos.CENTER_LEFT);
        indicatorLight = new Circle(10, Color.GRAY);
        indicatorLight.setStroke(Color.BLACK);
        lblTlState = new Label("State: Unknown");
        statusBox.getChildren().addAll(indicatorLight, lblTlState);
        
        lblPhaseTime = new Label("Time left: -- s");
        
        Button btnSwitch = new Button("Switch Phase");
        styleBtn(btnSwitch);
        btnSwitch.setMaxWidth(Double.MAX_VALUE);
        
        tlCard.getChildren().addAll(
            new Label("Junction ID"), cbTrafficLight,
            statusBox,
            lblPhaseTime,
            btnSwitch
        );
        
        getChildren().addAll(header, lblTime, vehicleCard, tlCard);
        
        // --- LOGIQUE BOUTONS VEHICULE ---
        
        btnCreate.setOnAction(e -> {
            try {
                if (vehicleManager != null) {
                    String routeId = cbRoute.getValue(); // Utilisation de cbRoute
                    if (routeId == null || routeId.isEmpty()) return;
                    
                    vehicleManager.injectVehicle(
                        routeId, 
                        cbLane.getValue(),
                        cbType.getValue(),
                        cbColor.getValue(),
                        spinnerCount.getValue(),
                        sliderSpeed.getValue()
                    );
                }
            } catch (Exception ex) { ex.printStackTrace(); }
        });
        
        btnModify.setOnAction(e -> {
            String id = txtSelectedId.getText();
            if (id != null && !id.isEmpty() && vehicleManager != null) {
                try {
                    vehicleManager.modifyVehicle(id, cbColor.getValue(), sliderSpeed.getValue());
                } catch (Exception ex) { ex.printStackTrace(); }
            }
        });
        
        btnDelete.setOnAction(e -> {
            try {
                 if (vehicleManager != null) {
                      vehicleManager.deleteVehicle(cbRoute.getValue(), cbColor.getValue(), sliderSpeed.getValue(), 1);
                      txtSelectedId.clear();
                 }
            } catch (Exception ex) { 
                System.out.println("Erreur suppression: " + ex.getMessage());
            }
        });
        
        // --- LOGIQUE TRAFFIC LIGHT ---
        
        btnSwitch.setOnAction(e -> {
            String tlId = cbTrafficLight.getValue();
            if (tlId != null && engine != null) {
                int currentPhase = engine.getTrafficLightPhase(tlId);
                // On passe à la phase suivante
                engine.setTrafficLightPhase(tlId, currentPhase + 1);
                // On force un rafraîchissement immédiat de l'UI
                updateTrafficLightInfo();
            }
        });
        
        // Mise à jour des infos quand on change de feu dans la liste
        cbTrafficLight.setOnAction(e -> updateTrafficLightInfo());
    }
    
    // --- METHODES ---
    
    public void selectVehicle(String id) {
        if (vehicleManager == null) return;
        txtSelectedId.setText(id);
        
        Optional<IVehicle> vOpt = vehicleManager.getAllVehicles().stream()
                .filter(v -> v.getId().equals(id)).findFirst();
                
        if (vOpt.isPresent()) {
            IVehicle v = vOpt.get();
            sliderSpeed.setValue(v.getSpeed());
            
            if(cbColor.getItems().contains(v.getColor())) cbColor.setValue(v.getColor());
            
            // Tentative de sélectionner la route si l'ID correspond
            String route = v.getEdgeId(); 
            if(route != null && cbRoute.getItems().contains(route)) {
                cbRoute.setValue(route);
            }
        }
    }
    
    public void updateRealTimeData() {
        if (engine == null) return;
        
        lblTime.setText(String.format("TIME: %.2f s", engine.getCurrentSimulationTime()));

        // NOTE: La logique de chargement des routes a été supprimée d'ici
        // car elles sont maintenant initialisées statiquement dans le constructeur.
        
        // 2. Chargement des Feux (Lazy)
        if (cbTrafficLight.getItems().isEmpty()) {
            try {
                List<String> tls = engine.getTrafficLightIdList();
                if (tls != null && !tls.isEmpty()) {
                    cbTrafficLight.getItems().setAll(tls);
                    cbTrafficLight.getSelectionModel().select(0);
                }
            } catch (Exception e) {}
        }
        
        // 3. Mise à jour de l'état du feu sélectionné
        updateTrafficLightInfo();
    }
    
    private void updateTrafficLightInfo() {
        String tlId = cbTrafficLight.getValue();
        if (tlId == null || engine == null) return;
        
        try {
            String state = engine.getTrafficLightState(tlId);
            long time = engine.getTrafficLightRemainingTime(tlId);
            int phase = engine.getTrafficLightPhase(tlId);
            
            lblTlState.setText("Phase: " + phase + " (" + state + ")");
            lblPhaseTime.setText("Time left: " + (time / 1000) + " s");
            
            // Indicateur visuel simple (Vert si 'G' ou 'g' présent, sinon rouge)
            if (state.toLowerCase().contains("g")) {
                indicatorLight.setFill(Color.LIME);
            } else if (state.toLowerCase().contains("y")) {
                indicatorLight.setFill(Color.ORANGE);
            } else {
                indicatorLight.setFill(Color.RED);
            }
        } catch (Exception e) {
            // Ignorer si pas prêt
        }
    }
    
    // --- HELPERS GRAPHIQUES ---
    
    private VBox createSection(String title) {
        VBox card = new VBox(10);
        card.setStyle("-fx-background-color: #cccccc; -fx-background-radius: 15; -fx-padding: 10;");
        Label lbl = new Label(title);
        lbl.setStyle("-fx-background-color: #404040; -fx-text-fill: white; -fx-padding: 5 10 5 10; -fx-background-radius: 5;");
        lbl.setMaxWidth(Double.MAX_VALUE);
        lbl.setAlignment(Pos.CENTER);
        card.getChildren().add(lbl);
        return card;
    }
    
    private Button createBtn(String txt, javafx.event.EventHandler<javafx.event.ActionEvent> h) {
        Button b = new Button(txt);
        b.setStyle("-fx-font-weight: bold; -fx-cursor: hand;");
        b.setOnAction(h);
        return b;
    }
    
    private void styleBtn(Button b) {
        b.setStyle("-fx-background-color: #808080; -fx-text-fill: white; -fx-cursor: hand;");
        b.setPrefWidth(70);
    }
}