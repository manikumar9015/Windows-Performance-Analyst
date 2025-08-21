package com.yourorg.telemetryagent.core;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.yourorg.telemetryagent.core.ai.APIKey;
import com.yourorg.telemetryagent.core.ai.GeminiRequest;
import com.yourorg.telemetryagent.domain.SystemMetricsSnapshot;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GeminiClient {

    private static final Logger logger = LoggerFactory.getLogger(GeminiClient.class);

    // --- IMPORTANT: REPLACE WITH YOUR OWN API KEY ---
    // For development only. In a real app, this must be stored securely.
//    private static final String API_KEY = "AIzaSyDCrPiMTeZ2GYLJ_qDP8Mi2r1j87O0de34";
    private static final String API_KEY = APIKey.GEMINI_API_KEY;


    private static final String API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=" + API_KEY;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public GeminiClient() {
        this.httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.objectMapper = new ObjectMapper();
    }

    public CompletableFuture<String> explainSystemState(SystemMetricsSnapshot snapshot) {
        // Run the entire network operation asynchronously on a background thread.
        return CompletableFuture.supplyAsync(() -> {
            try {
                String prompt = buildPrompt(snapshot);
                GeminiRequest requestPayload = GeminiRequest.create(prompt);
                String requestBody = objectMapper.writeValueAsString(requestPayload);

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(API_URL))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() != 200) {
                    logger.error("Non-200 response from API: Status={}, Body={}", response.statusCode(), response.body());
                    return "Error: Received non-200 response from API: " + response.body();
                }

                // Parse the response to extract just the text content
                return parseResponse(response.body());

            } catch (Exception e) {
                logger.error("Error calling AI service: {}", e.getMessage(), e);
                return "Error calling AI service: " + e.getMessage();
            }
        });
    }

    private String buildPrompt(SystemMetricsSnapshot snapshot) {
        // Creates a detailed summary of the system state for AI analysis.
        String topProcess = snapshot.getTopProcesses().isEmpty() ? "N/A" : snapshot.getTopProcesses().get(0).getName();
        double cpuLoad = snapshot.getCpuMetrics().getCpuLoad();
        long memUsed = snapshot.getMemoryMetrics().getUsedBytes();
        long memTotal = snapshot.getMemoryMetrics().getTotalBytes();
        long diskUsed = snapshot.getDiskMetrics().getUsedBytes();
        long diskTotal = snapshot.getDiskMetrics().getTotalBytes();
        int processCount = snapshot.getTopProcesses().size();

        // Validate inputs to prevent formatting errors
        if (memTotal == 0 || diskTotal == 0) {
            logger.error("Invalid system metrics: memoryTotal={} or diskTotal={}", memTotal, diskTotal);
            return "Error: Invalid system metrics data.";
        }

        String prompt = String.format(
                "You are a system diagnostics expert. Analyze this Windows system snapshot and provide a concise, actionable explanation focusing on potential performance issues. " +
                        "Consider high CPU load (>80%%), high memory usage (>90%%), high disk usage (>90%%), or unusual process behavior. " +
                        "DATA: " +
                        "CPU Load: %.1f%%. " +
                        "Memory Usage: %s / %s (%.1f%%). " +
                        "Disk Usage: %s / %s (%.1f%%). " +
                        "Top CPU Process: %s. " +
                        "Total Processes: %d. " +
                        "Provide the response in markdown format with the following structure: " +
                        "## Title\\n" +
                        "**Likely Causes:**\\n- Cause 1\\n- Cause 2\\n" +
                        "**Suggested Actions:**\\n- Action 1\\n- Action 2\\n" +
                        "Keep the response concise (150-300 words) and prioritize actionable insights.",
                cpuLoad,
                formatBytes(memUsed), formatBytes(memTotal), (double) memUsed / memTotal * 100.0,
                formatBytes(diskUsed), formatBytes(diskTotal), (double) diskUsed / diskTotal * 100.0,
                topProcess,
                processCount
        );

        logger.debug("Generated prompt: {}", prompt);
        return prompt;
    }

    private String parseResponse(String responseBody) {
        try {
            logger.debug("Raw API response: {}", responseBody);
            JsonNode root = objectMapper.readTree(responseBody);
            // Navigate to the text field based on Gemini API structure
            JsonNode textNode = root.path("candidates").path(0).path("content").path("parts").path(0).path("text");
            if (textNode.isMissingNode()) {
                logger.error("Text field not found in API response: {}", responseBody);
                return "Could not parse AI response: Text field not found.";
            }
            String text = textNode.asText();
            logger.debug("Parsed response text: {}", text);
            return text; // Jackson automatically handles escaped characters (e.g., \n, \t, \")
        } catch (Exception e) {
            logger.error("Error parsing AI response: {}", e.getMessage(), e);
            return "Error parsing AI response: " + e.getMessage();
        }
    }

    private String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        char pre = "KMGTPE".charAt(exp - 1);
        return String.format("%.1f %sB", bytes / Math.pow(1024, exp), pre);
    }
}