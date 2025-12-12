package de.frauas.group6.traffic.simulator.infrastructure;

import it.polito.appeal.traci.SumoTraciConnection;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;

public class TrafficDashboard extends JFrame {

    // --- CONFIGURATION ---
    private static final String SUMO_BIN = "sumo-gui";
    private static SumoTraciConnection conn;
    
    // Managers
    private static TrafficLightManager tlManager;
    private static Edgemanager edgeManager; 
    
    private static boolean isRunning = true;

    // --- UI COMPONENTS ---
    private JComboBox<String> edgeSelector; 
    private JLabel carCountDisplay; 
    
    private JComboBox<String> junctionSelector;
    private JLabel statusLabel;
    private JLabel timerLabel;
    private JSpinner durationSpinner; 
    
    private JButton btnForceGreen;
    private JButton btnForceRed;
    private JButton btnAuto;
    private JTextArea logArea;

    // --- COLORS ---
    private final Color PANEL_BG = new Color(52, 73, 94);
    private final Color ACCENT_GREEN = new Color(46, 204, 113);
    private final Color ACCENT_RED = new Color(231, 76, 60);
    private final Color ACCENT_BLUE = new Color(52, 152, 219);
    private final Color TIMER_COLOR = new Color(241, 196, 15); 

    public TrafficDashboard() {
        // Window Setup
        setTitle("SUMO Traffic Control System");
        setSize(980, 650);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        
        // Custom Background
        JPanel mainPanel = new GradientPanel();
        mainPanel.setLayout(new BorderLayout(15, 15));
        mainPanel.setBorder(new EmptyBorder(15, 15, 15, 15));
        setContentPane(mainPanel);

        // Header
        JLabel titleLabel = new JLabel("TRAFFIC MANAGEMENT DASHBOARD", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 26));
        titleLabel.setForeground(Color.WHITE);
        mainPanel.add(titleLabel, BorderLayout.NORTH);

        // Layout Split
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setOpaque(false);
        splitPane.setBorder(null);
        splitPane.setDividerLocation(480); 
        splitPane.setDividerSize(0);

        // ============================
        // LEFT PANEL: ROAD MONITOR
        // ============================
        JPanel edgesPanel = createStyledPanel("Road Network Monitor");
        edgesPanel.setLayout(new GridBagLayout());
        GridBagConstraints gbcL = new GridBagConstraints();
        gbcL.insets = new Insets(10, 10, 10, 10);
        gbcL.fill = GridBagConstraints.HORIZONTAL;

        // Label
        gbcL.gridx = 0; gbcL.gridy = 0;
        JLabel lblEdge = new JLabel("Select Road Segment:");
        lblEdge.setForeground(Color.WHITE);
        lblEdge.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        edgesPanel.add(lblEdge, gbcL);

        // Dropdown
        edgeSelector = new JComboBox<>();
        edgeSelector.setFont(new Font("Consolas", Font.BOLD, 14));
        gbcL.gridy = 1;
        edgesPanel.add(edgeSelector, gbcL);

        // Car Count Display
        gbcL.gridy = 2; gbcL.weighty = 1.0;
        carCountDisplay = new JLabel("0", SwingConstants.CENTER);
        carCountDisplay.setFont(new Font("Segoe UI", Font.BOLD, 100));
        carCountDisplay.setForeground(Color.CYAN);
        edgesPanel.add(carCountDisplay, gbcL);
        
        // Subtitle
        gbcL.gridy = 3; gbcL.weighty = 0;
        JLabel lblSub = new JLabel("Vehicles Detected", SwingConstants.CENTER);
        lblSub.setForeground(Color.LIGHT_GRAY);
        edgesPanel.add(lblSub, gbcL);

        splitPane.setLeftComponent(edgesPanel);

        // ============================
        // RIGHT PANEL: TRAFFIC CONTROLS
        // ============================
        JPanel controlsPanel = createStyledPanel("Junction Control Center");
        controlsPanel.setLayout(new GridBagLayout());
        GridBagConstraints gbcR = new GridBagConstraints();
        gbcR.insets = new Insets(8, 10, 8, 10);
        gbcR.fill = GridBagConstraints.HORIZONTAL;

