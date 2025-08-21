package com.yourorg.telemetryagent.app;

import com.yourorg.telemetryagent.core.TelemetryService;
import com.yourorg.telemetryagent.core.GeminiClient;
import com.yourorg.telemetryagent.domain.ProcessInfo;
import com.yourorg.telemetryagent.domain.SystemMetricsSnapshot;
import com.yourorg.telemetryagent.ui.components.StatusCard;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebView;
import javafx.stage.Stage;

public class TelemetryApplication extends Application {

    private TelemetryService telemetryService;
    private StatusCard cpuCard;
    private StatusCard memoryCard;
    private StatusCard diskCard;

    private LineChart<Number, Number> cpuChart;
    private XYChart.Series<Number, Number> cpuDataSeries;
    private LineChart<Number, Number> memoryChart;
    private XYChart.Series<Number, Number> memoryDataSeries;

    private static final int MAX_DATA_POINTS = 30; // 30 points * 2s interval = 60s window
    private int xSeriesDataCounter = 0;

    private TableView<ProcessInfo> processTable;

    private GeminiClient geminiClient;
    private Button explainButton;
    private WebView aiInsightView;
    private SystemMetricsSnapshot lastSnapshot;

    @Override
    public void start(Stage primaryStage) {
        // --- 1. Create the Card Components ---
        cpuCard = new StatusCard("CPU Load");
        memoryCard = new StatusCard("Memory Usage");
        diskCard = new StatusCard("Disk Usage (C:)");


        // --- 2. Create the Card Layout ---
        TilePane cardLayout = new TilePane();
        cardLayout.setHgap(15);
        cardLayout.setVgap(15);
        cardLayout.setAlignment(Pos.CENTER);
        cardLayout.getChildren().addAll(cpuCard, memoryCard, diskCard);

        // --- 3. Create the CPU Chart ---
        NumberAxis cpuXAxis = new NumberAxis();
        cpuXAxis.setTickLabelsVisible(false);
        NumberAxis cpuYAxis = new NumberAxis(0, 100, 10);
        cpuYAxis.setLabel("Usage (%)");
        cpuChart = new LineChart<>(cpuXAxis, cpuYAxis);
        cpuChart.setTitle("CPU Usage (60s)");
        cpuChart.setAnimated(false);
        cpuChart.setCreateSymbols(false);
        cpuDataSeries = new XYChart.Series<>();
        cpuDataSeries.setName("CPU Load");
        cpuChart.getData().add(cpuDataSeries);

        // --- 4. Create the Memory Chart ---
        NumberAxis memXAxis = new NumberAxis();
        memXAxis.setTickLabelsVisible(false);
        NumberAxis memYAxis = new NumberAxis(0, 100, 10);
        memYAxis.setLabel("Usage (%)");
        memoryChart = new LineChart<>(memXAxis, memYAxis);
        memoryChart.setTitle("Memory Usage (60s)");
        memoryChart.setAnimated(false);
        memoryChart.setCreateSymbols(false);
        memoryDataSeries = new XYChart.Series<>();
        memoryDataSeries.setName("Memory Usage");
        memoryChart.getData().add(memoryDataSeries);

        // --- 5. Create the Chart Layout ---
        HBox chartLayout = new HBox();
        chartLayout.setSpacing(15);
        HBox.setHgrow(cpuChart, Priority.ALWAYS);
        HBox.setHgrow(memoryChart, Priority.ALWAYS);
        chartLayout.getChildren().addAll(cpuChart, memoryChart);

        // --- 6. Create the Process Table ---
        processTable = new TableView<>();
        processTable.setPlaceholder(new Label("Loading process data..."));
        TableColumn<ProcessInfo, Integer> pidCol = new TableColumn<>("PID");
        pidCol.setCellValueFactory(new PropertyValueFactory<>("pid"));
        TableColumn<ProcessInfo, String> nameCol = new TableColumn<>("Name");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        nameCol.setPrefWidth(250);
        TableColumn<ProcessInfo, Double> cpuCol = new TableColumn<>("CPU %");
        cpuCol.setCellValueFactory(new PropertyValueFactory<>("cpuPercent"));
        cpuCol.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(String.format("%.1f%%", item));
                }
            }
        });
        TableColumn<ProcessInfo, String> memCol = new TableColumn<>("Memory");
        memCol.setCellValueFactory(new PropertyValueFactory<>("memoryUsage"));
        processTable.getColumns().addAll(pidCol, nameCol, cpuCol, memCol);

        // --- 7. Create the AI Insight Panel (WebView) ---
        aiInsightView = new WebView();
        aiInsightView.setPrefHeight(500);
        aiInsightView.setVisible(false);

        // Add error handling for WebView
        aiInsightView.getEngine().setOnError(event -> {
            System.err.println("WebView error: " + event.getMessage());
        });

        explainButton = new Button("Explain Current State");
        explainButton.setStyle(
                "-fx-background-color: #0078D7;" +
                        "-fx-text-fill: white;" +
                        "-fx-font-weight: bold;" +
                        "-fx-padding: 8 16 8 16;" +
                        "-fx-background-radius: 20;" +
                        "-fx-cursor: hand;"
        );
        explainButton.setOnMouseEntered(e -> explainButton.setStyle(
                "-fx-background-color: #005a9e;" +
                        "-fx-text-fill: white;" +
                        "-fx-font-weight: bold;" +
                        "-fx-padding: 8 16 8 16;" +
                        "-fx-background-radius: 20;" +
                        "-fx-cursor: hand;"
        ));
        explainButton.setOnMouseExited(e -> explainButton.setStyle(
                "-fx-background-color: #0078D7;" +
                        "-fx-text-fill: white;" +
                        "-fx-font-weight: bold;" +
                        "-fx-padding: 8 16 8 16;" +
                        "-fx-background-radius: 20;" +
                        "-fx-cursor: hand;"
        ));
        explainButton.setOnAction(e -> onExplainButtonClicked());

        VBox aiPanelLayout = new VBox(12, explainButton, aiInsightView);
        aiPanelLayout.setAlignment(Pos.TOP_CENTER);
        aiPanelLayout.setPadding(new Insets(10));
        VBox.setVgrow(aiInsightView, Priority.ALWAYS);

        // --- 8. Create the Main Application Layout ---
        VBox rootLayout = new VBox();
        rootLayout.setSpacing(20);
        rootLayout.setPadding(new Insets(15));

        VBox.setVgrow(processTable, Priority.ALWAYS);

        rootLayout.getChildren().addAll(cardLayout, chartLayout, processTable, aiPanelLayout);

        // --- 9. Create the Scene and configure the Stage ---
        Scene scene = new Scene(rootLayout, 900, 1000);
        primaryStage.setTitle("Windows System Telemetry Agent");
        primaryStage.setScene(scene);

        // --- 10. Start backend services ---
        this.geminiClient = new GeminiClient();
        this.telemetryService = new TelemetryService();
        telemetryService.startPolling(snapshot -> Platform.runLater(() -> updateUI(snapshot)));

        primaryStage.show();
    }

    private void updateUI(SystemMetricsSnapshot snapshot) {
        this.lastSnapshot = snapshot;

        // --- Update Cards ---
        double cpuLoad = snapshot.getCpuMetrics().getCpuLoad();
        cpuCard.setValue(String.format("%.1f%%", cpuLoad));

        long memUsed = snapshot.getMemoryMetrics().getUsedBytes();
        long memTotal = snapshot.getMemoryMetrics().getTotalBytes();
        memoryCard.setValue(String.format("%s / %s", formatBytes(memUsed), formatBytes(memTotal)));

        long diskUsed = snapshot.getDiskMetrics().getUsedBytes();
        long diskTotal = snapshot.getDiskMetrics().getTotalBytes();
        diskCard.setValue(String.format("%s / %s", formatBytes(diskUsed), formatBytes(diskTotal)));

        // --- Update Charts ---
        cpuDataSeries.getData().add(new XYChart.Data<>(xSeriesDataCounter, cpuLoad));
//        if (cpuDataSeries.getData().size() > MAX_DATA_POINTS) {
//            cpuDataSeries.getData().remove(0);
//        }

        double memPercent = (double) memUsed / memTotal * 100.0;
        memoryDataSeries.getData().add(new XYChart.Data<>(xSeriesDataCounter, memPercent));
//        if (memoryDataSeries.getData().size() > MAX_DATA_POINTS) {
//            memoryDataSeries.getData().remove(0);
//        }

        // --- Update Process Table ---
        processTable.getItems().setAll(snapshot.getTopProcesses());

        xSeriesDataCounter++;
    }

    private void onExplainButtonClicked() {
        if (lastSnapshot == null) {
            aiInsightView.getEngine().loadContent("<p><b>[!]</b> Not enough data collected yet. Please wait a moment.</p>");
            aiInsightView.setVisible(true);
            return;
        }

        explainButton.setDisable(true);
        aiInsightView.setVisible(true);
        aiInsightView.getEngine().loadContent("<p><i>[Analyzing system state with Gemini... Please wait.]</i></p>");

        geminiClient.explainSystemState(lastSnapshot)
                .whenComplete((response, error) -> {
                    Platform.runLater(() -> {
                        if (error != null) {
                            System.err.println("Gemini error: " + error.getMessage());
                            aiInsightView.getEngine().loadContent("<p><b>[X] Error:</b> " + error.getMessage() + "</p>");
                        } else {
                            System.out.println("Full Gemini response length: " + response.length());
                            System.out.println("Response preview: " + response.substring(0, Math.min(200, response.length())));
                            aiInsightView.getEngine().loadContent(formatAIResponse(response));
                        }
                        explainButton.setDisable(false);
                    });
                });
    }

    private String formatAIResponse(String response) {
        if (response == null || response.trim().isEmpty()) {
            return "<html><body style='font-family: Segoe UI; font-size:14px; padding:10px;'>" +
                    "<h2>[AI System Insight]</h2>" +
                    "<p>No response received.</p>" +
                    "</body></html>";
        }

        String html = response.trim();

        // Escape any existing HTML to prevent conflicts
        html = html.replaceAll("&", "&amp;")
                .replaceAll("<", "&lt;")
                .replaceAll(">", "&gt;");

        // Process line breaks FIRST to preserve structure
        html = html.replaceAll("\\r\\n|\\r|\\n", "\n");

        // Process markdown headers (before line break conversion)
        html = html.replaceAll("(?m)^### (.*?)$", "<h3>$1</h3>");
        html = html.replaceAll("(?m)^## (.*?)$", "<h2>$1</h2>");
        html = html.replaceAll("(?m)^# (.*?)$", "<h1>$1</h1>");

        // Process bold and italic (before line break conversion)
        html = html.replaceAll("\\*\\*(.*?)\\*\\*", "<strong>$1</strong>");
        html = html.replaceAll("\\*(.*?)\\*", "<em>$1</em>");

        // Process lists more carefully
        html = processMarkdownLists(html);

        // Convert remaining line breaks to HTML breaks
        html = html.replaceAll("\\n", "<br>\n");

        // Clean up any double breaks around block elements
        html = html.replaceAll("<br>\\s*(</?(?:h[1-6]|ul|li|p)>)", "$1");
        html = html.replaceAll("(</?(?:h[1-6]|ul|li|p)>)\\s*<br>", "$1");

        return "<html><body style='font-family: Segoe UI; font-size:14px; padding:10px; line-height:1.4;'>" +
                "<h2 style='color:#0078D7; border-bottom:2px solid #0078D7; padding-bottom:5px;'>[AI System Insight]</h2>" +
                "<div style='margin-top:15px;'>" + html + "</div>" +
                "<hr style='margin:20px 0; border:1px solid #ddd;'>" +
                "<p style='color:#666; font-style:italic;'>[Analysis provided by Gemini]</p>" +
                "</body></html>";
    }

    private String processMarkdownLists(String html) {
        // Split by paragraphs to process lists in context
        String[] paragraphs = html.split("\\n\\s*\\n");
        StringBuilder result = new StringBuilder();

        for (int i = 0; i < paragraphs.length; i++) {
            String paragraph = paragraphs[i].trim();

            if (paragraph.isEmpty()) {
                continue;
            }

            // Check if this paragraph contains list items
            if (paragraph.contains("\n* ") || paragraph.startsWith("* ")) {
                // Process as a list
                String[] lines = paragraph.split("\\n");
                StringBuilder listHtml = new StringBuilder("<ul>");

                for (String line : lines) {
                    line = line.trim();
                    if (line.startsWith("* ")) {
                        listHtml.append("<li>").append(line.substring(2).trim()).append("</li>");
                    } else if (!line.isEmpty()) {
                        // Non-list line in the middle - close list and start new paragraph
                        listHtml.append("</ul><p>").append(line).append("</p><ul>");
                    }
                }
                listHtml.append("</ul>");
                result.append(listHtml.toString());
            } else {
                // Regular paragraph
                result.append("<p>").append(paragraph).append("</p>");
            }

            if (i < paragraphs.length - 1) {
                result.append("\n\n");
            }
        }

        // Clean up any empty lists or double-nested elements
        return result.toString()
                .replaceAll("<ul>\\s*</ul>", "")
                .replaceAll("<p>\\s*</p>", "");
    }

    private String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String pre = "KMGTPE".charAt(exp - 1) + "B";
        return String.format("%.1f %s", bytes / Math.pow(1024, exp), pre);
    }

    @Override
    public void stop() {
        if (telemetryService != null) {
            telemetryService.stopPolling();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}