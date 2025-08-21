package com.yourorg.telemetryagent.domain;

/**
 * A data class (POJO) to hold a snapshot of CPU metrics.
 */
public class CpuMetrics {

    private final double cpuLoad; // Overall CPU load as a percentage (0-100)

    public CpuMetrics(double cpuLoad) {
        this.cpuLoad = cpuLoad;
    }

    public double getCpuLoad() {
        return cpuLoad;
    }

    @Override
    public String toString() {
        // Formats the output for easy printing, e.g., "CPU Load: 15.2%"
        return String.format("CPU Load: %.1f%%", cpuLoad);
    }
}