        // Label
        gbcR.gridx = 0; gbcR.gridy = 0;
        JLabel lblJunc = new JLabel("Select Junction:");
        lblJunc.setForeground(Color.WHITE);
        controlsPanel.add(lblJunc, gbcR);

        // Dropdown
        junctionSelector = new JComboBox<>();
        junctionSelector.setFont(new Font("Consolas", Font.BOLD, 14));
        gbcR.gridy = 1;
        controlsPanel.add(junctionSelector, gbcR);

        // Status
        statusLabel = new JLabel("WAITING", SwingConstants.CENTER);
        statusLabel.setOpaque(true);
        statusLabel.setBackground(Color.GRAY);
        statusLabel.setForeground(Color.WHITE);
        statusLabel.setFont(new Font("Segoe UI", Font.BOLD, 22));
        statusLabel.setPreferredSize(new Dimension(100, 50));
        gbcR.gridy = 2;
        controlsPanel.add(statusLabel, gbcR);

        // Timer
        timerLabel = new JLabel("Next Change: -- s", SwingConstants.CENTER);
        timerLabel.setFont(new Font("Consolas", Font.BOLD, 26));
        timerLabel.setForeground(TIMER_COLOR);
        gbcR.gridy = 3;
        controlsPanel.add(timerLabel, gbcR);

        // Duration Spinner
        JPanel durationPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        durationPanel.setOpaque(false);
        JLabel lblDur = new JLabel("Duration (sec): ");
        lblDur.setForeground(Color.WHITE);
        
        SpinnerNumberModel model = new SpinnerNumberModel(30, 5, 300, 5); 
        durationSpinner = new JSpinner(model);
        durationSpinner.setFont(new Font("Consolas", Font.BOLD, 18));
        durationSpinner.setPreferredSize(new Dimension(80, 35));
        
        durationPanel.add(lblDur);
        durationPanel.add(durationSpinner);
        gbcR.gridy = 4;
        controlsPanel.add(durationPanel, gbcR);

        // Buttons
        JPanel btnPanel = new JPanel(new GridLayout(1, 3, 10, 0));
        btnPanel.setOpaque(false);
        btnForceGreen = createStyledButton("FORCE GREEN", ACCENT_GREEN);
        btnForceRed   = createStyledButton("FORCE RED", ACCENT_RED);
        btnAuto       = createStyledButton("AUTO MODE", ACCENT_BLUE);
        btnPanel.add(btnForceGreen);
        btnPanel.add(btnForceRed);
        btnPanel.add(btnAuto);
        gbcR.gridy = 5;
        controlsPanel.add(btnPanel, gbcR);

        // Logs
        logArea = new JTextArea(5, 20);
        logArea.setBackground(new Color(30, 30, 30));
        logArea.setForeground(Color.GREEN);
        logArea.setFont(new Font("Consolas", Font.PLAIN, 12));
        logArea.setEditable(false);
        gbcR.gridy = 6; gbcR.weighty = 1.0; gbcR.fill = GridBagConstraints.BOTH;
        controlsPanel.add(new JScrollPane(logArea), gbcR);

        splitPane.setRightComponent(controlsPanel);
        mainPanel.add(splitPane, BorderLayout.CENTER);

