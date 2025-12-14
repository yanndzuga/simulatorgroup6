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
import javafx.scene.transform.Affine;
import java.awt.geom.Point2D;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

public class MapView extends Pane {

    private Canvas canvas;
    private ISimulationEngine engine;
    private IVehicleManager vehicleManager;

    // Paramètres de vue
    private double scale = 3.0; // Zoom par défaut plus grand pour voir les détails
    private double offsetX = 100;
    private double offsetY = 500; 
    private double lastMouseX, lastMouseY;

    private Consumer<String> onVehicleSelected;

    public MapView(ISimulationEngine engine, IVehicleManager vm) {
        this.engine = engine;
        this.vehicleManager = vm;
        
        this.canvas = new Canvas();
        getChildren().add(canvas);
        // Liaison dynamique de la taille
        canvas.widthProperty().bind(widthProperty());
        canvas.heightProperty().bind(heightProperty());

        // --- GESTION SOURIS (Zoom & Pan) ---
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
        
        // Fond sombre pour faire ressortir les feux
        setStyle("-fx-background-color: #1e1e1e;"); 
    }
    
    public void setOnVehicleSelected(Consumer<String> callback) {
        this.onVehicleSelected = callback;
    }

    private void handleSelection(double screenX, double screenY) {
        double simX = (screenX - offsetX) / scale;
        double simY = (offsetY - screenY) / scale; 
        // Tolérance de clic de 10 mètres
        String id = engine.getVehicleIdAtPosition(simX, simY, 10.0);
        if (id != null && onVehicleSelected != null) {
            onVehicleSelected.accept(id);
        }
    }

    // --- RENDU PRINCIPAL ---
    public void render() {
        if (getWidth() <= 0 || getHeight() <= 0) return;

        GraphicsContext gc = canvas.getGraphicsContext2D();
        double w = getWidth();
        double h = getHeight();

        // 1. Fond (Sol)
        gc.setFill(Color.web("#222222")); 
        gc.fillRect(0, 0, w, h);

        if (engine == null) return;

        // 2. Infrastructure (Routes)
        drawInfrastructure(gc);

        // 3. Feux Tricolores (Sous les voitures ou dessus ? Dessus pour visibilité)
        drawDetailedTrafficLights(gc);

        // 4. Véhicules
        if (vehicleManager != null) {
            Collection<IVehicle> vehicles = vehicleManager.getAllVehicles();
            for (IVehicle v : vehicles) {
                drawVehicle(gc, v);
            }
        }
    }
    
    private void drawInfrastructure(GraphicsContext gc) {
        // Style Route : Asphalte foncé avec bordures
        gc.setLineCap(javafx.scene.shape.StrokeLineCap.ROUND);
        
        List<String> edges = engine.getEdgeIdList();
        
        // Bordure de route (Trottoir)
        gc.setLineWidth(7 * scale);
        gc.setStroke(Color.web("#444444"));
        for (String edgeId : edges) {
            drawPolyline(gc, engine.getEdgeShape(edgeId));
        }

        // Bitume
        gc.setLineWidth(5 * scale);
        gc.setStroke(Color.web("#333333"));
        for (String edgeId : edges) {
            drawPolyline(gc, engine.getEdgeShape(edgeId));
        }
        
        // Jonctions
        gc.setFill(Color.web("#333333"));
        for (String jId : engine.getJunctionIdList()) {
            List<Point2D> shape = engine.getJunctionShape(jId);
            if (shape != null && shape.size() > 2) {
                fillPolygon(gc, shape);
            }
        }

        // Lignes pointillées centrales
        gc.setStroke(Color.web("#888888"));
        gc.setLineWidth(0.5 * scale); 
        gc.setLineDashes(3 * scale); // Pointillés
        for (String edgeId : edges) {
            drawPolyline(gc, engine.getEdgeShape(edgeId));
        }
        gc.setLineDashes(null); // Reset
    }
    
