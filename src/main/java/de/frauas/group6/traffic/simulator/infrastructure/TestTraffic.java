 /*package de.frauas.group6.traffic.simulator.infrastructure;

import it.polito.appeal.traci.SumoTraciConnection;
import java.io.File;
import java.util.List;

public class TestTraffic {

    
    private static final String SUMO_BIN = "sumo-gui"; 

    public static void main(String[] args) {
        System.out.println("🚀 Bdayt Test Global (Traffic Lights + Edges)...");

       
        String projectPath = System.getProperty("user.dir");
        String configPath = projectPath + "/sumo_data/meine_sim.sumocfg"; 

        
        File file = new File(configPath);
        if (!file.exists()) {
            System.err.println("❌ ERROR: Ma lqitch fichier config!");
            System.err.println("👉 Path: " + configPath);
            return;
        }

        // -----------------------------------------------------------
        // 2. CONNEXION WITH SUMO
        // -----------------------------------------------------------
        SumoTraciConnection conn = new SumoTraciConnection(SUMO_BIN, configPath);

        try {
           
            conn.addOption("start", "1");
            
            conn.runServer();
            conn.do_timestep(); // Step 1 darori
            
            System.out.println("✅ SUMO Connecté Mzyan!");

            // -----------------------------------------------------------
            // 3. INITIALISATION DYAL MANAGERS
            // -----------------------------------------------------------
            TrafficLightManager tlManager = new TrafficLightManager(conn);
            Edgemanager edgemanager = new Edgemanager(conn);

            // Test Rapide: Afficher IDs
            System.out.println("🚦 Traffic Lights IDs: " + tlManager.getJunctionIds());
            System.out.println("🛣️  Total Edges found: " + edgemanager.getEdgeIds().size());

            // -----------------------------------------------------------
            // 4. SIMULATION LOOP (SCENARIO GUI)
            // -----------------------------------------------------------
            System.out.println("\n⏳ Bdayt Simulation Loop (300 Steps)...");

            for(int i = 0; i < 300; i++) {
                // Zid l-waqt f SUMO
                conn.do_timestep();

                // --- 🔴 SCENARIO: USER CLICKED "FORCE GREEN" ---
                
                // Mtal: F Step 50, l-User khtar J57 w bgha y-rdou KHḌER
                if(i == 50) {
                    System.out.println("\n[Step 50] 🚨 ACTION: User Forced J57 to GREEN");
                    
                    // Phase 0 = Green (Main) & Red (Side) -> Sécurité madmouna mn XML
                    // Duration = 15 secondes (15000ms)
                    tlManager.switchToPhase("J57", 0, 15); 
                }

                // --- 🔄 SCENARIO: USER CLICKED "BACK TO AUTO" ---
                
                // Mtal: F Step 150, l-User bgha y-rje3 l-System Auto
                if(i == 150) {
                    System.out.println("\n[Step 150] 🔄 ACTION: User Clicked 'Automatic Mode'");
                    tlManager.setAutomaticControl(true);
                }

                // --- 🔍 MONITORING (Mouraqaba) ---
                
                // Kolla 10 steps: Chouf l-7ala dyal J57
                if(i % 10 == 0) {
                    int phase = tlManager.getPhase("J57");
                    
                    // N-sta3mlo l-helper method li zdna bach n-fhomo l-wan
                    String colorStatus = tlManager.isGreen("J57", phase) ? "🟢 GREEN" : "🔴 RED/YELLOW";
                    
                    // N-choufo ta Triq -E48 wach fiha tomobilat
                    int cars = edgemanager.getVehicleCount("-E48");
                    
                    System.out.println("   Step " + i + " | J57: " + colorStatus + " (Phase " + phase + ") | Cars on -E48: " + cars);
                }

                // Zid chwia d-lwaqt bach t-chouf b 3inik (50ms)
                Thread.sleep(50);
            }
            
            // -----------------------------------------------------------
            // 5. FIN
            // -----------------------------------------------------------
            conn.close();
            System.out.println("\n🛑 Test Sala B-naja7.");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}  */