        setupListeners();
        setVisible(true);
    }

    private void setupListeners() {
        btnForceGreen.addActionListener(e -> {
            String id = (String) junctionSelector.getSelectedItem();
            int duration = (Integer) durationSpinner.getValue(); 
            if (id != null) {
                tlManager.switchToPhase(id, 0, duration); // Phase 0 = Green
                log("ACTION: Force GREEN -> " + id + " (" + duration + "s)");
            }
        });

        btnForceRed.addActionListener(e -> {
            String id = (String) junctionSelector.getSelectedItem();
            int duration = (Integer) durationSpinner.getValue(); 
            if (id != null) {
                tlManager.switchToPhase(id, 2, duration); // Phase 2 = Red
                log("ACTION: Force RED -> " + id + " (" + duration + "s)");
            }
        });

        btnAuto.addActionListener(e -> {
            tlManager.setAutomaticControl(true);
            log("ACTION: Automatic Mode Restored");
        });
    }

    // --- MAIN EXECUTION LOOP ---
    public static void main(String[] args) {
        String projectPath = System.getProperty("user.dir");
        String configPath = projectPath + "/sumo_data/meine_sim.sumocfg";

        TrafficDashboard dashboard = new TrafficDashboard();

        new Thread(() -> {
            try {
                conn = new SumoTraciConnection(SUMO_BIN, configPath);
                conn.addOption("start", "1");
                conn.runServer();
                conn.do_timestep();
                
                tlManager = new TrafficLightManager(conn);
                edgeManager = new Edgemanager(conn);
                
                dashboard.log("System: Connected to SUMO.");

                // Populate UI Dropdowns
                SwingUtilities.invokeLater(() -> {
                    for (String id : tlManager.getJunctionIds()) dashboard.junctionSelector.addItem(id);
                    for (String id : edgeManager.getEdgeIds()) dashboard.edgeSelector.addItem(id);
                });

                // Simulation Loop
                while (isRunning) {
                    conn.do_timestep();
                    
                    // CRITICAL: Check for expired timers every step
                    tlManager.updateTrafficLights(); 

                    String selectedTL = (String) dashboard.junctionSelector.getSelectedItem();
                    String selectedEdge = (String) dashboard.edgeSelector.getSelectedItem();

                    SwingUtilities.invokeLater(() -> {
                        // Update Traffic Lights & Timer
                        if (selectedTL != null) {
                            int phase = tlManager.getPhase(selectedTL);
                            boolean isGreen = tlManager.isGreen(selectedTL, phase);
                            long timeLeft = tlManager.getTimeUntilSwitch(selectedTL);

                            if (isGreen) {
                                dashboard.statusLabel.setText("GREEN");
                                dashboard.statusLabel.setBackground(dashboard.ACCENT_GREEN);
                            } else {
                                dashboard.statusLabel.setText("RED");
                                dashboard.statusLabel.setBackground(dashboard.ACCENT_RED);
                            }
                            dashboard.timerLabel.setText("Switch in: " + timeLeft + "s");
                        }

                        // Update Vehicle Count
                        if (selectedEdge != null) {
                            int count = edgeManager.getVehicleCount(selectedEdge);
                            dashboard.carCountDisplay.setText(String.valueOf(count));
                            
                            if(count == 0) dashboard.carCountDisplay.setForeground(Color.GRAY);
                            else if(count < 5) dashboard.carCountDisplay.setForeground(Color.CYAN);
                            else dashboard.carCountDisplay.setForeground(Color.ORANGE);
                        }
                    });

                    Thread.sleep(100); // Simulation speed
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    // --- UI HELPERS ---
    private JPanel createStyledPanel(String title) {
        JPanel p = new JPanel();
        p.setBackground(PANEL_BG);
        TitledBorder border = BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(Color.LIGHT_GRAY), title);
        border.setTitleColor(Color.WHITE);
        border.setTitleFont(new Font("Segoe UI", Font.BOLD, 14));
        p.setBorder(border);
        return p;
    }

    private JButton createStyledButton(String text, Color bg) {
        JButton btn = new JButton(text);
        btn.setBackground(bg);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return btn;
    }

    private void log(String msg) {
        logArea.append(msg + "\n");
        logArea.setCaretPosition(logArea.getDocument().getLength());
    }

    class GradientPanel extends JPanel {
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            GradientPaint gp = new GradientPaint(0, 0, new Color(44, 62, 80), 0, getHeight(), Color.BLACK);
            g2d.setPaint(gp);
            g2d.fillRect(0, 0, getWidth(), getHeight());
        }
    }
}