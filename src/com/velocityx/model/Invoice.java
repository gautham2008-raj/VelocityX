package com.velocityx.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Invoice {
    private long id;
    private long bookingId;
    private long totalDays;
    private BigDecimal dailyRate;
    private BigDecimal subtotal;
    private BigDecimal taxAmount; // 18% GST
    private BigDecimal grandTotal;
    private String paymentStatus; // PAID, PENDING, REFUNDED
    private LocalDateTime issuedAt;

    public Invoice() {}

    public Invoice(long id, long bookingId, long totalDays, BigDecimal dailyRate, BigDecimal subtotal, BigDecimal taxAmount, BigDecimal grandTotal, String paymentStatus, LocalDateTime issuedAt) {
        this.id = id;
        this.bookingId = bookingId;
        this.totalDays = totalDays;
        this.dailyRate = dailyRate;
        this.subtotal = subtotal;
        this.taxAmount = taxAmount;
        this.grandTotal = grandTotal;
        this.paymentStatus = paymentStatus;
        this.issuedAt = issuedAt;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public long getBookingId() { return bookingId; }
    public void setBookingId(long bookingId) { this.bookingId = bookingId; }
    public long getTotalDays() { return totalDays; }
    public void setTotalDays(long totalDays) { this.totalDays = totalDays; }
    public BigDecimal getDailyRate() { return dailyRate; }
    public void setDailyRate(BigDecimal dailyRate) { this.dailyRate = dailyRate; }
    public BigDecimal getSubtotal() { return subtotal; }
    public void setSubtotal(BigDecimal subtotal) { this.subtotal = subtotal; }
    public BigDecimal getTaxAmount() { return taxAmount; }
    public void setTaxAmount(BigDecimal taxAmount) { this.taxAmount = taxAmount; }
    public BigDecimal getGrandTotal() { return grandTotal; }
    public void setGrandTotal(BigDecimal grandTotal) { this.grandTotal = grandTotal; }
    public String getPaymentStatus() { return paymentStatus; }
    public void setPaymentStatus(String paymentStatus) { this.paymentStatus = paymentStatus; }
    public LocalDateTime getIssuedAt() { return issuedAt; }
    public void setIssuedAt(LocalDateTime issuedAt) { this.issuedAt = issuedAt; }
}