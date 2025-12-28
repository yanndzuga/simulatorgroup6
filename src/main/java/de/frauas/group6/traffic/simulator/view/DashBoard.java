package de.frauas.group6.traffic.simulator.view;

import de.frauas.group6.traffic.simulator.analytics.ExportFilter;
import de.frauas.group6.traffic.simulator.analytics.ExportType;
import de.frauas.group6.traffic.simulator.analytics.IStatsCollector;
import de.frauas.group6.traffic.simulator.analytics.StatsCollector;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Real-time Dashboard view for traffic analytics.
 */
public class DashBoard extends StackPane {

    private final IStatsCollector statsCollector;
    
    // --- VIEWS (Containers) ---
    private ScrollPane mainScrollPane; // ScrollPane for Charts
    private VBox mainView;             // Charts content
    
    private ScrollPane exportScrollPane; // NEW: ScrollPane for Export
    private VBox exportView;             // Export content

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
    private ToggleGroup dataTypeGroup; // Single choice for report type
    private ToggleGroup formatGroup;   // Single choice for format
    private TextField txtFileName;
    private ComboBox<String> cbColorFilter;

    public DashBoard(IStatsCollector statsCollector) {
        this.statsCollector = statsCollector;
        this.setStyle("-fx-background-color: #f8f9fa;"); 
        
        // 1. Initialize charts view (and its ScrollPane)
        initMainView();
        mainScrollPane = new ScrollPane(mainView);
        configureScrollPane(mainScrollPane);

        // 2. Initialize export view (and its ScrollPane)
        initExportView();
        exportScrollPane = new ScrollPane(exportView);
        configureScrollPane(exportScrollPane);
        
        // Hide export ScrollPane by default
        exportScrollPane.setVisible(false);

        // 3. Add BOTH ScrollPanes to the StackPane
        this.getChildren().addAll(mainScrollPane, exportScrollPane);
        
        // 4. Initial state: show charts
        showMainView();
    }
    
    // Helper to configure scroll panes consistently
    private void configureScrollPane(ScrollPane sp) {
        sp.setFitToWidth(true);
        sp.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        sp.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        sp.setPannable(true); // Allows "dragging" with mouse
        sp.setStyle("-fx-background: #f8f9fa; -fx-border-color: transparent; -fx-control-inner-background: #f8f9fa;");
    }

    // ==========================================
    // 1. MAIN VIEW (CHARTS)
    // ==========================================
    private void initMainView() {
        mainView = new VBox(20);
        // Generous bottom padding (150px) to ensure the last button is accessible
        mainView.setPadding(new Insets(20, 20, 20, 20));
        mainView.setStyle("-fx-background-color: #f8f9fa;");
        
        // --- HEADER ---
        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);
        
        Label title = new Label("Real-Time Traffic Analytics");
        title.setFont(Font.font("Segoe UI", FontWeight.BOLD, 22));
        title.setStyle("-fx-text-fill: #2c3e50;");
        
        header.getChildren().add(title);

        // --- CHART 1: SPEED ---
        speedChart = createLineChart("Avg Network Speed", "Speed (m/s)");
        speedSeries = new XYChart.Series<>();
        speedSeries.setName("Real-time");
        speedChart.getData().add(speedSeries);
        styleChart(speedChart);

        // --- CHART 2: CONGESTION (Real-Time) ---
        NumberAxis xAxis = new NumberAxis();
        xAxis.setLabel("Vehicles on Edge");
        CategoryAxis yAxis = new CategoryAxis();
        yAxis.setLabel("Edge ID");
        
        congestionChart = new BarChart<>(xAxis, yAxis);
        congestionChart.setTitle("Live Congested Edges");
        congestionChart.setAnimated(false);
        congestionChart.setLegendVisible(false);
        congestionChart.setPrefHeight(300);
        styleChart(congestionChart);
        
        congestionSeries = new XYChart.Series<>();
        congestionChart.getData().add(congestionSeries);

        // --- OTHER CHARTS ---
        densityChart = createVerticalBarChart("Global Density", "Density");
        densitySeries = new XYChart.Series<>();
        densityChart.getData().add(densitySeries);
        styleChart(densityChart);

