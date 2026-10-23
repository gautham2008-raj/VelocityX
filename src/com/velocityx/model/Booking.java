package com.velocityx.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class Booking {
    private long id;
    private long userId;
    private long unitId;
    private LocalDate startDate;
    private LocalDate endDate;
    private BookingStatus status;
    private LocalDateTime createdAt;

    public Booking() {}

    public Booking(long id, long userId, long unitId, LocalDate startDate, LocalDate endDate, BookingStatus status, LocalDateTime createdAt) {
        this.id = id;
        this.userId = userId;
        this.unitId = unitId;
        this.startDate = startDate;
        this.endDate = endDate;
        this.status = status;
        this.createdAt = createdAt;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public long getUserId() { return userId; }
    public void setUserId(long userId) { this.userId = userId; }
    public long getUnitId() { return unitId; }
    public void setUnitId(long unitId) { this.unitId = unitId; }
    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
    public BookingStatus getStatus() { return status; }
    public void setStatus(BookingStatus status) { this.status = status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}