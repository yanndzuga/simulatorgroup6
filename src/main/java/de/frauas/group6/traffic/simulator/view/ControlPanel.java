package de.frauas.group6.traffic.simulator.view;

import javafx.scene.layout.VBox;
import javafx.geometry.Insets;
import javafx.scene.control.Separator;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.ComboBox;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.text.Font;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;

public class ControlPanel extends VBox {
    
    private SimulationController controller;
    
    public ControlPanel(SimulationController controller) { 
        this.controller = controller;
        
        // --- General Layout Settings ---
        this.setPadding(new Insets(10)); 
        this.setSpacing(15); 
        this.setStyle("-fx-background-color:#555555; -fx-border-color: black;"); 
        this.setPrefWidth(340); // Standard width
        
        // Try to load CSS
        try {
            this.getStylesheets().add(getClass().getResource("dark_style.css").toExternalForm());
        } catch (Exception e) {
            System.out.println("Warning: CSS file not found.");
        }

        this.setAlignment(Pos.TOP_CENTER);
        
        // --- Header ---
        Label titleLabel = new Label("Simulation Control");
        titleLabel.setFont(new Font("Arial Bold", 20));
        titleLabel.setStyle("-fx-text-fill: white;");
        this.getChildren().addAll(titleLabel, new Separator());
        
        // --- Main Buttons (Start/Pause/Step) ---
        HBox buttonBox = new HBox(10);
        buttonBox.setAlignment(Pos.CENTER);
        
        Button startButton = new Button("Start");
        startButton.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white;");
        startButton.setOnAction(e -> controller.startSimulation());
        
        Button pauseButton = new Button("Pause");
        pauseButton.setStyle("-fx-background-color: #FFC107; -fx-text-fill: black;");
        pauseButton.setOnAction(e -> controller.pauseSimulation());
        
        Button stepButton = new Button("Step");
        stepButton.setStyle("-fx-background-color: #2196F3; -fx-text-fill: white;");
        stepButton.setOnAction(e -> controller.singleStep());
        
        buttonBox.getChildren().addAll(startButton, pauseButton, stepButton);
        this.getChildren().add(buttonBox);
        this.getChildren().add(new Separator());
        
        // ============================================================
        // 🚗 SECTION 1: CAR MANAGEMENT
        // ============================================================
        Label carManagementLabel = new Label("🚗 Car Management"); 
        carManagementLabel.setFont(new Font("Arial Bold", 16));
        carManagementLabel.setStyle("-fx-text-fill: white;"); 
        this.getChildren().add(carManagementLabel);
        
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        ColumnConstraints col0 = new ColumnConstraints(); col0.setPercentWidth(35);
        ColumnConstraints col1 = new ColumnConstraints(); col1.setPercentWidth(25);
        ColumnConstraints col2 = new ColumnConstraints(); col2.setPercentWidth(40); col2.setHgrow(Priority.ALWAYS);
        grid.getColumnConstraints().addAll(col0, col1, col2);
        
        // Vehicle Inputs
        ComboBox<String> edgeBox = new ComboBox<>(); 
        edgeBox.getItems().addAll("-E48", "E45", "E46", "E50", "E49", "-E51");
        edgeBox.getSelectionModel().selectFirst();
        grid.add(new Label("Edge ID:"), 1, 0); grid.add(edgeBox, 2, 0);
        
        ComboBox<String> laneBox = new ComboBox<>();
        laneBox.getItems().addAll("Right", "Left");
        laneBox.getSelectionModel().selectFirst();
        grid.add(new Label("Lane:"), 1, 1); grid.add(laneBox, 2, 1);
        
        // Update Lane logic based on Edge
        edgeBox.setOnAction(e -> { 
            laneBox.getItems().clear(); 
            if (edgeBox.getValue().equals("E45")) { laneBox.getItems().addAll("Right", "Middle", "Left"); } 
            else { laneBox.getItems().addAll("Right", "Left"); }
            laneBox.getSelectionModel().selectFirst(); 
        });
        
        ComboBox<String> typeBox = new ComboBox<>();
        typeBox.getItems().addAll("Standard-Car", "Truck", "Emergency-Vehicle", "City-Bus");
        typeBox.getSelectionModel().selectFirst();
        grid.add(new Label("Type:"), 1, 2); grid.add(typeBox, 2, 2);
        
        ComboBox<String> colorBox = new ComboBox<>();
        colorBox.getItems().addAll("Yellow", "Rot", "Green");
        colorBox.getSelectionModel().selectFirst();
        grid.add(new Label("Color:"), 1, 3); grid.add(colorBox, 2, 3);
        
        TextField speedField = new TextField("50");
        grid.add(new Label("Speed:"), 1, 4); grid.add(speedField, 2, 4);
        
        TextField countField = new TextField("1");
        grid.add(new Label("Count:"), 1, 5); grid.add(countField, 2, 5);
        
        TextField vehicleIdField = new TextField(""); 
        vehicleIdField.setPromptText("(for modify)");
        grid.add(new Label("Vehicle ID:"), 1, 6); grid.add(vehicleIdField, 2, 6);
        
        // Vehicle Actions
        Button spawnButton = new Button("Spawn Car");
        spawnButton.setStyle("-fx-background-color: #00897B; -fx-text-fill: white;");
        spawnButton.setMaxWidth(Double.MAX_VALUE);
        spawnButton.setOnAction(e -> {
            try { controller.spawnCar(edgeBox.getValue(), laneBox.getValue(), typeBox.getValue(), colorBox.getValue(), Integer.parseInt(countField.getText()), Double.parseDouble(speedField.getText())); }
            catch(Exception ex) { showAlert("Spawn Failed", ex.getMessage()); }
        });
        
        Button removeButton = new Button("Remove Car(s)");
        removeButton.setStyle("-fx-background-color: #D32F2F; -fx-text-fill: white;");
        removeButton.setMaxWidth(Double.MAX_VALUE);
        removeButton.setOnAction(e -> {
            try { controller.removeCar(edgeBox.getValue(), colorBox.getValue(), Double.parseDouble(speedField.getText()), Integer.parseInt(countField.getText())); }
            catch(Exception ex) { showAlert("Remove Failed", ex.getMessage()); }
        });
        
        Button modifyButton = new Button("Modify Vehicle");
        modifyButton.setStyle("-fx-background-color: #673AB7; -fx-text-fill: white;");
        modifyButton.setMaxWidth(Double.MAX_VALUE);
        modifyButton.setOnAction(e -> {
            try { controller.modifyVehicle(vehicleIdField.getText(), colorBox.getValue(), Double.parseDouble(speedField.getText())); } 
            catch (Exception ex) { showAlert("Modify Failed", ex.getMessage()); }
        });
        
        grid.add(spawnButton, 0, 2); 
        grid.add(removeButton, 0, 3); 
        grid.add(modifyButton, 0, 4);
        
        this.getChildren().add(grid);

        // ============================================================
        // 🚦 SECTION 2: TRAFFIC LIGHT CONTROL
        // ============================================================
        
        this.getChildren().add(new Separator());

        Label trafficLabel = new Label("🚦 Traffic Light Control");
        trafficLabel.setFont(new Font("Arial Bold", 16));
        trafficLabel.setStyle("-fx-text-fill: white;");
        this.getChildren().add(trafficLabel);

        GridPane trafficGrid = new GridPane();
        trafficGrid.setHgap(10);
        trafficGrid.setVgap(10);
        trafficGrid.getColumnConstraints().addAll(col0, col1, col2);

        // 1. Selection Box
        ComboBox<String> trafficLightBox = new ComboBox<>();
        trafficLightBox.setPromptText("Select...");
        trafficLightBox.setPrefWidth(220); 
        trafficLightBox.setStyle("-fx-font-size: 10px; -fx-text-fill: white; -fx-background-color: #444;"); 
        
        // 2. Refresh Button
        Button refreshListBtn = new Button("↻");
        refreshListBtn.setMinWidth(40); 
        refreshListBtn.setStyle("-fx-background-color: #555; -fx-text-fill: white; -fx-font-size: 14px;");
        
        refreshListBtn.setOnAction(e -> {
            if (controller == null) return;
            java.util.List<String> ids = controller.getTrafficLightIds();
            trafficLightBox.getItems().clear();
            
            if (ids != null && !ids.isEmpty()) {
                 trafficLightBox.getItems().addAll(ids);
                 trafficLightBox.getSelectionModel().selectFirst();
            } else {
                 System.out.println("Info: No Traffic Lights found on current map.");
                 trafficLightBox.setPromptText("No Lights Found");
            }
        });

        // 3. Status Info Label & Button
        Label statusLabel = new Label("Status: --");
        statusLabel.setStyle("-fx-text-fill: #FFA726; -fx-font-size: 11px; -fx-font-weight: bold;");
        
        Button checkStatusBtn = new Button("Check Info");
        checkStatusBtn.setStyle("-fx-background-color: #008CBA; -fx-text-fill: white; -fx-font-size: 10px;");
        checkStatusBtn.setMaxWidth(Double.MAX_VALUE);
        
        checkStatusBtn.setOnAction(e -> {
            String selected = trafficLightBox.getValue();
            if (selected != null) {
                // Fetch info from controller
                String info = controller.getTrafficLightInfo(selected); 
                statusLabel.setText(info); 
            } else {
                statusLabel.setText("Select ID first!");
            }
        });

        // Add to Layout
        trafficGrid.add(new Label("TL ID:"), 1, 0);
        HBox tlBox = new HBox(5);
        tlBox.getChildren().addAll(trafficLightBox, refreshListBtn);
        trafficGrid.add(tlBox, 2, 0);
        
        trafficGrid.add(checkStatusBtn, 0, 1); 
        trafficGrid.add(statusLabel, 1, 1, 2, 1); 

        // 4. Force Control Buttons
        Button btnForceGreen = new Button("FORCE GREEN");
        btnForceGreen.setStyle("-fx-background-color: #2e7d32; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 11px;");
        btnForceGreen.setMaxWidth(Double.MAX_VALUE);
        
        Button btnForceRed = new Button("FORCE RED");
        btnForceRed.setStyle("-fx-background-color: #c62828; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 11px;");
        btnForceRed.setMaxWidth(Double.MAX_VALUE);

        btnForceGreen.setOnAction(e -> {
            String selected = trafficLightBox.getValue();
            if (selected != null) controller.forceGreen(selected);
            else showAlert("Selection Error", "Please select a Traffic Light first!");
        });

        btnForceRed.setOnAction(e -> {
            String selected = trafficLightBox.getValue();
            if (selected != null) controller.forceRed(selected);
            else showAlert("Selection Error", "Please select a Traffic Light first!");
        });

        trafficGrid.add(btnForceGreen, 0, 2, 3, 1);
        trafficGrid.add(btnForceRed, 0, 3, 3, 1);
        
        this.getChildren().add(trafficGrid);
    }
    
    // Alert Helper
    private void showAlert(String header, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }
}