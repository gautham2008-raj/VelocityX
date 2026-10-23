package com.velocityx.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public class RentalQuote {
    private long unitId;
    private String registrationNumber;
    private String vehicleName;
    private LocalDate startDate;
    private LocalDate endDate;
    private long totalDays;
    private BigDecimal dailyRate;
    private BigDecimal subtotal;
    private BigDecimal gstAmount; // 18% GST
    private BigDecimal grandTotal;

    public RentalQuote(long unitId, String registrationNumber, String vehicleName, LocalDate startDate,
                       LocalDate endDate, long totalDays, BigDecimal dailyRate, BigDecimal subtotal,
                       BigDecimal gstAmount, BigDecimal grandTotal) {
        this.unitId = unitId;
        this.registrationNumber = registrationNumber;
        this.vehicleName = vehicleName;
        this.startDate = startDate;
        this.endDate = endDate;
        this.totalDays = totalDays;
        this.dailyRate = dailyRate;
        this.subtotal = subtotal;
        this.gstAmount = gstAmount;
        this.grandTotal = grandTotal;
    }

    public long getUnitId() { return unitId; }
    public String getRegistrationNumber() { return registrationNumber; }
    public String getVehicleName() { return vehicleName; }
    public LocalDate getStartDate() { return startDate; }
    public LocalDate getEndDate() { return endDate; }
    public long getTotalDays() { return totalDays; }
    public BigDecimal getDailyRate() { return dailyRate; }
    public BigDecimal getSubtotal() { return subtotal; }
    public BigDecimal getGstAmount() { return gstAmount; }
    public BigDecimal getGrandTotal() { return grandTotal; }
}