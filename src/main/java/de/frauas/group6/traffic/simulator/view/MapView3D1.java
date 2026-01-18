package de.frauas.group6.traffic.simulator.view;

import java.util.Map;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;


import de.frauas.group6.traffic.simulator.core.ISimulationEngine;
import de.frauas.group6.traffic.simulator.vehicles.IVehicle;
import de.frauas.group6.traffic.simulator.vehicles.IVehicleManager;

import javafx.scene.AmbientLight;
import javafx.scene.Group;
import javafx.scene.PerspectiveCamera;
import javafx.scene.PointLight;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Box;
import javafx.scene.shape.CullFace;
import javafx.scene.shape.Sphere;
import javafx.scene.transform.Rotate;

/*--------------------------------------------------------------------------------------------
  MapView3D handles the 3D visualization of the traffic simulation.
  It manages the rendering of roads, junctions, vehicles, traffic lights, and environment.
  --------------------------------------------------------------------------------------------
 */
public class MapView3D1 {
	
    //-- Scene Graph Groups--
    private Group world;
    private Group roadGroup;
    
    // --Core Dependencies--
    private ISimulationEngine engine;
    private PerspectiveCamera camera;
    private HashMap<String, Box> Vehicleboxes;
    private ControlPanel controlpanel;
    
    //-- Camera Rotation Transforms--
    private Rotate RotateX;
    private Rotate RotateY;
    
    // --Environment and Infrastructure Registry--
    private Map<String, List<Sphere>> trafficLightRegistry;
    private Sphere sky;

    // --Traffic Light Material States (ON/OFF colors)--
    private final PhongMaterial RED_ON = new PhongMaterial(Color.RED);
    private final PhongMaterial RED_OFF = new PhongMaterial(Color.web("#440000"));

    private final PhongMaterial YELLOW_ON = new PhongMaterial(Color.YELLOW);
    private final PhongMaterial YELLOW_OFF = new PhongMaterial(Color.web("#444400"));

    private final PhongMaterial GREEN_ON = new PhongMaterial(Color.LIME);
    private final PhongMaterial GREEN_OFF = new PhongMaterial(Color.web("#004400"));
    
    /*-----------------------------------------------------------------------------------
      Constructor initializes the 3D world, camera, lighting, and static infrastructure.
      -----------------------------------------------------------------------------------
     */
    public MapView3D1(ISimulationEngine engine, IVehicleManager manager, ControlPanel controlpanel) {
        this.controlpanel = controlpanel;
        this.engine = engine;
        this.Vehicleboxes = new HashMap<String, Box>();
        this.trafficLightRegistry = new HashMap<>();

        //-- Initialize Scene Groups--
        world = new Group();
        roadGroup = new Group();
        world.getChildren().add(roadGroup);
        
        //-- Lighting Setup--
        AmbientLight light = new AmbientLight(Color.WHITE);

        //-- Camera Configuration--
        camera = new PerspectiveCamera(true);  
        camera.setNearClip(1.0); 
        camera.setFarClip(60000.0);
        camera.setTranslateX(0);
        camera.setTranslateY(0);
        camera.setTranslateZ(-1200);

        // --Camera Pivot Setup for Rotation Logic--
        Group cameraPivot = new Group(camera);
        RotateX = new Rotate(-20, Rotate.X_AXIS);
        RotateY = new Rotate(0, Rotate.Y_AXIS);
        cameraPivot.getTransforms().addAll(RotateX, RotateY);
        cameraPivot.setTranslateX(200);
        cameraPivot.setTranslateY(-300);
        cameraPivot.setTranslateZ(-400);

        //-- Assembly of the 3D Scene --
        world.getChildren().addAll(light, cameraPivot);
        
        // -- Initial Scene Rendering --
        drawRoads();
        drawJunctions();
        setupLights();
        drawSky();
        drawGround();
    }

