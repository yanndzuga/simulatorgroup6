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
import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;

public class MapView extends Pane {

    private Canvas canvas;
    private ISimulationEngine engine;
    private IVehicleManager vehicleManager;

    private double scale = 3.0; 
    private double offsetX = 100;
    private double offsetY = 500; 
    private double lastMouseX, lastMouseY;

    private Consumer<String> onVehicleSelected;

    public MapView(ISimulationEngine engine, IVehicleManager vm) {
        this.engine = engine;
        this.vehicleManager = vm;
        this.canvas = new Canvas();
        getChildren().add(canvas);
        
        canvas.widthProperty().bind(widthProperty());
        canvas.heightProperty().bind(heightProperty());

        this.setOnScroll((ScrollEvent event) -> {
            double zoomFactor = 1.1;
            if (event.getDeltaY() < 0) zoomFactor = 1 / zoomFactor;
            scale *= zoomFactor;
            render(); 
        });

        this.setOnMousePressed(e -> {
            lastMouseX = e.getX();
            lastMouseY = e.getY();
            if (e.getButton() == MouseButton.PRIMARY) {
                handleSelection(e.getX(), e.getY());
            }
        });

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
        
        setStyle("-fx-background-color: #1e1e1e;"); 
    }

    public void setOnVehicleSelected(Consumer<String> callback) {
        this.onVehicleSelected = callback;
    }

    private void handleSelection(double screenX, double screenY) {
        double simX = (screenX - offsetX) / scale;
        double simY = (offsetY - screenY) / scale; 
        String id = engine.getVehicleIdAtPosition(simX, simY, 10.0);
        if (id != null && onVehicleSelected != null) {
            onVehicleSelected.accept(id);
        }
    }

    // --- MAIN RENDER LOOP ---

    public void render() {
        if (getWidth() <= 0 || getHeight() <= 0) return;
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.setFill(Color.web("#222222")); 
        gc.fillRect(0, 0, getWidth(), getHeight());

        if (engine == null) return;

        drawInfrastructure(gc);
        drawDetailedTrafficLights(gc);

        if (vehicleManager != null) {
            for (IVehicle v : vehicleManager.getAllVehicles()) {
                if (v.isIsVisible()) drawVehicle(gc, v);
            }
        }
    }

    private void drawInfrastructure(GraphicsContext gc) {
        gc.setLineCap(javafx.scene.shape.StrokeLineCap.ROUND);
        List<String> edges = engine.getEdgeIdList();
        double baseLaneWidth = 3.5; 

        // 1. Road Borders
        for (String edgeId : edges) {
            int laneCount = engine.getLaneList(edgeId).size();
            gc.setLineWidth((laneCount * baseLaneWidth + 1.5) * scale);
            gc.setStroke(Color.web("#444444"));
            drawPolyline(gc, engine.getEdgeShape(edgeId));
        }

        // 2. Asphalt
        for (String edgeId : edges) {
            int laneCount = engine.getLaneList(edgeId).size();
            gc.setLineWidth((laneCount * baseLaneWidth) * scale);
            gc.setStroke(Color.web("#333333"));
            drawPolyline(gc, engine.getEdgeShape(edgeId));
        }

        // 3. Junctions
        gc.setFill(Color.web("#333333"));
        for (String jId : engine.getJunctionIdList()) {
            List<Point2D> shape = engine.getJunctionShape(jId);
            if (shape != null && shape.size() > 2) fillPolygon(gc, shape);
        }

        // 4. Lane Markings
        gc.setStroke(Color.web("#888888"));
        gc.setLineWidth(0.3 * scale); 
        for (String edgeId : edges) {
            int laneCount = engine.getLaneList(edgeId).size();
            if (laneCount > 1) {
                for (int i = 1; i < laneCount; i++) {
                    double offset = (i - laneCount / 2.0) * baseLaneWidth;
                    gc.setLineDashes(3 * scale);
                    drawOffsetPolyline(gc, engine.getEdgeShape(edgeId), offset);
                }
            }
        }
        gc.setLineDashes(null);
    }

