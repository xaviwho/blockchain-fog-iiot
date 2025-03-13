package org.example;

import org.example.data.CombinerData;
import org.example.data.FactoryDataPoint;
import org.example.data.MachineData;
import org.example.data.Measurement;
import org.example.data.SecondStageMachineData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Factory Fog Topology
 * Manages the fog computing nodes for processing factory data
 */
public class FactoryFogTopology {
    private final Map<String, FogNode> fogNodes;
    private final DataLoader dataLoader;
    private final BlockchainLogger blockchainLogger;
    
    private final ExecutorService executorService;
    private final AtomicBoolean isRunning;
    
    /**
     * Create a new fog topology
     * @param dataLoader Data loader for factory data
     * @param blockchainLogger Blockchain logger
     */
    public FactoryFogTopology(DataLoader dataLoader, BlockchainLogger blockchainLogger) {
        this.fogNodes = new HashMap<>();
        this.dataLoader = dataLoader;
        this.blockchainLogger = blockchainLogger;
        this.executorService = Executors.newCachedThreadPool();
        this.isRunning = new AtomicBoolean(false);
    }
    
    /**
     * Initialize the fog topology with machine nodes
     */
    public void initialize() {
        System.out.println("Initializing fog topology...");
        
        // Create fog nodes for each first-stage machine
        for (int machineId = 1; machineId <= 3; machineId++) {
            String nodeId = "machine-" + machineId;
            FogNode node = new FogNode(nodeId, blockchainLogger);
            fogNodes.put(nodeId, node);
            System.out.println("Created fog node: " + nodeId);
        }
        
        // Create fog node for combiner
        FogNode combinerNode = new FogNode("combiner", blockchainLogger);
        fogNodes.put("combiner", combinerNode);
        System.out.println("Created fog node: combiner");
        
        // Create fog nodes for second-stage machines
        for (int machineId = 4; machineId <= 5; machineId++) {
            String nodeId = "machine-" + machineId;
            FogNode node = new FogNode(nodeId, blockchainLogger);
            fogNodes.put(nodeId, node);
            System.out.println("Created fog node: " + nodeId);
        }
        
        // Create fog node for final output processing
        FogNode outputNode = new FogNode("output", blockchainLogger);
        fogNodes.put("output", outputNode);
        System.out.println("Created fog node: output");
    }
    
    /**
     * Start processing data with fog nodes
     */
    public void startProcessing() {
        if (isRunning.get()) {
            System.out.println("Fog topology is already running");
            return;
        }
        
        System.out.println("Starting fog topology processing...");
        isRunning.set(true);
        
        // Start each fog node in its own thread
        for (FogNode node : fogNodes.values()) {
            executorService.submit(() -> {
                try {
                    node.start();
                } catch (Exception e) {
                    System.err.println("Error in fog node " + node.getNodeId() + ": " + e.getMessage());
                    e.printStackTrace();
                }
            });
        }
    }
    
    /**
     * Process a batch of data points through the fog topology
     * @param dataPoints List of data points to process
     */
    public void processBatch(List<FactoryDataPoint> dataPoints) {
        if (!isRunning.get()) {
            System.out.println("Fog topology is not running, starting...");
            startProcessing();
        }
        
        System.out.println("Processing batch of " + dataPoints.size() + " data points");
        
        // Process each data point
        for (FactoryDataPoint dataPoint : dataPoints) {
            // Distribute data to first-stage machine nodes
            for (int machineId = 1; machineId <= 3; machineId++) {
                String nodeId = "machine-" + machineId;
                FogNode node = fogNodes.get(nodeId);
                
                if (node != null && dataPoint.getFirstStageMachineData().containsKey(machineId)) {
                    // Clone and extract just the relevant machine data to minimize network traffic
                    FogDataPacket packet = new FogDataPacket(
                        dataPoint.getTimestamp(),
                        nodeId,
                        extractMachineData(dataPoint, machineId)
                    );
                    
                    node.receiveData(packet);
                }
            }
            
            // Send to combiner node
            FogNode combinerNode = fogNodes.get("combiner");
            if (combinerNode != null) {
                // Create packet with combiner data and stage 1 measurements
                FogDataPacket packet = new FogDataPacket(
                    dataPoint.getTimestamp(),
                    "combiner",
                    extractCombinerData(dataPoint)
                );
                
                combinerNode.receiveData(packet);
            }
            
            // Distribute to second-stage machine nodes
            for (int machineId = 4; machineId <= 5; machineId++) {
                String nodeId = "machine-" + machineId;
                FogNode node = fogNodes.get(nodeId);
                
                if (node != null && dataPoint.getSecondStageMachineData().containsKey(machineId)) {
                    // Create packet with second stage machine data
                    FogDataPacket packet = new FogDataPacket(
                        dataPoint.getTimestamp(),
                        nodeId,
                        extractSecondStageMachineData(dataPoint, machineId)
                    );
                    
                    node.receiveData(packet);
                }
            }
            
            // Send to output node for final processing
            FogNode outputNode = fogNodes.get("output");
            if (outputNode != null) {
                // Create packet with all measurement data
                FogDataPacket packet = new FogDataPacket(
                    dataPoint.getTimestamp(),
                    "output",
                    extractOutputData(dataPoint)
                );
                
                outputNode.receiveData(packet);
            }
        }
    }
    
