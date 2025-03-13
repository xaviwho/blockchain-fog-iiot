package org.example;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.example.data.FactoryDataPoint;
import org.example.data.MachineData;
import org.example.data.Measurement;

/**
 * Historical Data Processor
 * Analyzes historical manufacturing data for insights and model training
 */
public class HistoricalDataProcessor {
    private final DataLoader dataLoader;
    private final Map<Integer, List<Float>> stageOneDeviations;
    private final Map<Integer, List<Float>> stageTwoDeviations;
    private final Map<Integer, List<Float>> correlations;
    
    /**
     * Create a new historical data processor
     * @param dataLoader Data loader with historical data
     */
    public HistoricalDataProcessor(DataLoader dataLoader) {
        this.dataLoader = dataLoader;
        this.stageOneDeviations = new HashMap<>();
        this.stageTwoDeviations = new HashMap<>();
        this.correlations = new HashMap<>();
    }
    
    /**
     * Process all historical data
     */
    public void processHistoricalData() {
        System.out.println("Processing historical data...");
        
        List<FactoryDataPoint> allDataPoints = dataLoader.getDataPoints();
        
        if (allDataPoints.isEmpty()) {
            System.out.println("No historical data to process");
            return;
        }
        
        System.out.println("Processing " + allDataPoints.size() + " historical data points");
        
        // Calculate deviations for all data points
        calculateDeviations(allDataPoints);
        
        // Calculate correlations between input variables and deviations
        calculateCorrelations(allDataPoints);
        
        System.out.println("Historical data processing complete");
    }
    
    /**
     * Calculate deviations from setpoints for all data points
     * @param dataPoints List of data points
     */
    private void calculateDeviations(List<FactoryDataPoint> dataPoints) {
        // Initialize maps
        for (int i = 0; i < 15; i++) {
            stageOneDeviations.put(i, new ArrayList<>());
            stageTwoDeviations.put(i, new ArrayList<>());
        }
        
        // Calculate deviations
        for (FactoryDataPoint point : dataPoints) {
            // Stage one deviations
            for (Measurement m : point.getStage1Measurements()) {
                float deviation = m.getActual() - m.getSetpoint();
                stageOneDeviations.get(m.getFeatureId()).add(deviation);
            }
            
            // Stage two deviations
            for (Measurement m : point.getStage2Measurements()) {
                float deviation = m.getActual() - m.getSetpoint();
                stageTwoDeviations.get(m.getFeatureId()).add(deviation);
            }
        }
    }
    
    /**
     * Calculate correlations between process variables and measurement deviations
     * @param dataPoints List of data points
     */
    private void calculateCorrelations(List<FactoryDataPoint> dataPoints) {
        // This is a simplified correlation calculation
        // In a real system, you would use more sophisticated statistical methods
        
        // We'll calculate correlation between machine temperatures and measurement deviations
        for (int featureId = 0; featureId < 15; featureId++) {
            for (int machineId = 1; machineId <= 3; machineId++) {
                String key = machineId + "-exitTemp-" + featureId;
                List<Float> featureDeviations = stageOneDeviations.get(featureId);
                List<Float> exitTemps = new ArrayList<>();
                
                // Extract exit temperatures for this machine
                for (FactoryDataPoint point : dataPoints) {
                    MachineData machineData = point.getFirstStageMachineData().get(machineId);
                    if (machineData != null) {
                        exitTemps.add(machineData.getExitZoneTemperature());
                    }
                }
                
                // Calculate correlation if we have enough data
                if (featureDeviations.size() > 10 && exitTemps.size() == featureDeviations.size()) {
                    float correlation = calculatePearsonCorrelation(exitTemps, featureDeviations);
                    correlations.put(key.hashCode(), Arrays.asList(correlation));
                }
            }
        }
    }
    
    /**
     * Calculate Pearson correlation coefficient between two lists of values
     * @param list1 First list
     * @param list2 Second list
     * @return Correlation coefficient (-1 to 1)
     */
    private float calculatePearsonCorrelation(List<Float> list1, List<Float> list2) {
        int n = Math.min(list1.size(), list2.size());
        
        // Calculate means
        float mean1 = (float) list1.stream().mapToDouble(Float::doubleValue).average().orElse(0);
        float mean2 = (float) list2.stream().mapToDouble(Float::doubleValue).average().orElse(0);
        
        // Calculate correlation
        float sum = 0;
        float sum1Sq = 0;
        float sum2Sq = 0;
        
        for (int i = 0; i < n; i++) {
            float diff1 = list1.get(i) - mean1;
            float diff2 = list2.get(i) - mean2;
            
            sum += diff1 * diff2;
            sum1Sq += diff1 * diff1;
            sum2Sq += diff2 * diff2;
        }
        
        if (sum1Sq * sum2Sq == 0) {
            return 0; // Avoid division by zero
        }
        
        return sum / (float) Math.sqrt(sum1Sq * sum2Sq);
    }
    
