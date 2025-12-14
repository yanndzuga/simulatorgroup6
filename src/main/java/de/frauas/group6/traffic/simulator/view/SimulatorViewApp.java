package de.frauas.group6.traffic.simulator.view;

import de.frauas.group6.traffic.simulator.core.SimulationEngine;
import de.frauas.group6.traffic.simulator.infrastructure.IEdge;
import de.frauas.group6.traffic.simulator.infrastructure.ITrafficLight;
import de.frauas.group6.traffic.simulator.infrastructure.TrafficLightManager; 
import de.frauas.group6.traffic.simulator.vehicles.IVehicle;
import de.frauas.group6.traffic.simulator.vehicles.IVehicleManager;
import de.frauas.group6.traffic.simulator.vehicles.VehicleManager;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

public class SimulatorViewApp extends Application implements IMapObserver {

    private static final int WIDTH = 1200;
    private static final int HEIGHT = 800;

    // --- CAMERA ---
    private double zoom = 2.0;
    private double camX = 150;
    private double camY = 400;
    private double lastMouseX, lastMouseY;

    // --- VISUAL LAYERS ---
    private List<VisualLane> junctionPolygons = new ArrayList<>();
    private List<VisualLane> asphaltLanes = new ArrayList<>();

    // --- REAL BACKEND COMPONENTS ---
    private SimulationEngine engine;
    private TrafficLightManager lightMgr;
    private IVehicleManager vehicleMgr;
    
    // --- RENDERING CONTEXT ---
    private Canvas canvas;
    private GraphicsContext gc;

    @Override
    public void start(Stage primaryStage) {
        System.out.println(">>> LAUNCHING SIMULATOR VIEW...");

        // 1. Initialize Core Engine
        try {
            engine = new SimulationEngine();
            
            // 2. Initialize Managers
            lightMgr = new TrafficLightManager(engine);
            vehicleMgr = new VehicleManager(engine);

            // 3. Inject Dependencies
            engine.setVehicleManager(vehicleMgr);
            engine.setTrafficLightManager(lightMgr);
            engine.setMapObserver(this); 

            // 4. Connect to SUMO
            System.out.println(">>> Connecting to TraCI...");
            engine.initialize(); 
            
            // 5. Load Map Geometry directly from SUMO
            loadDynamicMapFromSumo();

            // 6. Start the Physics Thread
            engine.start();

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("CRITICAL ERROR: Make sure SUMO_HOME is set and SUMO is installed.");
            return;
        }

        // --- GUI SETUP ---
        BorderPane root = new BorderPane();
        canvas = new Canvas(WIDTH, HEIGHT);
        gc = canvas.getGraphicsContext2D();

        canvas.setOnMousePressed(e -> { lastMouseX = e.getX(); lastMouseY = e.getY(); });
        canvas.setOnMouseDragged(this::handlePan);
        canvas.setOnScroll(this::handleZoom);

        StackPane holder = new StackPane(canvas);
        holder.setStyle("-fx-background-color: #1e1e1e;");
        root.setCenter(holder);
        root.setBottom(createControlPanel()); 

        primaryStage.setScene(new Scene(root, WIDTH, HEIGHT + 50));
        primaryStage.setTitle("SUMO Traffic Simulator (Live Connection)");
        primaryStage.setOnCloseRequest(e -> {
            System.out.println(">>> Shutting down...");
            if(engine != null) engine.stop();
            Platform.exit();
            System.exit(0);
        });
        primaryStage.show();
    }

    @Override
    public void refresh() {
        Platform.runLater(() -> drawFrame(gc));
    }

    // --- DATA LOADING ---
    private void loadDynamicMapFromSumo() {
        System.out.println(">>> Loading Map Geometry from SUMO...");
        
        List<String> juncIds = engine.getJunctionIdList();
        for (String jId : juncIds) {
            if(jId.startsWith(":")) continue; 
            List<Point2D> shape = engine.getJunctionShape(jId);
            junctionPolygons.add(new VisualLane(jId, shape));
        }

        List<String> edgeIds = engine.getEdgeIdList();
        for (String eId : edgeIds) {
            if(eId.startsWith(":")) continue; 
            List<Point2D> shape = engine.getEdgeShape(eId);
            asphaltLanes.add(new VisualLane(eId, shape));
        }
        System.out.println(">>> Map Loaded: " + junctionPolygons.size() + " Junctions, " + asphaltLanes.size() + " Edges.");
    }

