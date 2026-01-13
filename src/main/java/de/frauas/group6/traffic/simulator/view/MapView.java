package de.frauas.group6.traffic.simulator.view;

import de.frauas.group6.traffic.simulator.core.ISimulationEngine;
import de.frauas.group6.traffic.simulator.vehicles.IVehicleManager;
import de.frauas.group6.traffic.simulator.vehicles.IVehicle; 
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.MouseButton;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import java.awt.geom.Point2D;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

/**
 * MapView class responsible for rendering the simulation environment.
 * It displays the infrastructure (roads, junctions), vehicles, and traffic lights.
 * Provides zoom and pan functionality for better navigation.
 */
public class MapView extends Pane {

    private Canvas canvas;
    private ISimulationEngine engine;
    private IVehicleManager vehicleManager;

    // View parameters
    private double scale = 3.0; // Default zoom level set higher to see details
    private double offsetX = 100;
    private double offsetY = 500; 
    private double lastMouseX, lastMouseY;

    // Callback for vehicle selection
    private Consumer<String> onVehicleSelected;

    /**
     * Constructor for MapView.
     * Initializes the canvas and sets up mouse event handlers.
     * * @param engine The simulation engine interface.
     * @param vm The vehicle manager interface.
     */
    public MapView(ISimulationEngine engine, IVehicleManager vm) {
        this.engine = engine;
        this.vehicleManager = vm;
        
        this.canvas = new Canvas();
        getChildren().add(canvas);
        
        // Dynamically bind canvas size to the pane size
        canvas.widthProperty().bind(widthProperty());
        canvas.heightProperty().bind(heightProperty());

        // --- MOUSE HANDLING (Zoom & Pan) ---
        
        // Handle zooming with scroll wheel
        this.setOnScroll((ScrollEvent event) -> {
            double zoomFactor = 1.1;
            if (event.getDeltaY() < 0) zoomFactor = 1 / zoomFactor;
            scale *= zoomFactor;
            render(); 
        });

        // Handle vehicle selection on left click
        this.setOnMousePressed(e -> {
            lastMouseX = e.getX();
            lastMouseY = e.getY();
            if (e.getButton() == MouseButton.PRIMARY) {
                handleSelection(e.getX(), e.getY());
            }
        });

        // Handle panning with right or middle mouse button
        this.setOnMouseDragged(e -> {
            if (e.getButton() == MouseButton.SECONDARY || e.getButton() == MouseButton.MIDDLE) {
                double dx = e.getX() - lastMouseX;
                double dy = e.getY() - lastMouseY;
                offsetX += dx;
                offsetY += dy;
                lastMouseX = e.getX();
                lastMouseY = e.getY();
                render();
            }
        });
        
        // Set dark background color for better contrast with lights
        setStyle("-fx-background-color: #1e1e1e;"); 
    }
    
    /**
     * Sets the callback to be triggered when a vehicle is selected.
     * @param callback Consumer that accepts the vehicle ID.
     */
    public void setOnVehicleSelected(Consumer<String> callback) {
        this.onVehicleSelected = callback;
    }

    /**
     * Handles the selection logic based on screen coordinates.
     * Converts screen coordinates to simulation coordinates and queries the engine.
     */
    private void handleSelection(double screenX, double screenY) {
        double simX = (screenX - offsetX) / scale;
        double simY = (offsetY - screenY) / scale; 
        
        // Use a 10-meter tolerance for easier clicking
        String id = engine.getVehicleIdAtPosition(simX, simY, 10.0);
        if (id != null && onVehicleSelected != null) {
            onVehicleSelected.accept(id);
        }
    }

    // --- MAIN RENDER LOOP ---
    
    /**
     * Renders the entire scene: background, infrastructure, traffic lights, and vehicles.
     */
    public void render() {
        if (getWidth() <= 0 || getHeight() <= 0) return;

        GraphicsContext gc = canvas.getGraphicsContext2D();
        double w = getWidth();
        double h = getHeight();

        // 1. Background (Ground)
        gc.setFill(Color.web("#222222")); 
        gc.fillRect(0, 0, w, h);

        if (engine == null) return;

        // 2. Infrastructure (Roads & Junctions)
        drawInfrastructure(gc);

        // 3. Traffic Lights (Drawn above roads but below vehicles for visibility - or above vehicles?)
        // Currently drawing them before vehicles.
        drawDetailedTrafficLights(gc);

        // 4. Vehicles
        if (vehicleManager != null) {
            Collection<IVehicle> vehicles = vehicleManager.getAllVehicles();
            for (IVehicle v : vehicles) {
                drawVehicle(gc, v);
            }
        }
    }
    