    /**
     * Get statistics for stage one deviations
     * @return Map of feature ID to statistics
     */
    public Map<Integer, Map<String, Float>> getStageOneDeviationStats() {
        Map<Integer, Map<String, Float>> stats = new HashMap<>();
        
        for (int featureId : stageOneDeviations.keySet()) {
            List<Float> deviations = stageOneDeviations.get(featureId);
            
            if (deviations.isEmpty()) {
                continue;
            }
            
            // Calculate statistics
            float mean = (float) deviations.stream().mapToDouble(Float::doubleValue).average().orElse(0);
            float min = Collections.min(deviations);
            float max = Collections.max(deviations);
            
            // Calculate standard deviation
            float sumSquaredDiff = 0;
            for (float val : deviations) {
                float diff = val - mean;
                sumSquaredDiff += diff * diff;
            }
            float stdDev = (float) Math.sqrt(sumSquaredDiff / deviations.size());
            
            // Store statistics
            Map<String, Float> featureStats = new HashMap<>();
            featureStats.put("mean", mean);
            featureStats.put("min", min);
            featureStats.put("max", max);
            featureStats.put("stdDev", stdDev);
            
            stats.put(featureId, featureStats);
        }
        
        return stats;
    }
    
    /**
     * Get statistics for stage two deviations
     * @return Map of feature ID to statistics
     */
    public Map<Integer, Map<String, Float>> getStageTwoDeviationStats() {
        Map<Integer, Map<String, Float>> stats = new HashMap<>();
        
        for (int featureId : stageTwoDeviations.keySet()) {
            List<Float> deviations = stageTwoDeviations.get(featureId);
            
            if (deviations.isEmpty()) {
                continue;
            }
            
            // Calculate statistics
            float mean = (float) deviations.stream().mapToDouble(Float::doubleValue).average().orElse(0);
            float min = Collections.min(deviations);
            float max = Collections.max(deviations);
            
            // Calculate standard deviation
            float sumSquaredDiff = 0;
            for (float val : deviations) {
                float diff = val - mean;
                sumSquaredDiff += diff * diff;
            }
            float stdDev = (float) Math.sqrt(sumSquaredDiff / deviations.size());
            
            // Store statistics
            Map<String, Float> featureStats = new HashMap<>();
            featureStats.put("mean", mean);
            featureStats.put("min", min);
            featureStats.put("max", max);
            featureStats.put("stdDev", stdDev);
            
            stats.put(featureId, featureStats);
        }
        
        return stats;
    }
    
    /**
     * Get the most significant correlations between process variables and measurement deviations
     * @param threshold Correlation threshold (absolute value)
     * @return List of significant correlations
     */
    public List<Map<String, Object>> getSignificantCorrelations(float threshold) {
        List<Map<String, Object>> significantCorrelations = new ArrayList<>();
        
        for (Map.Entry<Integer, List<Float>> entry : correlations.entrySet()) {
            int key = entry.getKey();
            float correlation = entry.getValue().get(0);
            
            if (Math.abs(correlation) >= threshold) {
                Map<String, Object> correlationInfo = new HashMap<>();
                
                // Decode the key (machineId-exitTemp-featureId)
                String keyStr = String.valueOf(key);
                String[] parts = keyStr.split("-");
                
               correlationInfo.put("machineId", parts.length > 0 ? parts[0] : "unknown");
               correlationInfo.put("variable", parts.length > 1 ? parts[1] : "unknown");
               correlationInfo.put("featureId", parts.length > 2 ? parts[2] : "unknown");
               correlationInfo.put("correlation", correlation);
                
                significantCorrelations.add(correlationInfo);
            }
        }
        
        // Sort by absolute correlation value (descending)
        significantCorrelations.sort((a, b) -> {
            float corrA = Math.abs((Float) a.get("correlation"));
            float corrB = Math.abs((Float) b.get("correlation"));
            return Float.compare(corrB, corrA);
        });
        
        return significantCorrelations;
    }
    
    /**
     * Find features with the most variation from setpoint
     * @param count Number of features to return
     * @return List of feature IDs with the most variation
     */
    public List<Integer> getMostVariableFeatures(int count) {
        Map<Integer, Float> featureVariations = new HashMap<>();
        
        // Calculate average absolute deviation for each feature
        for (int featureId : stageOneDeviations.keySet()) {
            List<Float> deviations = stageOneDeviations.get(featureId);
            
            if (!deviations.isEmpty()) {
                float avgAbsDeviation = (float) deviations.stream()
                    .mapToDouble(d -> Math.abs(d))
                    .average()
                    .orElse(0);
                
                featureVariations.put(featureId, avgAbsDeviation);
            }
        }
        
        // Sort features by variation (descending)
        return featureVariations.entrySet().stream()
            .sorted(Map.Entry.<Integer, Float>comparingByValue().reversed())
            .limit(count)
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());
    }
    
    /**
     * Get recommendations for improving process control
     * @return List of recommendations
     */
    public List<String> getProcessRecommendations() {
        List<String> recommendations = new ArrayList<>();
        
        // Get features with most variation
        List<Integer> mostVariableFeatures = getMostVariableFeatures(3);
        
        if (!mostVariableFeatures.isEmpty()) {
            recommendations.add("Focus on improving control of features: " + 
                                mostVariableFeatures.stream()
                                    .map(String::valueOf)
                                    .collect(Collectors.joining(", ")));
        }
        
        // Get significant correlations
        List<Map<String, Object>> significantCorrs = getSignificantCorrelations(0.5f);
        
        for (Map<String, Object> corr : significantCorrs) {
            String recommendation = String.format(
                "Feature %s is strongly correlated (%.2f) with %s from Machine %s",
                corr.get("featureId"),
                corr.get("correlation"),
                corr.get("variable"),
                corr.get("machineId")
            );
            
            recommendations.add(recommendation);
        }
        
        // Add general recommendations
        recommendations.add("Consider implementing predictive maintenance for machines with highest anomaly rates");
        recommendations.add("Evaluate raw material quality impact on feature variations");
        
        return recommendations;
    }
}