    /**
     * Extract data relevant for a first-stage machine
     */
    private Map<String, Object> extractMachineData(FactoryDataPoint dataPoint, int machineId) {
        Map<String, Object> data = new HashMap<>();
        
        // Add ambient conditions
        data.put("ambientTemperature", dataPoint.getAmbientTemperature());
        data.put("ambientHumidity", dataPoint.getAmbientHumidity());
        
        // Add machine data
        MachineData machineData = dataPoint.getFirstStageMachineData().get(machineId);
        data.put("rawMaterialProperty1", machineData.getRawMaterialProperty1());
        data.put("rawMaterialProperty2", machineData.getRawMaterialProperty2());
        data.put("rawMaterialProperty3", machineData.getRawMaterialProperty3());
        data.put("rawMaterialProperty4", machineData.getRawMaterialProperty4());
        data.put("rawMaterialFeederParameter", machineData.getRawMaterialFeederParameter());
        data.put("zone1Temperature", machineData.getZone1Temperature());
        data.put("zone2Temperature", machineData.getZone2Temperature());
        data.put("motorAmperage", machineData.getMotorAmperage());
        data.put("motorRPM", machineData.getMotorRPM());
        data.put("materialPressure", machineData.getMaterialPressure());
        data.put("materialTemperature", machineData.getMaterialTemperature());
        data.put("exitZoneTemperature", machineData.getExitZoneTemperature());
        
        return data;
    }
    
    /**
     * Extract data relevant for the combiner
     */
    private Map<String, Object> extractCombinerData(FactoryDataPoint dataPoint) {
        Map<String, Object> data = new HashMap<>();
        
        // Add combiner data
        CombinerData combinerData = dataPoint.getCombinerData();
        data.put("temperature1", combinerData.getTemperature1());
        data.put("temperature2", combinerData.getTemperature2());
        data.put("temperature3", combinerData.getTemperature3());
        
        // Add stage 1 measurements
        List<Map<String, Object>> measurements = new ArrayList<>();
        for (Measurement m : dataPoint.getStage1Measurements()) {
            Map<String, Object> measurement = new HashMap<>();
            measurement.put("featureId", m.getFeatureId());
            measurement.put("actual", m.getActual());
            measurement.put("setpoint", m.getSetpoint());
            measurements.add(measurement);
        }
        data.put("stage1Measurements", measurements);
        
        return data;
    }
    
    /**
     * Extract data relevant for a second-stage machine
     */
    private Map<String, Object> extractSecondStageMachineData(FactoryDataPoint dataPoint, int machineId) {
        Map<String, Object> data = new HashMap<>();
        
        // Add machine data
        SecondStageMachineData machineData = dataPoint.getSecondStageMachineData().get(machineId);
        data.put("temperature1", machineData.getTemperature1());
        data.put("temperature2", machineData.getTemperature2());
        data.put("temperature3", machineData.getTemperature3());
        data.put("temperature4", machineData.getTemperature4());
        data.put("temperature5", machineData.getTemperature5());
        
        if (machineId == 5) {
            data.put("temperature6", machineData.getTemperature6());
        } else {
            data.put("pressure", machineData.getPressure());
        }
        
        data.put("exitTemperature", machineData.getExitTemperature());
        
        return data;
    }
    
