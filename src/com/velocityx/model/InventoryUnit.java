package com.velocityx.model;

import java.math.BigDecimal;

public class InventoryUnit {
    private long id;
    private long modelId;
    private String registrationNumber; // E.g., TS09EX1001
    private UnitStatus status;
    private BigDecimal customDailyRate; // Override or fallback to model base rate
    private int odometerKm;

    public InventoryUnit() {}

    public InventoryUnit(long id, long modelId, String registrationNumber, UnitStatus status, BigDecimal customDailyRate, int odometerKm) {
        this.id = id;
        this.modelId = modelId;
        this.registrationNumber = registrationNumber;
        this.status = status;
        this.customDailyRate = customDailyRate;
        this.odometerKm = odometerKm;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public long getModelId() { return modelId; }
    public void setModelId(long modelId) { this.modelId = modelId; }
    public String getRegistrationNumber() { return registrationNumber; }
    public void setRegistrationNumber(String registrationNumber) { this.registrationNumber = registrationNumber; }
    public UnitStatus getStatus() { return status; }
    public void setStatus(UnitStatus status) { this.status = status; }
    public BigDecimal getCustomDailyRate() { return customDailyRate; }
    public void setCustomDailyRate(BigDecimal customDailyRate) { this.customDailyRate = customDailyRate; }
    public int getOdometerKm() { return odometerKm; }
    public void setOdometerKm(int odometerKm) { this.odometerKm = odometerKm; }
}