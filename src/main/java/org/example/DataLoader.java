package org.example;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.example.data.CombinerData;
import org.example.data.FactoryDataPoint;
import org.example.data.MachineData;
import org.example.data.Measurement;
import org.example.data.SecondStageMachineData;

/**
 * DataLoader for Smart Factory continuous process data
 * Loads and prepares data for fog computing nodes and blockchain storage
 */
public class DataLoader {
    
    // Data storage
    private List<FactoryDataPoint> dataPoints;
    private Map<String, Integer> columnIndices;
    private String[] columnNames;
    
    /**
     * Constructor initializes the data loader
     */
    public DataLoader() {
        this.dataPoints = new ArrayList<>();
        this.columnIndices = new HashMap<>();
    }
    
    /**
     * Loads data from CSV file
     * @param filePath Path to the CSV file
     * @return true if loading was successful
     */
    public boolean loadData(String filePath) {
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            // Read header line
            String headerLine = reader.readLine();
            if (headerLine == null) {
                System.err.println("Empty file");
                return false;
            }
            
            // Process column headers
            columnNames = headerLine.split(",");
            for (int i = 0; i < columnNames.length; i++) {
                columnIndices.put(columnNames[i].trim(), i);
            }
            
            // Read data rows
            String line;
            while ((line = reader.readLine()) != null) {
                String[] values = line.split(",");
                if (values.length != columnNames.length) {
                    System.err.println("Inconsistent data in row: " + line);
                    continue;
                }
                
                FactoryDataPoint dataPoint = parseDataPoint(values);
                dataPoints.add(dataPoint);
            }
            
            System.out.println("Loaded " + dataPoints.size() + " data points");
            return true;
            
        } catch (IOException e) {
            System.err.println("Error loading data: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Parses a row of data into a FactoryDataPoint
     * @param values Array of string values from CSV
     * @return Parsed FactoryDataPoint
     */
    private FactoryDataPoint parseDataPoint(String[] values) {
        FactoryDataPoint point = new FactoryDataPoint();
        
        // Set timestamp
        point.setTimestamp(values[0]);
        
        // Set ambient conditions
        point.setAmbientHumidity(parseFloat(values[1]));
        point.setAmbientTemperature(parseFloat(values[2]));
        
        // First stage machine data
        Map<Integer, MachineData> machineData = new HashMap<>();
        for (int machineId = 1; machineId <= 3; machineId++) {
            MachineData machine = new MachineData();
            int baseIdx = (machineId - 1) * 12 + 3; // Calculate base index for each machine
            
            // Raw material properties
            machine.setRawMaterialProperty1(parseFloat(values[baseIdx]));
            machine.setRawMaterialProperty2(parseInt(values[baseIdx + 1]));
            machine.setRawMaterialProperty3(parseFloat(values[baseIdx + 2]));
            machine.setRawMaterialProperty4(parseInt(values[baseIdx + 3]));
            
            // Process variables
            machine.setRawMaterialFeederParameter(parseFloat(values[baseIdx + 4]));
            machine.setZone1Temperature(parseFloat(values[baseIdx + 5]));
            machine.setZone2Temperature(parseFloat(values[baseIdx + 6]));
            machine.setMotorAmperage(parseFloat(values[baseIdx + 7]));
            machine.setMotorRPM(parseFloat(values[baseIdx + 8]));
            machine.setMaterialPressure(parseFloat(values[baseIdx + 9]));
            machine.setMaterialTemperature(parseFloat(values[baseIdx + 10]));
            machine.setExitZoneTemperature(parseFloat(values[baseIdx + 11]));
            
            machineData.put(machineId, machine);
        }
        point.setFirstStageMachineData(machineData);
        
        // Combiner data
        CombinerData combiner = new CombinerData();
        combiner.setTemperature1(parseFloat(values[39]));
        combiner.setTemperature2(parseFloat(values[40]));
        combiner.setTemperature3(parseFloat(values[41]));
        point.setCombinerData(combiner);
        
        // Stage 1 output measurements - the primary outputs to control
        List<Measurement> stage1Measurements = new ArrayList<>();
        for (int i = 0; i < 15; i++) {
            Measurement measurement = new Measurement();
            int baseIdx = 42 + i * 2;
            measurement.setActual(parseFloat(values[baseIdx]));
            measurement.setSetpoint(parseFloat(values[baseIdx + 1]));
            measurement.setFeatureId(i);
            stage1Measurements.add(measurement);
        }
        point.setStage1Measurements(stage1Measurements);
        
        // Stage 2 machine data (Machine 4 and 5)
        Map<Integer, SecondStageMachineData> secondStageMachineData = new HashMap<>();
        
        // Machine 4
        SecondStageMachineData machine4 = new SecondStageMachineData();
        machine4.setTemperature1(parseFloat(values[72]));
        machine4.setTemperature2(parseFloat(values[73]));
        machine4.setPressure(parseFloat(values[74]));
        machine4.setTemperature3(parseFloat(values[75]));
        machine4.setTemperature4(parseFloat(values[76]));
        machine4.setTemperature5(parseFloat(values[77]));
        machine4.setExitTemperature(parseFloat(values[78]));
        secondStageMachineData.put(4, machine4);
        
        // Machine 5
        SecondStageMachineData machine5 = new SecondStageMachineData();
        machine5.setTemperature1(parseFloat(values[79]));
        machine5.setTemperature2(parseFloat(values[80]));
        machine5.setTemperature3(parseFloat(values[81]));
        machine5.setTemperature4(parseFloat(values[82]));
        machine5.setTemperature5(parseFloat(values[83]));
        machine5.setTemperature6(parseFloat(values[84]));
        machine5.setExitTemperature(parseFloat(values[85]));
        secondStageMachineData.put(5, machine5);
        
        point.setSecondStageMachineData(secondStageMachineData);
        
        // Stage 2 output measurements - secondary outputs
        List<Measurement> stage2Measurements = new ArrayList<>();
        for (int i = 0; i < 15; i++) {
            Measurement measurement = new Measurement();
            int baseIdx = 86 + i * 2;
            measurement.setActual(parseFloat(values[baseIdx]));
            measurement.setSetpoint(parseFloat(values[baseIdx + 1]));
            measurement.setFeatureId(i);
            stage2Measurements.add(measurement);
        }
        point.setStage2Measurements(stage2Measurements);
        
        return point;
    }
    
    /**
     * Safely parse float values
     */
    private float parseFloat(String value) {
        try {
            return Float.parseFloat(value);
        } catch (NumberFormatException e) {
            return 0.0f;
        }
    }
    
    /**
     * Safely parse integer values
     */
    private int parseInt(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
    
    /**
     * Get all loaded data points
     */
    public List<FactoryDataPoint> getDataPoints() {
        return dataPoints;
    }
    
    /**
     * Get a subset of data points within a time range
     * @param startIdx Start index
     * @param endIdx End index
     * @return List of data points in the range
     */
    public List<FactoryDataPoint> getDataPointsInRange(int startIdx, int endIdx) {
        if (startIdx < 0) startIdx = 0;
        if (endIdx >= dataPoints.size()) endIdx = dataPoints.size() - 1;
        
        return dataPoints.subList(startIdx, endIdx + 1);
    }
    
    /**
     * Calculate average deviation from setpoint for Stage 1 measurements
     * This can be used for anomaly detection
     * @return Map of measurement index to average deviation
     */
    public Map<Integer, Float> calculateStage1Deviations() {
        Map<Integer, Float> deviations = new HashMap<>();
        Map<Integer, Integer> counts = new HashMap<>();
        
        for (FactoryDataPoint point : dataPoints) {
            for (Measurement m : point.getStage1Measurements()) {
                int featureId = m.getFeatureId();
                float deviation = Math.abs(m.getActual() - m.getSetpoint());
                
                if (!deviations.containsKey(featureId)) {
                    deviations.put(featureId, deviation);
                    counts.put(featureId, 1);
                } else {
                    deviations.put(featureId, deviations.get(featureId) + deviation);
                    counts.put(featureId, counts.get(featureId) + 1);
                }
            }
        }
        
        // Calculate averages
        for (int featureId : deviations.keySet()) {
            deviations.put(featureId, deviations.get(featureId) / counts.get(featureId));
        }
        
        return deviations;
    }
    
    /**
     * Get data in blockchain-compatible format 
     * (for selected metrics that need to be recorded on-chain)
     * @param dataPoint The data point to convert
     * @return Map of key values for blockchain storage
     */
    public Map<String, Object> getBlockchainData(FactoryDataPoint dataPoint) {
        Map<String, Object> blockchainData = new HashMap<>();
        
        // Add timestamp
        blockchainData.put("timestamp", dataPoint.getTimestamp());
        
        // Add key process variables
        blockchainData.put("ambientTemperature", dataPoint.getAmbientTemperature());
        
        // Add machine states (simplified)
        for (int machineId = 1; machineId <= 3; machineId++) {
            MachineData machine = dataPoint.getFirstStageMachineData().get(machineId);
            blockchainData.put("machine" + machineId + "ExitTemp", machine.getExitZoneTemperature());
            blockchainData.put("machine" + machineId + "Pressure", machine.getMaterialPressure());
        }
        
        // Add stage 1 measurement deviations
        for (Measurement m : dataPoint.getStage1Measurements()) {
            float deviation = m.getActual() - m.getSetpoint();
            blockchainData.put("stage1Deviation" + m.getFeatureId(), deviation);
        }
        
        return blockchainData;
    }
}