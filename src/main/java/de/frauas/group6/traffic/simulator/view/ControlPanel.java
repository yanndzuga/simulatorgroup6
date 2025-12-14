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
import javafx.scene.control.Tooltip;


public class ControlPanel extends VBox {
	//Atributte
	private SimulationController controller;
	
	//konstroktur
	 ControlPanel( SimulationController controller){ this.controller=controller;
	 this.setPadding(new Insets(10)); 
	 this.setSpacing(15); this.setStyle("-fx-background-color:#555555; -fx-border-color: black;"); 
	 this.setPrefWidth(300);
	 
	 this.getStylesheets().add(getClass().getResource("dark_style.css").toExternalForm());
	 this.setAlignment(Pos.TOP_CENTER);
	 Label titleLabel = new Label("Simulation Control Panel");
	 titleLabel.setFont(new Font("Arial Bold", 22));
	 titleLabel.setStyle("-fx-text-fill: white;");
	 this.getChildren().addAll(titleLabel, new Separator());
	 
	 HBox buttonBox = new HBox(10);
	 buttonBox.setAlignment(Pos.CENTER);
	
	 //Start Button
	 Button startButton = new Button("Start");
	 startButton.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white;");
	 startButton.setOnAction(e -> controller.startSimulation());
	 //Pause Button
	 Button pauseButton = new Button("Pause");
	 pauseButton.setStyle("-fx-background-color: #FFC107; -fx-text-fill: black;");
	 pauseButton.setOnAction(e -> controller.pauseSimulation());
	 //Step Button
	 Button stepButton = new Button("Step");
	 stepButton.setStyle("-fx-background-color: #2196F3; -fx-text-fill: white;");
	 stepButton.setOnAction(e -> controller.singleStep());
	 
	 buttonBox.getChildren().addAll(startButton, pauseButton, stepButton);
	 this.getChildren().add(buttonBox);
	 this.getChildren().add(new Separator());
	 //Vehicle bereich
	 Label carManagementLabel = new Label("🚗 Car Management"); 
	 carManagementLabel.setFont(new Font("Arial Bold", 18));
	 carManagementLabel.setStyle("-fx-text-fill: white;"); 
	 this.getChildren().add(carManagementLabel);
	 
	 GridPane grid = new GridPane();
	 grid.setHgap(10);
	 grid.setVgap(10);
	 ColumnConstraints col0 = new ColumnConstraints();
	 col0.setPercentWidth(40); // 40% für die Buttons (Spalte 0)
	 
	 ColumnConstraints col1 = new ColumnConstraints();
	 col1.setPercentWidth(23); // 23% für die Labels (Spalte 1)
    
	 
	 ColumnConstraints col2 = new ColumnConstraints();
	 col2.setPercentWidth(37); // 37% für die Eingabefelder (Spalte 2)
	 col2.setHgrow(Priority.ALWAYS); // Erlaubt der Spalte, bei Fenstervergrößerung zu wachsen
	 
	 grid.getColumnConstraints().addAll(col0, col1, col2);
	 
	 //Informationen Bereich
	 ComboBox<String> edgeBox = new ComboBox<>(); 
	 edgeBox.getItems().addAll("-E48", "E45", "E46", "E50", "E49", "-E51");
	 edgeBox.getSelectionModel().selectFirst();
	 grid.add(new Label("Edge ID:"), 1, 0);
	 grid.add(edgeBox, 2, 0);
	 
	 ComboBox<String> laneBox = new ComboBox<>();
	 laneBox.getItems().addAll("Right", "Left");
	 laneBox.getSelectionModel().selectFirst();
	 grid.add(new Label("Lane:"), 1, 1);
	 grid.add(laneBox, 2, 1);
	 
	 
	 edgeBox.setOnAction(e -> { laneBox.getItems().clear(); 
	 if (edgeBox.getValue().equals("E45")) { laneBox.getItems().addAll("Right", "Middle", "Left"); } 
	 else { laneBox.getItems().addAll("Right", "Left"); }
	 laneBox.getSelectionModel().selectFirst(); });
	 
	 ComboBox<String> typeBox = new ComboBox<>();
	 typeBox.getItems().addAll("Standard-Car", "Truck", "Emergency-Vehicle", "City-Bus");
	 typeBox.getSelectionModel().selectFirst();
	 grid.add(new Label("Type:"), 1, 2);
	 grid.add(typeBox, 2, 2);
	 
	 
	 ComboBox<String> colorBox = new ComboBox<>();
	 colorBox.getItems().addAll("Yellow", "Rot", "Green");
	 colorBox.getSelectionModel().selectFirst();
	 grid.add(new Label("Color:"), 1, 3);
	 grid.add(colorBox, 2, 3);
	 
	 
	 
	 TextField speedField = new TextField("50");
	 grid.add(new Label("Speed:"), 1, 4);
	 grid.add(speedField, 2, 4);
	 
	 
	 TextField countField = new TextField("1");
	 grid.add(new Label("Count:"), 1, 5);
	 grid.add(countField, 2, 5);
	 
	 TextField vehicleIdField = new TextField(""); 
	 vehicleIdField.setPromptText("(bei modify)"); // Platzhaltertext
	 grid.add(new Label("Vehicle ID:"), 1, 6);
	 grid.add(vehicleIdField, 2, 6);
	 
	 this.getChildren().add(grid); // HIER NACH DEM GRID WIRD DIE NEUE LOGIK EINGEFÜGT

	 // inject button
	 Button spawnButton = new Button("Spawn Car");
	 spawnButton.setStyle("-fx-background-color: #00897B; -fx-text-fill: white;");
	 spawnButton.setTooltip(new Tooltip("Erzeugt ein neues Fahrzeug."));
	 spawnButton.setMaxWidth(Double.MAX_VALUE); // Button füllt die Breite aus
	 //delete button
	 Button removeButton = new Button("Remove Car(s)");
	 removeButton.setStyle("-fx-background-color: #D32F2F; -fx-text-fill: white;");
	 removeButton.setTooltip(new Tooltip("Entfernt alle passenden Fahrzeuge."));
	 removeButton.setMaxWidth(Double.MAX_VALUE); // Button füllt die Breite aus
	 //modify button
	 Button modifyButton = new Button("Modify Vehicle");
	 modifyButton.setStyle("-fx-background-color: #673AB7; -fx-text-fill: white;"); // Lila
	 modifyButton.setMaxWidth(Double.MAX_VALUE);
	 
	 
	 

	 // Action nach dem klick auf Spawn Car
	 spawnButton.setOnAction(e -> {
		   try{controller.spawnCar(edgeBox.getValue(), 
			 laneBox.getValue(), 
			 typeBox.getValue(),
			 colorBox.getValue(),
			 Integer.parseInt(countField.getText()),
			 Double.parseDouble(speedField.getText()));
		   }
		   
		   catch(Exception ex) {
			   Alert alert = new Alert(Alert.AlertType.ERROR);
		         alert.setTitle("Fahrzeug-inject fehlgeschlagen");
		         alert.setHeaderText("injection konnte nicht durchgeführt werden.");
		         alert.setContentText("Details: " + ex.getMessage());
		         alert.showAndWait();
			   
		   }
			   
		   });
	 
	// Action nach dem klick auf remove Car(s)
	 
	 removeButton.setOnAction(e ->{
		 
		    try{controller.removeCar(edgeBox.getValue(), 
			 colorBox.getValue(),
			 Double.parseDouble(speedField.getText()), 
			 Integer.parseInt(countField.getText()));}
		    
		    catch(IllegalArgumentException ex) {
		    	 Alert alert = new Alert(Alert.AlertType.ERROR);
		         alert.setTitle("Fahrzeug-remove fehlgeschlagen");
		         alert.setHeaderText("löschen konnte nicht durchgeführt werden.");
		         alert.setContentText("Details: " + ex.getMessage());
		         alert.showAndWait();
		    }
			 
	 });
	 
	// Action nach dem klick auf Modify Vehicle
	 
	 modifyButton.setOnAction(e -> {
	     try {
	         controller.modifyVehicle(vehicleIdField.getText(), // edgeBox wird hier als Vehicle ID genutzt.
	                                  colorBox.getValue(), 
	                                  Double.parseDouble(speedField.getText())); 
	     } catch (Exception ex) {
	         
	    	 Alert alert = new Alert(Alert.AlertType.ERROR);
	         alert.setTitle("Fahrzeug-Modifikation fehlgeschlagen");
	         alert.setHeaderText("Modifizierung konnte nicht durchgeführt werden.");
	         alert.setContentText("Details: " + ex.getMessage());
	         alert.showAndWait();
	     }
	 });
	 
	//Position von spawn remove modify Button 
	 grid.add(spawnButton, 0, 2); 
	 
	 grid.add(removeButton, 0, 3); 
	 
	 grid.add(modifyButton, 0, 4);
	 
	
	 }
	
}
