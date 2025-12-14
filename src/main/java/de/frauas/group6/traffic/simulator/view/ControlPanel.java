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
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import java.util.Optional;

public class ControlPanel extends VBox {

    private ISimulationEngine engine;
    private IVehicleManager vehicleManager;
    
    // UI Elements
    private TextField txtSelectedId;
    private ComboBox<String> cbEdge;
    private ComboBox<String> cbLane;   
    private ComboBox<String> cbType;   
    private ComboBox<String> cbColor;  
    
    private Slider sliderSpeed;
    private Spinner<Integer> spinnerCount;
    // private Label lblTime; // [FIX MAC CRASH] Supprimé pour éviter le bug de layout

    // Cache pour éviter le jitter du layout sur Mac
    // [FIX MAC CRASH] Timestamp pour limiter la fréquence de mise à jour
    private long lastUpdateTimestamp = 0;

    public ControlPanel(ISimulationEngine engine, IVehicleManager vm) {
        this.engine = engine;
        this.vehicleManager = vm;
        
        setPadding(new Insets(15));
        setSpacing(15);
        setAlignment(Pos.TOP_CENTER);
        // Fixer la largeur préférée pour éviter les redimensionnements dynamiques
        setPrefWidth(320);
        
        // --- HEADER ---
        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER);
        header.getChildren().addAll(
            createBtn("▶", e -> { if(engine.isPaused()) engine.resume(); else engine.start(); }),
            createBtn("⏸", e -> engine.pause()),
            createBtn("⏯", e -> engine.step())
        );
        
        // [FIX MAC CRASH] Le label de temps est supprimé du layout graphique
        // On affichera le temps dans le titre de la fenêtre pour une stabilité totale
        
        // --- VEHICLE CARD ---
        VBox card = new VBox(10);
        card.setStyle("-fx-background-color: #cccccc; -fx-background-radius: 15; -fx-padding: 10;");
        
        Label lblTitle = new Label("VEHICULE COMMANDS");
        lblTitle.setStyle("-fx-background-color: #404040; -fx-text-fill: white; -fx-padding: 5 10 5 10; -fx-background-radius: 5;");
        // [FIX MAC CRASH] Fixer largeur pour stabilité
        lblTitle.setMaxWidth(Double.MAX_VALUE);
        lblTitle.setAlignment(Pos.CENTER);
        
        // Actions
        HBox actions = new HBox(5);
        actions.setAlignment(Pos.CENTER);
        Button btnCreate = new Button("Inject");
        Button btnDelete = new Button("Delete");
        Button btnModify = new Button("Modify");
        styleBtn(btnCreate); styleBtn(btnDelete); styleBtn(btnModify);
        actions.getChildren().addAll(btnCreate, btnDelete, btnModify);
        
        // Form Inputs (Adaptés au Manager du collègue)
        txtSelectedId = new TextField(); 
        txtSelectedId.setPromptText("Selected ID"); 
        txtSelectedId.setDisable(true);
        
        cbEdge = new ComboBox<>();
        cbEdge.setPromptText("Edge ID");
        cbEdge.getItems().addAll(engine.getEdgeIdList());
        if(!cbEdge.getItems().isEmpty()) cbEdge.getSelectionModel().select(0);
        // [FIX MAC CRASH] Empêcher les ComboBox de varier en largeur
        cbEdge.setMaxWidth(Double.MAX_VALUE);
        
        cbLane = new ComboBox<>();
        cbLane.setPromptText("Lane");
        // Les valeurs exactes attendues par le switch du collègue
        cbLane.getItems().addAll("Right", "middle", "Left"); 
        cbLane.getSelectionModel().select(0);
        cbLane.setMaxWidth(Double.MAX_VALUE);
        
        cbType = new ComboBox<>();
        cbType.setPromptText("Type");
        // Les valeurs exactes attendues
        cbType.getItems().addAll("Standard-Car", "Truck", "Emergency-Vehicle", "City-Bus");
        cbType.getSelectionModel().select(0);
        cbType.setMaxWidth(Double.MAX_VALUE);
        
        cbColor = new ComboBox<>();
        cbColor.setPromptText("Color");
        // Les valeurs exactes attendues
        cbColor.getItems().addAll("Rot", "Green", "Yellow");
        cbColor.getSelectionModel().select(0);
        cbColor.setMaxWidth(Double.MAX_VALUE);
        
