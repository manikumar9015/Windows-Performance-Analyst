package com.yourorg.telemetryagent.core;

import com.yourorg.telemetryagent.domain.*;
import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.hardware.HardwareAbstractionLayer;
import oshi.software.os.OperatingSystem;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import oshi.hardware.GlobalMemory;
import oshi.software.os.OSFileStore;
import oshi.software.os.OSProcess;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import com.yourorg.telemetryagent.domain.ProcessInfo;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

/**
 * A service class responsible for collecting all system telemetry data using the OSHI library.
 */
public class TelemetryService {

    private final SystemInfo systemInfo;
    private final HardwareAbstractionLayer hardware;
    private final OperatingSystem operatingSystem;
    private final CentralProcessor processor;
    private ScheduledExecutorService executorService;
    private long[] prevTicks;
    private final GlobalMemory memory;

    /**
     * Constructor for the TelemetryService.
     * Initializes the core OSHI objects.
     */
    public TelemetryService() {
        this.systemInfo = new SystemInfo();
        this.hardware = systemInfo.getHardware();
        this.operatingSystem = systemInfo.getOperatingSystem();
        this.processor = hardware.getProcessor();
        this.memory = hardware.getMemory();
        this.prevTicks = new long[CentralProcessor.TickType.values().length];
    }

    public MemoryMetrics getCurrentMemoryMetrics() {
        long total = memory.getTotal();
        long available = memory.getAvailable();
        return new MemoryMetrics(total - available, total);
    }

    public DiskMetrics getPrimaryDiskMetrics() {
        // Find the C: drive. This is a simplified approach for Windows.
        for (OSFileStore store : operatingSystem.getFileSystem().getFileStores()) {
            if (store.getMount().startsWith("C:")) {
                long total = store.getTotalSpace();
                long free = store.getFreeSpace();
                return new DiskMetrics(store.getName(), total - free, total);
            }
        }
        // Fallback if C: drive isn't found
        return new DiskMetrics("N/A", 0, 0);
    }

    /**
     * Gathers some basic, static information about the computer system.
     *
     * @return A formatted string with OS and CPU details.
     */
    public String getStaticSystemInfo() {
        // Get CPU information
        CentralProcessor processor = hardware.getProcessor();
        CentralProcessor.ProcessorIdentifier identifier = processor.getProcessorIdentifier();

        // Build a formatted string with the details
        StringBuilder infoBuilder = new StringBuilder();
        infoBuilder.append("System Information:\n");
        infoBuilder.append("-------------------\n");
        infoBuilder.append("Operating System: ").append(operatingSystem).append("\n");
        infoBuilder.append("CPU Model: ").append(identifier.getName()).append("\n");
        infoBuilder.append("Physical Cores: ").append(processor.getPhysicalProcessorCount()).append("\n");
        infoBuilder.append("Logical Cores (Threads): ").append(processor.getLogicalProcessorCount()).append("\n");

        return infoBuilder.toString();
    }

    /**
     * Fetches the current CPU load.
     * Note: This method is stateful and relies on the previous tick counts.
     * @return A CpuMetrics object with the current load percentage.
     */
    public CpuMetrics getCurrentCpuMetrics() {
        // Get the current ticks and calculate the load since the last call
        double cpuLoad = processor.getSystemCpuLoadBetweenTicks(prevTicks) * 100;

        // Update the previous ticks array for the next calculation
        this.prevTicks = processor.getSystemCpuLoadTicks();

        return new CpuMetrics(cpuLoad);
    }

    public List<ProcessInfo> getTopProcesses(int limit) {
        // Get a list of all processes, sort them by CPU usage in descending order
        List<OSProcess> processes = operatingSystem.getProcesses();
        processes.sort(Comparator.comparingDouble(OSProcess::getProcessCpuLoadCumulative).reversed());

        List<ProcessInfo> topProcesses = new ArrayList<>();
        for (int i = 0; i < processes.size() && i < limit; i++) {
            OSProcess p = processes.get(i);

            // OSHI gives CPU load as a fraction (0.0 to 1.0), so we multiply by 100
            double cpuPercent = p.getProcessCpuLoadCumulative() * 100;
            // RSS is Resident Set Size, the physical memory it's using
            String memory = formatBytes(p.getResidentSetSize());

            topProcesses.add(new ProcessInfo(p.getProcessID(), p.getName(), cpuPercent, memory));
        }
        return topProcesses;
    }

    private String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        char pre = "KMGTPE".charAt(exp - 1);
        return String.format("%.1f %sB", bytes / Math.pow(1024, exp), pre);
    }

    /**
     * Starts polling for telemetry data at a fixed interval.
     */
    public void startPolling(Consumer<SystemMetricsSnapshot> snapshotConsumer) {
        // Creates a single-threaded scheduler.
        executorService = Executors.newSingleThreadScheduledExecutor();

        // The task to run. It will fetch metrics and print them.
        Runnable pollingTask = () -> {
            // Collect all metrics
            CpuMetrics cpu = getCurrentCpuMetrics();
            MemoryMetrics mem = getCurrentMemoryMetrics();
            DiskMetrics disk = getPrimaryDiskMetrics();
            List<ProcessInfo> processes = getTopProcesses(10);

            // Create the snapshot object
            SystemMetricsSnapshot snapshot = new SystemMetricsSnapshot(cpu, mem, disk, processes);

            // Pass the complete snapshot to the listener
            snapshotConsumer.accept(snapshot);
        };

        // Schedule the task to run every 2 seconds, after an initial 0-second delay.
        executorService.scheduleAtFixedRate(pollingTask, 0, 2, TimeUnit.SECONDS);
    }

    /**
     * Stops the polling service gracefully.
     */
    public void stopPolling() {
        if (executorService != null && !executorService.isShutdown()) {
            System.out.println("Shutting down telemetry service...");
            executorService.shutdown();
        }
    }
}