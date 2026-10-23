package com.velocityx.repository;

import com.velocityx.db.DatabaseManager;
import com.velocityx.model.Booking;
import com.velocityx.model.BookingStatus;
import com.velocityx.model.Invoice;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class BookingRepository {

    public boolean hasDateConflict(Connection conn, long unitId, LocalDate start, LocalDate end) throws SQLException {
        String sql = """
            SELECT COUNT(*) FROM bookings
            WHERE unit_id = ?
              AND status IN ('CONFIRMED', 'ACTIVE')
              AND NOT (end_date < ? OR start_date > ?)
        """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, unitId);
            ps.setDate(2, Date.valueOf(start));
            ps.setDate(3, Date.valueOf(end));
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        }
        return false;
    }

    public Booking createBooking(Connection conn, Booking booking) throws SQLException {
        String sql = "INSERT INTO bookings (user_id, unit_id, start_date, end_date, status) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, booking.getUserId());
            ps.setLong(2, booking.getUnitId());
            ps.setDate(3, Date.valueOf(booking.getStartDate()));
            ps.setDate(4, Date.valueOf(booking.getEndDate()));
            ps.setString(5, booking.getStatus().name());
            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    booking.setId(rs.getLong(1));
                }
            }
        }
        return booking;
    }

    public Invoice createInvoice(Connection conn, Invoice invoice) throws SQLException {
        String sql = """
            INSERT INTO invoices (booking_id, total_days, daily_rate, subtotal, tax_amount, grand_total, payment_status)
            VALUES (?, ?, ?, ?, ?, ?, ?)
        """;
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, invoice.getBookingId());
            ps.setLong(2, invoice.getTotalDays());
            ps.setBigDecimal(3, invoice.getDailyRate());
            ps.setBigDecimal(4, invoice.getSubtotal());
            ps.setBigDecimal(5, invoice.getTaxAmount());
            ps.setBigDecimal(6, invoice.getGrandTotal());
            ps.setString(7, invoice.getPaymentStatus());
            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    invoice.setId(rs.getLong(1));
                }
            }
        }
        return invoice;
    }

    public Optional<Booking> findById(Connection conn, long bookingId) throws SQLException {
        String sql = "SELECT * FROM bookings WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, bookingId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(new Booking(
                        rs.getLong("id"),
                        rs.getLong("user_id"),
                        rs.getLong("unit_id"),
                        rs.getDate("start_date").toLocalDate(),
                        rs.getDate("end_date").toLocalDate(),
                        BookingStatus.valueOf(rs.getString("status")),
                        rs.getTimestamp("created_at").toLocalDateTime()
                    ));
                }
            }
        }
        return Optional.empty();
    }

    public void updateBookingStatus(Connection conn, long bookingId, BookingStatus newStatus) throws SQLException {
        String sql = "UPDATE bookings SET status = ? WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, newStatus.name());
            ps.setLong(2, bookingId);
            ps.executeUpdate();
        }
    }

    public List<Map<String, Object>> getUserBookings(long userId) throws SQLException {
        String sql = """
            SELECT b.id AS booking_id, b.start_date, b.end_date, b.status AS booking_status,
                   u.registration_number, m.name AS vehicle_name, m.brand, m.category,
                   i.daily_rate, i.subtotal, i.tax_amount, i.grand_total, i.payment_status
            FROM bookings b
            JOIN inventory_units u ON b.unit_id = u.id
            JOIN vehicle_models m ON u.model_id = m.id
            LEFT JOIN invoices i ON b.id = i.booking_id
            WHERE b.user_id = ?
            ORDER BY b.created_at DESC
        """;

        List<Map<String, Object>> list = new ArrayList<>();
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> map = new HashMap<>();
                    map.put("bookingId", rs.getLong("booking_id"));
                    map.put("startDate", rs.getString("start_date"));
                    map.put("endDate", rs.getString("end_date"));
                    map.put("status", rs.getString("booking_status"));
                    map.put("regNumber", rs.getString("registration_number"));
                    map.put("vehicleName", rs.getString("vehicle_name"));
                    map.put("brand", rs.getString("brand"));
                    map.put("category", rs.getString("category"));
                    map.put("dailyRate", rs.getBigDecimal("daily_rate"));
                    map.put("subtotal", rs.getBigDecimal("subtotal"));
                    map.put("taxAmount", rs.getBigDecimal("tax_amount"));
                    map.put("grandTotal", rs.getBigDecimal("grand_total"));
                    map.put("paymentStatus", rs.getString("payment_status"));
                    list.add(map);
                }
            }
        }
        return list;
    }

    public List<Map<String, Object>> getAllBookingsForAdmin() throws SQLException {
        String sql = """
            SELECT b.id AS booking_id, b.start_date, b.end_date, b.status AS booking_status,
                   usr.full_name AS customer_name, usr.email AS customer_email,
                   u.id AS unit_id, u.registration_number, m.name AS vehicle_name,
                   i.grand_total, i.payment_status
            FROM bookings b
            JOIN users usr ON b.user_id = usr.id
            JOIN inventory_units u ON b.unit_id = u.id
            JOIN vehicle_models m ON u.model_id = m.id
            LEFT JOIN invoices i ON b.id = i.booking_id
            ORDER BY b.created_at DESC
        """;

        List<Map<String, Object>> list = new ArrayList<>();
        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                Map<String, Object> map = new HashMap<>();
                map.put("bookingId", rs.getLong("booking_id"));
                map.put("customerName", rs.getString("customer_name"));
                map.put("customerEmail", rs.getString("customer_email"));
                map.put("unitId", rs.getLong("unit_id"));
                map.put("regNumber", rs.getString("registration_number"));
                map.put("vehicleName", rs.getString("vehicle_name"));
                map.put("startDate", rs.getString("start_date"));
                map.put("endDate", rs.getString("end_date"));
                map.put("status", rs.getString("booking_status"));
                map.put("grandTotal", rs.getBigDecimal("grand_total"));
                map.put("paymentStatus", rs.getString("payment_status"));
                list.add(map);
            }
        }
        return list;
    }

    public BigDecimal calculateTotalRevenue() throws SQLException {
        String sql = "SELECT SUM(grand_total) FROM invoices WHERE payment_status = 'PAID'";
        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                BigDecimal sum = rs.getBigDecimal(1);
                return sum != null ? sum : BigDecimal.ZERO;
            }
        }
        return BigDecimal.ZERO;
    }

    public int countActiveRentals() throws SQLException {
        String sql = "SELECT COUNT(*) FROM bookings WHERE status IN ('CONFIRMED', 'ACTIVE')";
        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) return rs.getInt(1);
        }
        return 0;
    }
}