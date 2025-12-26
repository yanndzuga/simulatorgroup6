package de.frauas.group6.traffic.simulator.vehicles;

import java.awt.geom.Point2D;

public class Vehicle implements IVehicle {
	//Atributte
			private String id;
			private String TypeId;
			private double speed;
		    private String Farbe;
			private Point2D Position;
			private String edgeid;
			private byte Lane;
			private String routeId;
			
			//Methoden
			public 	Vehicle() { id=""; speed=0.0;Farbe="Black"; Position=new Point2D.Double(0.0,0.0); edgeid=""; Lane=0; TypeId="DEFAULT_VEHTYPE"; routeId = null;}
			
			public Vehicle(String id,String TypeId,double speed, String Farbe,String edgeid,byte Lane, String routeId) { this.id=id; this.TypeId=TypeId; this.speed=speed; this.Farbe=Farbe; this.edgeid=edgeid; this.Lane=Lane; this.routeId = routeId;}
			
			public Vehicle(String id,String TypeId,double speed,String color,double x,double y,String edgeid,byte Lane) { this.id=id; this.TypeId=TypeId; this.speed=speed; this.Farbe=color; this.Position= new Point2D.Double(x,y);this.edgeid=edgeid;this.Lane=Lane; }
			//Getter
			public double getSpeed() { return speed;}
			public String getId() { return id; }
			public String getColor() { return Farbe; }
			public Point2D getPosition() { return Position; }
			public String getEdgeId() { return edgeid; }
			public int getEdgeLane() { return Lane; }
			public String getTypeId() { return TypeId; }
			public String getRouteId() { return routeId; }

			public void setRouteId(String routeId) { this.routeId = routeId; }

			//Setter
			public void setColor(String newColor) { Farbe=newColor; }
			public void setSpeed(double newSpeed) { speed=newSpeed; }
			public void setPosition(Point2D newpos) { Position=newpos; }
			public void setEdgeId(String newEdgeId) { edgeid=newEdgeId; }
			public void setEdgeLane(byte newEdgeLane) { Lane=newEdgeLane; }

			
		
	}
	
	
	


