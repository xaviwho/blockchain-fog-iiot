package org.example;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.example.data.FactoryDataPoint;

/**
 * Main Simulation for Blockchain-Integrated Fog Computing
 * This class orchestrates the entire smart factory simulation
 */
public class MainSimulation {
    
    // Configuration
    private static final String DATA_FILE = "src/main/resources/continuous_factory_process.csv";
    private static final int DASHBOARD_PORT = 8080;
    private static final String EVALUATION_FILE = "system_evaluation.md";
    
    // Components
    private DataLoader dataLoader;
    private PureChainConnector blockchainConnector;
    private BlockchainLogger blockchainLogger;
    private FactoryFogTopology fogTopology;
    private AIAnomalyDetector anomalyDetector;
    private HistoricalDataProcessor historicalProcessor;
    private WebDashboard webDashboard;
    private SystemEvaluator systemEvaluator;
    
    /**
     * Initialize the simulation components
     */
    public void initialize() {
        // Set log level
        LoggingConfig.setLogLevel(LoggingConfig.LEVEL_INFO);
        LoggingConfig.info("MainSimulation", "Initializing Blockchain-Integrated Fog Computing Simulation...");
        
        // Initialize system evaluator
        systemEvaluator = new SystemEvaluator(EVALUATION_FILE);
        
        // Initialize data loader
        dataLoader = new DataLoader();
        boolean dataLoaded = dataLoader.loadData(DATA_FILE);
        
        if (!dataLoaded) {
            LoggingConfig.error("MainSimulation", "Failed to load data. Exiting.");
            System.exit(1);
        }
        
        // Initialize blockchain connector
        blockchainConnector = new PureChainConnector(BlockchainConfig.getRPC_URL(), BlockchainConfig.getPrivateKey());
        
        // Try to load the existing contract
        String contractAddress = BlockchainConfig.getContractAddress();
        boolean contractLoaded = blockchainConnector.loadContract(contractAddress);
        
        if (!contractLoaded) {
            LoggingConfig.warn("MainSimulation", "Failed to load existing contract at address: " + contractAddress);
            LoggingConfig.warn("MainSimulation", "Starting without blockchain integration. The simulation will run but data won't be recorded on-chain.");
        } else {
            LoggingConfig.info("MainSimulation", "Contract loaded successfully at: " + contractAddress);
        }
        
        // Initialize blockchain logger (even if contract failed, it might succeed later)
        blockchainLogger = new BlockchainLogger(blockchainConnector);
        
        // Initialize fog topology
        fogTopology = new FactoryFogTopology(dataLoader, blockchainLogger);
        fogTopology.initialize();
        
        // Initialize anomaly detector
        anomalyDetector = new AIAnomalyDetector();
        
        // Initialize historical data processor
        historicalProcessor = new HistoricalDataProcessor(dataLoader);
        
        // Initialize web dashboard
        try {
            webDashboard = new WebDashboard(DASHBOARD_PORT);
            webDashboard.setDataLoader(dataLoader);
            webDashboard.setBlockchainConnector(blockchainConnector);
            webDashboard.start();
            LoggingConfig.info("MainSimulation", "Web dashboard started at http://localhost:" + DASHBOARD_PORT);
        } catch (Exception e) {
            LoggingConfig.error("MainSimulation", "Failed to start web dashboard", e);
        }
        
        LoggingConfig.info("MainSimulation", "Simulation initialized successfully!");
    }
    