    /**
     * Draws the road network including edges and junctions.
     */
    private void drawInfrastructure(GraphicsContext gc) {
        // Road style: Dark asphalt with borders
        gc.setLineCap(javafx.scene.shape.StrokeLineCap.ROUND);
        
        List<String> edges = engine.getEdgeIdList();
        
        // Draw road borders (Sidewalks)
        gc.setLineWidth(7 * scale);
        gc.setStroke(Color.web("#444444"));
        for (String edgeId : edges) {
            drawPolyline(gc, engine.getEdgeShape(edgeId));
        }

        // Draw Asphalt
        gc.setLineWidth(5 * scale);
        gc.setStroke(Color.web("#333333"));
        for (String edgeId : edges) {
            drawPolyline(gc, engine.getEdgeShape(edgeId));
        }
        
        // Draw Junctions
        gc.setFill(Color.web("#333333"));
        for (String jId : engine.getJunctionIdList()) {
            List<Point2D> shape = engine.getJunctionShape(jId);
            if (shape != null && shape.size() > 2) {
                fillPolygon(gc, shape);
            }
        }

        // Draw central lane markings (dashed lines)
        gc.setStroke(Color.web("#888888"));
        gc.setLineWidth(0.5 * scale); 
        gc.setLineDashes(3 * scale); // Dash pattern
        for (String edgeId : edges) {
            drawPolyline(gc, engine.getEdgeShape(edgeId));
        }
        gc.setLineDashes(null); // Reset dashes
    }
    
    /**
     * DRAWS "SEMAPHORE" STYLE TRAFFIC LIGHTS (Box with 3 lights)
     * Iterates through controlled lanes to place lights correctly.
     */
    private void drawDetailedTrafficLights(GraphicsContext gc) {
        List<String> tlIds = engine.getTrafficLightIdList();
        if (tlIds == null) return;

        for (String tlId : tlIds) {
            // Get global state string (e.g., "GGrrGGrr")
            String state = engine.getTrafficLightState(tlId);
            // Get controlled lanes (e.g., ["E1_0", "E1_1", "E2_0"...])
            List<String> lanes = engine.getControlledLanes(tlId);
            
            if (lanes == null || lanes.isEmpty()) {
                // Fallback: Draw simple light at center if no lane details available
                drawSimpleTrafficLight(gc, tlId);
                continue;
            }

            // Draw a light at the end of each incoming controlled lane
            for (int i = 0; i < lanes.size(); i++) {
                String laneId = lanes.get(i);
                
                // Determine color for THIS specific lane
                char signalChar = 'r';
                if (state != null && i < state.length()) {
                    signalChar = state.charAt(i);
                } else if (state != null && state.length() > 0) {
                    signalChar = state.charAt(0); // Fallback
                }

                // Retrieve lane/edge shape to determine position and angle
                String edgeId = laneId; 
                // If ID contains underscore (e.g., "E45_0"), it's a lane, get parent edge "E45"
                if (laneId.contains("_")) {
                    edgeId = laneId.substring(0, laneId.lastIndexOf('_'));
                }
                
                List<Point2D> shape = engine.getEdgeShape(edgeId);
                if (shape == null || shape.size() < 2) continue;

                // Position: The last point of the road (the stop line)
                Point2D pEnd = shape.get(shape.size() - 1);
                Point2D pPrev = shape.get(shape.size() - 2);

                // Calculate road angle to orient the traffic light
                double dx = pEnd.getX() - pPrev.getX();
                double dy = pEnd.getY() - pPrev.getY();
                double angleRad = Math.atan2(dy, dx);
                double angleDeg = Math.toDegrees(angleRad);

                // Draw the semaphore box
                drawSemaphore(gc, pEnd.getX(), pEnd.getY(), angleDeg, signalChar);
            }
        }
    }
    
