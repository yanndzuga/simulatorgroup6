package de.frauas.group6.traffic.simulator.vehicles;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import de.frauas.group6.traffic.simulator.core.ISimulationEngine;

public class VehicleManager implements IVehicleManager {

    private ISimulationEngine SumolationEngine;
    private Map<String, IVehicle> Vehicles;
    private Map<String, Long> creationTimes; 
    static Long counter; 

    public VehicleManager(ISimulationEngine SumolationEngine) {
        this.SumolationEngine = SumolationEngine;
        this.Vehicles = new ConcurrentHashMap<>();
        this.creationTimes = new ConcurrentHashMap<>();
        counter = (long) 0;
        System.out.println("ACHTUNG: Ein neuer VehicleManager wurde erstellt! " + this);
    }

    public void injectVehicle(String edgeId, String lane, String VehicleType, String color, int number, double speed) {
        new Thread(() -> {
            String vehicleId;
            String TypeId = "";
            List<String> successfullyAddedIds = new ArrayList<>();
            byte edgeLane = 0;
            int r = 0, g = 0, b = 0;

            switch (color) {
                case "Yellow": r = 255; g = 255; b = 0; break;
                case "Rot":    r = 255; g = 0;   b = 0; break;
                case "Green":  r = 0;   g = 222; b = 0; break;
            }

            if (edgeId.equals("E45")) {
                if (lane.equals("middle")) { edgeLane = 1; }
                else if (lane.equals("Left")) { edgeLane = 2; }
            } else {
                if (lane.equals("Left")) { edgeLane = 1; }
            }

            switch (VehicleType) {
                case "Standard-Car": TypeId = "DEFAULT_VEHTYPE"; break;
                case "Truck":        TypeId = "DEFAULT_CONTAINERTYPE"; break;
                case "Emergency-Vehicle": TypeId = "RESCUE_TYPE"; break;
                case "City-Bus":     TypeId = "BUS_TYPE"; break;
            }

            String routeid = " ";
            switch (edgeId) {
                case "-E48": routeid = (edgeLane == 0) ? "R0" : "R1"; break;
                case "E46":  routeid = (edgeLane == 0) ? "R2" : "R3"; break;
                case "-E51": routeid = (edgeLane == 0) ? "R5" : "R6"; break;
                case "E50":  routeid = (edgeLane == 0) ? "R7" : "R8"; break;
                case "E45":  routeid = (edgeLane == 0) ? "R9" : (edgeLane == 1 ? "R10" : "R11"); break;
                case "E49":  routeid = (edgeLane == 0) ? "R13" : "R12"; break;
            }

            try {
                synchronized (Vehicles) {
                    for (int i = 0; i < number; i++) {
                        counter++;
                        vehicleId = "VEH_" + counter;
                        Vehicle newvehicle = new Vehicle(vehicleId, TypeId, speed, color, edgeId, edgeLane);

                        // Protection étendue : 10 secondes pour laisser SUMO insérer toute la file
                        creationTimes.put(vehicleId, System.currentTimeMillis());
                        Vehicles.put(vehicleId, newvehicle);
                        
                        System.out.println("DEBUG: Auto " + vehicleId + " ajouté map (taille: " + Vehicles.size() + ")");
                        
                        // Paramètre clé : departPos="base" ou laisser vide pour insertion intelligente
                        SumolationEngine.spawnVehicle(vehicleId, routeid, edgeLane, TypeId, r, g, b, speed);
                        
                        successfullyAddedIds.add(vehicleId);
                        
                        // Petit délai pour espacer les requêtes TraCI
                        Thread.sleep(50); 
                    }
                }
            } catch (Exception e) {
                System.err.println("CRITICAL TRAFFIC INJECTION FAILURE: " + e.getMessage());
                for (String id : successfullyAddedIds) {
                    Vehicles.remove(id);
                    creationTimes.remove(id);
                }
            }
        }).start();
    }

