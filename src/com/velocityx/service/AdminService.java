package com.velocityx.service;

import com.velocityx.model.UnitStatus;
import com.velocityx.repository.BookingRepository;
import com.velocityx.repository.UserRepository;
import com.velocityx.repository.VehicleRepository;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AdminService {
    private final VehicleRepository vehicleRepo = new VehicleRepository();
    private final BookingRepository bookingRepo = new BookingRepository();
    private final UserRepository userRepo = new UserRepository();

    public Map<String, Object> getSystemMetrics() throws SQLException {
        Map<String, Object> metrics = new HashMap<>();

        int totalUnits = vehicleRepo.countTotalUnits();
        int availableUnits = vehicleRepo.countUnitsByStatus(UnitStatus.AVAILABLE);
        int rentedUnits = vehicleRepo.countUnitsByStatus(UnitStatus.RENTED);
        int maintenanceUnits = vehicleRepo.countUnitsByStatus(UnitStatus.MAINTENANCE);
        int reservedUnits = vehicleRepo.countUnitsByStatus(UnitStatus.RESERVED);

        int activeRentals = bookingRepo.countActiveRentals();
        int totalCustomers = userRepo.countTotalUsers();
        BigDecimal totalRevenue = bookingRepo.calculateTotalRevenue();

        metrics.put("totalUnits", totalUnits);
        metrics.put("availableUnits", availableUnits);
        metrics.put("rentedUnits", rentedUnits);
        metrics.put("maintenanceUnits", maintenanceUnits);
        metrics.put("reservedUnits", reservedUnits);
        metrics.put("activeRentals", activeRentals);
        metrics.put("totalUsers", totalCustomers);
        metrics.put("totalRevenueINR", totalRevenue);

        return metrics;
    }

    public List<Map<String, Object>> getAllSystemBookings() throws SQLException {
        return bookingRepo.getAllBookingsForAdmin();
    }

    public void provisionInventoryUnit(long modelId, String registrationNumber, BigDecimal customRate, int initialOdometer) throws SQLException {
        if (registrationNumber == null || registrationNumber.trim().isEmpty()) {
            throw new IllegalArgumentException("Registration identifier cannot be empty.");
        }
        if (customRate != null && customRate.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Daily rental rate cannot be negative.");
        }
        vehicleRepo.addInventoryUnit(modelId, registrationNumber.trim(), customRate, Math.max(0, initialOdometer));
    }

    public void bulkProvisionInventoryUnits(long modelId, String prefix, int count, BigDecimal customRate) throws SQLException {
        if (prefix == null || prefix.trim().isEmpty()) {
            throw new IllegalArgumentException("Registration prefix is required for batch provisioning.");
        }
        if (count <= 0 || count > 500) {
            throw new IllegalArgumentException("Batch unit count must be between 1 and 500.");
        }
        if (customRate != null && customRate.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Daily rental rate cannot be negative.");
        }
        vehicleRepo.bulkAddInventoryUnits(modelId, prefix.trim(), count, customRate);
    }
}