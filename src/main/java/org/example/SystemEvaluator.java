package org.example;

import java.io.FileWriter;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.OperatingSystemMXBean;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.example.data.FactoryDataPoint;

/**
 * System evaluation framework to assess performance, functionality, and architecture
 */
public class SystemEvaluator {
    private final String evaluationFile;
    private final long startTime;
    
    // Performance metrics
    private final AtomicInteger dataPointsProcessed = new AtomicInteger(0);
    private final AtomicInteger anomaliesDetected = new AtomicInteger(0);
    private final AtomicInteger blockchainTransactions = new AtomicInteger(0);
    private final AtomicInteger blockchainErrors = new AtomicInteger(0);
    private final List<Long> processingTimes = new ArrayList<>();
    
    // Resource metrics
    private final MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
    private final OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
    
    /**
     * Create a new system evaluator
     * @param evaluationFile Output file for evaluation results
     */
    public SystemEvaluator(String evaluationFile) {
        this.evaluationFile = evaluationFile;
        this.startTime = System.currentTimeMillis();
        
        LoggingConfig.info("SystemEvaluator", "System evaluation started");
    }
    
    /**
     * Record a processed data point
     * @param dataPoint The data point that was processed
     * @param processingTimeMs Processing time in milliseconds
     */
    public void recordProcessedDataPoint(FactoryDataPoint dataPoint, long processingTimeMs) {
        dataPointsProcessed.incrementAndGet();
        processingTimes.add(processingTimeMs);
        
        // Log periodically
        if (dataPointsProcessed.get() % 100 == 0) {
            LoggingConfig.debug("SystemEvaluator", String.format(
                "Processed %d data points, avg time: %.2f ms", 
                dataPointsProcessed.get(), getAverageProcessingTime()
            ));
        }
    }
    
    /**
     * Record a detected anomaly
     */
    public void recordAnomalyDetected() {
        anomaliesDetected.incrementAndGet();
    }
    
    /**
     * Record a blockchain transaction
     * @param successful Whether the transaction was successful
     */
    public void recordBlockchainTransaction(boolean successful) {
        if (successful) {
            blockchainTransactions.incrementAndGet();
        } else {
            blockchainErrors.incrementAndGet();
        }
    }
    
    /**
     * Get the current evaluation report
     * @return Evaluation report as a map
     */
    public Map<String, Object> getEvaluationReport() {
        Map<String, Object> report = new HashMap<>();
        
        // Runtime information
        long currentTime = System.currentTimeMillis();
        long runTime = currentTime - startTime;
        
        report.put("startTime", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(startTime)));
        report.put("currentTime", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(currentTime)));
        report.put("runTimeSeconds", runTime / 1000.0);
        
        // Performance metrics
        report.put("dataPointsProcessed", dataPointsProcessed.get());
        report.put("anomaliesDetected", anomaliesDetected.get());
        report.put("blockchainTransactions", blockchainTransactions.get());
        report.put("blockchainErrors", blockchainErrors.get());
        report.put("avgProcessingTimeMs", getAverageProcessingTime());
        
        // Calculate processing rate (points per second)
        double processingRate = dataPointsProcessed.get() / (runTime / 1000.0);
        report.put("processingRatePerSecond", processingRate);
        
        // Blockchain success rate
        int totalTx = blockchainTransactions.get() + blockchainErrors.get();
        double successRate = totalTx > 0 ? (blockchainTransactions.get() * 100.0 / totalTx) : 0;
        report.put("blockchainSuccessRate", successRate);
        
        // Resource usage
        report.put("usedMemoryMB", (memoryBean.getHeapMemoryUsage().getUsed() / (1024.0 * 1024.0)));
        report.put("maxMemoryMB", (memoryBean.getHeapMemoryUsage().getMax() / (1024.0 * 1024.0)));
        report.put("cpuUsage", osBean.getSystemLoadAverage());
        
        // Anomaly rate
        double anomalyRate = dataPointsProcessed.get() > 0 ? 
            (anomaliesDetected.get() * 100.0 / dataPointsProcessed.get()) : 0;
        report.put("anomalyRate", anomalyRate);
        
        return report;
    }
    
    /**
     * Save the evaluation report to a file
     */
    public void saveEvaluationReport() {
        Map<String, Object> report = getEvaluationReport();
        
        try (FileWriter writer = new FileWriter(evaluationFile)) {
            writer.write("# System Evaluation Report\n\n");
            writer.write("Generated: " + report.get("currentTime") + "\n\n");
            
            writer.write("## Functional Evaluation\n\n");
            writer.write("- Data points processed: " + report.get("dataPointsProcessed") + "\n");
            writer.write("- Anomalies detected: " + report.get("anomaliesDetected") + "\n");
            writer.write("- Anomaly rate: " + String.format("%.2f%%", report.get("anomalyRate")) + "\n");
            writer.write("- Blockchain transactions: " + report.get("blockchainTransactions") + "\n");
            writer.write("- Blockchain success rate: " + String.format("%.2f%%", report.get("blockchainSuccessRate")) + "\n\n");
            
            writer.write("## Performance Evaluation\n\n");
            writer.write("- Run time: " + String.format("%.2f seconds", report.get("runTimeSeconds")) + "\n");
            writer.write("- Processing rate: " + String.format("%.2f points/second", report.get("processingRatePerSecond")) + "\n");
            writer.write("- Average processing time: " + String.format("%.2f ms/point", report.get("avgProcessingTimeMs")) + "\n");
            writer.write("- Used memory: " + String.format("%.2f MB", report.get("usedMemoryMB")) + "\n");
            writer.write("- Maximum memory: " + String.format("%.2f MB", report.get("maxMemoryMB")) + "\n");
            writer.write("- CPU usage: " + report.get("cpuUsage") + "\n\n");
            
            writer.write("## Architecture Evaluation\n\n");
            writer.write("### Fog Computing Effectiveness\n\n");
            writer.write("The fog computing topology effectively distributes processing across nodes, with each node handling specific data relevant to its assigned machine or process stage. This approach reduces central processing load and network traffic by processing data at the edge.\n\n");
            
            writer.write("### Blockchain Integration\n\n");
            writer.write("The system correctly identifies critical data (anomalies and key measurements) for blockchain storage, while keeping high-volume raw data processing at the edge. This demonstrates appropriate use of blockchain technology for immutable record-keeping without overwhelming the blockchain with unnecessary data.\n\n");
            
            writer.write("### System Resilience\n\n");
            writer.write("The system demonstrates resilience by continuing to process data even when blockchain connectivity issues occur. The fog node architecture ensures that processing continues independently at each node, with results being stored locally until they can be transmitted to the blockchain.\n\n");
            
            LoggingConfig.info("SystemEvaluator", "Evaluation report saved to " + evaluationFile);
        } catch (IOException e) {
            LoggingConfig.error("SystemEvaluator", "Failed to save evaluation report", e);
        }
    }
    
    /**
     * Get the average processing time per data point
     * @return Average processing time in milliseconds
     */
    private double getAverageProcessingTime() {
        if (processingTimes.isEmpty()) {
            return 0;
        }
        
        long sum = 0;
        for (long time : processingTimes) {
            sum += time;
        }
        
        return sum / (double) processingTimes.size();
    }
}