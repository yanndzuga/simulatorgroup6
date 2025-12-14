package de.frauas.group6.traffic.simulator.view;

public class ControlPanelTest extends javafx.application.Application {

	public static void main(String[] args) {
		
		launch(args);
		
	}
	
	@Override public void start(javafx.stage.Stage primaryStage) {
		SimulationController dummyController = new SimulationController(null, null, null);
		ControlPanel myPanel = new ControlPanel(dummyController);
		javafx.scene.layout.BorderPane root = new javafx.scene.layout.BorderPane();
		root.setRight(myPanel);
		javafx.scene.Scene scene = new javafx.scene.Scene(root, 800, 600);
		primaryStage.setTitle("Test des Control Panels");
		primaryStage.setScene(scene);
		primaryStage.show();
	}
}
