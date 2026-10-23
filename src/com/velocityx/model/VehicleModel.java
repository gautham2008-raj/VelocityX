package com.velocityx.model;

import java.math.BigDecimal;

public class VehicleModel {
    private long id;
    private String name;
    private Category category;
    private String brand;
    private int modelYear;
    private BigDecimal baseDailyRate;
    private String specsJson;
    private String imageUrl;

    public VehicleModel() {}

    public VehicleModel(long id, String name, Category category, String brand, int modelYear, BigDecimal baseDailyRate, String specsJson, String imageUrl) {
        this.id = id;
        this.name = name;
        this.category = category;
        this.brand = brand;
        this.modelYear = modelYear;
        this.baseDailyRate = baseDailyRate;
        this.specsJson = specsJson;
        this.imageUrl = imageUrl;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Category getCategory() { return category; }
    public void setCategory(Category category) { this.category = category; }
    public String getBrand() { return brand; }
    public void setBrand(String brand) { this.brand = brand; }
    public int getModelYear() { return modelYear; }
    public void setModelYear(int modelYear) { this.modelYear = modelYear; }
    public BigDecimal getBaseDailyRate() { return baseDailyRate; }
    public void setBaseDailyRate(BigDecimal baseDailyRate) { this.baseDailyRate = baseDailyRate; }
    public String getSpecsJson() { return specsJson; }
    public void setSpecsJson(String specsJson) { this.specsJson = specsJson; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
}