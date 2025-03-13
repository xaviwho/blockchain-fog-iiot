package org.example;

import org.example.data.FactoryDataPoint;
import org.example.data.Measurement;

import java.util.*;
import java.util.stream.Collectors;

/**
 * AI-based anomaly detector for smart factory data
 * Uses statistical methods to detect anomalies in the manufacturing process
 */
public class AIAnomalyDetector {

    // Configuration parameters
    private final float deviationThreshold;
    private final int windowSize;
    private final float anomalyScoreThreshold;
    
    // Statistical tracking
    private final Map<Integer, List<Float>> featureHistory;
    private final Map<Integer, Float> featureMeans;
    private final Map<Integer, Float> featureStdDevs;
    
    /**
     * Create a new anomaly detector with default parameters
     */
    public AIAnomalyDetector() {
        this(3.0f, 100, 0.75f);
    }
    
    /**
     * Create a new anomaly detector with custom parameters
     * @param deviationThreshold Standard deviations to consider anomalous
     * @param windowSize Number of data points to use for baseline
     * @param anomalyScoreThreshold Threshold to flag as anomaly
     */
    public AIAnomalyDetector(float deviationThreshold, int windowSize, float anomalyScoreThreshold) {
        this.deviationThreshold = deviationThreshold;
        this.windowSize = windowSize;
        this.anomalyScoreThreshold = anomalyScoreThreshold;
        
        this.featureHistory = new HashMap<>();
        this.featureMeans = new HashMap<>();
        this.featureStdDevs = new HashMap<>();
    }
    
    /**
     * Initialize the detector with historical data
     * @param dataPoints List of historical data points
     */
    public void initializeWithHistory(List<FactoryDataPoint> dataPoints) {
        System.out.println("Initializing anomaly detector with " + dataPoints.size() + " historical data points");
        
        // Process each data point
        for (FactoryDataPoint point : dataPoints) {
            // We focus on Stage 1 measurements (primary goal is to predict these)
            for (Measurement measurement : point.getStage1Measurements()) {
                int featureId = measurement.getFeatureId();
                float deviation = measurement.getActual() - measurement.getSetpoint();
                
                // Initialize lists if needed
                featureHistory.putIfAbsent(featureId, new ArrayList<>());
                
                // Add deviation to history
                List<Float> history = featureHistory.get(featureId);
                history.add(deviation);
                
                // Keep only the last windowSize values
                if (history.size() > windowSize) {
                    history.remove(0);
                }
            }
        }
        
        // Calculate baseline statistics
        updateBaselines();
    }
    
    /**
     * Update the baseline statistics for all features
     */
    private void updateBaselines() {
        for (int featureId : featureHistory.keySet()) {
            List<Float> history = featureHistory.get(featureId);
            
            if (history.size() >= 10) { // Need at least 10 points for meaningful statistics
                // Calculate mean
                float sum = 0;
                for (float val : history) {
                    sum += val;
                }
                float mean = sum / history.size();
                featureMeans.put(featureId, mean);
                
                // Calculate standard deviation
                float sumSquaredDiff = 0;
                for (float val : history) {
                    float diff = val - mean;
                    sumSquaredDiff += diff * diff;
                }
                float stdDev = (float) Math.sqrt(sumSquaredDiff / history.size());
                featureStdDevs.put(featureId, stdDev);
            }
        }
    }
    
    /**
     * Process a new data point and detect anomalies
     * @param dataPoint The new data point to analyze
     * @return AnomalyResult with score and details
     */
    public AnomalyResult detectAnomalies(FactoryDataPoint dataPoint) {
        List<String> anomalies = new ArrayList<>();
        float totalScore = 0;
        int featureCount = 0;
        
        // Analyze Stage 1 measurements
        for (Measurement measurement : dataPoint.getStage1Measurements()) {
            int featureId = measurement.getFeatureId();
            
            // Skip if we don't have baseline for this feature
            if (!featureMeans.containsKey(featureId) || !featureStdDevs.containsKey(featureId)) {
                continue;
            }
            
            float deviation = measurement.getActual() - measurement.getSetpoint();
            float mean = featureMeans.get(featureId);
            float stdDev = featureStdDevs.get(featureId);
            
            // Skip features with very small standard deviation (to avoid division by near-zero)
            if (stdDev < 0.0001f) {
                continue;
            }
            
            // Calculate z-score
            float zScore = Math.abs(deviation - mean) / stdDev;
            
            // Calculate feature anomaly score (0-1)
            float featureScore = Math.min(zScore / deviationThreshold, 1.0f);
            totalScore += featureScore;
            featureCount++;
            
            // If this feature is anomalous, add it to the list
            if (featureScore > anomalyScoreThreshold) {
                String anomalyDetail = String.format(
                    "Feature %d: actual=%.2f, setpoint=%.2f, deviation=%.2f, z-score=%.2f", 
                    featureId, measurement.getActual(), measurement.getSetpoint(), deviation, zScore
                );
                anomalies.add(anomalyDetail);
            }
            
            // Update history
            featureHistory.get(featureId).add(deviation);
            if (featureHistory.get(featureId).size() > windowSize) {
                featureHistory.get(featureId).remove(0);
            }
        }
        
        // Calculate overall anomaly score
        float overallScore = featureCount > 0 ? totalScore / featureCount : 0;
        
        // Periodically update baselines
        if (Math.random() < 0.1) { // 10% chance to update on each data point
            updateBaselines();
        }
        
        return new AnomalyResult(
            overallScore,
            overallScore > anomalyScoreThreshold,
            anomalies,
            dataPoint.getTimestamp()
        );
    }
    
    /**
     * Get historical Z-scores for a feature
     * @param featureId The feature ID
     * @return List of recent Z-scores
     */
    public List<Float> getFeatureZScores(int featureId) {
        if (!featureHistory.containsKey(featureId) || 
            !featureMeans.containsKey(featureId) ||
            !featureStdDevs.containsKey(featureId)) {
            return Collections.emptyList();
        }
        
        float mean = featureMeans.get(featureId);
        float stdDev = featureStdDevs.get(featureId);
        
        if (stdDev < 0.0001f) {
            return Collections.emptyList();
        }
        
        // Calculate z-scores for historical values
        return featureHistory.get(featureId).stream()
            .map(val -> Math.abs(val - mean) / stdDev)
            .collect(Collectors.toList());
    }
    
    /**
     * Class to represent the result of anomaly detection
     */
    public static class AnomalyResult {
        private final float score;
        private final boolean isAnomaly;
        private final List<String> details;
        private final String timestamp;
        
        public AnomalyResult(float score, boolean isAnomaly, List<String> details, String timestamp) {
            this.score = score;
            this.isAnomaly = isAnomaly;
            this.details = details;
            this.timestamp = timestamp;
        }
        
        public float getScore() { return score; }
        public boolean isAnomaly() { return isAnomaly; }
        public List<String> getDetails() { return details; }
        public String getTimestamp() { return timestamp; }
        
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("Anomaly Score: ").append(String.format("%.4f", score))
              .append(", Is Anomaly: ").append(isAnomaly)
              .append(", Timestamp: ").append(timestamp)
              .append("\nDetails:\n");
            
            for (String detail : details) {
                sb.append("  - ").append(detail).append("\n");
            }
            
            return sb.toString();
        }
    }
}