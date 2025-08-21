package com.yourorg.telemetryagent.domain;

import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;

/**
 * A data class representing a single process, designed for use with a JavaFX TableView.
 */
public class ProcessInfo {
    private final SimpleIntegerProperty pid;
    private final SimpleStringProperty name;
    private final SimpleDoubleProperty cpuPercent;
    private final SimpleStringProperty memoryUsage;

    public ProcessInfo(int pid, String name, double cpuPercent, String memoryUsage) {
        this.pid = new SimpleIntegerProperty(pid);
        this.name = new SimpleStringProperty(name);
        this.cpuPercent = new SimpleDoubleProperty(cpuPercent);
        this.memoryUsage = new SimpleStringProperty(memoryUsage);
    }

    // --- Getters for JavaFX Property ---
    // The TableView will use these methods to get the data for each cell.
    public int getPid() { return pid.get(); }
    public String getName() { return name.get(); }
    public double getCpuPercent() { return cpuPercent.get(); }
    public String getMemoryUsage() { return memoryUsage.get(); }
}