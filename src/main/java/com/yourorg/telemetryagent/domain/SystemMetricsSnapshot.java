package com.yourorg.telemetryagent.domain;
import java.util.List;

/**
 * A container for all metrics collected at a single point in time.
 */
public class SystemMetricsSnapshot {
    private final CpuMetrics cpuMetrics;
    private final MemoryMetrics memoryMetrics;
    private final DiskMetrics diskMetrics;
    private final List<ProcessInfo> topProcesses;

    public SystemMetricsSnapshot(CpuMetrics cpu, MemoryMetrics mem, DiskMetrics disk, List<ProcessInfo> topProcesses) {
        this.cpuMetrics = cpu;
        this.memoryMetrics = mem;
        this.diskMetrics = disk;
        this.topProcesses = topProcesses;

    }

    public CpuMetrics getCpuMetrics() { return cpuMetrics; }
    public MemoryMetrics getMemoryMetrics() { return memoryMetrics; }
    public DiskMetrics getDiskMetrics() { return diskMetrics; }
    public List<ProcessInfo> getTopProcesses() { return topProcesses; }
}