    /*-------------------------------------------------------------------------
     Renders the road network using textured Boxes and white line markings.
     --------------------------------------------------------------------------
     */
    private void drawRoads() {
        List<String> EdgeIds = engine.getEdgeIdList();
        
        // Asphalt Texture Mapping
       /* PhongMaterial asphaltMaterial = new PhongMaterial();
        try {
            Image asphaltImg = new Image(getClass().getResourceAsStream("/asphalt.jpg"));
            asphaltMaterial.setDiffuseMap(asphaltImg);
            asphaltMaterial.setSpecularColor(Color.BLACK); 
            asphaltMaterial.setDiffuseColor(Color.web("#333333"));
        } catch (Exception e) {
            asphaltMaterial.setDiffuseColor(Color.DARKGRAY);
        }*/
        
        PhongMaterial asphaltMaterial = new PhongMaterial();
        asphaltMaterial.setDiffuseColor(Color.web("#1A1A1A")); 
        asphaltMaterial.setSpecularColor(Color.BLACK);
        

        PhongMaterial lineMat = new PhongMaterial(Color.WHITE);

        for (String edge : EdgeIds) {
            List<Point2D> EdgePoints = engine.getEdgeShape(edge);
            Point2D pStart = EdgePoints.get(0);
            Point2D pEnd = EdgePoints.get(EdgePoints.size() - 1);
            double Edgelenght = engine.getEdgeLength(edge);
            Point2D Midellepoint = new Point2D.Double((pStart.getX() + pEnd.getX()) / 2, (pStart.getY() + pEnd.getY()) / 2);
            
            int lanenummer = engine.getLaneList(edge).size();
            double roadWidth = (edge.equals("E45")) ? lanenummer * 5.1 : lanenummer * 4.9;

            // --Road Geometry--
            Box roadBox = new Box(Edgelenght, 0.2, roadWidth);
            roadBox.setMaterial(asphaltMaterial);
            
            double rotation = (Math.abs(pStart.getX() - pEnd.getX()) < 0.001) ? -90 : 0;
            roadBox.setRotationAxis(Rotate.Y_AXIS);
            roadBox.setRotate(rotation);
            
            roadBox.setTranslateX(Midellepoint.getX());
            roadBox.setTranslateZ(Midellepoint.getY());
            roadBox.setTranslateY(0);
            roadGroup.getChildren().add(roadBox);

            //-- Lane Divider Markings Offset Calculation--
            double offset = roadWidth / 2.0;
            Box whiteLine = new Box(Edgelenght, 0.02, 0.15); 
            whiteLine.setMaterial(lineMat);
            whiteLine.setRotationAxis(Rotate.Y_AXIS);
            whiteLine.setRotate(rotation);

            if (rotation == 0) { 
                whiteLine.setTranslateX(Midellepoint.getX());
                whiteLine.setTranslateZ(Midellepoint.getY() + offset); 
            } else { 
                whiteLine.setTranslateX(Midellepoint.getX() + offset);
                whiteLine.setTranslateZ(Midellepoint.getY());
            }

            whiteLine.setTranslateY(-0.12); // Lifted slightly above road to prevent flickering
            roadGroup.getChildren().add(whiteLine);

            drawTrafficLight(roadBox, edge, lanenummer, pStart, pEnd);
        }
    }
    
    /*------------------------------------------------------------------
     Configures ambient and point lights for the 3D environment.
     -------------------------------------------------------------------
     */
    public void setupLights() {
        AmbientLight ambient = new AmbientLight(Color.color(1.0, 1.0, 1.0));
        PointLight sun = new PointLight(Color.WHITE);
        sun.setTranslateY(-20000); 
        roadGroup.getChildren().addAll(ambient, sun);
    }
    
    /*----------------------------------------------------------
      Renders a large box representing the ground/terrain.
      ----------------------------------------------------------
     */
    public void drawGround() {
        Box ground = new Box(50000, 1, 50000);
        ground.setTranslateY(10); 
        PhongMaterial groundMat = new PhongMaterial();
        groundMat.setDiffuseColor(Color.web("#1b3310"));
        ground.setMaterial(groundMat);
        roadGroup.getChildren().add(ground);
    }

    /*-----------------------------------------------------
     Renders an inverted sphere for the skybox effect.
     ------------------------------------------------------
     */
    private void drawSky() {
        Sphere sky = new Sphere(40000); 
        PhongMaterial skyMaterial = new PhongMaterial();
        this.sky = sky;
        skyMaterial.setDiffuseColor(Color.web("#87CEEB"));
        skyMaterial.setSelfIlluminationMap(null); 

        this.sky.setMaterial(skyMaterial);
        this.sky.setCullFace(CullFace.NONE);
        roadGroup.getChildren().add(this.sky);
    }
  
