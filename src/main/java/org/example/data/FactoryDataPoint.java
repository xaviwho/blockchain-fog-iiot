package org.example.data;

import java.util.List;
import java.util.Map;

/**
 * Main data point class representing a single measurement timestamp
 */
public class FactoryDataPoint {
    private String timestamp;
    private float ambientHumidity;
    private float ambientTemperature;
    private Map<Integer, MachineData> firstStageMachineData;
    private CombinerData combinerData;
    private List<Measurement> stage1Measurements;
    private Map<Integer, SecondStageMachineData> secondStageMachineData;
    private List<Measurement> stage2Measurements;

    // Getters and setters
    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }

    public float getAmbientHumidity() { return ambientHumidity; }
    public void setAmbientHumidity(float ambientHumidity) { this.ambientHumidity = ambientHumidity; }

    public float getAmbientTemperature() { return ambientTemperature; }
    public void setAmbientTemperature(float ambientTemperature) { this.ambientTemperature = ambientTemperature; }

    public Map<Integer, MachineData> getFirstStageMachineData() { return firstStageMachineData; }
    public void setFirstStageMachineData(Map<Integer, MachineData> firstStageMachineData) { 
        this.firstStageMachineData = firstStageMachineData; 
    }

    public CombinerData getCombinerData() { return combinerData; }
    public void setCombinerData(CombinerData combinerData) { this.combinerData = combinerData; }

    public List<Measurement> getStage1Measurements() { return stage1Measurements; }
    public void setStage1Measurements(List<Measurement> stage1Measurements) { 
        this.stage1Measurements = stage1Measurements; 
    }

    public Map<Integer, SecondStageMachineData> getSecondStageMachineData() { return secondStageMachineData; }
    public void setSecondStageMachineData(Map<Integer, SecondStageMachineData> secondStageMachineData) { 
        this.secondStageMachineData = secondStageMachineData; 
    }

    public List<Measurement> getStage2Measurements() { return stage2Measurements; }
    public void setStage2Measurements(List<Measurement> stage2Measurements) { 
        this.stage2Measurements = stage2Measurements; 
    }
}