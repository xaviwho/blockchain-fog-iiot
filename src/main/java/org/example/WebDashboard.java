package org.example;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;

import org.example.data.FactoryDataPoint;
import org.example.data.Measurement;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

/**
 * Web dashboard for visualizing factory data and anomalies
 */
public class WebDashboard {
    private HttpServer server;
    private final int port;
    private final Map<String, List<AnomalyRecord>> anomalyRecords = new ConcurrentHashMap<>();
    private final Map<String, List<MeasurementRecord>> measurementRecords = new ConcurrentHashMap<>();
    private final Map<String, String> blockchainRecords = new ConcurrentHashMap<>();
    
    // Dashboard state
    private DataLoader dataLoader;
    private PureChainConnector blockchainConnector;
    
    /**
     * Create a new web dashboard
     * @param port The port to listen on
     */
    public WebDashboard(int port) {
        this.port = port;
    }
    
    /**
     * Start the web dashboard
     * @throws IOException If an error occurs
     */
    public void start() throws IOException {
        LoggingConfig.info("WebDashboard", "Starting web dashboard on port " + port);
        server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/", new DashboardHandler());
        server.createContext("/api/anomalies", new AnomalyApiHandler());
        server.createContext("/api/measurements", new MeasurementApiHandler());
        server.createContext("/api/blockchain", new BlockchainApiHandler());
        server.setExecutor(Executors.newFixedThreadPool(10));
        server.start();
        LoggingConfig.info("WebDashboard", "Web dashboard started on http://localhost:" + port);
    }
    
    /**
     * Stop the web dashboard
     */
    public void stop() {
        if (server != null) {
            server.stop(0);
            LoggingConfig.info("WebDashboard", "Web dashboard stopped");
        }
    }
    
    /**
     * Record an anomaly
     * @param machineId Machine ID
     * @param timestamp Timestamp
     * @param anomalyScore Anomaly score
     * @param details Anomaly details
     */
    public void recordAnomaly(String machineId, String timestamp, float anomalyScore, List<String> details) {
        anomalyRecords.computeIfAbsent(machineId, k -> new ArrayList<>())
            .add(new AnomalyRecord(timestamp, anomalyScore, details));
        LoggingConfig.debug("WebDashboard", "Recorded anomaly for machine " + machineId);
    }
    
    /**
     * Record a measurement
     * @param dataPoint Factory data point
     */
    public void recordMeasurement(FactoryDataPoint dataPoint) {
        // Record stage 1 measurements
        for (Measurement m : dataPoint.getStage1Measurements()) {
            String featureId = "feature-" + m.getFeatureId();
            measurementRecords.computeIfAbsent(featureId, k -> new ArrayList<>())
                .add(new MeasurementRecord(
                    dataPoint.getTimestamp(),
                    m.getSetpoint(),
                    m.getActual(),
                    Math.abs(m.getActual() - m.getSetpoint())
                ));
        }
        LoggingConfig.trace("WebDashboard", "Recorded measurements for timestamp " + dataPoint.getTimestamp());
    }
    
    /**
     * Record a blockchain transaction
     * @param transactionType Type of transaction
     * @param transactionHash Transaction hash
     * @param details Transaction details
     */
    public void recordBlockchainTransaction(String transactionType, String transactionHash, String details) {
        blockchainRecords.put(transactionHash, transactionType + ": " + details);
        LoggingConfig.debug("WebDashboard", "Recorded blockchain transaction: " + transactionHash);
    }
    
    /**
     * Set the data loader
     * @param dataLoader Data loader
     */
    public void setDataLoader(DataLoader dataLoader) {
        this.dataLoader = dataLoader;
    }
    
    /**
     * Set the blockchain connector
     * @param blockchainConnector Blockchain connector
     */
    public void setBlockchainConnector(PureChainConnector blockchainConnector) {
        this.blockchainConnector = blockchainConnector;
    }
    
    /**
     * Handler for dashboard web page
     */
    private class DashboardHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String html = new String(Files.readAllBytes(Paths.get("dashboard.html")));
            exchange.getResponseHeaders().set("Content-Type", "text/html");
            exchange.sendResponseHeaders(200, html.length());
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(html.getBytes());
            }
        }
    }
    
    /**
     * Handler for anomaly API
     */
    private class AnomalyApiHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String response = buildJsonResponse(anomalyRecords);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.length());
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response.getBytes());
            }
        }
    }
    
    /**
     * Handler for measurement API
     */
    private class MeasurementApiHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String response = buildJsonResponse(measurementRecords);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.length());
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response.getBytes());
            }
        }
    }
    
    /**
     * Handler for blockchain API
     */
    private class BlockchainApiHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            StringBuilder json = new StringBuilder("{\"transactions\": [");
            boolean first = true;
            for (Map.Entry<String, String> entry : blockchainRecords.entrySet()) {
                if (!first) {
                    json.append(",");
                }
                first = false;
                json.append("{\"hash\":\"").append(entry.getKey())
                    .append("\",\"details\":\"").append(entry.getValue())
                    .append("\"}");
            }
            json.append("]}");
            
            String response = json.toString();
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.length());
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response.getBytes());
            }
        }
    }
    
    /**
     * Build a JSON response from a map of records
     * @param records Map of records
     * @return JSON string
     */
    private String buildJsonResponse(Map<String, ?> records) {
        StringBuilder json = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, ?> entry : records.entrySet()) {
            if (!first) {
                json.append(",");
            }
            first = false;
            json.append("\"").append(entry.getKey()).append("\":");
            if (entry.getValue() instanceof List) {
                json.append("[");
                List<?> list = (List<?>) entry.getValue();
                for (int i = 0; i < list.size(); i++) {
                    if (i > 0) {
                        json.append(",");
                    }
                    json.append(list.get(i).toString());
                }
                json.append("]");
            } else {
                json.append("\"").append(entry.getValue()).append("\"");
            }
        }
        json.append("}");
        return json.toString();
    }
    
    /**
     * Record class for anomalies
     */
    private static class AnomalyRecord {
        private final String timestamp;
        private final float score;
        private final List<String> details;
        
        public AnomalyRecord(String timestamp, float score, List<String> details) {
            this.timestamp = timestamp;
            this.score = score;
            this.details = details;
        }
        
        @Override
        public String toString() {
            StringBuilder json = new StringBuilder("{");
            json.append("\"timestamp\":\"").append(timestamp).append("\",");
            json.append("\"score\":").append(score).append(",");
            json.append("\"details\":[");
            for (int i = 0; i < details.size(); i++) {
                if (i > 0) {
                    json.append(",");
                }
                json.append("\"").append(details.get(i).replace("\"", "\\\"")).append("\"");
            }
            json.append("]}");
            return json.toString();
        }
    }
    
    /**
     * Record class for measurements
     */
    private static class MeasurementRecord {
        private final String timestamp;
        private final float setpoint;
        private final float actual;
        private final float deviation;
        
        public MeasurementRecord(String timestamp, float setpoint, float actual, float deviation) {
            this.timestamp = timestamp;
            this.setpoint = setpoint;
            this.actual = actual;
            this.deviation = deviation;
        }
        
        @Override
        public String toString() {
            return "{" +
                "\"timestamp\":\"" + timestamp + "\"," +
                "\"setpoint\":" + setpoint + "," +
                "\"actual\":" + actual + "," +
                "\"deviation\":" + deviation +
                "}";
        }
    }
}