        travelTimeChart = createVerticalBarChart("Route Travel Time", "Time (s)");
        travelTimeSeries = new XYChart.Series<>();
        travelTimeChart.getData().add(travelTimeSeries);
        styleChart(travelTimeChart);

        // --- EXPORT BUTTON (Moved to Bottom) ---
        Button btnGoToExport = new Button("Export Reports ⤓");
        btnGoToExport.setStyle("-fx-background-color: #34495e; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand; -fx-font-size: 14px;");
        btnGoToExport.setPrefHeight(40);
        btnGoToExport.setMaxWidth(Double.MAX_VALUE); 
        btnGoToExport.setOnAction(e -> showExportView());

        // Add charts to container (Stacked vertically)
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
    // 2. EXPORT VIEW (REPLACES CHARTS)
    // ==========================================
    private void initExportView() {
        exportView = new VBox(20);
        // Generous bottom padding for export as well
        exportView.setPadding(new Insets(40, 40, 40, 40));
        exportView.setAlignment(Pos.TOP_CENTER);
        exportView.setStyle("-fx-background-color: #f8f9fa;");

        // Export Title
        Label lblTitle = new Label("Export Report Wizard");
        lblTitle.setFont(Font.font("Segoe UI", FontWeight.BOLD, 26));
        lblTitle.setStyle("-fx-text-fill: #2c3e50;");

        // Central white card container
        VBox card = new VBox(20);
        card.setMaxWidth(800);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 8; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 5, 0, 0, 1);");

        // --- SECTION 1: REPORT TYPE (Radio Buttons) ---
        Label l1 = new Label("1. Select Report Type (Single Choice)");
        l1.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        l1.setStyle("-fx-text-fill: #34495e;");

        GridPane typeGrid = new GridPane();
        typeGrid.setHgap(20); typeGrid.setVgap(10);
        dataTypeGroup = new ToggleGroup();
        
        int col = 0; int row = 0;
        for (ExportType type : ExportType.values()) {
            RadioButton rb = new RadioButton(formatEnumName(type.name()));
            rb.setUserData(type);
            rb.setToggleGroup(dataTypeGroup);
            if (type == ExportType.SUMMARY) rb.setSelected(true); // Default

            typeGrid.add(rb, col, row);
            col++;
            if (col > 1) { col = 0; row++; }
        }

        // --- SECTION 2: FORMAT (Radio Buttons) ---
        Label l2 = new Label("2. Select Format");
        l2.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        l2.setStyle("-fx-text-fill: #34495e;");

        HBox formatBox = new HBox(20);
        formatGroup = new ToggleGroup();
        RadioButton rbCsv = new RadioButton("CSV (Data Table)");
        rbCsv.setToggleGroup(formatGroup);
        rbCsv.setSelected(true);
        
        RadioButton rbPdf = new RadioButton("PDF (Printable Report)");
        rbPdf.setToggleGroup(formatGroup);
        formatBox.getChildren().addAll(rbCsv, rbPdf);

        // --- SECTION 3: FILE & FILTERS ---
        Label l3 = new Label("3. Settings");
        l3.setFont(Font.font("Arial", FontWeight.BOLD, 14));
        l3.setStyle("-fx-text-fill: #34495e;");

        GridPane settingsGrid = new GridPane();
        settingsGrid.setHgap(15); settingsGrid.setVgap(15);

        txtFileName = new TextField("traffic_report");
        txtFileName.setPromptText("Enter filename...");
        
        cbColorFilter = new ComboBox<>();
        cbColorFilter.getItems().addAll("All", "RED", "GREEN", "BLUE", "YELLOW");
        cbColorFilter.getSelectionModel().selectFirst();

        settingsGrid.addRow(0, new Label("Filename:"), txtFileName);
        settingsGrid.addRow(1, new Label("Filter Color:"), cbColorFilter);

        // --- BUTTONS BAR ---
        HBox actions = new HBox(20);
        actions.setAlignment(Pos.CENTER_RIGHT);
        actions.setPadding(new Insets(20, 0, 0, 0));