    // --- DRAWING ---
    private void drawFrame(GraphicsContext gc) {
        gc.setFill(Color.web("#1e1e1e"));
        gc.fillRect(0, 0, WIDTH, HEIGHT);

        drawLayer(gc, junctionPolygons, true, Color.rgb(80,80,80));
        drawLayer(gc, asphaltLanes, false, Color.rgb(60,60,60));
        
        if (vehicleMgr != null) {
            for (IVehicle v : vehicleMgr.getAllVehicles()) {
                drawVehicle(gc, v);
            }
        }

        if (lightMgr != null) {
            for (ITrafficLight tl : lightMgr.getAllTrafficLights()) {
                // FIXED: Changed getState() to getCurrentState()
                String state = tl.getCurrentState();
                drawSingleLight(gc, tl.getPosition(), state);
            }
        }
    }

    private void drawVehicle(GraphicsContext gc, IVehicle v) {
        Point2D pos = v.getPosition();
        if (pos == null) return;
        
        double x = transformX(pos.getX());
        double y = transformY(pos.getY());
        
        Color c = Color.CYAN; 
        try { c = Color.web(v.getColor()); } catch(Exception e) { c = Color.CYAN; }

        double r = (v.getTypeId() != null && v.getTypeId().contains("BUS")) ? 3.0 * zoom : 2.0 * zoom;
        gc.setFill(c); gc.fillOval(x-r, y-r, r*2, r*2);
        gc.setStroke(Color.WHITE); gc.setLineWidth(1); gc.strokeOval(x-r, y-r, r*2, r*2);
    }

    private void drawSingleLight(GraphicsContext gc, Point2D pos, String state) {
        if(pos == null) return;

        Color c = Color.DARKGRAY;
        if (state != null && !state.isEmpty()) {
            char s = state.charAt(0); 
            if (s == 'G' || s == 'g') c = Color.LIME; 
            else if (s == 'y' || s == 'Y') c = Color.YELLOW;
            else if (s == 'r' || s == 'R') c = Color.RED;
        }

        double x = transformX(pos.getX());
        double y = transformY(pos.getY());
        double s = 1.8 * zoom;

        gc.setFill(Color.BLACK); gc.fillRect(x - s/2, y - s/2, s, s);
        gc.setFill(c); gc.fillOval(x - s*0.4, y - s*0.4, s*0.8, s*0.8);
    }

    private void drawLayer(GraphicsContext gc, List<VisualLane> list, boolean fill, Color c) {
        gc.setLineWidth(3.2 * zoom); gc.setStroke(c); gc.setFill(c);
        for (VisualLane e : list) {
            List<Point2D> pts = e.getShape(); 
            if(pts == null || pts.isEmpty()) continue;
            
            double[] x = new double[pts.size()];
            double[] y = new double[pts.size()];
            for(int i=0; i<pts.size(); i++){ 
                x[i] = transformX(pts.get(i).getX()); 
                y[i] = transformY(pts.get(i).getY()); 
            }
            if(fill) gc.fillPolygon(x, y, pts.size()); 
            else { gc.beginPath(); gc.moveTo(x[0],y[0]); for(int i=1;i<x.length;i++) gc.lineTo(x[i],y[i]); gc.stroke(); }
        }
    }

    // --- CONTROLS ---
    private HBox createControlPanel() { 
        HBox h = new HBox(15); h.setAlignment(Pos.CENTER); 
        h.setStyle("-fx-background-color: #333; -fx-padding: 10;");
        
        Button btnGreenWave = new Button("Green Wave J55"); 
        btnGreenWave.setOnAction(e -> engine.forceGreenWave("J55"));
        
        Button btnForceRed = new Button("Force Red J55"); 
        btnForceRed.setOnAction(e -> engine.forceRedStop("J55"));
        
        Button btnCongestion = new Button("Check Congestion");
        btnCongestion.setOnAction(e -> engine.checkAndHandleCongestion());

        Label lbl = new Label("Live Control:"); lbl.setTextFill(Color.WHITE);
        h.getChildren().addAll(lbl, btnGreenWave, btnForceRed, btnCongestion);
        return h; 
    }

    // --- HELPER CLASSES ---
    
    // FIXED: Added setVehicleCount to satisfy IEdge interface
    class VisualLane implements IEdge { 
        String id; List<Point2D> s;
        VisualLane(String i, List<Point2D> p){ id=i; s=p; } 
        @Override public String getId(){ return id; } 
        @Override public List<Point2D> getShape(){ return s; } 
        @Override public int getVehicleCount(){ return 0; } 
        @Override public double getLength(){ return 0; }
        
        // This was the missing method causing your first error
        @Override public void setVehicleCount(int count) { /* Visual only, do nothing */ }
    }

    // --- COORDINATE MATH ---
    private double transformX(double x) { return x * zoom + camX; }
    private double transformY(double y) { return -y * zoom + camY; } 
    private void handlePan(MouseEvent e) { camX += e.getX() - lastMouseX; camY += e.getY() - lastMouseY; lastMouseX = e.getX(); lastMouseY = e.getY(); }
    private void handleZoom(ScrollEvent e) { zoom *= (e.getDeltaY()<0)?0.95:1.05; }
    
    public static void main(String[] args) { launch(args); }
}