    /**
     * Draws a realistic-style traffic light box.
     */
    private void drawSemaphore(GraphicsContext gc, double simX, double simY, double angleDeg, char stateChar) {
        gc.save();
        
        double x = tx(simX);
        double y = ty(simY);
        
        // 1. Position at the end of the road
        gc.translate(x, y);
        
        // 2. Rotate to face the road (perpendicular to road axis)
        // Adjust rotation as JavaFX coordinates might invert Y
        gc.rotate(-angleDeg); 
        
        double boxW = 4 * scale;
        double boxH = 10 * scale;
        
        // Offset to place the light on the right sidewalk
        double roadOffset = 6 * scale; 
        
        // Draw Black Box
        gc.setFill(Color.BLACK);
        gc.setStroke(Color.DARKGRAY);
        gc.setLineWidth(0.5);
        
        // Local position after rotation: shifted right (local Y+) and slightly back (local X-)
        gc.fillRect(0, roadOffset, boxW, boxH);
        gc.strokeRect(0, roadOffset, boxW, boxH);
        
        // --- THE 3 LIGHTS ---
        double r = 1.2 * scale; // Light radius
        double centerX = boxW / 2;
        double lightYStart = roadOffset + 2 * scale;
        double gap = 3 * scale;

        // Default colors (Off state)
        Color cRed = Color.web("#330000");
        Color cYellow = Color.web("#333300");
        Color cGreen = Color.web("#003300");

        // Neon Glow effect
        Color glow = Color.TRANSPARENT;

        String s = String.valueOf(stateChar).toLowerCase();
        
        if (s.equals("r")) {
            cRed = Color.RED;
            glow = Color.RED;
        } else if (s.equals("y")) {
            cYellow = Color.YELLOW;
            glow = Color.YELLOW;
        } else if (s.equals("g")) {
            cGreen = Color.LIME;
            glow = Color.LIME;
        }

        // Draw Red (Top)
        drawLightBulb(gc, centerX, lightYStart, r, cRed, glow.equals(Color.RED));
        // Draw Yellow (Middle)
        drawLightBulb(gc, centerX, lightYStart + gap, r, cYellow, glow.equals(Color.YELLOW));
        // Draw Green (Bottom)
        drawLightBulb(gc, centerX, lightYStart + gap*2, r, cGreen, glow.equals(Color.LIME));

        gc.restore();
    }
    
    /**
     * Helper to draw a single light bulb with optional glow.
     */
    private void drawLightBulb(GraphicsContext gc, double cx, double cy, double radius, Color c, boolean isOn) {
        gc.setFill(c);
        gc.fillOval(cx - radius, cy - radius, radius * 2, radius * 2);
        
        if (isOn) {
            // Center shine effect
            gc.setFill(Color.WHITE.deriveColor(0, 1, 1, 0.5));
            gc.fillOval(cx - radius/2, cy - radius/2, radius, radius);
        }
    }
    
    // Fallback for junctions without lane details
    private void drawSimpleTrafficLight(GraphicsContext gc, String tlId) {
        Point2D pos = engine.getTrafficLightPosition(tlId);
        if (pos == null) return;
        drawSemaphore(gc, pos.getX(), pos.getY(), 0, engine.getTrafficLightState(tlId).charAt(0));
    }

    private void drawVehicle(GraphicsContext gc, IVehicle v) {
        Point2D pos = v.getPosition();
        if(pos == null) return;
        
        double x = tx(pos.getX());
        double y = ty(pos.getY());
        
        String colorStr = v.getColor();
        Color c = Color.WHITE; 
        if(colorStr != null) {
            switch(colorStr) {
                case "Rot": case "Red": c = Color.web("#ff4444"); break;
                case "Green": c = Color.web("#44ff44"); break;
                case "Yellow": c = Color.web("#ffff44"); break;
            }
        }
        
        // Simple but clean vehicle drawing
        gc.setFill(c);
        double w = 5 * scale;
        double l = 9 * scale;
        gc.fillRoundRect(x - w/2, y - l/2, w, l, 2, 2);
    }
    
    // --- UTILITIES ---
    
    private void drawPolyline(GraphicsContext gc, List<Point2D> points) {
        if (points == null || points.size() < 2) return;
        gc.beginPath();
        gc.moveTo(tx(points.get(0).getX()), ty(points.get(0).getY()));
        for (int i = 1; i < points.size(); i++) {
            gc.lineTo(tx(points.get(i).getX()), ty(points.get(i).getY()));
        }
        gc.stroke();
    }
    
    private void fillPolygon(GraphicsContext gc, List<Point2D> points) {
        double[] x = new double[points.size()];
        double[] y = new double[points.size()];
        for(int i=0; i<points.size(); i++) {
            x[i] = tx(points.get(i).getX());
            y[i] = ty(points.get(i).getY());
        }
        gc.fillPolygon(x, y, points.size());
    }

    // Coordinate transformers
    private double tx(double val) { return val * scale + offsetX; }
    private double ty(double val) { return offsetY - (val * scale); }
}