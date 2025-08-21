package com.yourorg.telemetryagent.domain;

public class DiskMetrics {
    private final String driveName;
    private final long usedBytes;
    private final long totalBytes;

    public DiskMetrics(String driveName, long usedBytes, long totalBytes) {
        this.driveName = driveName;
        this.usedBytes = usedBytes;
        this.totalBytes = totalBytes;
    }

    public String getDriveName() { return driveName; }
    public long getUsedBytes() { return usedBytes; }
    public long getTotalBytes() { return totalBytes; }
}