package de.frauas.group6.traffic.simulator.view;


import java.util.HashMap;
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
import javafx.scene.Node;
import javafx.scene.PerspectiveCamera;
import javafx.scene.SceneAntialiasing;
import javafx.scene.SubScene;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Box;
import javafx.scene.shape.Cylinder;
import javafx.scene.shape.Sphere;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.transform.Rotate;
import java.util.HashMap;
import java.util.Map;
import java.util.HashMap;
import java.util.Map;

public class MapView3D {
    private Group world;
   private Group roadGroup;
   // private SubScene subScene;
    private IVehicleManager manager;
    private ISimulationEngine engine;
    private PerspectiveCamera camera;
    private HashMap<String, Box> Vehicleboxes;
    private Rotate RotateX;
    private Rotate RotateY;
    private ControlPanel controlpanel;
    private Map<String, List<Sphere>> trafficLightRegistry ;
    private Map<String,Box> Edges;
 // RED
    private final PhongMaterial RED_ON = new PhongMaterial(Color.RED);
    private final PhongMaterial RED_OFF = new PhongMaterial(Color.web("#440000")); // H'mer bared/t'afi

    // YELLOW
    private final PhongMaterial YELLOW_ON = new PhongMaterial(Color.YELLOW);
    private final PhongMaterial YELLOW_OFF = new PhongMaterial(Color.web("#444400")); // S'fer bared/t'afi

    // GREEN
    private final PhongMaterial GREEN_ON = new PhongMaterial(Color.LIME); // LIME k-i-ban m-ch'e3el k-tar mn Green
    private final PhongMaterial GREEN_OFF = new PhongMaterial(Color.web("#004400")); // Kh'der bared/t'afi
    
    
    
    public MapView3D( ISimulationEngine engine,IVehicleManager manager,ControlPanel controlpanel) {
    	
    	this.controlpanel=controlpanel;
    	this.Edges=new HashMap();
    	//engine
    	this.manager=manager;
    	//vehiclemanager
    	this.engine=engine;
    	 Vehicleboxes=new HashMap<String, Box>();
        // 1. L-Hangar (SubScene)
    	 world = new Group();

    	 roadGroup = new Group();

    	 world.getChildren().add(roadGroup);
        
    	 trafficLightRegistry = new HashMap<>();
    	 
        // 3. D-dou (AmbientLight) - Darouri bezzaf!
        AmbientLight light = new AmbientLight(Color.WHITE);

        // 4. L-Kamira (L-eayn)
        camera = new PerspectiveCamera(true);  
        camera.setNearClip(0.1); // Darouri bach t-chuf l-qrib
        camera.setFarClip(10000.0); // Darouri bach t-chuf l-be'id
        camera.setTranslateX(0);

        camera.setTranslateY(0);

        camera.setTranslateZ(-1200);

        // 5. Milo l-eayn (Pivot) bach n-farkou l-3D
        Group cameraPivot = new Group(camera);
        RotateX=new Rotate(-20,Rotate.X_AXIS);
        RotateY = new Rotate(0,Rotate.Y_AXIS);
        cameraPivot.getTransforms().addAll(
            RotateX, // Milo l-teht
           RotateY   // Dour l-iyem
        );
        cameraPivot.setTranslateX(200);
        cameraPivot.setTranslateY(-300);
      cameraPivot.setTranslateZ(-400);
        // 6. Hat't'i kolchi f l-world
        world.getChildren().addAll( light, cameraPivot);
        
        
        drawRoads();
        drawJunctions();
       
    }

  
    
