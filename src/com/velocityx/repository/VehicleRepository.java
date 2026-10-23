package com.velocityx.repository;

import com.velocityx.db.DatabaseManager;
import com.velocityx.model.Category;
import com.velocityx.model.InventoryUnit;
import com.velocityx.model.UnitStatus;
import com.velocityx.model.VehicleCardDTO;
import com.velocityx.model.VehicleModel;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class VehicleRepository {

    public List<VehicleCardDTO> getFleetCards(Category categoryFilter) throws SQLException {
        StringBuilder sql = new StringBuilder("""
            SELECT m.id, m.name, m.category, m.brand, m.model_year, m.base_daily_rate, m.specs_json, m.image_url,
                   COUNT(u.id) AS total_units,
                   SUM(CASE WHEN u.status = 'AVAILABLE' THEN 1 ELSE 0 END) AS available_units
            FROM vehicle_models m
            LEFT JOIN inventory_units u ON m.id = u.model_id
        """);

        if (categoryFilter != null) {
            sql.append(" WHERE m.category = ? ");
        }

        sql.append(" GROUP BY m.id ORDER BY m.category, m.base_daily_rate ASC");

        List<VehicleCardDTO> results = new ArrayList<>();
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            if (categoryFilter != null) {
                ps.setString(1, categoryFilter.name());
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    results.add(new VehicleCardDTO(
                        rs.getLong("id"),
                        rs.getString("name"),
                        Category.valueOf(rs.getString("category")),
                        rs.getString("brand"),
                        rs.getInt("model_year"),
                        rs.getBigDecimal("base_daily_rate"),
                        rs.getString("specs_json"),
                        rs.getString("image_url"),
                        rs.getInt("total_units"),
                        rs.getInt("available_units")
                    ));
                }
            }
        }
        return results;
    }

    public List<InventoryUnit> getUnitsForModel(long modelId) throws SQLException {
        String sql = "SELECT * FROM inventory_units WHERE model_id = ? ORDER BY registration_number ASC";
        List<InventoryUnit> units = new ArrayList<>();
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, modelId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    units.add(new InventoryUnit(
                        rs.getLong("id"),
                        rs.getLong("model_id"),
                        rs.getString("registration_number"),
                        UnitStatus.valueOf(rs.getString("status")),
                        rs.getBigDecimal("custom_daily_rate"),
                        rs.getInt("odometer_km")
                    ));
                }
            }
        }
        return units;
    }

    public Optional<InventoryUnit> findUnitById(Connection conn, long unitId) throws SQLException {
        String sql = "SELECT * FROM inventory_units WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, unitId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(new InventoryUnit(
                        rs.getLong("id"),
                        rs.getLong("model_id"),
                        rs.getString("registration_number"),
                        UnitStatus.valueOf(rs.getString("status")),
                        rs.getBigDecimal("custom_daily_rate"),
                        rs.getInt("odometer_km")
                    ));
                }
            }
        }
        return Optional.empty();
    }

    public Optional<VehicleModel> findModelById(Connection conn, long modelId) throws SQLException {
        String sql = "SELECT * FROM vehicle_models WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, modelId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(new VehicleModel(
                        rs.getLong("id"),
                        rs.getString("name"),
                        Category.valueOf(rs.getString("category")),
                        rs.getString("brand"),
                        rs.getInt("model_year"),
                        rs.getBigDecimal("base_daily_rate"),
                        rs.getString("specs_json"),
                        rs.getString("image_url")
                    ));
                }
            }
        }
        return Optional.empty();
    }

    public void updateUnitStatus(Connection conn, long unitId, UnitStatus newStatus) throws SQLException {
        String sql = "UPDATE inventory_units SET status = ? WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, newStatus.name());
            ps.setLong(2, unitId);
            ps.executeUpdate();
        }
    }

    public void updateUnitOdometer(Connection conn, long unitId, int addedKm) throws SQLException {
        String sql = "UPDATE inventory_units SET odometer_km = odometer_km + ? WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, Math.max(0, addedKm));
            ps.setLong(2, unitId);
            ps.executeUpdate();
        }
    }

    public void addInventoryUnit(long modelId, String regNumber, BigDecimal dailyRate, int initialOdo) throws SQLException {
        String sql = "INSERT INTO inventory_units (model_id, registration_number, status, custom_daily_rate, odometer_km) VALUES (?, ?, 'AVAILABLE', ?, ?)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, modelId);
            ps.setString(2, regNumber.toUpperCase().trim());
            ps.setBigDecimal(3, dailyRate);
            ps.setInt(4, initialOdo);
            ps.executeUpdate();
        }
    }

    public void bulkAddInventoryUnits(long modelId, String prefix, int count, BigDecimal dailyRate) throws SQLException {
        String sql = "INSERT INTO inventory_units (model_id, registration_number, status, custom_daily_rate, odometer_km) VALUES (?, ?, 'AVAILABLE', ?, 0)";
        try (Connection conn = DatabaseManager.getConnection()) {
            conn.setAutoCommit(false);
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                for (int i = 1; i <= count; i++) {
                    String reg = String.format("%s-%04d", prefix.toUpperCase().trim(), i);
                    ps.setLong(1, modelId);
                    ps.setString(2, reg);
                    ps.setBigDecimal(3, dailyRate);
                    ps.addBatch();
                }
                ps.executeBatch();
                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    public int countTotalUnits() throws SQLException {
        String sql = "SELECT COUNT(*) FROM inventory_units";
        try (Connection conn = DatabaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) return rs.getInt(1);
        }
        return 0;
    }

    public int countUnitsByStatus(UnitStatus status) throws SQLException {
        String sql = "SELECT COUNT(*) FROM inventory_units WHERE status = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status.name());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        return 0;
    }
}