        Button btnCancel = new Button("Cancel / Back");
        btnCancel.setPrefWidth(120);
        btnCancel.setOnAction(e -> showMainView()); // Back to charts

        Button btnConfirm = new Button("Export File");
        btnConfirm.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand;");
        btnConfirm.setPrefWidth(150);
        btnConfirm.setOnAction(e -> handleExport());

        actions.getChildren().addAll(btnCancel, btnConfirm);

        // Card assembly
        card.getChildren().addAll(
            l1, typeGrid, new Separator(),
            l2, formatBox, new Separator(),
            l3, settingsGrid, new Separator(),
            actions
        );

        exportView.getChildren().addAll(lblTitle, card);
    }

    // ==========================================
    // NAVIGATION LOGIC
    // ==========================================
    private void showExportView() {
        mainScrollPane.setVisible(false);
        exportScrollPane.setVisible(true); // Show Export ScrollPane
    }

    private void showMainView() {
        exportScrollPane.setVisible(false); // Hide Export ScrollPane
        mainScrollPane.setVisible(true);
    }

    // ==========================================
    // EXPORT LOGIC
    // ==========================================
    private void handleExport() {
        if (statsCollector == null) return;

        // 1. Get Type (Unique)
        RadioButton selectedTypeBtn = (RadioButton) dataTypeGroup.getSelectedToggle();
        if (selectedTypeBtn == null) {
            showAlert("Warning", "Please select a report type.");
            return;
        }
        ExportType selectedType = (ExportType) selectedTypeBtn.getUserData();

        // 2. Format
        RadioButton selectedFormatBtn = (RadioButton) formatGroup.getSelectedToggle();
        boolean isCsv = selectedFormatBtn.getText().contains("CSV");

        // 3. Filter
        ExportFilter filter = new ExportFilter();
        if (cbColorFilter.getValue() != null && !"All".equals(cbColorFilter.getValue())) {
            filter.setVehicleColor(cbColorFilter.getValue());
        }

        // 4. Execution
        try {
            String fName = txtFileName.getText().trim().isEmpty() ? "report" : txtFileName.getText().trim();
            fName += "_" + selectedType.name().toLowerCase(); 
            String ext = isCsv ? ".csv" : ".pdf";
            String path = System.getProperty("user.home") + File.separator + fName + ext;
            
            List<ExportType> singleTypeList = Collections.singletonList(selectedType);
            
            if (isCsv) statsCollector.exportToCsv(path, filter, singleTypeList);
            else statsCollector.exportToPdf(path, filter, singleTypeList);
            
            showAlert("Success", "Report exported to:\n" + path);
            
            showMainView();
            
        } catch (Exception ex) {
            showAlert("Error", "Export failed: " + ex.getMessage());
        }
    }

    // ==========================================
    // UPDATE LOOP
    // ==========================================
    public void update() {
        // Optimization: Do not update charts if on the export page
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
             densities.entrySet().stream().limit(20).forEach(e -> densitySeries.getData().add(new XYChart.Data<>(e.getKey(), e.getValue())));
        }
        
     // 4. Average Travel Time per Route
        Map<String, Double> travelTimes = statsCollector.getAverageTravelTime();
        if (travelTimes != null) {
            travelTimeSeries.getData().clear();
            travelTimes.entrySet().stream()
                .limit(14)
                .forEach(e -> travelTimeSeries.getData().add(new XYChart.Data<>(e.getKey(), e.getValue())));
        }
  
    }

    // --- Helpers ---
    private VBox createCard(Chart chart) {
        VBox card = new VBox(chart);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 5; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 5, 0, 0, 1); -fx-padding: 10;");
        return card;
    }
    private void styleChart(XYChart<?, ?> chart) {
        chart.lookup(".chart-plot-background").setStyle("-fx-background-color: transparent;");
    }
    private void showAlert(String title, String content) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle(title); a.setHeaderText(null); a.setContentText(content);
        a.showAndWait();
    }
    private String formatEnumName(String name) {
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