    /**
     * Run the simulation
     */
    public void runSimulation() {
        LoggingConfig.info("MainSimulation", "Starting simulation...");
        
        // Process historical data for insights
        historicalProcessor.processHistoricalData();
        
        // Get process recommendations
        List<String> recommendations = historicalProcessor.getProcessRecommendations();
        LoggingConfig.info("MainSimulation", "\nProcess Recommendations:");
        for (String recommendation : recommendations) {
            LoggingConfig.info("MainSimulation", "- " + recommendation);
        }
        
        // Initialize anomaly detector with historical data
        List<FactoryDataPoint> trainingData = dataLoader.getDataPointsInRange(0, 1000);
        anomalyDetector.initializeWithHistory(trainingData);
        
        // Start fog topology processing
        fogTopology.startProcessing();
        
        // Process data in batches (simulating real-time data flow)
        List<FactoryDataPoint> allData = dataLoader.getDataPoints();
        int totalDataPoints = allData.size();
        int batchSize = 100;
        int totalBatches = (totalDataPoints + batchSize - 1) / batchSize;
        
        LoggingConfig.info("MainSimulation", "\nProcessing " + totalDataPoints + " data points in " + totalBatches + " batches");
        
        for (int i = 0; i < totalBatches; i++) {
            int startIdx = i * batchSize;
            int endIdx = Math.min(startIdx + batchSize, totalDataPoints);
            
            List<FactoryDataPoint> batch = dataLoader.getDataPointsInRange(startIdx, endIdx - 1);
            
            LoggingConfig.debug("MainSimulation", "Processing batch " + (i + 1) + "/" + totalBatches + 
                               " (" + batch.size() + " data points)");
            
            long startTime = System.currentTimeMillis();
            
            // Process batch through fog topology
            fogTopology.processBatch(batch);
            
            long endTime = System.currentTimeMillis();
            long processingTime = endTime - startTime;
            
            // Record batch processing in evaluator
            for (FactoryDataPoint dataPoint : batch) {
                systemEvaluator.recordProcessedDataPoint(dataPoint, processingTime / batch.size());
                
                // Record for dashboard visualization
                if (webDashboard != null) {
                    webDashboard.recordMeasurement(dataPoint);
                }
            }
            
            // Check for anomalies using the anomaly detector
            for (FactoryDataPoint dataPoint : batch) {
                AIAnomalyDetector.AnomalyResult result = anomalyDetector.detectAnomalies(dataPoint);
                
                if (result.isAnomaly()) {
                    LoggingConfig.info("MainSimulation", "Anomaly detected: " + result);
                    systemEvaluator.recordAnomalyDetected();
                    
                    // Record for dashboard visualization
                    if (webDashboard != null) {
                        webDashboard.recordAnomaly(
                            "output", 
                            dataPoint.getTimestamp(), 
                            result.getScore(), 
                            result.getDetails()
                        );
                    }
                    
                    // Record on blockchain (if connected)
                    try {
                        blockchainLogger.logAnomaly(
                            "output",
                            dataPoint.getTimestamp(),
                            result.getScore(),
                            String.join("\n", result.getDetails())
                        );
                        systemEvaluator.recordBlockchainTransaction(true);
                        
                        // Record for dashboard visualization
                        if (webDashboard != null) {
                            webDashboard.recordBlockchainTransaction(
                                "Anomaly",
                                "tx-" + System.currentTimeMillis(),
                                "Anomaly detected with score " + result.getScore()
                            );
                        }
                    } catch (Exception e) {
                        LoggingConfig.error("MainSimulation", "Failed to log anomaly to blockchain", e);
                        systemEvaluator.recordBlockchainTransaction(false);
                    }
                }
            }
            
            // Simulate delay between batches (for demonstration)
            try {
                TimeUnit.MILLISECONDS.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            
            // Print progress every 10 batches
            if ((i + 1) % 10 == 0 || i == totalBatches - 1) {
                LoggingConfig.info("MainSimulation", "Processed " + (i + 1) + "/" + totalBatches + " batches");
                
                // Print current evaluation metrics
                Map<String, Object> metrics = systemEvaluator.getEvaluationReport();
                LoggingConfig.info("MainSimulation", String.format(
                    "Processing rate: %.2f points/sec, Anomalies: %d, Blockchain TXs: %d",
                    metrics.get("processingRatePerSecond"),
                    metrics.get("anomaliesDetected"),
                    metrics.get("blockchainTransactions")
                ));
            }
        }
        
        // Wait for blockchain processing to complete
        try {
            TimeUnit.SECONDS.sleep(5);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // Save evaluation report
        systemEvaluator.saveEvaluationReport();
        
        LoggingConfig.info("MainSimulation", "\nSimulation complete!");
        LoggingConfig.info("MainSimulation", "Evaluation report saved to " + EVALUATION_FILE);
        if (webDashboard != null) {
            LoggingConfig.info("MainSimulation", "Web dashboard available at http://localhost:" + DASHBOARD_PORT);
        }
    }
    
    /**
     * Shutdown the simulation components
     */
    public void shutdown() {
        LoggingConfig.info("MainSimulation", "Shutting down simulation...");
        
        // Stop web dashboard
        if (webDashboard != null) {
            webDashboard.stop();
        }
        
        // Shutdown components
        fogTopology.shutdown();
        blockchainLogger.shutdown();
        blockchainConnector.shutdown();
        
        LoggingConfig.info("MainSimulation", "Simulation shutdown complete");
    }
    
    /**
     * Main entry point
     */
    public static void main(String[] args) {
        MainSimulation simulation = new MainSimulation();
        
        try {
            // Initialize
            simulation.initialize();
            
            // Run simulation
            simulation.runSimulation();
        } catch (Exception e) {
            LoggingConfig.error("MainSimulation", "Error in simulation: " + e.getMessage(), e);
        } finally {
            // Shutdown
            simulation.shutdown();
        }
    }
}