    private void drawRoads() {
    	
    	List<String> EdgeIds = engine.getEdgeIdList() ;
    	
    	for(String edge: EdgeIds ) {
    		
    		List<Point2D> EdgePoints = engine.getEdgeShape(edge);
    		
    		Point2D pStart= EdgePoints.get(0);
    		Point2D pEnd = EdgePoints.get(EdgePoints.size()-1);
    		double Edgelenght= engine.getEdgeLength(edge);
    		Point2D Midellepoint= new Point2D.Double((pStart.getX()+pEnd.getX())/2, (pStart.getY()+pEnd.getY())/2);
    		
    		int lanenummer= engine.getLaneList(edge).size();
    		Box Edge;
    		if(edge.equals("E45")) {Edge = new Box(Edgelenght,0.2,(lanenummer*5.1)); }
    		
    		else {Edge = new Box(Edgelenght,0.2,(lanenummer*4.6));}
    		
    		
    		double X = Math.abs(pStart.getX()-pEnd.getX());
    		if(X<0.001) {
    			
    			Edge.setRotationAxis(Rotate.Y_AXIS);
    			Edge.setRotate(-90);
    			
    			
    			
    		}
    		 
    		else {
    			Edge.setRotationAxis(Rotate.Y_AXIS);
    			Edge.setRotate(0);
    		
    		}
    		Edge.setTranslateZ(Midellepoint.getY());
    		Edge.setTranslateX(Midellepoint.getX());
    		Edge.setTranslateY(0);
    		
    		
    		
    		
    		PhongMaterial material = new PhongMaterial();
            material.setDiffuseColor(Color.BLACK);
            
            Edge.setMaterial(material);
            roadGroup.getChildren().add(Edge);
            drawTrafficLight(Edge, edge, lanenummer, pStart, pEnd);
            
    		
    	}
    	
  
    	 
    }
    
    
    private void drawJunctions() {
        List<String> junctionIds = engine.getJunctionIdList();

        for (String jId : junctionIds) {
            
        	if (!(jId.equals("J55") || jId.equals("J57"))) continue;
        	
            Point2D pos = engine.getJunctionPosition(jId);
            
            // 1. Kabbrou l-h'ajm chwiya l-ga' l-junctions 
            // 12.0 oulla 14.0 ghadi i-ghatti J55 o ma-ghadi-sh i-khorj l-triqat l-akhrin
            double junctionSize = 17; 
            
            // 2. Cylinder (Dā'ira) f blast Box bach t-kon l-khidma nqiya
            // Radius dyalha houwa junctionSize / 2
            Cylinder junctionDisc = new Cylinder(junctionSize / 2, 0.22);
            
            PhongMaterial mat = new PhongMaterial();
            mat.setDiffuseColor(Color.BLACK); // Nafs loun l-triq
            junctionDisc.setMaterial(mat);

            // 3. Cylinder khassou i-welli nã'ess (Rotate 90 daraja 3la X)
            junctionDisc.setRotationAxis(Rotate.X_AXIS);
            junctionDisc.setRotate(90);

            // 4. L-Position m3a l-offset dyal +3 li derti f drawRoads
            junctionDisc.setTranslateX(pos.getX());
            junctionDisc.setTranslateZ(pos.getY() ); 
            junctionDisc.setTranslateY(0.01); // Bach t-koun m-ghatya l-triq s-wa s-wa

            roadGroup.getChildren().add(junctionDisc);
        }
    }
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
                // 1. Creeyi l-Poteau
            	if (edgeId.equals("E45")) {
            	    // 1. Poteau kbir (Gantry)
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
            	        housing.setTranslateZ(gantryPillar.getTranslateZ() - 1); // Housing f -1.0
            	        housing.setTranslateY(-15);

            	        // Spheres Red o Yellow (Index 0 o 1)
            	        Sphere r = new Sphere(0.8);
            	        r.setTranslateX(housing.getTranslateX()); r.setTranslateY(-17);
            	        r.setTranslateZ(housing.getTranslateZ() - 0.7);
            	        r.setMaterial(RED_OFF);

            	        Sphere y = new Sphere(0.8);
            	        y.setTranslateX(housing.getTranslateX()); y.setTranslateY(-15);
            	        y.setTranslateZ(housing.getTranslateZ() - 0.7);
            	        y.setMaterial(YELLOW_OFF);

            	        // --- L-KHADRA (Index 2): S'hem m-jebbed o khārej l-barre ---
            	        Sphere gArrow = new Sphere(0.7); 
            	        gArrow.setTranslateX(housing.getTranslateX());
            	        gArrow.setTranslateY(-13);
            	        
            	        // MUHIM: Khrejh l-iddam bzaf 3la l-Housing bash i-ban nichan
            	        gArrow.setTranslateZ(housing.getTranslateZ() - 1.2); 
            	        gArrow.setMaterial(GREEN_OFF);

            	        // --- Gaddi s'hem (Pro Scaling & Rotation) ---
            	        if (i == 0) { // Left (<-)
            	            gArrow.setScaleX(1.8); gArrow.setScaleY(0.4); gArrow.setScaleZ(0.4);
            	            gArrow.setRotate(-45); // S'hem k-i-chouf l-issar
            	            gArrow.setTranslateX(gArrow.getTranslateX() - 0.2);
            	        } else if (i == 1) { // Straight (|)
            	            gArrow.setScaleX(0.4); gArrow.setScaleY(1.8); gArrow.setScaleZ(0.4);
            	        } else if (i == 2) { // Right (->)
            	            gArrow.setScaleX(1.8); gArrow.setScaleY(0.4); gArrow.setScaleZ(0.4);
            	            gArrow.setRotate(45); // S'hem k-i-chouf l-immin
            	            gArrow.setTranslateX(gArrow.getTranslateX() + 0.2);
            	        }

            	        roadGroup.getChildren().addAll(housing, r, y, gArrow);

            	        // Registry (Compatibility 100% m3a List<Sphere>)
            	        List<Sphere> spheres = new ArrayList<>();
            	        spheres.add(r);      // Index 0
            	        spheres.add(y);      // Index 1
            	        spheres.add(gArrow); // Index 2 (Hada houwa s'hem d-aba)
            	        
            	        trafficLightRegistry.put(edgeId + "_" + i, spheres);
            	    }
            	}
                 else {
            	 Box poteau = new Box(0.6, 12, 0.6);
            	
                poteau.setMaterial(new PhongMaterial(Color.DARKSLATEGRAY));
                poteau.setTranslateY(-6); // Tla3ih foq l-ard

                // 2. L-Side Offset (ba33di 3la triq chwiya)
                double sideOffset = (lanenummer * 3.2) / 2.0 + 1.2;

                // 3. L-Hisab dyal l-direction (Vector Logic)
                double xDiff = pEnd.getX() - pStart.getX();
                double zDiff = pEnd.getY() - pStart.getY();

                if (Math.abs(xDiff) > Math.abs(zDiff)) {
                    // Triq horizontal
                    poteau.setTranslateX(pEnd.getX());
                    double sign = (xDiff > 0) ? -1 : 1;
                    poteau.setTranslateZ(pEnd.getY() + (sign * sideOffset));
                } else {
                    // Triq vertical
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
               // roadGroup.getChildren().add(housing);
                
                Sphere redLight = new Sphere(0.8);
                redLight.setTranslateX(poteau.getTranslateX());
                redLight.setTranslateZ(poteau.getTranslateZ() - 1.1); // Ziyeh'ha l-l-iddam chwiya bach t-ban
                redLight.setTranslateY(-14);
                redLight.setMaterial(new PhongMaterial(Color.web("#330000")));
                
                Sphere yellowLight = new Sphere(0.8);
                yellowLight.setTranslateX(poteau.getTranslateX());
                yellowLight.setTranslateZ(poteau.getTranslateZ() - 1.1);
                yellowLight.setTranslateY(-12);
                yellowLight.setMaterial(new PhongMaterial(Color.web("#333300"))); // Yellow ghameq (t'afi)

                Sphere greenLight = new Sphere(0.8);
                greenLight.setTranslateX(poteau.getTranslateX());
                greenLight.setTranslateZ(poteau.getTranslateZ() - 1.1);
                greenLight.setTranslateY(-10);
                greenLight.setMaterial(new PhongMaterial(Color.web("#003300"))); // Green ghameq (t'afi)

                // 3. Zidihom kamlin l-l-Scene
                roadGroup.getChildren().addAll(housing, redLight, yellowLight, greenLight);
                
                List<Sphere> spheres = new ArrayList<>();
                spheres.add(redLight);    // index 0
                spheres.add(yellowLight); // index 1
                spheres.add(greenLight);  // index 2
                trafficLightRegistry.put(edgeId, spheres);
               
                break; 
            
            }
        }
    }
        }
    
   public void  updateTrafficLights(){
	   
	   List<String> trafficLightIdList = this.engine.getTrafficLightIdList();
	   for(String Tid: trafficLightIdList) {
	   List<String> controlledLanes = this.engine.getControlledLanes(Tid);
    	
    	String state= 	this.engine.getTrafficLightState(Tid);
    	
    	String eId;
    	
    	for(int i=0;i<controlledLanes.size();i++) {
    
      if(!(controlledLanes.get(i).startsWith("E45")) ) { eId = controlledLanes.get(i).split("_")[0];  i+=1;}
        else { eId= controlledLanes.get(i);}
    	List<Sphere> Spheres = trafficLightRegistry.get(eId);
    	
    	
    	
    	if (Spheres != null && !state.isEmpty()) {
    		
    		char s= state.charAt(i);
    		
    		if(s=='r'||s=='R') { Spheres.get(0).setMaterial(RED_ON);
    	    Spheres.get(1).setMaterial(YELLOW_OFF);
    	   Spheres.get(2).setMaterial(GREEN_OFF);  }
    		
    		
    		else if(s=='y'||s=='Y') {  Spheres.get(0).setMaterial(RED_OFF);
    	    Spheres.get(1).setMaterial(YELLOW_ON);
    	   Spheres.get(2).setMaterial(GREEN_OFF);    }
    		
    		
    		else if(s=='g'||s=='G') {
    			Spheres.get(0).setMaterial(RED_OFF);
        	    Spheres.get(1).setMaterial(YELLOW_OFF);
        	   Spheres.get(2).setMaterial(GREEN_ON); 
    		}
    	}
    		
    	}
    	}
    
   }
    public PerspectiveCamera getCamera() {
        return this.camera; // Bach GuiManager i-pousit'ionni l-view
    }
    
    public Group getRoot() {
        return this.world; // Bach SubScene t-shouf l-ajsam
    }
    
    public void renderRoads() {
        // Ila kant l-world fiha ghir l-axes o l-light, rsemi s-mita
        if (world.getChildren().size() <= 6) { 
            drawRoads();
        }
    }
    private void drawAxes() {
        // X-Achse (Rot)
        Cylinder xAxis = new Cylinder(1, 1000);
        xAxis.setRotationAxis(Rotate.Z_AXIS);
        xAxis.setRotate(90);
        xAxis.setMaterial(new PhongMaterial(Color.RED));

        // Y-Achse (Green)
        Cylinder yAxis = new Cylinder(1, 1000);
        yAxis.setMaterial(new PhongMaterial(Color.GREEN));

        // Z-Achse (Blue)
        Cylinder zAxis = new Cylinder(1, 1000);
        zAxis.setRotationAxis(Rotate.X_AXIS);
        zAxis.setRotate(90);
        zAxis.setMaterial(new PhongMaterial(Color.BLUE));

        world.getChildren().addAll(xAxis, yAxis, zAxis);
    }
    
    public void updateVehicles(IVehicleManager manager) {
        if(manager!=null) {
    	Collection<IVehicle> Vehicles = manager.getAllVehicles();
    	
    	for(IVehicle v: Vehicles) {
    		String VehicleId= v.getId();
    		Point2D position= v.getPosition();
    		if(position == null) {continue;}
    		if(!(Vehicleboxes.containsKey(VehicleId))) {
    			
    			
    			Box car = this.drawVehicle(v,controlpanel);
    		
    			
    			roadGroup.getChildren().add(car);
    		}
    		

    			 Vehicleboxes.get(VehicleId).setVisible(v.isIsVisible()); 
    			
    			
    				Vehicleboxes.get(VehicleId).setTranslateX(position.getX());
    	    		Vehicleboxes.get(VehicleId).setTranslateZ(position.getY()); 
    	    		Box b= Vehicleboxes.get(VehicleId);
    	    		PhongMaterial material = new PhongMaterial();
    	            switch(v.getColor()) {
    	                case "Yellow": material.setDiffuseColor(Color.YELLOW); break;
    	                case "Green":  material.setDiffuseColor(Color.GREEN);  break;
    	                case "Rot":    material.setDiffuseColor(Color.RED);    break;
    	                default:       material.setDiffuseColor(Color.BLUE);
    	            }
    	            b.setMaterial(material);
    	            
    	            
    	            double angle = engine.getVehicleAngle(VehicleId);
    	            b.setRotationAxis(Rotate.Y_AXIS);
    	            b.setRotate(angle);
    			
    	     		
    		
    		
    	}
    	List<String> ids= new ArrayList<>();
		for(String id: Vehicleboxes.keySet()) {
			
			ids.add(id);
		}
		
		for(String id: ids) {
			boolean check =false;
			for(IVehicle v : Vehicles) {
			
			if(id.equals(v.getId())) { check=true; }
			  
			
		}
			
			 if(!check) {
					Box b= Vehicleboxes.get(id);
					roadGroup.getChildren().remove(b);
					Vehicleboxes.remove(id);
				   }
	   
        }
        }
    			
    		
    	  
    	 
    	  
    			
    			
    		}
    
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

        // --- L-H'ALL L-IHTIRAFI: ---
        // Khassna n-diiru Length (l) f l-itijah dyal Z (Width dyal Box)
        // o Width (w) f l-itijah dyal X (Length dyal Box)
        // Bach l-Box t-koun mn d-deqqa l-oula k-at-chouf l-itijah vertical
        Box Car = new Box(w, h, l); 

        double initialAngle = engine.getVehicleAngle(VehicleId);

        // D-aba sta'mly Y_AXIS (houwa l-itijah s-hih' f 3D)
        Car.setRotationAxis(Rotate.Y_AXIS); 
        Car.setRotate(initialAngle);

        // --- L-POSITION ---
        Car.setTranslateX(position.getX());
        Car.setTranslateZ(position.getY()); // Y dyal SUMO hiya Z
        Car.setTranslateY(-h/2); // Bach l-tomobil t-ji foq l-triq nichan

        Car.setOnMouseClicked(event -> {
            // Finma clika l-user 3la had l-box
            controlpanel.selectVehicle(VehicleId); 
        });

        // Darouri bach l-JavaFX t-detecti l-click 3la l-h'doud dyal l-Box
        Car.setPickOnBounds(true);
        
        Vehicleboxes.put(VehicleId, Car);
        return Car;
    }
		
    public void updateRotation(double deltaX , double deltaY){
    	
    	double newAngelX = RotateX.getAngle()-deltaY;
    	double newAngelY = RotateY.getAngle()+ deltaX;
    	RotateX.setAngle(newAngelX);
    	RotateY.setAngle(newAngelY);
    }
    		
   public void Zoom(double dz) {
	   double altposZ = camera.getTranslateZ();
	   double newposZ = altposZ+dz;
	   if(newposZ >-100) {
	   camera.setTranslateZ(-100.00);
	   }
	   
	   else if(newposZ<-2000){
		   camera.setTranslateZ(-2000.00);
	   }
	   
	   else {
		   camera.setTranslateZ(newposZ);
	   }
   }
   
  public void moveRoadsVertical(double dy) {
	  double newposY =  roadGroup.getTranslateY()+ dy;
	  roadGroup.setTranslateY(newposY);
  }
  
  public void moveHorizontale(double dx, double dz) {
	  double newPosX= roadGroup.getTranslateX()+dx;
	  double newPosZ= roadGroup.getTranslateZ()+dz;
	  roadGroup.setTranslateX(newPosX);
	  roadGroup.setTranslateZ(newPosZ);
  }
    	
    }
