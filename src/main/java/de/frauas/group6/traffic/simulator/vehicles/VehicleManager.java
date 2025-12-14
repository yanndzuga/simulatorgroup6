package de.frauas.group6.traffic.simulator.vehicles;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import de.frauas.group6.traffic.simulator.core.ISimulationEngine;

//import de.frauas.group6.traffic.simulator.core.ISimulationEngine;
//import de.frauas.group6.traffic.simulator.vehicles.Vehicle;

public class VehicleManager implements IVehicleManager{
	
	private ISimulationEngine SumolationEngine ;//SumolationEngine object
	private 	Map<String,IVehicle> Vehicles;// Map ist ein interface und ConcurrentHashMap implementiert dieses interface 
	static Long counter;//Anzahl von Autos bis jetzt 
	
	//Konstruktor mit Parammeter
	public VehicleManager(ISimulationEngine SumolationEngine){ this. SumolationEngine= SumolationEngine;Vehicles = new ConcurrentHashMap<String,IVehicle>(); counter=(long)0; System.out.println("ACHTUNG: Ein neuer VehicleManager wurde erstellt! " + this); }
	
	
	
	
	
	//inject ein AUto oder mehr
   
	public void injectVehicle(String edgeId,String lane,String VehicleType, String color,int number, double speed) {
		
		new Thread(new Runnable() {
			@Override
			public void run() {
			
		String vehicleId;
		String TypeId="";
		List<String> successfullyAddedIds = new ArrayList<>();
		byte edgeLane=0;
		int r=0;
		int g=0;
		int b=0;
		
		switch(color) {
	     
	     case "Yellow": {r=255; g=255; b=0; }
	     break;
	     case "Rot": {r=255; g=0; b=0; }
	     break;
	     case "Green":{r=0; g=222; b=0; }
	     break;
		}
     if(edgeId.equals("E45")){
	    if(lane.equals("middle")){ edgeLane=1; }//in the middle
        else  if(lane.equals("Left")) { edgeLane=2; }//Right bleibt  0
	            }
	else{  if(lane.equals("Left")) { edgeLane=1; } } //Right bleibt  0
         
         
 switch(VehicleType) {
         
         case "Standard-Car": {TypeId="DEFAULT_VEHTYPE" ; }
         break;
         case "Truck": {TypeId="DEFAULT_CONTAINERTYPE" ;}
         break;
         case "Emergency-Vehicle":{TypeId="RESCUE_TYPE"; }
         break;
         case "City-Bus": { TypeId="BUS_TYPE";}     
         }
 String routeid=" ";
 switch(edgeId){

 case "-E48": {if(edgeLane==0){routeid="R0";} else if(edgeLane==1) {routeid="R1";}  }
 break;   
 case "-E46": {if(edgeLane==0){routeid="R2";} else if(edgeLane==1) {routeid="R3";}  }
 break;   
 case "-E51": {if(edgeLane==0){routeid="R5";} else if(edgeLane==1) {routeid="R6";}  }
 break;   
 case "E50": {if(edgeLane==0){routeid="R7";} else if(edgeLane==1) {routeid="R8";}  }
 break;   
 case "45": {if(edgeLane==0){routeid="R9";} else if(edgeLane==1) {routeid="R10";}  else{routeid="R11";}}  
 break;   

 case "49": {if(edgeLane==0){routeid="R13";} else if(edgeLane==1) {routeid="R12";} }  
 break;  
 }
 
 
 try {
 synchronized(this) {
		for(int i=0;i<number;i++) {
		counter++;
		vehicleId="VEH_"+counter;
		Vehicle newvehicle= new Vehicle( vehicleId,TypeId,speed,color,edgeId,edgeLane);
		System.out.println("DEBUG: Auto " + vehicleId + " in Map gelegt. Map-Größe: " + Vehicles.size());
		SumolationEngine.spawnVehicle(vehicleId, routeid,edgeLane,TypeId, r, g, b, speed);
		Vehicles.put(vehicleId, newvehicle);
		System.out.println("DEBUG: Auto " + vehicleId + " an SUMO gesendet.");
		successfullyAddedIds.add(vehicleId);
		Thread.sleep(20);
		}
 }
 }
		catch(Exception e) {    
		//wenn ein problem bei inject in sumo passiert löschen wir die autos von unserer Liste
 System.err.println("CRITICAL TRAFFIC INJECTION FAILURE: " + e.getMessage());
 

 for (String id : successfullyAddedIds) {
     Vehicles.remove(id);}
		}
			} }
			 ).start();
		                                                                                                                }
	