    /**
     * Extract data relevant for the output node
     */
    private Map<String, Object> extractOutputData(FactoryDataPoint dataPoint) {
        Map<String, Object> data = new HashMap<>();
        
        // Add stage 1 measurements
        List<Map<String, Object>> stage1Measurements = new ArrayList<>();
        for (Measurement m : dataPoint.getStage1Measurements()) {
            Map<String, Object> measurement = new HashMap<>();
            measurement.put("featureId", m.getFeatureId());
            measurement.put("actual", m.getActual());
            measurement.put("setpoint", m.getSetpoint());
            stage1Measurements.add(measurement);
        }
        data.put("stage1Measurements", stage1Measurements);
        
        // Add stage 2 measurements
        List<Map<String, Object>> stage2Measurements = new ArrayList<>();
        for (Measurement m : dataPoint.getStage2Measurements()) {
            Map<String, Object> measurement = new HashMap<>();
            measurement.put("featureId", m.getFeatureId());
            measurement.put("actual", m.getActual());
            measurement.put("setpoint", m.getSetpoint());
            stage2Measurements.add(measurement);
        }
        data.put("stage2Measurements", stage2Measurements);
        
        return data;
    }
    
    /**
     * Stop processing
     */
    public void stopProcessing() {
        System.out.println("Stopping fog topology processing...");
        isRunning.set(false);
        
        // Stop all fog nodes
        for (FogNode node : fogNodes.values()) {
            node.stop();
        }
    }
    
    /**
     * Shutdown the fog topology
     */
    public void shutdown() {
        stopProcessing();
        executorService.shutdown();
        System.out.println("Fog topology shut down");
    }
    
    /**
     * Get a fog node by ID
     * @param nodeId Node ID
     * @return FogNode or null if not found
     */
    public FogNode getNode(String nodeId) {
        return fogNodes.get(nodeId);
    }
    
    /**
     * Get all fog nodes
     * @return Map of node IDs to nodes
     */
    public Map<String, FogNode> getAllNodes() {
        return new HashMap<>(fogNodes);
    }
    
    /**
     * Inner class representing a fog computing node
     */
    public static class FogNode {
        private final String nodeId;
        private final BlockchainLogger blockchainLogger;
        private final List<FogDataPacket> dataQueue;
        private final AIAnomalyDetector anomalyDetector;
        private final AtomicBoolean isRunning;
        
        /**
         * Create a new fog node
         * @param nodeId Node ID
         * @param blockchainLogger Blockchain logger
         */
        public FogNode(String nodeId, BlockchainLogger blockchainLogger) {
            this.nodeId = nodeId;
            this.blockchainLogger = blockchainLogger;
            this.dataQueue = new ArrayList<>();
            this.anomalyDetector = new AIAnomalyDetector();
            this.isRunning = new AtomicBoolean(false);
        }
        
        /**
         * Start processing data
         */
        public void start() {
            isRunning.set(true);
            System.out.println("Fog node " + nodeId + " started");
        }
        
        /**
         * Stop processing data
         */
        public void stop() {
            isRunning.set(false);
            System.out.println("Fog node " + nodeId + " stopped");
        }
        
        /**
         * Get the node ID
         * @return Node ID
         */
        public String getNodeId() {
            return nodeId;
        }
        
        /**
         * Receive data for processing
         * @param packet Data packet
         */
        public void receiveData(FogDataPacket packet) {
            // Process data immediately
            processDataPacket(packet);
            
            // Also queue for batch processing
            synchronized (dataQueue) {
                dataQueue.add(packet);
                
                // Keep queue size reasonable
                if (dataQueue.size() > 1000) {
                    dataQueue.remove(0);
                }
            }
        }
        
        /**
         * Process a data packet
         * @param packet Data packet
         */
        private void processDataPacket(FogDataPacket packet) {
            try {
                // For output node, check for anomalies
                if (nodeId.equals("output")) {
                    processOutputNodeData(packet);
                } 
                // For machine nodes, log to blockchain
                else if (nodeId.startsWith("machine-")) {
                    processMachineNodeData(packet);
                }
                // For combiner node, process stage 1 measurements
                else if (nodeId.equals("combiner")) {
                    processCombinerNodeData(packet);
                }
            } catch (Exception e) {
                System.err.println("Error processing data in node " + nodeId + ": " + e.getMessage());
                e.printStackTrace();
            }
        }
        