        sliderSpeed = new Slider(0, 50, 15);
        Label lblSpeed = new Label("Speed: 15.0 m/s");
        // Optimization: Use formatted binding or simple listener to avoid excessive updates causing Mac issues
        sliderSpeed.valueProperty().addListener((o,old,val) -> {
             if (Math.abs(val.doubleValue() - old.doubleValue()) > 0.1) {
                 lblSpeed.setText(String.format("Speed: %.1f m/s", val.doubleValue()));
             }
        });
        
        spinnerCount = new Spinner<>(1, 10, 1);
        spinnerCount.setMaxWidth(Double.MAX_VALUE);
        
        card.getChildren().addAll(lblTitle, actions, txtSelectedId, 
            new Label("Edge"), cbEdge,
            new Label("Lane"), cbLane,
            new Label("Type"), cbType,
            new Label("Color"), cbColor,
            lblSpeed, sliderSpeed,
            new Label("Count"), spinnerCount
        );
        
        getChildren().addAll(header, card); // [FIX] lblTime retiré
        
        // --- LOGIQUE (Connexion au Manager) ---
        
        // 1. INJECT
        btnCreate.setOnAction(e -> {
            try {
                vehicleManager.injectVehicle(
                    cbEdge.getValue(),
                    cbLane.getValue(),
                    cbType.getValue(),
                    cbColor.getValue(),
                    spinnerCount.getValue(),
                    sliderSpeed.getValue()
                );
            } catch (Exception ex) { ex.printStackTrace(); }
        });
        
        // 2. MODIFY
        btnModify.setOnAction(e -> {
            String id = txtSelectedId.getText();
            if (id != null && !id.isEmpty()) {
                try {
                    vehicleManager.modifyVehicle(
                        id, 
                        cbColor.getValue(), 
                        sliderSpeed.getValue()
                    );
                } catch (Exception ex) { ex.printStackTrace(); }
            }
        });
        
        // 3. DELETE (Adaptation difficile)
        // Le manager n'a pas deleteById. On utilise sa méthode par critères.
        btnDelete.setOnAction(e -> {
            // On essaie de récupérer les infos de la voiture sélectionnée pour remplir les critères
            String id = txtSelectedId.getText();
            // Pour faire simple, on utilise les valeurs du formulaire comme critères de suppression
            try {
                 vehicleManager.deleteVehicle(
                     cbEdge.getValue(),
                     cbColor.getValue(),
                     sliderSpeed.getValue(),
                     1 // On en supprime 1
                 );
                 txtSelectedId.clear();
            } catch (Exception ex) { 
                System.out.println("Erreur suppression: " + ex.getMessage());
            }
        });
    }
    
    public void selectVehicle(String id) {
        txtSelectedId.setText(id);
        
        // On essaie de retrouver l'objet pour pré-remplir le formulaire
        // C'est un peu couteux de parcourir tout, mais nécessaire car pas de getById dans l'interface
        Optional<IVehicle> vOpt = vehicleManager.getAllVehicles().stream()
                .filter(v -> v.getId().equals(id)).findFirst();
                
        if (vOpt.isPresent()) {
            IVehicle v = vOpt.get();
            sliderSpeed.setValue(v.getSpeed());
            if(cbColor.getItems().contains(v.getColor())) cbColor.setValue(v.getColor());
            if(cbEdge.getItems().contains(v.getEdgeId())) cbEdge.setValue(v.getEdgeId());
            // Note: Lane et Type sont plus durs à déduire si IVehicle ne les expose pas parfaitement
        }
    }
    
    public void updateRealTimeData() {
        long now = System.currentTimeMillis();
        // [FIX MAC CRASH] Limitation drastique : Mise à jour max 4 fois par seconde (250ms)
        if (now - lastUpdateTimestamp < 250) {
            return;
        }
        lastUpdateTimestamp = now;

        String timeText = String.format("Simulation Time: %.2f s", engine.getCurrentSimulationTime());
        
        // [FIX MAC CRASH] Mise à jour du titre de la fenêtre au lieu d'un label
        // Récupérer la fenêtre parente de manière sûre
        if (getScene() != null && getScene().getWindow() instanceof Stage) {
            Stage stage = (Stage) getScene().getWindow();
            stage.setTitle("Traffic Simulator - Group 6 | " + timeText);
        }
    }
    
    private Button createBtn(String txt, javafx.event.EventHandler<javafx.event.ActionEvent> h) {
        Button b = new Button(txt);
        b.setStyle("-fx-font-weight: bold;");
        b.setOnAction(h);
        return b;
    }
    
    private void styleBtn(Button b) {
        b.setStyle("-fx-background-color: #808080; -fx-text-fill: white;");
        b.setPrefWidth(70);
    }
}