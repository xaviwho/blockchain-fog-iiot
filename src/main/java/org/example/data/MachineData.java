package org.example.data;

/**
 * Data for a first stage machine (Machines 1-3)
 */
public class MachineData {
    private float rawMaterialProperty1;
    private int rawMaterialProperty2;
    private float rawMaterialProperty3;
    private int rawMaterialProperty4;
    private float rawMaterialFeederParameter;
    private float zone1Temperature;
    private float zone2Temperature;
    private float motorAmperage;
    private float motorRPM;
    private float materialPressure;
    private float materialTemperature;
    private float exitZoneTemperature;

    // Getters and setters
    public float getRawMaterialProperty1() { return rawMaterialProperty1; }
    public void setRawMaterialProperty1(float rawMaterialProperty1) { this.rawMaterialProperty1 = rawMaterialProperty1; }

    public int getRawMaterialProperty2() { return rawMaterialProperty2; }
    public void setRawMaterialProperty2(int rawMaterialProperty2) { this.rawMaterialProperty2 = rawMaterialProperty2; }

    public float getRawMaterialProperty3() { return rawMaterialProperty3; }
    public void setRawMaterialProperty3(float rawMaterialProperty3) { this.rawMaterialProperty3 = rawMaterialProperty3; }

    public int getRawMaterialProperty4() { return rawMaterialProperty4; }
    public void setRawMaterialProperty4(int rawMaterialProperty4) { this.rawMaterialProperty4 = rawMaterialProperty4; }

    public float getRawMaterialFeederParameter() { return rawMaterialFeederParameter; }
    public void setRawMaterialFeederParameter(float rawMaterialFeederParameter) { 
        this.rawMaterialFeederParameter = rawMaterialFeederParameter; 
    }

    public float getZone1Temperature() { return zone1Temperature; }
    public void setZone1Temperature(float zone1Temperature) { this.zone1Temperature = zone1Temperature; }

    public float getZone2Temperature() { return zone2Temperature; }
    public void setZone2Temperature(float zone2Temperature) { this.zone2Temperature = zone2Temperature; }

    public float getMotorAmperage() { return motorAmperage; }
    public void setMotorAmperage(float motorAmperage) { this.motorAmperage = motorAmperage; }

    public float getMotorRPM() { return motorRPM; }
    public void setMotorRPM(float motorRPM) { this.motorRPM = motorRPM; }

    public float getMaterialPressure() { return materialPressure; }
    public void setMaterialPressure(float materialPressure) { this.materialPressure = materialPressure; }

    public float getMaterialTemperature() { return materialTemperature; }
    public void setMaterialTemperature(float materialTemperature) { this.materialTemperature = materialTemperature; }

    public float getExitZoneTemperature() { return exitZoneTemperature; }
    public void setExitZoneTemperature(float exitZoneTemperature) { this.exitZoneTemperature = exitZoneTemperature; }
}