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
import java.util.List;
import java.util.function.Consumer;

public class MapView extends Pane {

    private Canvas canvas;
    private ISimulationEngine engine;
    private IVehicleManager vehicleManager;

    // Zoom & Pan
    private double scale = 2.0;
    private double offsetX = 50;
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

        // Zoom
        this.setOnScroll((ScrollEvent event) -> {
            double zoomFactor = 1.1;
            if (event.getDeltaY() < 0) zoomFactor = 1 / zoomFactor;
            scale *= zoomFactor;
            render(); 
        });

        // Pan & Click
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
        // Conversion Écran -> Monde
        double simX = (screenX - offsetX) / scale;
        double simY = (offsetY - screenY) / scale; 
        
        // Utilisation de la méthode de l'Engine (recherche géométrique)
        String id = engine.getVehicleIdAtPosition(simX, simY, 10.0);
        if (id != null && onVehicleSelected != null) {
            onVehicleSelected.accept(id);
        }
    }

    public void render() {
        GraphicsContext gc = canvas.getGraphicsContext2D();
        double w = getWidth();
        double h = getHeight();

        // 1. Fond
        gc.setFill(Color.web("#2b2b2b")); 
        gc.fillRect(0, 0, w, h);

        if (engine == null) return;

        // 2. Routes & Jonctions
        drawInfrastructure(gc);

        // 3. Véhicules (Adaptation au Manager du collègue)
        // On récupère la collection d'objets IVehicle
        for (IVehicle v : vehicleManager.getAllVehicles()) {
            drawVehicle(gc, v);
        }
        
        // 4. Feux
        drawTrafficLights(gc);
    }
    
    private void drawVehicle(GraphicsContext gc, IVehicle v) {
        Point2D pos = v.getPosition();
        if(pos == null) return;
        
        double x = tx(pos.getX());
        double y = ty(pos.getY());
        
        // TRADUCTION COULEUR (String -> JavaFX Color)
        String colorStr = v.getColor();
        Color c = Color.WHITE; // Défaut
        if(colorStr != null) {
            switch(colorStr) {
                case "Rot": c = Color.RED; break;
                case "Green": c = Color.LIME; break;
                case "Yellow": c = Color.YELLOW; break;
                default: c = Color.GRAY; break;
            }
        }
        gc.setFill(c);
        
        // TRADUCTION FORME (Type -> Rectangle/Triangle)
        // Le manager mappe: "Standard-Car", "Truck", "Emergency-Vehicle", "City-Bus"
        String typeId = v.getTypeId(); // ex: "BUS_TYPE" ou "RESCUE_TYPE"
        
        double width = 4 * scale;
        double length = 8 * scale;
        
        if (typeId != null) {
            if (typeId.contains("BUS")) {
                length = 14 * scale;
                width = 5 * scale;
                gc.fillRect(x - width/2, y - length/2, width, length);
            } else if (typeId.contains("RESCUE")) {
                // Triangle
                double[] xP = {x, x - width, x + width};
                double[] yP = {y - length/2, y + length/2, y + length/2};
                gc.fillPolygon(xP, yP, 3);
            } else if (typeId.contains("CONTAINER")) { // Truck
                length = 12 * scale;
                gc.fillRect(x - width/2, y - length/2, width, length);
            } else {
                // Voiture standard (Cercle/Ovale)
                gc.fillOval(x - width/2, y - length/2, width, length);
            }
        } else {
             gc.fillOval(x - width/2, y - length/2, width, length);
        }
        
        // ID si zoomé
        if (scale > 2.0) {
            gc.setFill(Color.WHITE);
            gc.fillText(v.getId(), x + 5, y);
        }
    }
    
    private void drawInfrastructure(GraphicsContext gc) {
        // Jonctions
        gc.setFill(Color.web("#404040"));
        for (String jId : engine.getJunctionIdList()) {
            List<Point2D> shape = engine.getJunctionShape(jId);
            if (shape.size() > 2) {
                double[] xP = new double[shape.size()];
                double[] yP = new double[shape.size()];
                for (int i = 0; i < shape.size(); i++) {
                    xP[i] = tx(shape.get(i).getX());
                    yP[i] = ty(shape.get(i).getY());
                }
                gc.fillPolygon(xP, yP, shape.size());
            }
        }
        // Routes
        gc.setLineWidth(4 * scale);
        gc.setStroke(Color.web("#505050"));
        for (String edgeId : engine.getEdgeIdList()) {
            drawPolyline(gc, engine.getEdgeShape(edgeId));
        }
        // Lignes
        gc.setStroke(Color.WHITE);
        gc.setLineWidth(1); gc.setLineDashes(10);
        for (String edgeId : engine.getEdgeIdList()) {
            drawPolyline(gc, engine.getEdgeShape(edgeId));
        }
        gc.setLineDashes(null);
    }
    
    private void drawTrafficLights(GraphicsContext gc) {
        for(String tlId : engine.getTrafficLightIdList()) {
             Point2D pos = engine.getTrafficLightPosition(tlId);
             String state = engine.getTrafficLightState(tlId);
             Color c = (state.toLowerCase().contains("g")) ? Color.GREEN : Color.RED;
             gc.setFill(c);
             gc.fillOval(tx(pos.getX()) - 3, ty(pos.getY()) - 3, 6, 6);
        }
    }

    private void drawPolyline(GraphicsContext gc, List<Point2D> points) {
        if (points.size() < 2) return;
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