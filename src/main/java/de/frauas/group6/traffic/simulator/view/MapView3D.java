package de.frauas.group6.traffic.simulator.view;

import javafx.scene.shape.Cylinder;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import javafx.scene.Node;

import de.frauas.group6.traffic.simulator.core.ISimulationEngine;
import de.frauas.group6.traffic.simulator.vehicles.IVehicle;
import de.frauas.group6.traffic.simulator.vehicles.IVehicleManager;
import javafx.animation.AnimationTimer;
import javafx.scene.AmbientLight;
import javafx.scene.Group;
import javafx.scene.PerspectiveCamera;
import javafx.scene.PointLight;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Box;
import javafx.scene.shape.CullFace;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.Sphere;
import javafx.scene.shape.TriangleMesh;
import javafx.scene.shape.VertexFormat;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Translate;

public class MapView3D {
    // --- SCENE GROUPS ---
    private Group world;
    private Group roadGroup;
    private Group sceneryGroup; 
    private Group cameraPivot; 

    // --- DATA & ANIMATION ---
    private HashMap<String, Group> VehicleGroups;
    private HashMap<String, VehicleAnimation> vehicleAnimations = new HashMap<>();
    private Map<String, List<Sphere>> trafficLightRegistry;

    // --- ASSET CACHES ---
    // Stores the raw 3D geometry (The Shape)
    private Map<String, MeshView> modelCache = new HashMap<>(); 
    // Stores the colored skins (The Paint)
    private Map<String, PhongMaterial> textureCache = new HashMap<>();

    // --- CAMERA TRANSFORMS ---
    private PerspectiveCamera camera;
    private Rotate rotateX;       
    private Rotate rotateY;       
    private Translate pivotTranslate; 
    private Translate cameraDist;     

    // --- DEPENDENCIES ---
    private IVehicleManager manager;
    private ISimulationEngine engine;
    private ControlPanel controlpanel;

    // --- MATERIALS ---
    private final PhongMaterial ROAD_MAT = new PhongMaterial();
    private final PhongMaterial GRASS_MAT = new PhongMaterial();
    private final PhongMaterial BUILDING_MAT = new PhongMaterial();
    private final PhongMaterial SKY_MAT = new PhongMaterial();

    // Traffic Lights Materials
    private final PhongMaterial RED_ON = new PhongMaterial(Color.RED);
    private final PhongMaterial RED_OFF = new PhongMaterial(Color.web("#440000"));
    private final PhongMaterial YELLOW_ON = new PhongMaterial(Color.YELLOW);
    private final PhongMaterial YELLOW_OFF = new PhongMaterial(Color.web("#444400"));
    private final PhongMaterial GREEN_ON = new PhongMaterial(Color.LIME);
    private final PhongMaterial GREEN_OFF = new PhongMaterial(Color.web("#004400"));

    public MapView3D(ISimulationEngine engine, IVehicleManager manager, ControlPanel controlpanel) {
        this.controlpanel = controlpanel;
        this.manager = manager;
        this.engine = engine;

        VehicleGroups = new HashMap<>();
        trafficLightRegistry = new HashMap<>();
        
        world = new Group();
        sceneryGroup = new Group(); 
        roadGroup = new Group();    

        loadAssets(); // <--- UNIFIED LOADING METHOD

        // LIGHTING
        AmbientLight ambient = new AmbientLight(Color.rgb(180, 180, 180)); 
        PointLight sun = new PointLight(Color.WHITE);
        sun.setTranslateY(-5000); 
        sun.setTranslateX(1000);
        sun.setTranslateZ(-1000);
        
        world.getChildren().addAll(sceneryGroup, roadGroup, ambient, sun);

        // CAMERA SETUP
        camera = new PerspectiveCamera(true);
        camera.setNearClip(1.0);
        camera.setFarClip(50000.0); 

        pivotTranslate = new Translate(200, 0, 200); 
        rotateY = new Rotate(0, Rotate.Y_AXIS);      
        rotateX = new Rotate(-45, Rotate.X_AXIS);    
        cameraDist = new Translate(0, 0, -1200);     

        cameraPivot = new Group();
        cameraPivot.getTransforms().addAll(pivotTranslate, rotateY, rotateX, cameraDist);
        
        cameraPivot.getChildren().add(camera);
        world.getChildren().add(cameraPivot);

        // WORLD GENERATION
        createEnvironment(); 
        generateCity();      
        drawRoads();         
        drawJunctions();
        
        startAnimationLoop();
    }

