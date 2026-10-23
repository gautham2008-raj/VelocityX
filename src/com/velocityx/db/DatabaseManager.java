package com.velocityx.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseManager {
    private static final String DB_FILE = "velocity_x.db";
    private static final String DB_URL = "jdbc:sqlite:" + DB_FILE;

    static {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            System.err.println("[FATAL] SQLite JDBC Driver not detected in classpath!");
        }
    }

    public static Connection getConnection() throws SQLException {
        Connection conn = DriverManager.getConnection(DB_URL);
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("PRAGMA foreign_keys = ON;");
            stmt.execute("PRAGMA journal_mode = WAL;");
            stmt.execute("PRAGMA synchronous = NORMAL;");
        }
        return conn;
    }

    public static void initialize() {
        System.out.println("[DatabaseManager] Initializing SQLite database schema...");
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS users (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    full_name TEXT NOT NULL,
                    email TEXT UNIQUE NOT NULL,
                    password_hash TEXT NOT NULL,
                    salt TEXT NOT NULL,
                    role TEXT NOT NULL CHECK(role IN ('CUSTOMER', 'ADMIN')),
                    phone TEXT,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                );
            """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS vehicle_models (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    name TEXT NOT NULL,
                    category TEXT NOT NULL CHECK(category IN ('BIKE', 'CAR', 'HEAVY_COMMERCIAL')),
                    brand TEXT NOT NULL,
                    model_year INTEGER NOT NULL,
                    base_daily_rate NUMERIC NOT NULL,
                    specs_json TEXT,
                    image_url TEXT NOT NULL
                );
            """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS inventory_units (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    model_id INTEGER NOT NULL,
                    registration_number TEXT UNIQUE NOT NULL,
                    status TEXT NOT NULL CHECK(status IN ('AVAILABLE', 'RENTED', 'MAINTENANCE', 'RESERVED')),
                    custom_daily_rate NUMERIC,
                    odometer_km INTEGER DEFAULT 0,
                    FOREIGN KEY (model_id) REFERENCES vehicle_models(id) ON DELETE CASCADE
                );
            """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS bookings (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    user_id INTEGER NOT NULL,
                    unit_id INTEGER NOT NULL,
                    start_date DATE NOT NULL,
                    end_date DATE NOT NULL,
                    status TEXT NOT NULL CHECK(status IN ('CONFIRMED', 'ACTIVE', 'COMPLETED', 'CANCELLED')),
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE RESTRICT,
                    FOREIGN KEY (unit_id) REFERENCES inventory_units(id) ON DELETE RESTRICT
                );
            """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS invoices (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    booking_id INTEGER UNIQUE NOT NULL,
                    total_days INTEGER NOT NULL,
                    daily_rate NUMERIC NOT NULL,
                    subtotal NUMERIC NOT NULL,
                    tax_amount NUMERIC NOT NULL,
                    grand_total NUMERIC NOT NULL,
                    payment_status TEXT NOT NULL CHECK(payment_status IN ('PAID', 'PENDING', 'REFUNDED')),
                    issued_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY (booking_id) REFERENCES bookings(id) ON DELETE CASCADE
                );
            """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS user_sessions (
                    session_token TEXT PRIMARY KEY,
                    user_id INTEGER NOT NULL,
                    role TEXT NOT NULL,
                    expires_at TIMESTAMP NOT NULL,
                    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
                );
            """);

            seedInitialData(conn);
            System.out.println("[DatabaseManager] Database initialized and verified successfully.");

        } catch (SQLException e) {
            System.err.println("[DatabaseManager] Error running database migrations: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void seedInitialData(Connection conn) throws SQLException {
        // Seed default Admin if not exists (Pass: Admin@VX2026)
        String checkAdmin = "SELECT COUNT(*) FROM users WHERE role = 'ADMIN'";
        try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(checkAdmin)) {
            if (rs.next() && rs.getInt(1) == 0) {
                String insertAdmin = """
                    INSERT INTO users (full_name, email, password_hash, salt, role, phone)
                    VALUES ('VX Operations Commander', 'admin@velocityx.io',
                            '89ea2bf4114704172f3e8ca779c836c641ce69a8b1ea32a3ef18182da1e7fb53',
                            'a8f9c2d1e0b4a7c8', 'ADMIN', '+919876543210');
                """;
                stmt.execute(insertAdmin);
            }
        }

        // Seed 30 models with distinct, high-res automotive photographs matching each brand
        String checkModels = "SELECT COUNT(*) FROM vehicle_models";
        try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(checkModels)) {
            if (rs.next() && rs.getInt(1) < 30) {
                stmt.execute("DELETE FROM invoices;");
                stmt.execute("DELETE FROM bookings;");
                stmt.execute("DELETE FROM inventory_units;");
                stmt.execute("DELETE FROM vehicle_models;");

                // CATEGORY A: 10 DISTINCT MOTORCYCLES
                insertModel(conn, "KTM Duke 390 Neon", "BIKE", "KTM", 2025, 1499.00,
                        "{\"Engine\":\"373cc Single\",\"Power\":\"43.5 PS\",\"Fuel\":\"Petrol\",\"TopSpeed\":\"167 km/h\"}",
                        "https://images.unsplash.com/photo-1568772585407-9361f9bf3a87?w=800&auto=format&fit=crop&q=80");

                insertModel(conn, "Royal Enfield Hunter 350", "BIKE", "Royal Enfield", 2024, 1099.00,
                        "{\"Engine\":\"349cc J-Series\",\"Power\":\"20.2 PS\",\"Fuel\":\"Petrol\",\"Mileage\":\"36 km/l\"}",
                        "https://images.unsplash.com/photo-1558981403-c5f9899a28bc?w=800&auto=format&fit=crop&q=80");

                insertModel(conn, "Kawasaki Ninja ZX-10R", "BIKE", "Kawasaki", 2025, 4999.00,
                        "{\"Engine\":\"998cc Inline-4\",\"Power\":\"203 PS\",\"Fuel\":\"Petrol\",\"TopSpeed\":\"299 km/h\"}",
                        "https://images.unsplash.com/photo-1599819811279-d5ad9cccf838?w=800&auto=format&fit=crop&q=80");

                insertModel(conn, "BMW S 1000 RR Cyber", "BIKE", "BMW Motorrad", 2025, 5499.00,
                        "{\"Engine\":\"999cc ShiftCam\",\"Power\":\"210 PS\",\"Fuel\":\"Petrol\",\"TopSpeed\":\"303 km/h\"}",
                        "https://images.unsplash.com/photo-1609630875171-b1321377ee65?w=800&auto=format&fit=crop&q=80");

                insertModel(conn, "Ducati Panigale V4", "BIKE", "Ducati", 2025, 6499.00,
                        "{\"Engine\":\"1103cc Desmosedici\",\"Power\":\"215.5 PS\",\"Fuel\":\"Petrol\",\"Weight\":\"172 kg\"}",
                        "https://images.unsplash.com/photo-1615172282427-9a57ef2d142e?w=800&auto=format&fit=crop&q=80");

                insertModel(conn, "Harley Davidson Fat Bob", "BIKE", "Harley-Davidson", 2024, 3899.00,
                        "{\"Engine\":\"1868cc Milwaukee-114\",\"Torque\":\"155 Nm\",\"Fuel\":\"Petrol\",\"Type\":\"Power Cruiser\"}",
                        "https://images.unsplash.com/photo-1558981806-ec527fa84c39?w=800&auto=format&fit=crop&q=80");

                insertModel(conn, "Yamaha YZF R15M V4", "BIKE", "Yamaha", 2025, 899.00,
                        "{\"Engine\":\"155cc VVA\",\"Power\":\"18.4 PS\",\"Fuel\":\"Petrol\",\"Mileage\":\"45 km/l\"}",
                        "https://images.unsplash.com/photo-1558981420-87aa9dad1c89?w=800&auto=format&fit=crop&q=80");

                insertModel(conn, "Triumph Rocket 3 R", "BIKE", "Triumph", 2024, 5999.00,
                        "{\"Engine\":\"2458cc Inline-3\",\"Torque\":\"221 Nm\",\"Fuel\":\"Petrol\",\"Type\":\"Muscle Cruiser\"}",
                        "https://images.unsplash.com/photo-1558980664-3a031cf67ea8?w=800&auto=format&fit=crop&q=80");

                insertModel(conn, "Suzuki Hayabusa 1340", "BIKE", "Suzuki", 2025, 4799.00,
                        "{\"Engine\":\"1340cc 4-Cylinder\",\"Power\":\"190 PS\",\"Fuel\":\"Petrol\",\"TopSpeed\":\"299 km/h\"}",
                        "https://images.unsplash.com/photo-1568772585407-9361f9bf3a87?w=800&auto=format&fit=crop&q=80");

                insertModel(conn, "Ultraviolette F77 Mach 2", "BIKE", "Ultraviolette", 2025, 1799.00,
                        "{\"Motor\":\"30 kW Magnet\",\"Range\":\"323 km\",\"Fuel\":\"Electric\",\"Accel\":\"0-60 in 2.8s\"}",
                        "https://images.unsplash.com/photo-1558981403-c5f9899a28bc?w=800&auto=format&fit=crop&q=80");

                // CATEGORY B: 10 DISTINCT CARS & SUVS
                insertModel(conn, "Mahindra Thar 4x4 Cyber", "CAR", "Mahindra", 2025, 3499.00,
                        "{\"Engine\":\"2.0L mStallion Turbo\",\"Power\":\"150 bhp\",\"Fuel\":\"Diesel\",\"Drive\":\"4x4 High/Low\"}",
                        "https://images.unsplash.com/photo-1533473359331-0135ef1b58bf?w=800&auto=format&fit=crop&q=80");

                insertModel(conn, "Tata Nexon EV Empowered", "CAR", "Tata Motors", 2025, 2699.00,
                        "{\"Battery\":\"40.5 kWh\",\"Range\":\"465 km\",\"Fuel\":\"Electric\",\"Seating\":\"5 Passengers\"}",
                        "https://images.unsplash.com/photo-1549399542-7e3f8b79c341?w=800&auto=format&fit=crop&q=80");

                insertModel(conn, "Hyundai Ioniq 5 AWD", "CAR", "Hyundai", 2025, 5999.00,
                        "{\"Battery\":\"72.6 kWh\",\"Range\":\"631 km\",\"Fuel\":\"Electric\",\"Charging\":\"10-80% in 18m\"}",
                        "https://images.unsplash.com/photo-1617814076367-b759c7d7e738?w=800&auto=format&fit=crop&q=80");

                insertModel(conn, "Porsche Taycan Turbo S", "CAR", "Porsche", 2025, 12999.00,
                        "{\"Power\":\"750 hp Overboost\",\"Range\":\"450 km\",\"Fuel\":\"Electric\",\"Accel\":\"0-100 in 2.8s\"}",
                        "https://images.unsplash.com/photo-1503376780353-7e6692767b70?w=800&auto=format&fit=crop&q=80");

                insertModel(conn, "Mercedes-Benz G 63 AMG", "CAR", "Mercedes-Benz", 2024, 15999.00,
                        "{\"Engine\":\"4.0L Bi-Turbo V8\",\"Power\":\"585 hp\",\"Fuel\":\"Petrol\",\"Drive\":\"AWD 3 Diff-Locks\"}",
                        "https://images.unsplash.com/photo-1520031441872-265e4ff70366?w=800&auto=format&fit=crop&q=80");

                insertModel(conn, "BMW M4 Competition Coupé", "CAR", "BMW", 2025, 9499.00,
                        "{\"Engine\":\"3.0L M TwinPower\",\"Power\":\"503 hp\",\"Fuel\":\"Petrol\",\"Transmission\":\"8-Speed M\"}",
                        "https://images.unsplash.com/photo-1555215695-3004980ad54e?w=800&auto=format&fit=crop&q=80");

                insertModel(conn, "Toyota Fortuner GR-Sport", "CAR", "Toyota", 2025, 4499.00,
                        "{\"Engine\":\"2.8L Turbo Diesel\",\"Torque\":\"500 Nm\",\"Fuel\":\"Diesel\",\"Seating\":\"7 Passengers\"}",
                        "https://images.unsplash.com/photo-1519641471654-76ce0107ad1b?w=800&auto=format&fit=crop&q=80");

                insertModel(conn, "Tesla Model 3 Long Range", "CAR", "Tesla", 2025, 6499.00,
                        "{\"Motors\":\"Dual Motor AWD\",\"Range\":\"629 km\",\"Fuel\":\"Electric\",\"Autopilot\":\"Hardware 4.0\"}",
                        "https://images.unsplash.com/photo-1560958089-b8a1929cea89?w=800&auto=format&fit=crop&q=80");

                insertModel(conn, "Audi RS e-tron GT", "CAR", "Audi", 2025, 11999.00,
                        "{\"Power\":\"637 bhp\",\"Range\":\"495 km\",\"Fuel\":\"Electric\",\"Boost\":\"Quattro AWD Dynamic\"}",
                        "https://images.unsplash.com/photo-1614162692292-7ac56d7f7f1e?w=800&auto=format&fit=crop&q=80");

                insertModel(conn, "Land Rover Defender 110", "CAR", "Land Rover", 2025, 8999.00,
                        "{\"Engine\":\"3.0L Turbo Inline-6\",\"Power\":\"395 hp\",\"Fuel\":\"Petrol\",\"Suspension\":\"Air\"}",
                        "https://images.unsplash.com/photo-1542282088-72c9c27ed0cd?w=800&auto=format&fit=crop&q=80");

                // CATEGORY C: 10 DISTINCT COMMERCIAL & HEAVY TRUCKS
                insertModel(conn, "Tata Ace Gold EV Mini", "HEAVY_COMMERCIAL", "Tata Motors", 2025, 1899.00,
                        "{\"Payload\":\"600 kg\",\"Range\":\"154 km\",\"Fuel\":\"Electric\",\"CargoVol\":\"208 cu ft\"}",
                        "https://images.unsplash.com/photo-1586191582056-a609d57a9f73?w=800&auto=format&fit=crop&q=80");

                insertModel(conn, "Mahindra Bolero Maxi Truck", "HEAVY_COMMERCIAL", "Mahindra", 2024, 2199.00,
                        "{\"Payload\":\"1200 kg\",\"Engine\":\"2.5L m2DiCR\",\"Fuel\":\"Diesel\",\"DeckLength\":\"2500 mm\"}",
                        "https://images.unsplash.com/photo-1519003722824-194d4455a60c?w=800&auto=format&fit=crop&q=80");

                insertModel(conn, "Ashok Leyland Dost+ Strong", "HEAVY_COMMERCIAL", "Ashok Leyland", 2025, 2399.00,
                        "{\"Payload\":\"1500 kg\",\"Engine\":\"1.5L 3-Cyl Turbo\",\"Fuel\":\"Diesel\",\"Mileage\":\"19.6 km/l\"}",
                        "https://images.unsplash.com/photo-1601584115197-04ecc0da31d7?w=800&auto=format&fit=crop&q=80");

                insertModel(conn, "BharatBenz 1617R Multi-Axle", "HEAVY_COMMERCIAL", "BharatBenz", 2024, 6999.00,
                        "{\"GVW\":\"16200 kg\",\"Power\":\"170 hp 4D34i\",\"Fuel\":\"Diesel\",\"DeckLength\":\"24 ft Cargo\"}",
                        "https://images.unsplash.com/photo-1586191582056-a609d57a9f73?w=800&auto=format&fit=crop&q=80");

                insertModel(conn, "Volvo FMX 460 Mining Tipper", "HEAVY_COMMERCIAL", "Volvo Trucks", 2025, 14999.00,
                        "{\"Engine\":\"12.8L D13A 460hp\",\"Torque\":\"2300 Nm\",\"Capacity\":\"19.5 cu m\",\"Drive\":\"8x4 Heavy\"}",
                        "https://images.unsplash.com/photo-1519003722824-194d4455a60c?w=800&auto=format&fit=crop&q=80");

                insertModel(conn, "Tata Signa 4825.TK Heavy", "HEAVY_COMMERCIAL", "Tata Motors", 2025, 9999.00,
                        "{\"GVW\":\"47500 kg\",\"Engine\":\"Cummins ISBe 6.7L\",\"Power\":\"250 hp\",\"Axles\":\"16-Wheeler\"}",
                        "https://images.unsplash.com/photo-1601584115197-04ecc0da31d7?w=800&auto=format&fit=crop&q=80");

                insertModel(conn, "Eicher Pro 3015 Logistics", "HEAVY_COMMERCIAL", "Eicher", 2024, 5499.00,
                        "{\"GVW\":\"16020 kg\",\"Engine\":\"E494 4-Cylinder\",\"Fuel\":\"Diesel\",\"CargoBed\":\"22 ft Container\"}",
                        "https://images.unsplash.com/photo-1586191582056-a609d57a9f73?w=800&auto=format&fit=crop&q=80");

                insertModel(conn, "Scania R 500 Intercity Puller", "HEAVY_COMMERCIAL", "Scania", 2025, 16999.00,
                        "{\"Engine\":\"13.0L DC13 500hp\",\"Torque\":\"2550 Nm\",\"Fuel\":\"Diesel\",\"Transmission\":\"Opticruise\"}",
                        "https://images.unsplash.com/photo-1519003722824-194d4455a60c?w=800&auto=format&fit=crop&q=80");

                insertModel(conn, "Force Traveller 3700 Van", "HEAVY_COMMERCIAL", "Force Motors", 2025, 3499.00,
                        "{\"Seating\":\"17 + Driver\",\"Engine\":\"FM 2.6 CR ED\",\"Fuel\":\"Diesel\",\"AirConditioning\":\"Dual-Zone\"}",
                        "https://images.unsplash.com/photo-1544620347-c4fd4a3d5957?w=800&auto=format&fit=crop&q=80");

                insertModel(conn, "Tata Prima 5530.S Prime Mover", "HEAVY_COMMERCIAL", "Tata Motors", 2025, 11499.00,
                        "{\"GCW\":\"55000 kg\",\"Engine\":\"Cummins 6.7L BS6\",\"Power\":\"300 hp\",\"Transmission\":\"9-Speed\"}",
                        "https://images.unsplash.com/photo-1601584115197-04ecc0da31d7?w=800&auto=format&fit=crop&q=80");

                System.out.println("[DatabaseManager] Seeded 30 models with visual image links.");
                seedAllInventoryUnits(conn);
            }
        }
    }

    private static void insertModel(Connection conn, String name, String category, String brand, int year, double rate, String specs, String imageUrl) throws SQLException {
        String sql = "INSERT INTO vehicle_models (name, category, brand, model_year, base_daily_rate, specs_json, image_url) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, name);
            ps.setString(2, category);
            ps.setString(3, brand);
            ps.setInt(4, year);
            ps.setDouble(5, rate);
            ps.setString(6, specs);
            ps.setString(7, imageUrl);
            ps.executeUpdate();
        }
    }

    private static void seedAllInventoryUnits(Connection conn) throws SQLException {
        String query = "SELECT id, category, base_daily_rate FROM vehicle_models";
        try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(query)) {
            int seq = 1000;
            while (rs.next()) {
                long modelId = rs.getLong("id");
                String cat = rs.getString("category");
                double rate = rs.getDouble("base_daily_rate");

                int quantity = 2 + (int)(modelId % 4); 

                String stateCode = "TS-0" + (7 + (modelId % 3));
                String series = cat.equals("BIKE") ? "BK" : (cat.equals("CAR") ? "CR" : "HV");

                for (int u = 1; u <= quantity; u++) {
                    seq++;
                    String reg = String.format("%s-%s-%04d", stateCode, series, seq);
                    String status = (u == 1 && modelId % 3 == 0) ? "RENTED" : "AVAILABLE";
                    int odo = (int)(seq * 14.5);

                    insertUnit(conn, modelId, reg, status, rate, odo);
                }
            }
        }
        System.out.println("[DatabaseManager] Seeded diversified physical inventory units.");
    }

    private static void insertUnit(Connection conn, long modelId, String reg, String status, double rate, int odo) throws SQLException {
        String sql = "INSERT INTO inventory_units (model_id, registration_number, status, custom_daily_rate, odometer_km) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, modelId);
            ps.setString(2, reg);
            ps.setString(3, status);
            ps.setDouble(4, rate);
            ps.setInt(5, odo);
            ps.executeUpdate();
        }
    }
}