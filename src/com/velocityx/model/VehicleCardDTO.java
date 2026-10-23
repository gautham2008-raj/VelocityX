package com.velocityx.model;

import java.math.BigDecimal;

public class VehicleCardDTO {
    private long modelId;
    private String modelName;
    private Category category;
    private String brand;
    private int modelYear;
    private BigDecimal baseDailyRate;
    private String specsJson;
    private String imageUrl;
    private int totalUnits;
    private int availableUnits;

    public VehicleCardDTO() {}

    public VehicleCardDTO(long modelId, String modelName, Category category, String brand, int modelYear,
                          BigDecimal baseDailyRate, String specsJson, String imageUrl,
                          int totalUnits, int availableUnits) {
        this.modelId = modelId;
        this.modelName = modelName;
        this.category = category;
        this.brand = brand;
        this.modelYear = modelYear;
        this.baseDailyRate = baseDailyRate;
        this.specsJson = specsJson;
        this.imageUrl = imageUrl;
        this.totalUnits = totalUnits;
        this.availableUnits = availableUnits;
    }

    public long getModelId() { return modelId; }
    public String getModelName() { return modelName; }
    public Category getCategory() { return category; }
    public String getBrand() { return brand; }
    public int getModelYear() { return modelYear; }
    public BigDecimal getBaseDailyRate() { return baseDailyRate; }
    public String getSpecsJson() { return specsJson; }
    public String getImageUrl() { return imageUrl; }
    public int getTotalUnits() { return totalUnits; }
    public int getAvailableUnits() { return availableUnits; }
}