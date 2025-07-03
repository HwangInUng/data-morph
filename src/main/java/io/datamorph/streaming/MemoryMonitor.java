package io.datamorph.streaming;

import io.datamorph.exceptions.TransformException;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;

/**
 * 실시간 메모리 사용량을 추적하고 관리하는 클래스
 */
public class MemoryMonitor {
    
    private final MemoryMXBean memoryBean;
    private final double memoryThreshold;
    private final double criticalThreshold;
    
    // 기본 임계값: 80% 경고, 90% 위험
    public MemoryMonitor() {
        this(0.8, 0.9);
    }
    
    public MemoryMonitor(double memoryThreshold, double criticalThreshold) {
        this.memoryBean = ManagementFactory.getMemoryMXBean();
        this.memoryThreshold = memoryThreshold;
        this.criticalThreshold = criticalThreshold;
    }
    
    /**
     * 현재 메모리 사용량을 확인하고 임계값 초과 시 경고를 발생시킵니다.
     */
    public void checkMemoryUsage() {
        double usageRatio = getMemoryUsageRatio();
        
        if (usageRatio > criticalThreshold) {
            throw new TransformException(
                String.format("Critical memory usage: %.2f%% (threshold: %.2f%%)", 
                    usageRatio * 100, criticalThreshold * 100));
        } else if (usageRatio > memoryThreshold) {
            System.err.printf("Warning: High memory usage: %.2f%% (threshold: %.2f%%)%n", 
                usageRatio * 100, memoryThreshold * 100);
        }
    }
    
    /**
     * 현재 메모리 사용 비율을 반환합니다 (0.0 ~ 1.0)
     */
    public double getMemoryUsageRatio() {
        MemoryUsage heapUsage = memoryBean.getHeapMemoryUsage();
        return (double) heapUsage.getUsed() / heapUsage.getMax();
    }
    
    /**
     * 메모리 압박이 높은지 확인합니다.
     */
    public boolean isMemoryPressureHigh() {
        return getMemoryUsageRatio() > memoryThreshold;
    }
    
    /**
     * 사용 가능한 메모리량을 반환합니다 (바이트)
     */
    public long getAvailableMemory() {
        MemoryUsage heapUsage = memoryBean.getHeapMemoryUsage();
        return heapUsage.getMax() - heapUsage.getUsed();
    }
    
    /**
     * 상세한 메모리 정보를 반환합니다.
     */
    public String getMemoryInfo() {
        MemoryUsage heapUsage = memoryBean.getHeapMemoryUsage();
        return String.format("Memory usage: %s / %s (%.1f%%)",
            formatBytes(heapUsage.getUsed()),
            formatBytes(heapUsage.getMax()),
            getMemoryUsageRatio() * 100);
    }
    
    /**
     * 바이트를 읽기 쉬운 단위로 변환
     */
    private String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024));
        return String.format("%.1f GB", bytes / (1024.0 * 1024 * 1024));
    }
}