    /**
     * Loads Environment Textures and 3D .OBJ Models
     */
    private void loadAssets() {
        // 1. Load Environment Textures (Grass, Asphalt, Sky)
        Color ASPHALT_COLOR = Color.rgb(30, 30, 30);
        Color GRASS_COLOR   = Color.rgb(34, 139, 34);
        Color BUILD_COLOR   = Color.rgb(200, 200, 200);
        
        try {
            String path = "/texture/";
            
            // Road
            if (getClass().getResource(path + "asphalt.jpg") != null) 
                ROAD_MAT.setDiffuseMap(new Image(getClass().getResourceAsStream(path + "asphalt.jpg")));
            else ROAD_MAT.setDiffuseColor(ASPHALT_COLOR);

            // Grass
            if (getClass().getResource(path + "grass.jpg") != null) {
                GRASS_MAT.setDiffuseMap(new Image(getClass().getResourceAsStream(path + "grass.jpg")));
                if (getClass().getResource(path + "grass_normal.jpg") != null) 
                    GRASS_MAT.setBumpMap(new Image(getClass().getResourceAsStream(path + "grass_normal.jpg")));
            } else GRASS_MAT.setDiffuseColor(GRASS_COLOR);

            // Buildings
            if (getClass().getResource(path + "building.jpg") != null) 
                BUILDING_MAT.setDiffuseMap(new Image(getClass().getResourceAsStream(path + "building.jpg")));
            else BUILDING_MAT.setDiffuseColor(BUILD_COLOR);
            
            // Sky
            if (getClass().getResource(path + "sky.jpg") != null) {
                SKY_MAT.setDiffuseMap(new Image(getClass().getResourceAsStream(path + "sky.jpg")));
                SKY_MAT.setSelfIlluminationMap(new Image(getClass().getResourceAsStream(path + "sky.jpg")));
            } else SKY_MAT.setDiffuseColor(Color.LIGHTBLUE);

            // --- 2. LOAD 3D MODELS (The .obj files) ---
            System.out.println("Loading 3D Models...");
            modelCache.put("CAR", ObjLoader.load("/models/car.obj"));
            modelCache.put("TRUCK", ObjLoader.load("/models/truck.obj"));
            modelCache.put("BUS", ObjLoader.load("/models/bus.obj"));
            modelCache.put("RESCUE", ObjLoader.load("/models/rescue.obj")); 
            System.out.println("3D Models Loaded.");

        } catch (Exception e) {
            System.err.println("Error loading Assets: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void createEnvironment() {
        float size = 50000;
        float tiling = 500; 
        TriangleMesh mesh = new TriangleMesh();
        mesh.setVertexFormat(VertexFormat.POINT_TEXCOORD);
        mesh.getPoints().addAll(-size/2, 0, -size/2, size/2, 0, -size/2, size/2, 0, size/2, -size/2, 0, size/2);
        mesh.getTexCoords().addAll(0, 0, tiling, 0, tiling, tiling, 0, tiling);
        mesh.getFaces().addAll(0, 0, 1, 1, 2, 2, 0, 0, 2, 2, 3, 3);
        
        MeshView ground = new MeshView(mesh);
        ground.setMaterial(GRASS_MAT);
        ground.setTranslateY(2.0); 
        sceneryGroup.getChildren().add(ground);
        
        Sphere sky = new Sphere(20000);
        sky.setMaterial(SKY_MAT);
        sky.setCullFace(CullFace.NONE); 
        sceneryGroup.getChildren().add(sky);
    }

    private void generateCity() {
        List<String> edges = engine.getEdgeIdList();
        Random rand = new Random();
        for (String edgeId : edges) {
            if(edgeId.startsWith(":")) continue; 
            List<Point2D> shape = engine.getEdgeShape(edgeId);
            if(shape.size() < 2) continue;
            Point2D pStart = shape.get(0);
            Point2D pEnd = shape.get(shape.size()-1);
            
            double dx = pEnd.getX() - pStart.getX();
            double dy = pEnd.getY() - pStart.getY();
            double len = Math.sqrt(dx*dx + dy*dy);
            double rx = -dy / len; double ry = dx / len;
            double currentDist = 30;
            
            while(currentDist < len - 30) {
                double t = currentDist / len;
                double roadX = pStart.getX() + dx * t;
                double roadZ = pStart.getY() + dy * t;
                
                double h = 8 + rand.nextDouble() * 17; 
                double w = 15 + rand.nextDouble() * 10;  
                double d = 15 + rand.nextDouble() * 10;  
                
                if (rand.nextDouble() > 0.4) { 
                    Group b = createSimpleBuilding(w, h, d);
                    double offset = 30 + w/2; 
                    b.setTranslateX(roadX + (-rx * offset));
                    b.setTranslateZ(roadZ + (-ry * offset));
                    sceneryGroup.getChildren().add(b);
                }
                if (rand.nextDouble() > 0.4) {
                    Group b = createSimpleBuilding(w, h, d);
                    double offset = 30 + w/2; 
                    b.setTranslateX(roadX + (rx * offset));
                    b.setTranslateZ(roadZ + (ry * offset));
                    sceneryGroup.getChildren().add(b);
                }
                currentDist += 30 + rand.nextDouble() * 30; 
            }
        }
    }

    private Group createSimpleBuilding(double w, double h, double d) {
        Group building = new Group();
        Box body = new Box(w, h, d);
        body.setMaterial(BUILDING_MAT);
        body.setTranslateY(-h / 2);
        
        Box roof = new Box(w + 0.5 , 0.5, d + 0.5); 
        roof.setMaterial(ROAD_MAT);
        roof.setTranslateY(-h);
        
        building.getChildren().addAll(body, roof);
        return building;
    }
    
    //a flat paper-thin strip that we can "paint" onto the road
    private MeshView createLinePlane(double length, double width, float tiling) {
        TriangleMesh mesh = new TriangleMesh();
        mesh.setVertexFormat(VertexFormat.POINT_TEXCOORD);

        float hL = (float) (length / 2.0);
        float hW = (float) (width / 2.0);

        // Flat plane coordinates (Y is 0)
        mesh.getPoints().addAll(
            -hL, 0, -hW,  // 0
             hL, 0, -hW,  // 1
             hL, 0,  hW,  // 2
            -hL, 0,  hW   // 3
        );

        // Texture coordinates for the dashes
        mesh.getTexCoords().addAll(
            0, 0,      
            1, 0,      
            1, tiling, 
            0, tiling  
        );

        mesh.getFaces().addAll(0,0, 2,2, 1,1, 0,0, 3,3, 2,2);
        MeshView mv = new MeshView(mesh);
        mv.setCullFace(CullFace.NONE);// we paint on both sides.
        return mv;
    }
  private void drawRoads() {
    List<String> EdgeIds = engine.getEdgeIdList();
    
    // Materials
    PhongMaterial DASHED_MAT = new PhongMaterial();
    // IMPORTANT: To see dashes, you need a dash.png in your texture folder
    String dashPath = "/texture/dash.png";
    if (getClass().getResource(dashPath) != null) {
        DASHED_MAT.setDiffuseMap(new Image(getClass().getResourceAsStream(dashPath)));
    } else {
        DASHED_MAT.setDiffuseColor(Color.WHITESMOKE);
    }

    PhongMaterial SOLID_MAT = new PhongMaterial(Color.WHITE);

    for (String edge : EdgeIds) {
        if (edge.startsWith(":")) continue;

        List<Point2D> EdgePoints = engine.getEdgeShape(edge);
        if (EdgePoints.size() < 2) continue;

        Point2D pStart = EdgePoints.get(0);
        Point2D pEnd = EdgePoints.get(EdgePoints.size() - 1);
        
        double dx = pEnd.getX() - pStart.getX();
        double dy = pEnd.getY() - pStart.getY();
        double length = Math.sqrt(dx * dx + dy * dy) + 10.0; // Overlap junctions
        
        double centerX = (pStart.getX() + pEnd.getX()) / 2.0;
        double centerZ = (pStart.getY() + pEnd.getY()) / 2.0;
        double angleRad = Math.atan2(dy, dx);
        double angleDeg = Math.toDegrees(angleRad);

        int numLanes = engine.getLaneList(edge).size();
        double visualLaneWidth = 5.0; 
        double totalRoadWidth = (numLanes * visualLaneWidth) + 1.0; 

        // 1. Draw Asphalt
        Box roadBox = new Box(length, 0.2, totalRoadWidth);
        roadBox.setMaterial(ROAD_MAT);
        roadBox.setTranslateX(centerX);
        roadBox.setTranslateZ(centerZ);
        roadBox.setTranslateY(0.1); 
        roadBox.setRotationAxis(Rotate.Y_AXIS);
        roadBox.setRotate(-angleDeg); 
        roadGroup.getChildren().add(roadBox);

        // 2. Draw "Painted" Lines
        double startOffset = -((numLanes * visualLaneWidth) / 2.0);

        for (int i = 0; i <= numLanes; i++) {
            double offset = startOffset + (i * visualLaneWidth);
            boolean isLeftEdge = (i == numLanes);
            boolean isInternal = (i > 0 && i < numLanes);

            if (!isLeftEdge && !isInternal) continue;

            double stripWidth = isLeftEdge ? 0.4 : 0.15;
            Node line;

            if (isLeftEdge) {
                // Solid line (Left side)
                line = createLinePlane(length, stripWidth, 1.0f);
                ((MeshView)line).setMaterial(SOLID_MAT);
            } else {
                // Dashed lines (Internal)
                float tiling = (float) (length / 6.0); // Patterns repeat every 6m
                line = createLinePlane(length, stripWidth, tiling);
                ((MeshView)line).setMaterial(DASHED_MAT);
            }

            // Position math
            double offX = offset * Math.sin(angleRad);
            double offZ = offset * Math.cos(angleRad);

            line.setTranslateX(centerX + offX);
            line.setTranslateZ(centerZ - offZ);
            // Height 0.0 matches the top surface of the asphalt box perfectly
            line.setTranslateY(-0.2); 
            
            line.setRotationAxis(Rotate.Y_AXIS);
            line.setRotate(-angleDeg);
            roadGroup.getChildren().add(line);
        }

        drawTrafficLight(roadBox, edge, numLanes, pStart, pEnd);
    }
}

   private void drawJunctions() {
       for (String jId : engine.getJunctionIdList()) {
           Point2D pos = engine.getJunctionPosition(jId);
           
           // USE A CYLINDER INSTEAD OF A BOX
           // Radius 18 covers huge 3-lane intersections.
           // Height 0.2 matches the road thickness.
           Cylinder junctionDisc = new Cylinder(30, 0.2); 
           
           junctionDisc.setMaterial(ROAD_MAT);
           junctionDisc.setTranslateX(pos.getX());
           junctionDisc.setTranslateZ(pos.getY());
           junctionDisc.setTranslateY(0.1); // Same height as roads
           
           roadGroup.getChildren().add(junctionDisc);
       }
   }
    
    // --- CAMERA CONTROLS ---

    public void pan(double dx, double dy) {
        double panSpeed = 1.5; 
        double moveX = -dx * panSpeed; 
        double moveZ = dy * panSpeed;   

        double angleRad = Math.toRadians(rotateY.getAngle());
        double sin = Math.sin(angleRad);
        double cos = Math.cos(angleRad);

        double deltaX = (moveX * cos) + (moveZ * sin);
        double deltaZ = (moveX * -sin) + (moveZ * cos);

        pivotTranslate.setX(pivotTranslate.getX() + deltaX);
        pivotTranslate.setZ(pivotTranslate.getZ() + deltaZ);
    }

    public void rotate(double dx, double dy) {
        double rotSpeed = 0.2;
        rotateY.setAngle(rotateY.getAngle() + dx * rotSpeed);
        
        double newTilt = rotateX.getAngle() - dy * rotSpeed;
        rotateX.setAngle(Math.max(-89, Math.min(-5, newTilt)));
    }

    public void zoom(double rawDelta, double mouseX, double mouseY, double width, double height) {
        double direction = Math.signum(rawDelta);
        double zoomSpeed = 150.0; 
        double oldZ = cameraDist.getZ();
        double targetZ = oldZ + (direction * zoomSpeed);
        
        double newZ = Math.max(-8000, Math.min(-200, targetZ));
        cameraDist.setZ(newZ);

        if (newZ != oldZ) {
            double centerX = width / 2.0;
            double centerY = height / 2.0;
            double dx = mouseX - centerX;
            double dy = mouseY - centerY;
            
            double shiftFactor = 0.2 * direction; 

            double angleRad = Math.toRadians(rotateY.getAngle());
            double sin = Math.sin(angleRad);
            double cos = Math.cos(angleRad);

            double worldDx = (dx * cos) + (dy * sin);
            double worldDz = (dx * -sin) + (dy * cos);

            pivotTranslate.setX(pivotTranslate.getX() + worldDx * shiftFactor);
            pivotTranslate.setZ(pivotTranslate.getZ() + worldDz * shiftFactor);
        }
    }

    // --- TRAFFIC LIGHTS ---
    public void drawTrafficLight(Box edgeBox, String edgeId, int lanenummer, Point2D pStart, Point2D pEnd) {
        List<String> trafficLightIdList = this.engine.getTrafficLightIdList();
        
        double dx = pEnd.getX() - pStart.getX(); double dz = pEnd.getY() - pStart.getY();
        double length = Math.sqrt(dx * dx + dz * dz);
        double dirX = dx / length; double dirZ = dz / length;
        double rightX = dirZ; double rightZ = -dirX;
        double roadAngleRad = Math.atan2(dz, dx);
        double roadAngleDeg = Math.toDegrees(roadAngleRad);
        double facingAngle = -roadAngleDeg + 90;

        for (String Tid : trafficLightIdList) {
            List<String> controlledLanes = this.engine.getControlledLanes(Tid);
            List<Integer> relevantIndices = new ArrayList<>();
            for (int i = 0; i < controlledLanes.size(); i++) {
                if (controlledLanes.get(i).startsWith(edgeId)) relevantIndices.add(i);
            }
            if (!relevantIndices.isEmpty()) {
                drawGantry(edgeId, Tid, relevantIndices, controlledLanes, pEnd, dirX, dirZ, rightX, rightZ, facingAngle, lanenummer);
                break; 
            }
        }
    }

    private void drawGantry(String edgeId, String Tid, List<Integer> relevantIndices, List<String> controlledLanes, 
                            Point2D pEnd, double dirX, double dirZ, double rightX, double rightZ, double facingAngle, int totalLanes) {
        int numLights = relevantIndices.size();
        double laneWidth = 3.5;
        double roadWidth = totalLanes * laneWidth;
        double sideDistance = roadWidth / 2.0 + 2.0; 
        double beamLength = roadWidth + 5.0; 
 
        //bigger box
        //Box pillar = new Box(0.8, 18, 0.8);
        //smaller box
        Box pillar = new Box(0.4, 12, 0.4);
        pillar.setMaterial(new PhongMaterial(Color.DARKSLATEGRAY));
        pillar.setTranslateX(pEnd.getX() + rightX * sideDistance);
        pillar.setTranslateZ(pEnd.getY() + rightZ * sideDistance);
        pillar.setTranslateY(-6); // we half the PillarHeight  so it sit on the ground
        roadGroup.getChildren().add(pillar);

        //bigger beam
        //Box beam = new Box(0.4, 0.4, beamLength);
        //smaller beam
        Box beam = new Box(0.2, 0.2, beamLength);
        beam.setMaterial(new PhongMaterial(Color.SILVER));
        beam.setRotationAxis(Rotate.Y_AXIS);
        beam.setRotate(facingAngle - 90); 
        
        double shiftAmount = (beamLength / 2.0) - 2.5; 
        double beamCenterX = (pEnd.getX() + rightX * sideDistance) - (rightX * shiftAmount);
        double beamCenterZ = (pEnd.getY() + rightZ * sideDistance) - (rightZ * shiftAmount);
        
        beam.setTranslateX(beamCenterX);
        beam.setTranslateZ(beamCenterZ);
        beam.setTranslateY(-10.0);
        roadGroup.getChildren().add(beam);

        for (int i = 0; i < numLights; i++) {
            int stateIndex = relevantIndices.get(i);
            double offset = ((totalLanes - 1) / 2.0 - i) * laneWidth;
            double posX = pEnd.getX() + (rightX * offset);
            double posZ = pEnd.getY() + (rightZ * offset);
            Group singleLight = createPhysicalLight(facingAngle);
            singleLight.setTranslateX(posX);
            singleLight.setTranslateZ(posZ);
            singleLight.setTranslateY(-9.5);
            roadGroup.getChildren().add(singleLight);
            registerSpheresInRegistry(edgeId + "_index_" + stateIndex, singleLight);
        }
    }

    private Group createPhysicalLight(double rotationY) {
        Group lightGroup = new Group();
        //bigger housing
        //Box housing = new Box(1.8, 6.0, 1.0);
        
        //smaller housing
        Box housing = new Box(1.0, 3.5, 0.6);
        housing.setMaterial(new PhongMaterial(Color.BLACK));
        double fZ = -0.35; 
        Sphere r = new Sphere(0.35); r.setTranslateY(-1.2); r.setTranslateZ(fZ); r.setMaterial(RED_OFF);
        Sphere y = new Sphere(0.35); y.setTranslateY(0);    y.setTranslateZ(fZ); y.setMaterial(YELLOW_OFF);
        Sphere g = new Sphere(0.35); g.setTranslateY(1.2);  g.setTranslateZ(fZ); g.setMaterial(GREEN_OFF);
        lightGroup.getChildren().addAll(housing, r, y, g);
        lightGroup.setRotationAxis(Rotate.Y_AXIS);
        lightGroup.setRotate(rotationY);
        return lightGroup;
    }

    private void registerSpheresInRegistry(String key, Group group) {
        List<Sphere> spheres = new ArrayList<>();
        group.getChildren().stream().filter(node -> node instanceof Sphere).forEach(node -> spheres.add((Sphere)node));
        spheres.sort((s1, s2) -> Double.compare(s1.getTranslateY(), s2.getTranslateY()));
        trafficLightRegistry.put(key, spheres);
    }

    public void updateTrafficLights() {
        List<String> trafficLightIdList = this.engine.getTrafficLightIdList();
        for (String Tid : trafficLightIdList) {
            String state = this.engine.getTrafficLightState(Tid);
            List<String> controlledLanes = this.engine.getControlledLanes(Tid);
            if (state == null || controlledLanes == null) continue;
            for (int i = 0; i < state.length(); i++) {
                String laneId = controlledLanes.get(i);
                String edgeId = laneId.contains("_") ? laneId.split("_")[0] : laneId;
                List<Sphere> spheres = trafficLightRegistry.get(edgeId + "_index_" + i);
                if (spheres != null) updateSpheresMaterial(spheres, state.charAt(i));
            }
        }
    }

    private void updateSpheresMaterial(List<Sphere> spheres, char s) {
        if (spheres.size() < 3) return;
        if (s == 'r' || s == 'R') {
            spheres.get(0).setMaterial(RED_ON); spheres.get(1).setMaterial(YELLOW_OFF); spheres.get(2).setMaterial(GREEN_OFF);
        } else if (s == 'y' || s == 'Y' || s == 'u' || s == 'U') {
            spheres.get(0).setMaterial(RED_OFF); spheres.get(1).setMaterial(YELLOW_ON); spheres.get(2).setMaterial(GREEN_OFF);
        } else if (s == 'g' || s == 'G') {
            spheres.get(0).setMaterial(RED_OFF); spheres.get(1).setMaterial(YELLOW_OFF); spheres.get(2).setMaterial(GREEN_ON);
        } else {
            spheres.get(0).setMaterial(RED_OFF); spheres.get(1).setMaterial(YELLOW_OFF); spheres.get(2).setMaterial(GREEN_OFF);
        }
    }

    public PerspectiveCamera getCamera() { return this.camera; }
    public Group getRoot() { return this.world; }

    private void startAnimationLoop() {
        new AnimationTimer() {
            @Override public void handle(long now) {
                long currentTime = System.currentTimeMillis();
                for (VehicleAnimation anim : vehicleAnimations.values()) anim.animate(currentTime);
            }
        }.start();
    }

    // --- VEHICLE UPDATE LOOP ---
    public void updateVehicles(IVehicleManager manager) {
        if (manager == null) return;
        Collection<IVehicle> Vehicles = manager.getAllVehicles();
        long now = System.currentTimeMillis();

        for (IVehicle v : Vehicles) {
            String VehicleId = v.getId();
            Point2D position = v.getPosition();
            if (position == null) continue;

            if (!VehicleGroups.containsKey(VehicleId)) {
                Group carGroup = this.drawVehicle(v); // Calls the 3D method
                roadGroup.getChildren().add(carGroup);
                VehicleAnimation anim = new VehicleAnimation(carGroup);
                anim.forcePosition(position.getX(), position.getY());
                vehicleAnimations.put(VehicleId, anim);
            }

            if (VehicleGroups.containsKey(VehicleId)) {
                VehicleAnimation anim = vehicleAnimations.get(VehicleId);
                if (anim != null) {
                    double angle = engine.getVehicleAngle(VehicleId);
                    anim.updateTarget(position.getX(), position.getY(), angle, now);
                }
                Group g = VehicleGroups.get(VehicleId);
                g.setVisible(v.isIsVisible());
            }
        }
        
        List<String> idsToRemove = new ArrayList<>();
        for (String id : VehicleGroups.keySet()) {
            boolean check = false;
            for (IVehicle v : Vehicles) if (id.equals(v.getId())) { check = true; break; }
            if (!check) idsToRemove.add(id);
        }
        for (String id : idsToRemove) {
            Group b = VehicleGroups.get(id);
            roadGroup.getChildren().remove(b);
            VehicleGroups.remove(id);
            vehicleAnimations.remove(id);
        }
    }

    // --- 3D VEHICLE DRAWING ---
    public Group drawVehicle(IVehicle v) {
        String typeKey = "CAR";
        double scale = 1.0; 

        // 1. Identify Model & Scale
        switch (v.getTypeId()) {
            case "DEFAULT_CONTAINERTYPE": 
                typeKey = "TRUCK"; 
                scale = 1.3; 
                break;
            case "BUS_TYPE":              
                typeKey = "BUS";   
                scale = 1.5; 
                break;
            case "RESCUE_TYPE":           
                typeKey = "RESCUE"; 
                scale = 1.2; 
                break;
            default:
                typeKey = "CAR";
                scale = 1.0;
                break;
        }

        // 2. Get the Base Mesh (Geometry)
        MeshView originalMesh = modelCache.get(typeKey);
        if (originalMesh == null || originalMesh.getMesh() == null) {
            System.err.println("Model missing for " + typeKey + " - Using Box Fallback");
            return new Group(new Box(2,2,4)); 
        }

        // 3. Create a view of the mesh
        MeshView vehicleMesh = new MeshView(originalMesh.getMesh());
        
        // 4. Determine Texture (Coloring)
        String colorString = (v.getColor() != null) ? v.getColor().toLowerCase() : "red";
        
        // Use "car_" prefix for shared textures (Truck/Bus/Car share same layout)
        String textureName = "car_" + colorString + ".png"; 

        if(typeKey.equals("RESCUE")) {
            textureName = "rescue_white.png"; 
        }
        
        // 5. Load/Cache Material
        // We check if we already loaded "car_red.png" to save memory
        String matKey = textureName;
        PhongMaterial mat = textureCache.get(matKey);
        
        if (mat == null) {
            mat = new PhongMaterial();
            String texPath = "/texture/" + textureName;
            if (getClass().getResource(texPath) != null) {
                mat.setDiffuseMap(new Image(getClass().getResourceAsStream(texPath)));
            } else {
                System.out.println("Missing texture: " + texPath + " (Using solid color)");
                mat.setDiffuseColor(Color.web(colorString.equals("white") ? "white" : colorString)); 
            }
            textureCache.put(matKey, mat);
        }
        vehicleMesh.setMaterial(mat);

        // 6. Orientation & Scale
        vehicleMesh.setRotationAxis(Rotate.X_AXIS); 
        vehicleMesh.setRotate(180); 
        
     // This shifts the geometry so the "Position" point is at the Front Bumper, not the venter of the car
        
        
        Group carGroup = new Group(vehicleMesh);
        carGroup.setRotationAxis(Rotate.Y_AXIS);
        carGroup.setRotate(180);
        carGroup.setScaleX(scale * 1.75); 
        carGroup.setScaleY(scale * 1.75);
        carGroup.setScaleZ(scale * 1.75);
        
        // Lift up so wheels touch ground (Adjust as needed)
        carGroup.setTranslateY(-1.75 * scale); 

        // 7. Interaction & Position
        carGroup.setOnMouseClicked(event -> controlpanel.selectVehicle(v.getId()));
        carGroup.setPickOnBounds(true); 

        double initialAngle = engine.getVehicleAngle(v.getId());
        Group wrapper = new Group(carGroup); 
        wrapper.setRotationAxis(Rotate.Y_AXIS);
        wrapper.setRotate(initialAngle);

        Point2D pos = v.getPosition();
        wrapper.setTranslateX(pos.getX());
        wrapper.setTranslateZ(pos.getY());

        VehicleGroups.put(v.getId(), wrapper);
        return wrapper;
    }

    private static class VehicleAnimation {
        Group group;
        double startX, startZ, startAngle;
        double targetX, targetZ, targetAngle;
        long startTime, endTime;

        VehicleAnimation(Group group) { this.group = group; }

        void forcePosition(double x, double y) {
            group.setTranslateX(x); 
            group.setTranslateZ(y);
            this.startX = x; this.startZ = y;
            this.targetX = x; this.targetZ = y;
        }

        void updateTarget(double x, double y, double angle, long now) {
            this.startX = group.getTranslateX(); 
            this.startZ = group.getTranslateZ(); 
            this.startAngle = group.getRotate();
            
            this.targetX = x; 
            this.targetZ = y; 
            this.targetAngle = angle; 
            
            this.startTime = now; 
            this.endTime = now + 1000; 
        }
        
        void animate(long now) {
            if (now >= endTime) {
                group.setTranslateX(targetX); 
                group.setTranslateZ(targetZ); 
                group.setRotate(targetAngle);
                return;
            }
            
            double t = (double) (now - startTime) / (endTime - startTime);
            group.setTranslateX(startX + (targetX - startX) * t);
            group.setTranslateZ(startZ + (targetZ - startZ) * t);
            
            double diff = targetAngle - startAngle;
            if (diff > 180) diff -= 360;
            if (diff < -180) diff += 360;
            group.setRotate(startAngle + diff * t);
        }
    }
}