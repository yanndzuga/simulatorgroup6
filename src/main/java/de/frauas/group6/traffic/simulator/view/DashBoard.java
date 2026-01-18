package de.frauas.group6.traffic.simulator.view;

import de.frauas.group6.traffic.simulator.analytics.ExportFilter;
import de.frauas.group6.traffic.simulator.analytics.ExportType;
import de.frauas.group6.traffic.simulator.analytics.IStatsCollector;
import de.frauas.group6.traffic.simulator.analytics.StatsCollector;
import de.frauas.group6.traffic.simulator.infrastructure.IEdge;
import de.frauas.group6.traffic.simulator.infrastructure.IInfrastructureManager;


import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Real-time Dashboard view for traffic analytics.
 * Displays charts for speed, density, and congestion, and provides an interface for exporting reports.
 * Refactored to include FileChooser for professional export handling.
 */
public class DashBoard extends StackPane {

    private final IStatsCollector statsCollector;
    private final IInfrastructureManager infraManager;
    
    // --- VIEWS (Containers) ---
    private ScrollPane mainScrollPane;
    private VBox mainView;            
    
    private ScrollPane exportScrollPane;
    private VBox exportView;            

    // --- CHARTS ---
    private LineChart<String, Number> speedChart;
    private XYChart.Series<String, Number> speedSeries;
    
    private BarChart<String, Number> densityChart;
    private XYChart.Series<String, Number> densitySeries;
    
    private BarChart<String, Number> travelTimeChart;
    private XYChart.Series<String, Number> travelTimeSeries;
    
    private BarChart<Number, String> congestionChart;
    private XYChart.Series<Number, String> congestionSeries;

    // --- EXPORT UI ELEMENTS ---
    private Map<ExportType, CheckBox> typeCheckBoxes = new HashMap<>();
    private ToggleGroup formatGroup;   
    private TextField txtFileName;
    
    // Updated filters (ComboBox for Route and Edge selection)
    private ComboBox<String> cbColorFilter;
    private ComboBox<String> cbRouteIdFilter;
    private ComboBox<String> cbEdgeIdFilter;
    private Spinner<Double> spinMinDensity;
    private Spinner<Double> spinMinTravelTime;
    private CheckBox cbOnlyCongested;

    /**
     * Constructor initializing the Dashboard view components.
     * @param statsCollector Service for retrieving traffic statistics.
     * @param infraManager   Service for accessing infrastructure data (edges, nodes).
     */
    public DashBoard(IStatsCollector statsCollector, IInfrastructureManager infraManager) {
        this.statsCollector = statsCollector;
        this.infraManager = infraManager;
        this.setStyle("-fx-background-color: #f8f9fa;"); 
        
        // 1. Initialize charts view
        initMainView();
        mainScrollPane = new ScrollPane(mainView);
        configureScrollPane(mainScrollPane);

        // 2. Initialize export view
        initExportView();
        exportScrollPane = new ScrollPane(exportView);
        configureScrollPane(exportScrollPane);
        
        // Initially hide export view
        exportScrollPane.setVisible(false);

        // 3. Add BOTH ScrollPanes to the StackPane
        this.getChildren().addAll(mainScrollPane, exportScrollPane);
        
        // 4. Set initial state
        showMainView();
    }
    
