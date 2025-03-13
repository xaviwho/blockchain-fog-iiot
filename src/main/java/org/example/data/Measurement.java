package org.example.data;

/**
 * Measurement data (actual vs setpoint)
 */
public class Measurement {
    private int featureId;
    private float actual;
    private float setpoint;

    // Getters and setters
    public int getFeatureId() { return featureId; }
    public void setFeatureId(int featureId) { this.featureId = featureId; }

    public float getActual() { return actual; }
    public void setActual(float actual) { this.actual = actual; }

    public float getSetpoint() { return setpoint; }
    public void setSetpoint(float setpoint) { this.setpoint = setpoint; }
}