    /*-----------------------------------------------------------------------------
     Renders junctions using textured boxes to cover overlapping road segments.
     ------------------------------------------------------------------------------
     */
    private void drawJunctions() {
        List<String> junctionIds = engine.getJunctionIdList();
        
        PhongMaterial junctionMaterial = new PhongMaterial();
        junctionMaterial.setDiffuseColor(Color.web("#1A1A1A")); 
        junctionMaterial.setSpecularColor(Color.BLACK);
        

        for (String jId : junctionIds) {
            if (!(jId.equals("J55") || jId.equals("J57"))) continue;
            
            Point2D pos = engine.getJunctionPosition(jId);
            double junctionSize = 17.5; 
            Box junctionSquare = new Box(junctionSize, 0.1, junctionSize);
            junctionSquare.setMaterial(junctionMaterial);
            junctionSquare.setTranslateX(pos.getX());
            junctionSquare.setTranslateZ(pos.getY());
            junctionSquare.setTranslateY(-0.1); 

            roadGroup.getChildren().add(junctionSquare);
        }
    }

    /*--------------------------------------------------------------------------------
     Creates traffic light models (Poles, Housing, and Spheres) for controlled lanes.
     ---------------------------------------------------------------------------------
     */
    public void drawTrafficLight(Box edgeBox, String edgeId, int lanenummer, Point2D pStart, Point2D pEnd) {
        List<String> trafficLightIdList = this.engine.getTrafficLightIdList();
        
        for(String Tid : trafficLightIdList) {
            List<String> controlledLanes = this.engine.getControlledLanes(Tid);
            boolean isControlled = false;
            for(String laneId : controlledLanes) {
                if(laneId.startsWith(edgeId)) {
                    isControlled = true;
                    break;
                }
            }

            if(isControlled) {
                //-- Special handling for Gantry Lights on main edge E45--
                if (edgeId.equals("E45")) {
                    Box gantryPillar = new Box(0.8, 18, 0.8);
                    gantryPillar.setMaterial(new PhongMaterial(Color.DARKSLATEGRAY));
                    gantryPillar.setTranslateX(pEnd.getX() - 10.0);
                    gantryPillar.setTranslateZ(pEnd.getY() + 1);
                    gantryPillar.setTranslateY(-9); 
                    roadGroup.getChildren().add(gantryPillar);

                    for (int i = 0; i < 3; i++) {
                        double horizontalOffset = (i - 1) * 2.8;
                        Box housing = new Box(2.2, 6.5, 1.2);
                        housing.setMaterial(new PhongMaterial(Color.BLACK));
                        housing.setTranslateX(gantryPillar.getTranslateX() + horizontalOffset);
                        housing.setTranslateZ(gantryPillar.getTranslateZ() - 1);
                        housing.setTranslateY(-15);

                        Sphere r = new Sphere(0.8);
                        r.setTranslateX(housing.getTranslateX()); r.setTranslateY(-17);
                        r.setTranslateZ(housing.getTranslateZ() - 0.7);
                        r.setMaterial(RED_OFF);

                        Sphere y = new Sphere(0.8);
                        y.setTranslateX(housing.getTranslateX()); y.setTranslateY(-15);
                        y.setTranslateZ(housing.getTranslateZ() - 0.7);
                        y.setMaterial(YELLOW_OFF);

                        Sphere gArrow = new Sphere(0.7); 
                        gArrow.setTranslateX(housing.getTranslateX());
                        gArrow.setTranslateY(-13);
                        gArrow.setTranslateZ(housing.getTranslateZ() - 1.2); 
                        gArrow.setMaterial(GREEN_OFF);

                        // Arrow Direction logic for lanes
                        if (i == 0) { 
                            gArrow.setScaleX(1.8); gArrow.setScaleY(0.4); gArrow.setScaleZ(0.4);
                            gArrow.setRotate(-45);
                            gArrow.setTranslateX(gArrow.getTranslateX() - 0.2);
                        } else if (i == 1) { 
                            gArrow.setScaleX(0.4); gArrow.setScaleY(1.8); gArrow.setScaleZ(0.4);
                        } else if (i == 2) { 
                            gArrow.setScaleX(1.8); gArrow.setScaleY(0.4); gArrow.setScaleZ(0.4);
                            gArrow.setRotate(45);
                            gArrow.setTranslateX(gArrow.getTranslateX() + 0.2);
                        }

                        roadGroup.getChildren().addAll(housing, r, y, gArrow);
                        List<Sphere> spheres = new ArrayList<>();
                        spheres.add(r); spheres.add(y); spheres.add(gArrow);
                        trafficLightRegistry.put(edgeId + "_" + i, spheres);
                    }
                } else {
                    // Standard pole-based Traffic Lights
                    Box poteau = new Box(0.6, 12, 0.6);
                    poteau.setMaterial(new PhongMaterial(Color.DARKSLATEGRAY));
                    poteau.setTranslateY(-6);
                    double sideOffset = (lanenummer * 3.2) / 2.0 + 1.2;
                    double xDiff = pEnd.getX() - pStart.getX();
                    double zDiff = pEnd.getY() - pStart.getY();

                    if (Math.abs(xDiff) > Math.abs(zDiff)) {
                        poteau.setTranslateX(pEnd.getX());
                        double sign = (xDiff > 0) ? -1 : 1;
                        poteau.setTranslateZ(pEnd.getY() + (sign * sideOffset));
                    } else {
                        poteau.setTranslateZ(pEnd.getY());
                        double sign = (zDiff > 0) ? -1 : 1;
                        poteau.setTranslateX(pEnd.getX() - (sign * sideOffset));
                    }

                    roadGroup.getChildren().add(poteau);
                    Box housing = new Box(2, 6, 2);
                    housing.setMaterial(new PhongMaterial(Color.BLACK));
                    housing.setTranslateX(poteau.getTranslateX());
                    housing.setTranslateZ(poteau.getTranslateZ());
                    housing.setTranslateY(-12);
                    
                    Sphere redLight = new Sphere(0.8);
                    redLight.setTranslateX(poteau.getTranslateX());
                    redLight.setTranslateZ(poteau.getTranslateZ() - 1.1);
                    redLight.setTranslateY(-14);
                    redLight.setMaterial(new PhongMaterial(Color.web("#330000")));
                    
                    Sphere yellowLight = new Sphere(0.8);
                    yellowLight.setTranslateX(poteau.getTranslateX());
                    yellowLight.setTranslateZ(poteau.getTranslateZ() - 1.1);
                    yellowLight.setTranslateY(-12);
                    yellowLight.setMaterial(new PhongMaterial(Color.web("#333300")));

                    Sphere greenLight = new Sphere(0.8);
                    greenLight.setTranslateX(poteau.getTranslateX());
                    greenLight.setTranslateZ(poteau.getTranslateZ() - 1.1);
                    greenLight.setTranslateY(-10);
                    greenLight.setMaterial(new PhongMaterial(Color.web("#003300")));

                    roadGroup.getChildren().addAll(housing, redLight, yellowLight, greenLight);
                    List<Sphere> spheres = new ArrayList<>();
                    spheres.add(redLight); spheres.add(yellowLight); spheres.add(greenLight);
                    trafficLightRegistry.put(edgeId, spheres);
                    break; 
                }
            }
        }
    }
        