        /**
         * Process data for an output node
         */
        private void processOutputNodeData(FogDataPacket packet) {
            // Extract measurements
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> stage1Measurements =
                (List<Map<String, Object>>) packet.getData().get("stage1Measurements");
                
            if (stage1Measurements != null && !stage1Measurements.isEmpty()) {
                // Build a pseudo data point for anomaly detection
                List<Measurement> measurements = new ArrayList<>();
                
                for (Map<String, Object> m : stage1Measurements) {
                    Measurement measurement = new Measurement();
                    measurement.setFeatureId((Integer) m.get("featureId"));
                    measurement.setActual(((Number) m.get("actual")).floatValue());
                    measurement.setSetpoint(((Number) m.get("setpoint")).floatValue());
                    measurements.add(measurement);
                }
                
                // Create a minimal data point for anomaly detection
                FactoryDataPoint dataPoint = new FactoryDataPoint();
                dataPoint.setTimestamp(packet.getTimestamp());
                dataPoint.setStage1Measurements(measurements);
                
                // Detect anomalies
                AIAnomalyDetector.AnomalyResult result = anomalyDetector.detectAnomalies(dataPoint);
                
                // If anomaly detected, log to blockchain
                if (result.isAnomaly()) {
                    System.out.println("Anomaly detected: " + result);
                    
                    // Log to blockchain (high priority)
                    blockchainLogger.logAnomaly(
                        nodeId,
                        packet.getTimestamp(),
                        result.getScore(),
                        String.join("\n", result.getDetails())
                    );
                }
            }
        }
        
        /**
         * Process data for a machine node
         */
        private void processMachineNodeData(FogDataPacket packet) {
            // For demonstration, just log a key measurement to the blockchain
            if (nodeId.startsWith("machine-")) {
                int machineId = Integer.parseInt(nodeId.split("-")[1]);
                
                // For first stage machines
                if (machineId <= 3) {
                    // Log exit temperature
                    float exitTemp = ((Number) packet.getData().get("exitZoneTemperature")).floatValue();
                    
                    blockchainLogger.logMeasurement(
                        nodeId,
                        packet.getTimestamp(),
                        0, // No setpoint for this
                        (long) (exitTemp * 100), // Scale to integer
                        0.0f // No anomaly score yet
                    );
                }
                // For second stage machines
                else {
                    // Log exit temperature
                    float exitTemp = ((Number) packet.getData().get("exitTemperature")).floatValue();
                    
                    blockchainLogger.logMeasurement(
                        nodeId,
                        packet.getTimestamp(),
                        0, // No setpoint for this
                        (long) (exitTemp * 100), // Scale to integer
                        0.0f // No anomaly score yet
                    );
                }
            }
        }
        
        /**
         * Process data for the combiner node
         */
        private void processCombinerNodeData(FogDataPacket packet) {
            // Log combiner temperature to blockchain
            float temp3 = ((Number) packet.getData().get("temperature3")).floatValue();
            
            blockchainLogger.logMeasurement(
                nodeId,
                packet.getTimestamp(),
                0, // No setpoint for this
                (long) (temp3 * 100), // Scale to integer
                0.0f // No anomaly score yet
            );
            
            // Process stage 1 measurements
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> stage1Measurements =
                (List<Map<String, Object>>) packet.getData().get("stage1Measurements");
                
            if (stage1Measurements != null) {
                for (Map<String, Object> m : stage1Measurements) {
                    int featureId = (Integer) m.get("featureId");
                    float actual = ((Number) m.get("actual")).floatValue();
                    float setpoint = ((Number) m.get("setpoint")).floatValue();
                    
                    // Calculate simple anomaly score as normalized deviation
                    float deviation = Math.abs(actual - setpoint);
                    float normalizedScore = Math.min(deviation / 5.0f, 1.0f); // Max score at 5mm deviation
                    
                    // Log significant deviations to blockchain
                    if (normalizedScore > 0.5f) {
                        blockchainLogger.logMeasurement(
                            "feature-" + featureId,
                            packet.getTimestamp(),
                            (long) (setpoint * 100),
                            (long) (actual * 100),
                            normalizedScore
                        );
                    }
                }
            }
        }
    }
    
    /**
     * Inner class representing a data packet sent between fog nodes
     */
    public static class FogDataPacket {
        private final String timestamp;
        private final String targetNodeId;
        private final Map<String, Object> data;
        
        /**
         * Create a new data packet
         * @param timestamp Timestamp
         * @param targetNodeId Target node ID
         * @param data Data payload
         */
        public FogDataPacket(String timestamp, String targetNodeId, Map<String, Object> data) {
            this.timestamp = timestamp;
            this.targetNodeId = targetNodeId;
            this.data = data;
        }
        
        /**
         * Get the timestamp
         * @return Timestamp
         */
        public String getTimestamp() {
            return timestamp;
        }
        
        /**
         * Get the target node ID
         * @return Target node ID
         */
        public String getTargetNodeId() {
            return targetNodeId;
        }
        
        /**
         * Get the data payload
         * @return Data payload
         */
        public Map<String, Object> getData() {
            return data;
        }
    }
}