	//modify ein Auto 

	 public   void modifyVehicle(String vehicleId, String newcolor, double newspeed ) throws Exception {
		int r=0;
		int g=0;
		int b=0;
		
		IVehicle myvehicle= Vehicles.get(vehicleId);
		
		if (myvehicle == null) {
	       
	        throw new IllegalArgumentException("ERROR: Fahrzeug ID '" + vehicleId + 
	                                           "' nicht im Manager gefunden. Modifizierung abgebrochen.");
	    }
		
		myvehicle.setColor(newcolor);
		myvehicle.setSpeed(newspeed);
		switch(newcolor) {
	     
	     case "Yellow": {r=255; g=255; b=0; }
	     break;
	     case "Rot": {r=255; g=0; b=0; }
	     break;
	     case "Green":{r=0; g=255; b=0; }
	     break;
		}
		
		SumolationEngine. setVehicleColor(vehicleId, r, g, b);
		SumolationEngine.setVehicleSpeed(vehicleId,newspeed);
		
		
		
	                                                                                                     }
	
	
//remove ein oder mehrere Autos auf einem bestimmten Edge

     public void deleteVehicle(String requestedEdgeId, String  requestedColor, double requestedSpeed,int requestnumber) {
		
	  int count=0;
	  List<String> validVehicleIds = new ArrayList<>();
	  final double SPEED_TOLERANCE = 0.001; // Toleranz für double-Vergleich 
       for (IVehicle vehicle : Vehicles.values()) {
        
    	   boolean speedMatches = Math.abs(vehicle.getSpeed() - requestedSpeed) < SPEED_TOLERANCE;
        
        if (vehicle.getEdgeId().equals(requestedEdgeId) &&
            vehicle.getColor().equals(requestedColor) &&
            speedMatches)
        {
            count++;
            validVehicleIds.add(vehicle.getId());
            
        }
        
        if(requestnumber==count) { break; }
        
        }
       //wenn wir die anforderte Anzahl auf dieser Strasse gefunden habe 
       if ( validVehicleIds.size() < requestnumber) {
           
           throw new IllegalArgumentException("ERROR: Nur " + validVehicleIds .size() + 
                                              " Fahrzeuge gefunden, aber " + requestnumber + 
                                              " benötigt. Keine Fahrzeuge gelöscht."); }
	
		for (String id : validVehicleIds) {
	        try {
			SumolationEngine.removeVehicle(id);
	        Vehicles.remove(id); }
	        catch(Exception e) {
	        	
	        	throw new RuntimeException("FEHLER beim Löschen von Fahrzeug " + id + ". Prozess gestoppt.", e);
	        }
	        
	      
	    }
		
	                                                                                                               }
	
//nach jedem step müssen wir position erneun
	public void updateVehicles() {
		 //Vehicler myvehicle=Vehicles.get(VehicleId);
		 List<String> activeIds=SumolationEngine.getVehicleIdList();
		 this.Vehicles.keySet().retainAll(activeIds);
		
		
	   Point2D newPos;
	   String newEdgeId;
	   byte newLane;
	   for(String v:activeIds) {
		  
	   newPos=SumolationEngine.getVehiclePosition(v);
	   newEdgeId=SumolationEngine.getVehicleRoadId(v);
	   newLane=SumolationEngine.getVehicleLaneIndex(v);
	   
	   IVehicle myvehicle=Vehicles.get(v);
	   if (myvehicle == null) {
	        
	        /*Error 
	        throw new RuntimeException("CRITICAL ERROR: SumolationEngine tried to update " + 
	                                   "non-existent Vehicle ID: " + v + 
	                                   ". Check Sim-Controller logic or deletion process."); */
		   System.out.println("Sync Info: Données reçues pour " + v + " avant sa création Java. Ignoré pour ce step.");
          
	    }
		myvehicle.setPosition(newPos);
		myvehicle.setEdgeId(newEdgeId);
		myvehicle.setEdgeLane(newLane);
	   }
		
	                                                                                                     }
	 
	
	
	//gibt alle exstierenden Autos auf dem strasse
	public Collection<IVehicle> getAllVehicles() {
	  
	    return this.Vehicles.values(); 
	                                             }



}