   /**
 * FIXED Vehicle drawing: Uses engine.getVehicleAngle() since IVehicle doesn't have it.
 */
/**
 * FIXED Vehicle drawing: Calculates Lane Offset to prevent "Center Line Driving".
 */
private void drawVehicle(GraphicsContext gc, IVehicle v) {
    Point2D pos = v.getPosition();
    if(pos == null) return;

    double x = tx(pos.getX());
    double y = ty(pos.getY());
    
    // 1. Get Vehicle Heading
    double angle = engine.getVehicleAngle(v.getId());

    // 2. Correct Lane Offset Logic
    String edgeId = v.getEdgeId();
    if (edgeId != null && !edgeId.startsWith(":")) { // Skip offset logic for internal junction edges
        try {
            int laneIndex = v.getEdgeLane(); 
            int totalLanes = engine.getLaneList(edgeId).size();
            double baseLaneWidth = 3.5; 

            // Offset Formula: Matches the Traffic Light Fix
            // ((Center) - Index) * Width
            double laneOffsetSim = ((totalLanes - 1) / 2.0 - laneIndex) * baseLaneWidth;
            double pixelOffset = laneOffsetSim * scale;

            // 3. Robust "Right Vector" Calculation
            // Instead of using dirX/dirY intermediate steps, we calculate the Right Vector directly.
            // Angle 0 (North) -> Right is East (+X)
            // Angle 90 (East) -> Right is South (+Y)
            // Formula: (Cos(a), Sin(a))
            
            double angleRad = Math.toRadians(angle);
            double rightX = Math.cos(angleRad);
            double rightY = Math.sin(angleRad);

            // Apply Shift
            x += rightX * pixelOffset;
            y += rightY * pixelOffset;

        } catch (Exception e) {
            // Fallback
        }
    }

    // 3. Size & Rotation
    double carWidth = 2.0 * scale; 
    double carLength = 4.0 * scale;

    gc.save();
    
    // Move to the corrected position
    gc.translate(x, y);
    // Rotate nose to face forward
    gc.rotate(angle - 90); 

    // Color Logic
    Color c = Color.web("#ff4444"); 
    String colorStr = v.getColor();
    if(colorStr != null) {
        if (colorStr.equalsIgnoreCase("Green")) c = Color.web("#44ff44");
        else if (colorStr.equalsIgnoreCase("Yellow")) c = Color.web("#ffff44");
        else if (colorStr.equalsIgnoreCase("Red")) c = Color.web("#ff4444");
    }
    
    gc.setFill(c);
    
    // Anchor at Front Bumper (0,0) so car stays behind stop lines
    gc.fillRoundRect(-carLength, -carWidth/2, carLength, carWidth, 1.5 * scale, 1.5 * scale);
    
    // Headlights
    gc.setFill(Color.WHITE);
    gc.fillOval(-1 * scale, -carWidth/2 + (0.2 * scale), 0.8 * scale, 0.8 * scale);
    gc.fillOval(-1 * scale, carWidth/2 - (1 * scale), 0.8 * scale, 0.8 * scale);

    gc.restore();
}
private void drawDetailedTrafficLights(GraphicsContext gc) {
    List<String> tlIds = engine.getTrafficLightIdList();
    if (tlIds == null) return;

    for (String tlId : tlIds) {
        String state = engine.getTrafficLightState(tlId);
        List<String> controlledLanes = engine.getControlledLanes(tlId);
        if (controlledLanes == null || controlledLanes.isEmpty()) continue;

        Map<String, List<Integer>> edgeToIndices = new HashMap<>();
        for (int i = 0; i < controlledLanes.size(); i++) {
            String laneId = controlledLanes.get(i);
            String edgeId = laneId.contains("_") ? laneId.split("_")[0] : laneId;
            edgeToIndices.computeIfAbsent(edgeId, k -> new ArrayList<>()).add(i);
        }

        for (Map.Entry<String, List<Integer>> entry : edgeToIndices.entrySet()) {
            String edgeId = entry.getKey();
            List<Integer> indices = entry.getValue();
            
            List<Point2D> shape = engine.getEdgeShape(edgeId);
            if (shape == null || shape.size() < 2) continue;

            Point2D pEnd = shape.get(shape.size() - 1);
            Point2D pPrev = shape.get(shape.size() - 2);

            double dx = pEnd.getX() - pPrev.getX();
            double dy = pEnd.getY() - pPrev.getY();
            double length = Math.sqrt(dx * dx + dy * dy);
            
            double rx = -dy / length; 
            double ry = dx / length;

            int totalLanes = engine.getLaneList(edgeId).size();
            double laneWidth = 3.5; 

            for (int i : indices) {
                String laneIdStr = controlledLanes.get(i);
                int realLaneIndex = 0;
                try { realLaneIndex = Integer.parseInt(laneIdStr.split("_")[1]); } catch (Exception e) {}

                char signalChar = (state != null && i < state.length()) ? state.charAt(i) : 'r';

                // Offset Logic (Keep the inverted logic that worked)
                double offset = (realLaneIndex - (totalLanes - 1) / 2.0) * laneWidth;
                
                // --- THE FIX: PUSH FORWARD ---
                // We ADD direction to push it slightly into the intersection/sidewalk
                // instead of pulling it back under the car.
                double pushForward = 1.0; 
                double posX = pEnd.getX() + (dx/length * pushForward) + (rx * offset);
                double posY = pEnd.getY() + (dy/length * pushForward) + (ry * offset);

                drawLaneSignal(gc, posX, posY, getSignalColor(signalChar));
            }
        }
    }
}

/**
 * Reverted to Circular Signal
 */
private void drawLaneSignal(GraphicsContext gc, double simX, double simY, Color color) {
    double x = tx(simX);
    double y = ty(simY);
    double size = 3.5 * scale; // Good visibility size

    gc.setFill(color);
    // Draw Circle
    gc.fillOval(x - size/2, y - size/2, size, size);
    
    // Clean Border
    gc.setStroke(Color.BLACK);
    gc.setLineWidth(0.6 * scale);
    gc.strokeOval(x - size/2, y - size/2, size, size);
}
/**
 * Updated to draw a "Stop Bar" rectangle instead of a circle.
 * Matches SUMO visuals.
 */
private void drawLaneSignal(GraphicsContext gc, double simX, double simY, double angleDeg, Color color) {
    double x = tx(simX);
    double y = ty(simY);
    
    // Bar Dimensions relative to scale
    double w = 2.8 * scale; // Width (across the lane)
    double h = 0.6 * scale; // Thickness (along the road)

    gc.save();
    gc.translate(x, y);
    gc.rotate(angleDeg - 90); // Rotate perpendicular to road
    
    gc.setFill(color);
    gc.fillRect(-w/2, -h/2, w, h);
    
    // Optional: Black outline for contrast
    gc.setStroke(Color.BLACK);
    gc.setLineWidth(0.3 * scale);
    gc.strokeRect(-w/2, -h/2, w, h);
    
    gc.restore();
}


