package org.example;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Blockchain logger for recording important manufacturing data on the blockchain
 * Includes batching and asynchronous processing to reduce blockchain transactions
 */
public class BlockchainLogger {
    private final PureChainConnector connector;
    private final ExecutorService executorService;
    private final ConcurrentLinkedQueue<LogEntry> logQueue;
    private final AtomicBoolean isRunning;
    
    // Batch processing parameters
    private int batchSize = 10; // Number of entries to combine in a batch
    private int maxQueueSize = 1000; // Maximum size before back pressure
    
    /**
     * Create a new blockchain logger
     * @param connector The blockchain connector
     */
    public BlockchainLogger(PureChainConnector connector) {
        this.connector = connector;
        this.executorService = Executors.newSingleThreadExecutor();
        this.logQueue = new ConcurrentLinkedQueue<>();
        this.isRunning = new AtomicBoolean(true);
        
        // Start the background processing thread
        startBackgroundProcessor();
    }
    
    /**
     * Start the background thread for processing log entries
     */
    private void startBackgroundProcessor() {
        executorService.submit(() -> {
            List<LogEntry> batch = new ArrayList<>();
            
            while (isRunning.get()) {
                try {
                    // Process entries in batches
                    LogEntry entry = logQueue.poll();
                    if (entry != null) {
                        batch.add(entry);
                        
                        // If we have enough entries, process the batch
                        if (batch.size() >= batchSize) {
                            processBatch(batch);
                            batch.clear();
                        }
                    } else {
                        // If queue is empty and we have entries, process them
                        if (!batch.isEmpty()) {
                            processBatch(batch);
                            batch.clear();
                        }
                        
                        // Sleep to avoid busy waiting
                        Thread.sleep(100);
                    }
                } catch (Exception e) {
                    System.err.println("Error in blockchain logger: " + e.getMessage());
                    e.printStackTrace();
                }
            }
            
            // Process any remaining entries before shutting down
            if (!batch.isEmpty()) {
                processBatch(batch);
            }
        });
    }
    
    /**
     * Process a batch of log entries
     * @param batch The batch to process
     */
    private void processBatch(List<LogEntry> batch) {
        try {
            // Group entries by machine
            Map<String, List<LogEntry>> entriesByMachine = new HashMap<>();
            
            for (LogEntry entry : batch) {
                entriesByMachine.putIfAbsent(entry.getMachineId(), new ArrayList<>());
                entriesByMachine.get(entry.getMachineId()).add(entry);
            }
            
            // Process each machine's entries
            for (Map.Entry<String, List<LogEntry>> entry : entriesByMachine.entrySet()) {
                String machineId = entry.getKey();
                List<LogEntry> entries = entry.getValue();
                
                // If there's only one entry, record it directly
                if (entries.size() == 1) {
                    LogEntry logEntry = entries.get(0);
                    connector.recordMeasurement(
                        logEntry.getMachineId(),
                        logEntry.getTimestamp(),
                        logEntry.getSetpoint(),
                        logEntry.getActualValue()
                    );
                } else {
                    // Otherwise, create a batch record
                    String startTime = entries.get(0).getTimestamp();
                    String endTime = entries.get(entries.size() - 1).getTimestamp();
                    
                    // Calculate batch hash
                    String batchData = entries.stream()
                        .map(LogEntry::toString)
                        .reduce("", (a, b) -> a + "\n" + b);
                    String batchHash = calculateSHA256(batchData);
                    
                    // Calculate anomaly score
                    float anomalyScore = entries.stream()
                        .map(LogEntry::getAnomalyScore)
                        .reduce(0.0f, Float::sum) / entries.size();
                    
                    // Record batch data
                    connector.recordBatchData(
                        machineId,
                        batchHash,
                        startTime,
                        endTime,
                        BigInteger.valueOf((long)(anomalyScore * 1000)) // Scale to integer
                    );
                }
            }
        } catch (Exception e) {
            System.err.println("Error processing batch: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Calculate SHA-256 hash of a string
     * @param input Input string
     * @return Hex string of hash
     */
    private String calculateSHA256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes("UTF-8"));
            StringBuilder hexString = new StringBuilder();
            
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            
            return hexString.toString();
        } catch (Exception e) {
            throw new RuntimeException("Could not calculate hash", e);
        }
    }
    
    /**
     * Log a measurement to the blockchain
     * @param machineId Machine ID
     * @param timestamp Timestamp string
     * @param setpoint Setpoint value
     * @param actualValue Actual measured value
     * @param anomalyScore Anomaly score (0-1)
     */
    public void logMeasurement(
            String machineId, 
            String timestamp, 
            long setpoint, 
            long actualValue,
            float anomalyScore) {
        
        // Check if queue is too large (back pressure)
        if (logQueue.size() >= maxQueueSize) {
            System.err.println("WARNING: Blockchain log queue full, dropping entry");
            return;
        }
        
        // Add entry to queue
        LogEntry entry = new LogEntry(
            machineId,
            timestamp,
            BigInteger.valueOf(setpoint),
            BigInteger.valueOf(actualValue),
            anomalyScore
        );
        
        logQueue.add(entry);
    }
    
    /**
     * Log an anomaly directly (high priority)
     * @param machineId Machine ID
     * @param timestamp Timestamp
     * @param anomalyScore Anomaly score
     * @param details Anomaly details
     */
    public void logAnomaly(String machineId, String timestamp, float anomalyScore, String details) {
        try {
            // For anomalies, bypass the queue and log immediately
            String anomalyHash = calculateSHA256(details);
            
            connector.recordBatchData(
                machineId,
                anomalyHash,
                timestamp,
                timestamp,
                BigInteger.valueOf((long)(anomalyScore * 1000))
            ).get(); // Wait for completion
            
            System.out.println("Anomaly logged to blockchain for machine " + machineId);
        } catch (Exception e) {
            System.err.println("Error logging anomaly: " + e.getMessage());
        }
    }
    
    /**
     * Shutdown the logger
     */
    public void shutdown() {
        isRunning.set(false);
        executorService.shutdown();
    }
    
    /**
     * Set batch size
     * @param batchSize New batch size
     */
    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }
    
    /**
     * Set maximum queue size
     * @param maxQueueSize New maximum queue size
     */
    public void setMaxQueueSize(int maxQueueSize) {
        this.maxQueueSize = maxQueueSize;
    }
    
    /**
     * Internal class representing a log entry
     */
    private static class LogEntry {
        private final String machineId;
        private final String timestamp;
        private final BigInteger setpoint;
        private final BigInteger actualValue;
        private final float anomalyScore;
        
        public LogEntry(
                String machineId, 
                String timestamp, 
                BigInteger setpoint, 
                BigInteger actualValue,
                float anomalyScore) {
            this.machineId = machineId;
            this.timestamp = timestamp;
            this.setpoint = setpoint;
            this.actualValue = actualValue;
            this.anomalyScore = anomalyScore;
        }
        
        public String getMachineId() { return machineId; }
        public String getTimestamp() { return timestamp; }
        public BigInteger getSetpoint() { return setpoint; }
        public BigInteger getActualValue() { return actualValue; }
        public float getAnomalyScore() { return anomalyScore; }
        
        @Override
        public String toString() {
            return String.format("%s,%s,%s,%s,%.4f", 
                machineId, timestamp, setpoint, actualValue, anomalyScore);
        }
    }
}