package de.frauas.group6.traffic.simulator.view;

import de.frauas.group6.traffic.simulator.core.ISimulationEngine;
import de.frauas.group6.traffic.simulator.vehicles.IVehicleManager;
import javafx.animation.AnimationTimer;
import javafx.geometry.Pos;
import javafx.scene.SubScene;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

/**
 * SimulationViewStack
 * Acts as the container for both 2D and 3D views.
 * Handles the Switching Logic and the High-Frequency Simulation Loop.
 */
public class SimulationViewStack extends StackPane {

    // The Two Views
    private final MapView view2D;     // Your existing 2D Class
    private final MapView3D view3D;   // Your existing 3D Class
    private final SubScene scene3D;   // The container for 3D

    // The Controls
    private final ToggleButton switchBtn;

    // Simulation Dependencies
    private final ISimulationEngine engine;
    private final IVehicleManager vehicleManager;

    // --- HIGH FREQUENCY LOOP SETTINGS ---
    private long lastUpdate = 0;
    // 50ms = 20 frames per second for Physics. 
    // This makes SUMO calculate small steps, eliminating teleportation.
    private static final long STEP_SIZE_NANOS = 50_000_000; 

    public SimulationViewStack(ISimulationEngine engine, IVehicleManager vm, ControlPanel cp) {
        this.engine = engine;
        this.vehicleManager = vm;

        // 1. Initialize Views
        this.view2D = new MapView(engine, vm); // The 2D code you sent me
        this.view3D = new MapView3D(engine, vm, cp); // The 3D code you sent me

        // 2. Setup 3D Container (SubScene)
        // We bind it to the StackPane size so it resizes with the window
        this.scene3D = new SubScene(view3D.getRoot(), 800, 600, true, javafx.scene.SceneAntialiasing.BALANCED);
        scene3D.setCamera(view3D.getCamera());
        scene3D.widthProperty().bind(this.widthProperty());
        scene3D.heightProperty().bind(this.heightProperty());
        
        // Pass mouse events to the 3D logic
        setup3DInputs();

        // 3. Setup The Switch Button
        switchBtn = new ToggleButton("SWITCH TO 3D");
        styleButton(switchBtn);
        switchBtn.setOnAction(e -> toggleView());

        // 4. Layout: Add 2D, 3D (Hidden), and Button
        // Order matters! 2D is bottom, 3D is middle, Button is top.
        this.getChildren().addAll(view2D, scene3D, switchBtn);
        StackPane.setAlignment(switchBtn, Pos.TOP_RIGHT);
        
        // Default State: 2D Visible, 3D Hidden
        scene3D.setVisible(false);
        view2D.setVisible(true);

        // 5. Start the Heartbeat
        startSimulationLoop();
    }

    private void toggleView() {
        boolean is3D = switchBtn.isSelected();
        if (is3D) {
            switchBtn.setText("SWITCH TO 2D");
            view2D.setVisible(false);
            scene3D.setVisible(true);
            scene3D.requestFocus(); // Give focus to 3D for keyboard/mouse
        } else {
            switchBtn.setText("SWITCH TO 3D");
            scene3D.setVisible(false);
            view2D.setVisible(true);
        }
    }

    /**
     * THE GAME LOOP
     * This replaces the old "Thread.sleep(1000)" logic.
     * It runs at 60FPS. It triggers a SUMO step every 50ms.
     */
    private void startSimulationLoop() {
        AnimationTimer timer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                // 1. Physics Update (throttled to 50ms)
                if (now - lastUpdate >= STEP_SIZE_NANOS) {
                    if (engine != null && !engine.isPaused()) { 
                        // Tells SUMO to calculate the next step
                        // IMPORTANT: For this to look smooth (and not fast-forward),
                        // ensure your SUMO config step-length is 0.05 or similar!
                        engine.step(); 
                    }
                    lastUpdate = now;
                }

                // 2. Render (Every Frame - 60FPS)
                if (switchBtn.isSelected()) {
                    // Render 3D
                    // Use the animation method if you kept it, or standard update
                    view3D.updateVehicles(vehicleManager);
                    view3D.updateTrafficLights();
                } else {
                    // Render 2D
                    view2D.render();
                }
            }
        };
        timer.start();
    }
    
    // --- Helpers ---

   // Inside SimulationViewStack.java

private void setup3DInputs() {
        final Delta dragDelta = new Delta();
        scene3D.setPickOnBounds(true);

        // 1. Mouse Pressed: Record starting position
        scene3D.setOnMousePressed(e -> {
            dragDelta.x = e.getSceneX();
            dragDelta.y = e.getSceneY();
            scene3D.requestFocus();
        });

        // 2. Mouse Dragged: Pan or Rotate
        scene3D.setOnMouseDragged(e -> {
            // Calculate movement
            double dx = e.getSceneX() - dragDelta.x;
            double dy = e.getSceneY() - dragDelta.y;

            if (e.isPrimaryButtonDown()) {
                // LEFT CLICK -> PAN
                view3D.pan(dx, dy);
            } else if (e.isSecondaryButtonDown()) {
                // RIGHT CLICK -> ROTATE
                view3D.rotate(dx, dy);
            }

            // Update start position for smooth dragging
            dragDelta.x = e.getSceneX();
            dragDelta.y = e.getSceneY();
        });

        // 3. Mouse Scroll: Zoom
        // FIXED: Use 'scene3D' instead of 'scene'
        // FIXED: Use 'view3D' instead of 'mapView3D'
        scene3D.setOnScroll(event -> {
            double delta = event.getDeltaY();
            double mouseX = event.getSceneX();
            double mouseY = event.getSceneY();
            
            // Use the dimensions of the SubScene
            double width = scene3D.getWidth();
            double height = scene3D.getHeight();

            view3D.zoom(delta, mouseX, mouseY, width, height);
        });
    }

    // Small helper class to place at the bottom of SimulationViewStack.java
    private static class Delta { double x, y; }

    private void styleButton(ToggleButton b) {
        b.setStyle(
            "-fx-background-color: rgba(0, 0, 0, 0.7);" +
            "-fx-text-fill: white;" +
            "-fx-background-radius: 20;" +
            "-fx-padding: 10 20;" +
            "-fx-cursor: hand;"
        );
        b.setFont(Font.font("System", FontWeight.BOLD, 12));
        b.setTranslateX(-20); // Margin from Right
        b.setTranslateY(20);  // Margin from Top
    }
}