    /*----------------------------------------------------------------
     Updates traffic light materials based on current engine state.
     -----------------------------------------------------------------
     */
    public void updateTrafficLights() {
        List<String> trafficLightIdList = this.engine.getTrafficLightIdList();
        for(String Tid: trafficLightIdList) {
            List<String> controlledLanes = this.engine.getControlledLanes(Tid);
            String state = this.engine.getTrafficLightState(Tid);
            String eId;
            for(int i=0; i<controlledLanes.size(); i++) {
                if(!(controlledLanes.get(i).startsWith("E45"))) { 
                    eId = controlledLanes.get(i).split("_")[0]; 
                    i += 1;
                } else { 
                    eId = controlledLanes.get(i);
                }
                List<Sphere> Spheres = trafficLightRegistry.get(eId);
                if (Spheres != null && !state.isEmpty()) {
                    char s = state.charAt(i);
                    if(s=='r'||s=='R') { 
                        Spheres.get(0).setMaterial(RED_ON);
                        Spheres.get(1).setMaterial(YELLOW_OFF);
                        Spheres.get(2).setMaterial(GREEN_OFF); 
                    } else if(s=='y'||s=='Y') {  
                        Spheres.get(0).setMaterial(RED_OFF);
                        Spheres.get(1).setMaterial(YELLOW_ON);
                        Spheres.get(2).setMaterial(GREEN_OFF); 
                    } else if(s=='g'||s=='G') {
                        Spheres.get(0).setMaterial(RED_OFF);
                        Spheres.get(1).setMaterial(YELLOW_OFF);
                        Spheres.get(2).setMaterial(GREEN_ON); 
                    }
                }
            }
        }
    }

