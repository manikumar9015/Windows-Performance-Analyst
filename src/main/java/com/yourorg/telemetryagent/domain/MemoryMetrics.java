package com.yourorg.telemetryagent.domain;

public class MemoryMetrics {
    private final long usedBytes;
    private final long totalBytes;

    public MemoryMetrics(long usedBytes, long totalBytes) {
        this.usedBytes = usedBytes;
        this.totalBytes = totalBytes;
    }

    public long getUsedBytes() { return usedBytes; }
    public long getTotalBytes() { return totalBytes; }
}