    private Color getSignalColor(char s) {
        switch (Character.toLowerCase(s)) {
            case 'g': return Color.LIME;
            case 'y': return Color.YELLOW;
            case 'r': return Color.RED;
            default: return Color.DARKGRAY;
        }
    }

    private void drawOffsetPolyline(GraphicsContext gc, List<Point2D> points, double offsetSim) {
        if (points == null || points.size() < 2) return;
        gc.beginPath();
        for (int i = 0; i < points.size() - 1; i++) {
            Point2D p1 = points.get(i);
            Point2D p2 = points.get(i+1);
            double dx = p2.getX() - p1.getX();
            double dy = p2.getY() - p1.getY();
            double len = Math.sqrt(dx*dx + dy*dy);
            double ox = (dy / len) * offsetSim;
            double oy = -(dx / len) * offsetSim;
            if (i == 0) gc.moveTo(tx(p1.getX() + ox), ty(p1.getY() + oy));
            gc.lineTo(tx(p2.getX() + ox), ty(p2.getY() + oy));
        }
        gc.stroke();
    }

    private void drawPolyline(GraphicsContext gc, List<Point2D> points) {
        if (points == null || points.size() < 2) return;
        gc.beginPath();
        gc.moveTo(tx(points.get(0).getX()), ty(points.get(0).getY()));
        for (int i = 1; i < points.size(); i++) gc.lineTo(tx(points.get(i).getX()), ty(points.get(i).getY()));
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

    private double tx(double val) { return val * scale + offsetX; }
    private double ty(double val) { return offsetY - (val * scale); }
}