    public void modifyVehicle(String vehicleId, String newcolor, double newspeed) throws Exception {
        IVehicle myvehicle = Vehicles.get(vehicleId);

        if (myvehicle == null) {
            throw new IllegalArgumentException("ERROR: Fahrzeug ID '" + vehicleId + 
                                               "' nicht im Manager gefunden. Modifizierung abgebrochen.");
        }

        myvehicle.setColor(newcolor);
        myvehicle.setSpeed(newspeed);
        int r = 0, g = 0, b = 0;
        switch (newcolor) {
            case "Yellow": r = 255; g = 255; b = 0; break;
            case "Rot":    r = 255; g = 0;   b = 0; break;
            case "Green":  r = 0;   g = 222; b = 0; break;
        }

        SumolationEngine.setVehicleColor(vehicleId, r, g, b);
        SumolationEngine.setVehicleSpeed(vehicleId, newspeed);
    }

    public void deleteVehicle(String requestedEdgeId, String requestedColor, double requestedSpeed, int requestnumber) {
        int count = 0;
        List<String> validVehicleIds = new ArrayList<>();
        final double SPEED_TOLERANCE = 0.001;

        for (IVehicle vehicle : Vehicles.values()) {
            boolean speedMatches = Math.abs(vehicle.getSpeed() - requestedSpeed) < SPEED_TOLERANCE;

            if (vehicle.getEdgeId().equals(requestedEdgeId) &&
                vehicle.getColor().equals(requestedColor) &&
                speedMatches) {
                count++;
                validVehicleIds.add(vehicle.getId());
            }

            if (requestnumber == count) { break; }
        }

        if (validVehicleIds.size() < requestnumber) {
            throw new IllegalArgumentException("ERROR: Nur " + validVehicleIds.size() + 
                                               " Fahrzeuge gefunden, aber " + requestnumber + 
                                               " benötigt. Keine Fahrzeuge gelöscht.");
        }

        for (String id : validVehicleIds) {
            try {
                SumolationEngine.removeVehicle(id);
                Vehicles.remove(id);
                creationTimes.remove(id); 
            } catch (Exception e) {
                throw new RuntimeException("FEHLER beim Löschen von Fahrzeug " + id + ". Prozess gestoppt.", e);
            }
        }
    }

    public void updateVehicles() {
        List<String> activeSumoIds = SumolationEngine.getVehicleIdList();
        Set<String> activeSet = new HashSet<>(activeSumoIds);
        long now = System.currentTimeMillis();

        Vehicles.entrySet().removeIf(entry -> {
            String id = entry.getKey();
            IVehicle vehicle = entry.getValue();

            // CAS A : Le véhicule est actif dans SUMO
            if (activeSet.contains(id)) {
                try {
                    Point2D newPos = SumolationEngine.getVehiclePosition(id);
                    String newEdgeId = SumolationEngine.getVehicleRoadId(id);
                    int newLane = SumolationEngine.getVehicleLaneIndex(id);

                    vehicle.setPosition(newPos);
                    vehicle.setEdgeId(newEdgeId);
                    vehicle.setEdgeLane((byte) newLane);

                    // Si le véhicule est actif, on met à jour son "ping" ou on le retire du délai de grâce
                    // Mais attention : si on le retire de creationTimes trop tôt et qu'il disparaît temporairement
                    // (ex: changement de voie ou bug SUMO d'une frame), il sera supprimé.
                    // On laisse creationTimes gérer le "démarrage", une fois démarré, c'est activeSet qui fait foi.
                    creationTimes.remove(id);

                } catch (Exception e) {
                    // Ignorer les erreurs de lecture ponctuelles
                }
                return false; // ON GARDE
            }
            // CAS B : Le véhicule n'est PAS (ou PLUS) dans SUMO
            else {
                Long createdAt = creationTimes.get(id);

                // Augmentation du délai de grâce à 10s (10000ms)
                // Cela permet à SUMO d'insérer les véhicules un par un s'il y a un embouteillage à l'insertion
                if (createdAt != null && (now - createdAt) < 10000) {
                    return false; // ON GARDE (En attente d'insertion SUMO)
                } else {
                    // Vraiment parti (fini ou supprimé par SUMO car trop d'attente)
                    // System.out.println("Cleaning up: " + id);
                    return true; // ON SUPPRIME
                }
            }
        });

        creationTimes.keySet().retainAll(Vehicles.keySet());
    }

    public Collection<IVehicle> getAllVehicles() {
        return this.Vehicles.values();
    }
}