    /**
     * DESSINE LES FEUX "SÉMAPHORES" (Boîtes avec 3 lumières)
     */
    private void drawDetailedTrafficLights(GraphicsContext gc) {
        List<String> tlIds = engine.getTrafficLightIdList();
        if (tlIds == null) return;

        for (String tlId : tlIds) {
            // Récupérer l'état global du feu (ex: "GGrrGGrr")
            String state = engine.getTrafficLightState(tlId);
            // Récupérer les voies contrôlées (ex: ["E1_0", "E1_1", "E2_0"...])
            List<String> lanes = engine.getControlledLanes(tlId);
            
            if (lanes == null || lanes.isEmpty()) {
                // Fallback : Dessiner un feu simple au centre si pas de détails de voies
                drawSimpleTrafficLight(gc, tlId);
                continue;
            }

            // Pour chaque voie entrante contrôlée, on dessine un feu au bout
            for (int i = 0; i < lanes.size(); i++) {
                String laneId = lanes.get(i);
                
                // Déterminer la couleur pour CETTE voie spécifique
                // Si la string d'état est assez longue, on prend le caractère correspondant
                char signalChar = 'r';
                if (state != null && i < state.length()) {
                    signalChar = state.charAt(i);
                } else if (state != null && state.length() > 0) {
                    signalChar = state.charAt(0); // Fallback
                }

                // Récupérer la forme de la voie/route pour trouver la position et l'angle
                // Note: SUMO expose souvent getEdgeShape. Si laneId est "edge_index", on prend l'edge.
                // Ici on suppose que engine.getEdgeShape fonctionne avec l'ID complet ou on parse.
                // Simplification : on tente de récupérer la shape via l'engine.
                String edgeId = laneId; 
                // Si l'ID contient un underscore (ex: "E45_0"), c'est une voie, on prend l'edge parent "E45"
                if (laneId.contains("_")) {
                    edgeId = laneId.substring(0, laneId.lastIndexOf('_'));
                }
                
                List<Point2D> shape = engine.getEdgeShape(edgeId);
                if (shape == null || shape.size() < 2) continue;

                // Position : Le dernier point de la route (la ligne d'arrêt)
                Point2D pEnd = shape.get(shape.size() - 1);
                Point2D pPrev = shape.get(shape.size() - 2);

                // Calcul de l'angle de la route pour orienter le feu
                double dx = pEnd.getX() - pPrev.getX();
                double dy = pEnd.getY() - pPrev.getY();
                double angleRad = Math.atan2(dy, dx);
                double angleDeg = Math.toDegrees(angleRad);

                // Dessiner le boîtier sémaphore
                drawSemaphore(gc, pEnd.getX(), pEnd.getY(), angleDeg, signalChar);
            }
        }
    }
    
    // Dessine un boîtier style "Réaliste"
    private void drawSemaphore(GraphicsContext gc, double simX, double simY, double angleDeg, char stateChar) {
        gc.save();
        
        double x = tx(simX);
        double y = ty(simY);
        
        // 1. Se positionner au bout de la route
        gc.translate(x, y);
        // 2. Pivoter pour faire face à la route (angle de la route + 90° pour être sur le côté droit ?)
        // On veut que le feu soit visible "face" au conducteur, donc perpendiculaire.
        gc.rotate(-angleDeg); // Système coord JavaFX inverse Y souvent, ajuster si besoin
        
        double boxW = 4 * scale;
        double boxH = 10 * scale;
        
        // Décalage pour mettre le feu sur le trottoir à droite
        double roadOffset = 6 * scale; 
        
        // Dessiner le boîtier Noir
        gc.setFill(Color.BLACK);
        gc.setStroke(Color.DARKGRAY);
        gc.setLineWidth(0.5);
        
        // Position locale après rotation : décalé à droite (Y positif local) et reculé un peu (X négatif)
        // Note: Dans le repère rotaté, X est l'axe de la route.
        gc.fillRect(0, roadOffset, boxW, boxH);
        gc.strokeRect(0, roadOffset, boxW, boxH);
        
        // --- LES 3 LUMIÈRES ---
        double r = 1.2 * scale; // Rayon lampe
        double centerX = boxW / 2;
        double lightYStart = roadOffset + 2 * scale;
        double gap = 3 * scale;

        // Couleurs par défaut (Eteintes)
        Color cRed = Color.web("#330000");
        Color cYellow = Color.web("#333300");
        Color cGreen = Color.web("#003300");

        // Effet Néon (Glow)
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

        // Dessin Rouge (Haut)
        drawLightBulb(gc, centerX, lightYStart, r, cRed, glow.equals(Color.RED));
        // Dessin Jaune (Milieu)
        drawLightBulb(gc, centerX, lightYStart + gap, r, cYellow, glow.equals(Color.YELLOW));
        // Dessin Vert (Bas)
        drawLightBulb(gc, centerX, lightYStart + gap*2, r, cGreen, glow.equals(Color.LIME));

        gc.restore();
    }
    
    private void drawLightBulb(GraphicsContext gc, double cx, double cy, double radius, Color c, boolean isOn) {
        gc.setFill(c);
        gc.fillOval(cx - radius, cy - radius, radius * 2, radius * 2);
        
        if (isOn) {
            // Effet brillance centrale
            gc.setFill(Color.WHITE.deriveColor(0, 1, 1, 0.5));
            gc.fillOval(cx - radius/2, cy - radius/2, radius, radius);
        }
    }
    
    // Fallback pour les jonctions sans détails
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
        
        // Dessin voiture simple mais propre
        gc.setFill(c);
        double w = 5 * scale;
        double l = 9 * scale;
        gc.fillRoundRect(x - w/2, y - l/2, w, l, 2, 2);
    }
    
    // Utilitaires de transformation et dessin
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

    private double tx(double val) { return val * scale + offsetX; }
    private double ty(double val) { return offsetY - (val * scale); }
}