    /*--------------------------------------------------------------
     Synchronizes vehicle models with the simulation engine state.
     ---------------------------------------------------------------
     */
    public void updateVehicles(IVehicleManager manager) {
        if(manager != null) {
            Collection<IVehicle> Vehicles = manager.getAllVehicles();
            for(IVehicle v: Vehicles) {
                String VehicleId = v.getId();
                Point2D position = v.getPosition();
                if(position == null) {continue;}
                
                // Add new vehicles if not present
                if(!(Vehicleboxes.containsKey(VehicleId))) {
                    Box car = this.drawVehicle(v, controlpanel);
                    roadGroup.getChildren().add(car);
                }
                
                //-- Update position, visibility and color--
                Box b = Vehicleboxes.get(VehicleId);
                b.setVisible(v.isIsVisible()); 
                b.setTranslateX(position.getX());
                b.setTranslateZ(position.getY()); 
                
                PhongMaterial material = new PhongMaterial();
                switch(v.getColor()) {
                    case "Yellow": material.setDiffuseColor(Color.YELLOW); break;
                    case "Green":  material.setDiffuseColor(Color.GREEN);  break;
                    case "Red":    material.setDiffuseColor(Color.RED);    break;
                }
                b.setMaterial(material);
                
                double angle = engine.getVehicleAngle(VehicleId);
                b.setRotationAxis(Rotate.Y_AXIS);
                b.setRotate(angle);
            }

            //-- Cleanup vehicles that left simulation--
            List<String> ids = new ArrayList<>();
            for(String id: Vehicleboxes.keySet()) {
                ids.add(id);
            }
            for(String id: ids) {
                boolean check = false;
                for(IVehicle v : Vehicles) {
                    if(id.equals(v.getId())) { check = true; }
                }
                if(!check) {
                    Box b = Vehicleboxes.get(id);
                    roadGroup.getChildren().remove(b);
                    Vehicleboxes.remove(id);
                }
            }
        }
    }
    
    /*--------------------------------------------------------------------
     Factory method for creating vehicle Boxes based on type dimensions.
     ---------------------------------------------------------------------
     */
    public Box drawVehicle(IVehicle v, ControlPanel controlpanel) {
        String VehicleId = v.getId();
        Point2D position = v.getPosition();
        double l = 0.0, h = 0.0, w = 0.0;

        switch (v.getTypeId()) {
            case "DEFAULT_VEHTYPE":  l = 4; h = 2; w = 1.8; break;
            case "DEFAULT_CONTAINERTYPE": l = 10; h = 4.5; w = 2.5; break;
            case "RESCUE_TYPE": l = 4.5; h = 2; w = 1.8; break;
            case "BUS_TYPE": l = 12; h = 3.5; w = 2.5; break;
        }

        Box Car = new Box(w, h, l); 
        double initialAngle = engine.getVehicleAngle(VehicleId);
        Car.setRotationAxis(Rotate.Y_AXIS); 
        Car.setRotate(initialAngle);

        Car.setTranslateX(position.getX());
        Car.setTranslateZ(position.getY()); 
        Car.setTranslateY(-h/2); 

        Car.setOnMouseClicked(event -> {
            controlpanel.selectVehicle(VehicleId); 
        });

        Car.setPickOnBounds(true);
        Vehicleboxes.put(VehicleId, Car);
        return Car;
    }
    
    /*----------------------------------------------------
      Updates camera rotation based on user input deltas.
      ----------------------------------------------------
     */
    public void updateRotation(double deltaX , double deltaY) {
        double newAngelX = RotateX.getAngle() - deltaY;
        double newAngelY = RotateY.getAngle() + deltaX;
        RotateX.setAngle(newAngelX);
        RotateY.setAngle(newAngelY);
    }
    
    /*--------------------------------------------------
     Adjusts camera zoom within defined constraints.
     ---------------------------------------------------
     */
    public void Zoom(double dz) {
        double altposZ = camera.getTranslateZ();
        double newposZ = altposZ + dz;
        
        if (newposZ > -20.0) { 
            camera.setTranslateZ(-20.0);
        } 
        else if (newposZ < -5000.0) {
            camera.setTranslateZ(-5000.0);
        } 
        else {
            camera.setTranslateZ(newposZ);
        }
    }
   
    /*---------------------------------------------------------------------
     Public getters and world translation methods for user interaction.
     ----------------------------------------------------------------------
     */
    public PerspectiveCamera getCamera() { return this.camera; }
    public Group getRoot() { return this.world; }
    
    public void renderRoads() {
        if (world.getChildren().size() <= 6) { drawRoads(); }
    }
   
    public void moveRoadsVertical(double dy) {
        double newposY = roadGroup.getTranslateY() + dy;
        roadGroup.setTranslateY(newposY);
    }
  
    public void moveHorizontale(double dx, double dz) {
        double newPosX = roadGroup.getTranslateX() + dx;
        double newPosZ = roadGroup.getTranslateZ() + dz;
        roadGroup.setTranslateX(newPosX);
        roadGroup.setTranslateZ(newPosZ);
    }
}