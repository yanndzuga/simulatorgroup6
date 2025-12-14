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

public class MapView extends Pane {

    private Canvas canvas;
    private ISimulationEngine engine;
    private IVehicleManager vehicleManager;

    private double scale = 2.0;
    private double offsetX = 50;
    private double offsetY = 500; 
    private double lastMouseX, lastMouseY;

    // Activez ceci pour voir les jonctions en jaune flashy
    private boolean debugColors = true; 

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
        
        setStyle("-fx-background-color: #2b2b2b;");
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

    public void render() {
        if (getWidth() <= 0 || getHeight() <= 0) return;

        GraphicsContext gc = canvas.getGraphicsContext2D();
        double w = getWidth();
        double h = getHeight();

        // 1. Fond
        gc.setFill(Color.web("#2b2b2b")); 
        gc.fillRect(0, 0, w, h);

        if (engine == null) return;

        // 2. Routes & Jonctions
        drawInfrastructure(gc);

        // 3. Véhicules
        if (vehicleManager != null) {
            Collection<IVehicle> vehicles = vehicleManager.getAllVehicles();
            for (IVehicle v : vehicles) {
                drawVehicle(gc, v);
            }
        }
        
        // 4. Feux (Dessinés EN DERNIER pour être au-dessus de tout)
        drawTrafficLights(gc);
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
                case "Rot": c = Color.RED; break;
                case "Green": c = Color.LIME; break;
                case "Yellow": c = Color.YELLOW; break;
                default: c = Color.GRAY; break;
            }
        }
        gc.setFill(c);
        
        String typeId = v.getTypeId(); 
        
        double width = 4 * scale;
        double length = 8 * scale;
        
        if (typeId != null) {
            if (typeId.contains("BUS")) {
                length = 14 * scale; width = 5 * scale;
                gc.fillRect(x - width/2, y - length/2, width, length);
            } else if (typeId.contains("RESCUE")) {
                double[] xP = {x, x - width, x + width};
                double[] yP = {y - length/2, y + length/2, y + length/2};
                gc.fillPolygon(xP, yP, 3);
            } else if (typeId.contains("CONTAINER")) { 
                length = 12 * scale;
                gc.fillRect(x - width/2, y - length/2, width, length);
            } else {
                gc.fillOval(x - width/2, y - length/2, width, length);
            }
        } else {
             gc.fillOval(x - width/2, y - length/2, width, length);
        }
    }
    
    private void drawInfrastructure(GraphicsContext gc) {
        // --- JONCTIONS ---
        // Couleur debug ou normale
        gc.setFill(debugColors ? Color.YELLOW.deriveColor(0, 1, 1, 0.3) : Color.web("#404040"));
        
        List<String> junctions = engine.getJunctionIdList();
        if (junctions.isEmpty()) {
            // System.out.println("DEBUG: Aucune jonction trouvée !");
        }

        for (String jId : junctions) {
            List<Point2D> shape = engine.getJunctionShape(jId);
            if (shape != null && shape.size() > 2) {
                double[] xP = new double[shape.size()];
                double[] yP = new double[shape.size()];
                for (int i = 0; i < shape.size(); i++) {
                    xP[i] = tx(shape.get(i).getX());
                    yP[i] = ty(shape.get(i).getY());
                }
                gc.fillPolygon(xP, yP, shape.size());
            }
        }

        // --- ROUTES ---
        gc.setLineWidth(4 * scale);
        gc.setStroke(Color.web("#505050"));
        for (String edgeId : engine.getEdgeIdList()) {
            drawPolyline(gc, engine.getEdgeShape(edgeId));
        }
        
        // Lignes pointillées
        gc.setStroke(Color.WHITE);
        gc.setLineWidth(1); 
        gc.setLineDashes(10);
        for (String edgeId : engine.getEdgeIdList()) {
            drawPolyline(gc, engine.getEdgeShape(edgeId));
        }
        gc.setLineDashes(null);
    }
    
    private void drawTrafficLights(GraphicsContext gc) {
        List<String> tls = engine.getTrafficLightIdList();
        if (tls.isEmpty()) {
            // System.out.println("DEBUG: Aucun feu tricolore trouvé !");
        }

        for(String tlId : tls) {
             Point2D pos = engine.getTrafficLightPosition(tlId);
             if (pos != null) {
                 String state = engine.getTrafficLightState(tlId);
                 
                 // Logique de couleur feu
                 Color c = Color.RED;
                 if (state != null) {
                     if (state.toLowerCase().contains("g")) c = Color.LIME; // Vert
                     else if (state.toLowerCase().contains("y")) c = Color.ORANGE; // Jaune
                 }
                 
                 // Dessin du feu (Cercle avec contour blanc pour visibilité)
                 double size = 8 * scale; // Plus gros pour bien voir
                 
                 gc.setStroke(Color.WHITE);
                 gc.setLineWidth(1);
                 gc.strokeOval(tx(pos.getX()) - size/2, ty(pos.getY()) - size/2, size, size);
                 
                 gc.setFill(c);
                 gc.fillOval(tx(pos.getX()) - size/2, ty(pos.getY()) - size/2, size, size);
                 
                 // ID du feu si debug
                 if (debugColors) {
                     gc.setFill(Color.CYAN);
                     gc.fillText(tlId, tx(pos.getX()) + size, ty(pos.getY()));
                 }
             }
        }
    }

    private void drawPolyline(GraphicsContext gc, List<Point2D> points) {
        if (points == null || points.size() < 2) return;
        gc.beginPath();
        gc.moveTo(tx(points.get(0).getX()), ty(points.get(0).getY()));
        for (int i = 1; i < points.size(); i++) {
            gc.lineTo(tx(points.get(i).getX()), ty(points.get(i).getY()));
        }
        gc.stroke();
    }

    private double tx(double val) { return val * scale + offsetX; }
    private double ty(double val) { return offsetY - (val * scale); }
}