    // ==========================================
    // UPDATE LOOP
    // ==========================================
    public void update() {
        if (statsCollector == null || !mainScrollPane.isVisible()) return;

        // 1. Avg Network Speed History
        List<Double> history = statsCollector.getSpeedHistory();
        if (history != null && !history.isEmpty()) {
            int start = Math.max(0, history.size() - 30);
            speedSeries.getData().clear();
            for (int i = start; i < history.size(); i++) {
                speedSeries.getData().add(new XYChart.Data<>(String.valueOf(i), history.get(i)));
            }
        }

        // 2. Real-Time LIVE Congestion
        Map<String, Integer> currentCongestion;
        // Check implementation type safely
        if (statsCollector instanceof StatsCollector) {
            currentCongestion = ((StatsCollector) statsCollector).getCurrentCongestedEdgeIds();
        } else {
            currentCongestion = statsCollector.getCongestedEdgeIds(); 
        }

        congestionSeries.getData().clear();
        if (currentCongestion != null && !currentCongestion.isEmpty()) {
            currentCongestion.entrySet().stream()
                .sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue()))
                .limit(5)
                .forEach(e -> {
                    XYChart.Data<Number, String> data = new XYChart.Data<>(e.getValue(), e.getKey());
                    congestionSeries.getData().add(data);
                    if (data.getNode() != null) data.getNode().setStyle("-fx-bar-fill: #e74c3c;");
                });
        }
        
        // 3. Edge Densities
        Map<String, Double> densities = statsCollector.getEdgeDensity();
        if (densities != null) {
             densitySeries.getData().clear();
             densities.entrySet().stream().limit(15).forEach(e -> densitySeries.getData().add(new XYChart.Data<>(e.getKey(), e.getValue())));
        }
        
        // 4. Average Travel Time per Route
        Map<String, Double> travelTimes = statsCollector.getAverageTravelTime();
        if (travelTimes != null) {
            travelTimeSeries.getData().clear();
            travelTimes.entrySet().stream()
                .limit(15) // Limit to avoid clutter
                .forEach(e -> travelTimeSeries.getData().add(new XYChart.Data<>(e.getKey(), e.getValue())));
        }
    }
    
    /**
     * Configures common properties for ScrollPanes.
     */
    private void configureScrollPane(ScrollPane sp) {
        sp.setFitToWidth(true);
        sp.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        sp.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        sp.setPannable(true); 
        sp.setStyle("-fx-background: #f8f9fa; -fx-border-color: transparent; -fx-control-inner-background: #f8f9fa;");
    }

    // ==========================================
    // 1. MAIN VIEW (CHARTS)
    // ==========================================
    private void initMainView() {
        mainView = new VBox(15); // Reduced spacing for compactness
        mainView.setPadding(new Insets(15)); 
        mainView.setStyle("-fx-background-color: #f8f9fa;");
        
        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);
        Label title = new Label("Real-Time Traffic Analytics");
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 20)); // Slightly smaller title
        title.setStyle("-fx-text-fill: #2c3e50;");
        header.getChildren().add(title);

        // --- CHARTS INIT ---
        speedChart = createLineChart("Avg Network Speed", "Speed (m/s)");
        speedSeries = new XYChart.Series<>();
        speedSeries.setName("Real-time");
        speedChart.getData().add(speedSeries);
        styleChart(speedChart);

        NumberAxis xAxis = new NumberAxis(); xAxis.setLabel("Vehicles on Edge");
        CategoryAxis yAxis = new CategoryAxis(); yAxis.setLabel("Edge ID");
        congestionChart = new BarChart<>(xAxis, yAxis);
        congestionChart.setTitle("Live Congested Edges");
        congestionChart.setAnimated(false); 
        congestionChart.setLegendVisible(false); 
        congestionChart.setPrefHeight(280); // Adjusted height
        styleChart(congestionChart);
        congestionSeries = new XYChart.Series<>();
        congestionChart.getData().add(congestionSeries);

        densityChart = createVerticalBarChart("Global Density", "Density");
        densitySeries = new XYChart.Series<>();
        densityChart.getData().add(densitySeries);
        styleChart(densityChart);

        travelTimeChart = createVerticalBarChart("Route Travel Time", "Time (s)");
        travelTimeSeries = new XYChart.Series<>();
        travelTimeChart.getData().add(travelTimeSeries);
        styleChart(travelTimeChart);

        Button btnGoToExport = new Button("Export Reports ⤓");
        btnGoToExport.setStyle("-fx-background-color: #34495e; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand; -fx-font-size: 13px;");
        btnGoToExport.setPrefHeight(35);
        btnGoToExport.setMaxWidth(Double.MAX_VALUE); 
        btnGoToExport.setOnAction(e -> showExportView());

        mainView.getChildren().addAll(
            header, 
            createCard(speedChart), 
            createCard(congestionChart), 
            createCard(densityChart),        
            createCard(travelTimeChart),    
            new Separator(),                
            btnGoToExport                    
        );
    }

    // ==========================================
    // 2. EXPORT VIEW (UPDATED LAYOUT & CONTROLS)
    // ==========================================
    private void initExportView() {
        exportView = new VBox(20);
        exportView.setPadding(new Insets(30));
        exportView.setAlignment(Pos.TOP_CENTER);
        exportView.setStyle("-fx-background-color: #f8f9fa;");

        Label lblTitle = new Label("Export Configuration");
        lblTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 22));
        lblTitle.setStyle("-fx-text-fill: #2c3e50;");

        VBox card = new VBox(20); 
        card.setMaxWidth(700);
        card.setPadding(new Insets(25));
        card.setStyle("-fx-background-color: white; -fx-background-radius: 8; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.05), 5, 0, 0, 1);");

        // --- SECTION 1: REPORT TYPES ---
        Label l1 = new Label("1. Select Data Types");
        l1.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
        l1.setStyle("-fx-text-fill: #34495e;");

        GridPane typeGrid = new GridPane();
        typeGrid.setHgap(15); typeGrid.setVgap(10);
        
        int col = 0; int row = 0;
        for (ExportType type : ExportType.values()) {
            CheckBox cb = new CheckBox(formatEnumName(type.name()));
            cb.setStyle("-fx-font-size: 12px;"); // Smaller font
            cb.setSelected(true);
            typeCheckBoxes.put(type, cb);

            typeGrid.add(cb, col, row);
            col++;
            if (col > 1) { col = 0; row++; }
        }

        // --- SECTION 2: ADVANCED FILTERS ---
        Label l2 = new Label("2. Filter Data");
        l2.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
        l2.setStyle("-fx-text-fill: #34495e;");

        VBox filterBox = new VBox(12);
        
        // -- Color Filter --
        cbColorFilter = new ComboBox<>();
        cbColorFilter.getItems().addAll("All", "Red", "Green", "Yellow");
        cbColorFilter.getSelectionModel().selectFirst();
        cbColorFilter.setMaxWidth(Double.MAX_VALUE);
        cbColorFilter.setStyle("-fx-font-size: 12px;");
        
        // -- Route ID Filter (Dynamic XML Loading) --
        cbRouteIdFilter = new ComboBox<>();
        List<String> routes = infraManager != null ? infraManager.loadRouteIds("minimal.rou.xml") : new ArrayList<>();
        cbRouteIdFilter.getItems().add("All Routes");
        cbRouteIdFilter.getItems().addAll(routes);
        cbRouteIdFilter.getSelectionModel().selectFirst();
        cbRouteIdFilter.setMaxWidth(Double.MAX_VALUE);
        cbRouteIdFilter.setStyle("-fx-font-size: 12px;");

        // -- Edge ID Filter (Dynamic from Infrastructure) --
        cbEdgeIdFilter = new ComboBox<>();
        cbEdgeIdFilter.getItems().add("All Edges");
        if (infraManager != null) {
            try {
                List<IEdge> edges = infraManager.getAllEdges();
                if (edges != null && !edges.isEmpty()) {
                    List<String> edgeIds = new ArrayList<>();
                    for (IEdge edge : edges) {
                        edgeIds.add(edge.getId());
                    }
                    Collections.sort(edgeIds);
                    cbEdgeIdFilter.getItems().addAll(edgeIds);
                }
            } catch (Exception e) {
                System.err.println("Warning: Could not fetch edges from Infrastructure Manager: " + e.getMessage());
            }
        }
        cbEdgeIdFilter.getSelectionModel().selectFirst();
        cbEdgeIdFilter.setMaxWidth(Double.MAX_VALUE);
        cbEdgeIdFilter.setStyle("-fx-font-size: 12px;");

        // -- Numeric Filters --
        spinMinDensity = new Spinner<>(0.0, 1.0, 0.0, 0.1);
        spinMinDensity.setEditable(true);
        spinMinDensity.setMaxWidth(Double.MAX_VALUE);
        spinMinDensity.setStyle("-fx-font-size: 12px;");

        spinMinTravelTime = new Spinner<>(0.0, 1000.0, 0.0, 5.0);
        spinMinTravelTime.setEditable(true);
        spinMinTravelTime.setMaxWidth(Double.MAX_VALUE);
        spinMinTravelTime.setStyle("-fx-font-size: 12px;");

        cbOnlyCongested = new CheckBox("Show only congested edges");
        cbOnlyCongested.setStyle("-fx-font-size: 12px;");

        // Adding rows with labels ABOVE inputs for clarity
        filterBox.getChildren().addAll(
            createFilterRow("Vehicle Color", cbColorFilter),
            createFilterRow("Route ID (from XML)", cbRouteIdFilter),
            createFilterRow("Edge ID (from Infra)", cbEdgeIdFilter),
            createFilterRow("Min. Edge Density (0.0 - 1.0)", spinMinDensity),
            createFilterRow("Min. Travel Time (seconds)", spinMinTravelTime),
            new VBox(5, new Label("Congestion Status"), cbOnlyCongested)
        );

        // --- SECTION 3: FORMAT ---
        Label l3 = new Label("3. Output Format");
        l3.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
        l3.setStyle("-fx-text-fill: #34495e;");

        HBox formatBox = new HBox(20);
        formatGroup = new ToggleGroup();
        RadioButton rbCsv = new RadioButton("CSV");
        rbCsv.setToggleGroup(formatGroup);
        rbCsv.setSelected(true);
        rbCsv.setStyle("-fx-font-size: 12px;");
        
        RadioButton rbPdf = new RadioButton("PDF");
        rbPdf.setToggleGroup(formatGroup);
        rbPdf.setStyle("-fx-font-size: 12px;");
        
        formatBox.getChildren().addAll(rbCsv, rbPdf);

        // --- SECTION 4: FILE DETAILS (Optional Default Name) ---
        Label l4 = new Label("4. Default Filename (Optional)");
        l4.setFont(Font.font("Segoe UI", FontWeight.BOLD, 13));
        l4.setStyle("-fx-text-fill: #34495e;");
        
        VBox fileBox = new VBox(5);
        txtFileName = new TextField("traffic_report");
        txtFileName.setPromptText("Enter default filename...");
        txtFileName.setStyle("-fx-font-size: 12px;");
        fileBox.getChildren().addAll(new Label("Filename Suggestion"), txtFileName);

        // --- BUTTONS ---
        HBox actions = new HBox(15);
        actions.setAlignment(Pos.CENTER_RIGHT);
        actions.setPadding(new Insets(15, 0, 0, 0));

        Button btnCancel = new Button("Cancel");
        btnCancel.setPrefWidth(100);
        btnCancel.setStyle("-fx-font-size: 12px;");
        btnCancel.setOnAction(e -> showMainView());

        Button btnConfirm = new Button("Export...");
        btnConfirm.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand; -fx-font-size: 12px;");
        btnConfirm.setPrefWidth(120);
        btnConfirm.setOnAction(e -> handleExport());

        actions.getChildren().addAll(btnCancel, btnConfirm);

        card.getChildren().addAll(
            l1, typeGrid, new Separator(),
            l2, filterBox, new Separator(),
            l3, formatBox, new Separator(),
            l4, fileBox, new Separator(),
            actions
        );

        exportView.getChildren().addAll(lblTitle, card);
    }
    
    /**
     * Helper to create a vertical filter row with a label above the control.
     */
    private VBox createFilterRow(String labelText, Control control) {
        VBox row = new VBox(3); 
        Label label = new Label(labelText);
        label.setStyle("-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #7f8c8d; -fx-text-transform: uppercase;");
        row.getChildren().addAll(label, control);
        return row;
    }

    // ==========================================
    // NAVIGATION
    // ==========================================
    private void showExportView() {
        mainScrollPane.setVisible(false);
        exportScrollPane.setVisible(true);
    }

    private void showMainView() {
        exportScrollPane.setVisible(false);
        mainScrollPane.setVisible(true);
    }

    // ==========================================
    // EXPORT LOGIC WITH FILE CHOOSER
    // ==========================================
    private void handleExport() {
        if (statsCollector == null) return;

        // 1. GATHER SELECTED TYPES
        List<ExportType> selectedTypes = new ArrayList<>();
        for (Map.Entry<ExportType, CheckBox> entry : typeCheckBoxes.entrySet()) {
            if (entry.getValue().isSelected()) {
                selectedTypes.add(entry.getKey());
            }
        }

        if (selectedTypes.isEmpty()) {
            showAlert("Warning", "Please select at least one data type to include.");
            return;
        }

        // 2. FORMAT
        RadioButton selectedFormatBtn = (RadioButton) formatGroup.getSelectedToggle();
        boolean isCsv = selectedFormatBtn.getText().contains("CSV");

        // 3. BUILD COMPLETE FILTER
        ExportFilter filter = new ExportFilter();
        
        // Color
        if (cbColorFilter.getValue() != null && !"All".equals(cbColorFilter.getValue())) {
            filter.setVehicleColor(cbColorFilter.getValue());
        }
        
        // Route ID
        String routeVal = cbRouteIdFilter.getValue();
        if (routeVal != null && !routeVal.equals("All Routes") && !routeVal.equals("Error Loading Routes")) {
            filter.setOnlyRouteId(routeVal);
        }
        
        // Edge ID
        String edgeVal = cbEdgeIdFilter.getValue();
        if (edgeVal != null && !edgeVal.equals("All Edges")) {
            filter.setOnlyEdgeId(edgeVal);
        }
        
        // Min Density
        Double density = spinMinDensity.getValue();
        if (density != null && density > 0.0) {
            filter.setMinEdgeDensity(density);
        }
        
        // Min Travel Time
        Double travelTime = spinMinTravelTime.getValue();
        if (travelTime != null && travelTime > 0.0) {
            filter.setMinAverageTravelTime(travelTime);
        }
        
        // Congested Only
        filter.setOnlyCongestedEdges(cbOnlyCongested.isSelected());

        // 4. CHOOSE EXPORT LOCATION (FileChooser)
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Export File");
        
        // Construct a default filename suggestion
        String defaultName = txtFileName.getText().trim().isEmpty() ? "traffic_report" : txtFileName.getText().trim();
        defaultName = defaultName.replaceAll("[^a-zA-Z0-9._-]", "_");
        
        if (selectedTypes.size() == 1) {
            defaultName += "_" + selectedTypes.get(0).name().toLowerCase(); 
        } else {
            defaultName += "_full_report";
        }
        
        // Set extension filters and initial name
        if (isCsv) {
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files (*.csv)", "*.csv"));
            fileChooser.setInitialFileName(defaultName + ".csv");
        } else {
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Files (*.pdf)", "*.pdf"));
            fileChooser.setInitialFileName(defaultName + ".pdf");
        }
        
        // Get the current stage window to show the dialog
        Stage stage = (Stage) this.getScene().getWindow();
        File file = fileChooser.showSaveDialog(stage);

        if (file != null) {
            try {
                String path = file.getAbsolutePath();
                
                if (isCsv) statsCollector.exportToCsv(path, filter, selectedTypes);
                else statsCollector.exportToPdf(path, filter, selectedTypes);
                
                showAlert("Success", "Report exported successfully to:\n" + path);
                showMainView();
                
            } catch (Exception ex) {
                showAlert("Error", "Export failed: " + ex.getMessage());
                ex.printStackTrace();
            }
        }
    }

    // --- Helpers ---
    private VBox createCard(Chart chart) {
        VBox card = new VBox(chart);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 5; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.05), 5, 0, 0, 1); -fx-padding: 8;");
        return card;
    }
    
    private void styleChart(XYChart<?, ?> chart) {
        // Transparent background for chart plot area
        if (chart.lookup(".chart-plot-background") != null) {
            chart.lookup(".chart-plot-background").setStyle("-fx-background-color: transparent;");
        }
        chart.getXAxis().setTickLabelFont(Font.font("Arial", 10));
        chart.getYAxis().setTickLabelFont(Font.font("Arial", 10));
    }
    
    private void showAlert(String title, String content) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle(title); a.setHeaderText(null); a.setContentText(content);
        a.showAndWait();
    }
    
    private String formatEnumName(String name) {
        if (name == null || name.isEmpty()) return "";
        return name.charAt(0) + name.substring(1).toLowerCase().replace('_', ' ');
    }
    
    private LineChart<String, Number> createLineChart(String t, String y) {
        CategoryAxis x = new CategoryAxis(); NumberAxis ya = new NumberAxis(); ya.setLabel(y);
        LineChart<String,Number> lc = new LineChart<>(x, ya);
        lc.setTitle(t); lc.setAnimated(false); lc.setLegendVisible(false); lc.setPrefHeight(250);
        return lc;
    }
    
    private BarChart<String, Number> createVerticalBarChart(String t, String y) {
        CategoryAxis x = new CategoryAxis(); NumberAxis ya = new NumberAxis(); ya.setLabel(y);
        BarChart<String,Number> bc = new BarChart<>(x, ya);
        bc.setTitle(t); bc.setAnimated(false); bc.setLegendVisible(false); bc.setPrefHeight(250);
        return bc;
    }
}