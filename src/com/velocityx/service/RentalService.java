package com.velocityx.service;

import com.velocityx.db.DatabaseManager;
import com.velocityx.model.Booking;
import com.velocityx.model.BookingStatus;
import com.velocityx.model.InventoryUnit;
import com.velocityx.model.Invoice;
import com.velocityx.model.RentalQuote;
import com.velocityx.model.UnitStatus;
import com.velocityx.model.VehicleModel;
import com.velocityx.repository.BookingRepository;
import com.velocityx.repository.VehicleRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public class RentalService {
    private static final BigDecimal GST_RATE = new BigDecimal("0.18"); // 18% GST

    private final VehicleRepository vehicleRepo = new VehicleRepository();
    private final BookingRepository bookingRepo = new BookingRepository();

    public RentalQuote calculateQuote(long unitId, LocalDate start, LocalDate end) throws SQLException {
        if (end.isBefore(start)) {
            throw new IllegalArgumentException("Rental end date cannot precede start date.");
        }

        long days = ChronoUnit.DAYS.between(start, end);
        if (days <= 0) days = 1; // Minimum 1 day rental

        try (Connection conn = DatabaseManager.getConnection()) {
            InventoryUnit unit = vehicleRepo.findUnitById(conn, unitId)
                .orElseThrow(() -> new IllegalArgumentException("Physical inventory unit not found: " + unitId));
            VehicleModel model = vehicleRepo.findModelById(conn, unit.getModelId())
                .orElseThrow(() -> new IllegalArgumentException("Model specification not found for unit: " + unitId));

            BigDecimal rate = unit.getCustomDailyRate() != null && unit.getCustomDailyRate().compareTo(BigDecimal.ZERO) > 0
                    ? unit.getCustomDailyRate()
                    : model.getBaseDailyRate();

            BigDecimal subtotal = rate.multiply(BigDecimal.valueOf(days)).setScale(2, RoundingMode.HALF_UP);
            BigDecimal gst = subtotal.multiply(GST_RATE).setScale(2, RoundingMode.HALF_UP);
            BigDecimal total = subtotal.add(gst).setScale(2, RoundingMode.HALF_UP);

            return new RentalQuote(unit.getId(), unit.getRegistrationNumber(), model.getName(), start, end, days, rate, subtotal, gst, total);
        }
    }

    public synchronized Invoice executeBooking(long userId, long unitId, LocalDate start, LocalDate end) throws SQLException {
        RentalQuote quote = calculateQuote(unitId, start, end);

        try (Connection conn = DatabaseManager.getConnection()) {
            conn.setAutoCommit(false);
            try {
                // Verify vehicle status
                InventoryUnit unit = vehicleRepo.findUnitById(conn, unitId)
                    .orElseThrow(() -> new IllegalStateException("Unit does not exist."));

                if (unit.getStatus() != UnitStatus.AVAILABLE) {
                    throw new IllegalStateException("Unit " + unit.getRegistrationNumber() + " is currently " + unit.getStatus());
                }

                // Check calendar conflicts
                if (bookingRepo.hasDateConflict(conn, unitId, start, end)) {
                    throw new IllegalStateException("Selected dates conflict with an existing reservation for this unit.");
                }

                // Create booking record
                Booking booking = new Booking();
                booking.setUserId(userId);
                booking.setUnitId(unitId);
                booking.setStartDate(start);
                booking.setEndDate(end);
                booking.setStatus(BookingStatus.CONFIRMED);
                booking = bookingRepo.createBooking(conn, booking);

                // Create invoice record
                Invoice invoice = new Invoice();
                invoice.setBookingId(booking.getId());
                invoice.setTotalDays(quote.getTotalDays());
                invoice.setDailyRate(quote.getDailyRate());
                invoice.setSubtotal(quote.getSubtotal());
                invoice.setTaxAmount(quote.getGstAmount());
                invoice.setGrandTotal(quote.getGrandTotal());
                invoice.setPaymentStatus("PAID"); // Marked paid in this workflow
                invoice = bookingRepo.createInvoice(conn, invoice);

                // Update physical inventory unit status to RENTED
                vehicleRepo.updateUnitStatus(conn, unitId, UnitStatus.RENTED);

                conn.commit();
                return invoice;
            } catch (Exception e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    public synchronized void returnVehicle(long bookingId, int addedKm) throws SQLException {
        try (Connection conn = DatabaseManager.getConnection()) {
            conn.setAutoCommit(false);
            try {
                Booking booking = bookingRepo.findById(conn, bookingId)
                    .orElseThrow(() -> new IllegalArgumentException("Booking not found: " + bookingId));

                if (booking.getStatus() == BookingStatus.COMPLETED) {
                    throw new IllegalStateException("This rental has already been checked in and completed.");
                }

                // Update booking status
                bookingRepo.updateBookingStatus(conn, bookingId, BookingStatus.COMPLETED);

                // Release physical unit back to AVAILABLE and update odometer
                vehicleRepo.updateUnitStatus(conn, booking.getUnitId(), UnitStatus.AVAILABLE);
                vehicleRepo.updateUnitOdometer(conn, booking.getUnitId(), addedKm);

                conn.commit();
            } catch (Exception e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }
}