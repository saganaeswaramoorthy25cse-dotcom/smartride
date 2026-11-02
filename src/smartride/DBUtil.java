package smartride;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.sql.*;

public class DBUtil {

    private static String dbFilePath = null;

    static {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            System.err.println("SQLite JDBC Driver not found: " + e.getMessage());
        }
    }

    public static synchronized String getDatabasePath() {
        if (dbFilePath != null) {
            return dbFilePath;
        }

        // Primary workspace path
        File target = new File("C:/smart new/smartride.db");
        if (target.getParentFile() != null && target.getParentFile().exists()) {
            dbFilePath = target.getAbsolutePath();
            return dbFilePath;
        }

        String sysProp = System.getProperty("smartride.db.path");
        if (sysProp != null && !sysProp.trim().isEmpty()) {
            dbFilePath = sysProp.trim();
            return dbFilePath;
        }

        String catalinaBase = System.getProperty("catalina.base");
        if (catalinaBase != null && !catalinaBase.trim().isEmpty()) {
            File catFile = new File(catalinaBase, "smartride.db");
            dbFilePath = catFile.getAbsolutePath();
            return dbFilePath;
        }

        dbFilePath = "smartride.db";
        return dbFilePath;
    }

    public static Connection getConnection() throws SQLException {
        String url = "jdbc:sqlite:" + getDatabasePath();
        return DriverManager.getConnection(url);
    }

    /* ------------------------------------------------------------------------
     * PASSWORD SECURITY (SALTED SHA-256)
     * ------------------------------------------------------------------------ */

    public static String generateSalt() {
        byte[] saltBytes = new byte[8];
        new SecureRandom().nextBytes(saltBytes);
        StringBuilder sb = new StringBuilder();
        for (byte b : saltBytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    public static String hashPassword(String password, String salt) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(salt.getBytes(StandardCharsets.UTF_8));
            byte[] hash = md.digest(password.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("SHA-256 unavailable: " + e.getMessage(), e);
        }
    }

    /* ------------------------------------------------------------------------
     * DATABASE INITIALIZATION & SCHEMA CREATION
     * ------------------------------------------------------------------------ */

    public static synchronized void initDatabase() {
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {

            // 1. Users Table
            stmt.execute("CREATE TABLE IF NOT EXISTS users (" +
                    "user_id TEXT PRIMARY KEY, " +
                    "username TEXT UNIQUE NOT NULL, " +
                    "password_hash TEXT NOT NULL, " +
                    "salt TEXT NOT NULL, " +
                    "name TEXT NOT NULL, " +
                    "contact TEXT, " +
                    "role TEXT NOT NULL, " +
                    "status TEXT NOT NULL, " +
                    "created_at TEXT NOT NULL)");

            // 2. Students Table
            stmt.execute("CREATE TABLE IF NOT EXISTS students (" +
                    "student_id TEXT PRIMARY KEY, " +
                    "roll_no TEXT UNIQUE NOT NULL, " +
                    "department TEXT NOT NULL, " +
                    "regular_stop_id TEXT NOT NULL, " +
                    "temp_stop_id TEXT, " +
                    "temp_bus_id TEXT, " +
                    "temp_stop_date TEXT, " +
                    "temp_request_status TEXT DEFAULT 'NONE', " +
                    "assigned_bus_id TEXT, " +
                    "is_present INTEGER DEFAULT 1, " +
                    "absence_date TEXT, " +
                    "estimated_pickup_time TEXT DEFAULT '07:30 AM', " +
                    "FOREIGN KEY(student_id) REFERENCES users(user_id))");

            // 3. Drivers Table
            stmt.execute("CREATE TABLE IF NOT EXISTS drivers (" +
                    "driver_id TEXT PRIMARY KEY, " +
                    "license_number TEXT NOT NULL, " +
                    "assigned_bus_id TEXT, " +
                    "FOREIGN KEY(driver_id) REFERENCES users(user_id))");

            // 4. Buses Table
            stmt.execute("CREATE TABLE IF NOT EXISTS buses (" +
                    "bus_id TEXT PRIMARY KEY, " +
                    "bus_number TEXT NOT NULL, " +
                    "capacity INTEGER NOT NULL, " +
                    "bus_type TEXT NOT NULL, " +
                    "status TEXT DEFAULT 'AVAILABLE')");

            // 5. Stops Table (Real Erode places with latitude & longitude)
            stmt.execute("CREATE TABLE IF NOT EXISTS stops (" +
                    "stop_id TEXT PRIMARY KEY, " +
                    "stop_name TEXT NOT NULL, " +
                    "latitude REAL NOT NULL, " +
                    "longitude REAL NOT NULL, " +
                    "road_type TEXT NOT NULL)");

            // 6. Road Edges Table (Weighted road network)
            stmt.execute("CREATE TABLE IF NOT EXISTS road_edges (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "from_stop TEXT NOT NULL, " +
                    "to_stop TEXT NOT NULL, " +
                    "travel_time INTEGER NOT NULL, " +
                    "distance_km REAL NOT NULL, " +
                    "road_type TEXT NOT NULL)");

            // 7. Bus Locations Table (Real-time GPS tracking)
            stmt.execute("CREATE TABLE IF NOT EXISTS bus_locations (" +
                    "bus_id TEXT PRIMARY KEY, " +
                    "latitude REAL NOT NULL, " +
                    "longitude REAL NOT NULL, " +
                    "accuracy REAL DEFAULT 5.0, " +
                    "speed REAL DEFAULT 0.0, " +
                    "last_updated TEXT NOT NULL, " +
                    "source TEXT NOT NULL, " +
                    "status TEXT NOT NULL)");

            // 8. Boarding Records Table (Duplicate boarding prevented via UNIQUE constraint)
            stmt.execute("CREATE TABLE IF NOT EXISTS boarding_records (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "trip_date TEXT NOT NULL, " +
                    "bus_id TEXT NOT NULL, " +
                    "student_id TEXT NOT NULL, " +
                    "roll_no TEXT NOT NULL, " +
                    "student_name TEXT NOT NULL, " +
                    "stop_id TEXT NOT NULL, " +
                    "boarding_time TEXT NOT NULL, " +
                    "status TEXT NOT NULL, " +
                    "UNIQUE(trip_date, student_id))");

            // 9. Breakdowns Table
            stmt.execute("CREATE TABLE IF NOT EXISTS breakdowns (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "bus_id TEXT NOT NULL, " +
                    "reason TEXT NOT NULL, " +
                    "reported_at TEXT NOT NULL, " +
                    "status TEXT NOT NULL, " +
                    "replacement_bus_id TEXT)");

            // 10. Temporary Change Requests Table
            stmt.execute("CREATE TABLE IF NOT EXISTS temporary_change_requests (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "student_id TEXT NOT NULL, " +
                    "roll_no TEXT NOT NULL, " +
                    "student_name TEXT NOT NULL, " +
                    "change_date TEXT NOT NULL, " +
                    "requested_stop_id TEXT NOT NULL, " +
                    "requested_stop_name TEXT NOT NULL, " +
                    "requested_bus_id TEXT, " +
                    "reason TEXT, " +
                    "status TEXT NOT NULL, " +
                    "created_at TEXT NOT NULL)");

            // Check if initial seeding is required
            ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM users");
            if (rs.next() && rs.getInt(1) == 0) {
                seedInitialData(conn);
            }

        } catch (SQLException e) {
            System.err.println("Database initialization error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /* ------------------------------------------------------------------------
     * INITIAL SEED DATA (ERODE PLACES, COORDINATES, ROADS, USERS)
     * ------------------------------------------------------------------------ */

    private static void seedInitialData(Connection conn) throws SQLException {
        System.out.println("Seeding SMARTRIDE initial database records with authentic Erode places...");

        // 1. Admin Account: admin / Admin@123
        String adminSalt = generateSalt();
        String adminHash = hashPassword("Admin@123", adminSalt);
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO users (user_id, username, password_hash, salt, name, contact, role, status, created_at) " +
                "VALUES ('ADM-001', 'admin', ?, ?, 'Campus Administrator', '9842100000', 'ADMIN', 'APPROVED', datetime('now', 'localtime'))")) {
            ps.setString(1, adminHash);
            ps.setString(2, adminSalt);
            ps.executeUpdate();
        }

        // 2. Real Places in and around Erode with authentic Coordinates
        Object[][] stops = {
            {"C00", "Kalvi College Campus Depot", 11.3180, 77.6740, "ARTERIAL"},
            {"S01", "Erode Bus Stand", 11.3410, 77.7172, "ARTERIAL"},
            {"S02", "Perundurai", 11.2750, 77.5830, "HIGHWAY"},
            {"S03", "Bhavani", 11.4485, 77.6820, "ARTERIAL"},
            {"S04", "Komarapalayam", 11.4500, 77.7150, "SUBURBAN"},
            {"S05", "Tiruchengode", 11.3800, 77.8900, "HIGHWAY"},
            {"S07", "Chithode", 11.4110, 77.6745, "ARTERIAL"},
            {"S08", "Thindal", 11.3195, 77.6790, "SUBURBAN"},
            {"S09", "Modakurichi", 11.2610, 77.7550, "SUBURBAN"},
            {"S10", "Solar", 11.3150, 77.7490, "ARTERIAL"},
            {"S11", "Veerappanchatram", 11.3650, 77.7150, "ARTERIAL"},
            {"S12", "Gobichettipalayam", 11.4550, 77.4380, "HIGHWAY"}
        };

        try (PreparedStatement ps = conn.prepareStatement("INSERT INTO stops (stop_id, stop_name, latitude, longitude, road_type) VALUES (?, ?, ?, ?, ?)")) {
            for (Object[] s : stops) {
                ps.setString(1, (String) s[0]);
                ps.setString(2, (String) s[1]);
                ps.setDouble(3, (Double) s[2]);
                ps.setDouble(4, (Double) s[3]);
                ps.setString(5, (String) s[4]);
                ps.executeUpdate();
            }
        }

        // 3. Real Road Connections with Travel Times (mins) and Distances (km)
        Object[][] edges = {
            {"C00", "S08", 4, 2.0, "SUBURBAN"},
            {"S08", "S01", 10, 6.5, "ARTERIAL"},
            {"S01", "S11", 6, 3.5, "ARTERIAL"},
            {"S11", "S07", 8, 5.0, "ARTERIAL"},
            {"S07", "S03", 9, 6.0, "ARTERIAL"},
            {"S03", "S04", 7, 4.0, "SUBURBAN"},
            {"S08", "S02", 15, 14.0, "HIGHWAY"},
            {"S02", "S07", 18, 16.0, "HIGHWAY"},
            {"S08", "S10", 12, 8.0, "ARTERIAL"},
            {"S10", "S09", 10, 7.5, "SUBURBAN"},
            {"S01", "S10", 9, 5.5, "ARTERIAL"},
            {"S07", "S12", 22, 24.0, "HIGHWAY"},
            {"C00", "S02", 16, 15.0, "HIGHWAY"},
            {"C00", "S10", 14, 9.5, "ARTERIAL"},
            {"C00", "S01", 11, 7.5, "ARTERIAL"}
        };

        try (PreparedStatement ps = conn.prepareStatement("INSERT INTO road_edges (from_stop, to_stop, travel_time, distance_km, road_type) VALUES (?, ?, ?, ?, ?)")) {
            for (Object[] e : edges) {
                // Forward direction
                ps.setString(1, (String) e[0]);
                ps.setString(2, (String) e[1]);
                ps.setInt(3, (Integer) e[2]);
                ps.setDouble(4, (Double) e[3]);
                ps.setString(5, (String) e[4]);
                ps.executeUpdate();

                // Reverse direction (bidirectional road)
                ps.setString(1, (String) e[1]);
                ps.setString(2, (String) e[0]);
                ps.setInt(3, (Integer) e[2]);
                ps.setDouble(4, (Double) e[3]);
                ps.setString(5, (String) e[4]);
                ps.executeUpdate();
            }
        }

        // 4. College Buses
        String[][] buses = {
            {"BUS-01", "TN 33 AA 1001", "50", "Standard 50-Seat", "AVAILABLE"},
            {"BUS-02", "TN 33 BB 2002", "50", "Standard 50-Seat", "AVAILABLE"},
            {"BUS-03", "TN 33 CC 3003", "30", "Mini 30-Seat", "AVAILABLE"}
        };

        try (PreparedStatement ps = conn.prepareStatement("INSERT INTO buses (bus_id, bus_number, capacity, bus_type, status) VALUES (?, ?, ?, ?, ?)")) {
            for (String[] b : buses) {
                ps.setString(1, b[0]);
                ps.setString(2, b[1]);
                ps.setInt(3, Integer.parseInt(b[2]));
                ps.setString(4, b[3]);
                ps.setString(5, b[4]);
                ps.executeUpdate();
            }
        }

        // 5. Drivers: driver1 / Driver@123 and driver2 / Driver@123
        String[][] drivers = {
            {"DRV-001", "driver1", "Driver@123", "Senthil Nathan P", "9842111111", "TN33-2015-D1001", "BUS-01"},
            {"DRV-002", "driver2", "Driver@123", "Murugesan K", "9842122222", "TN33-2018-D2002", "BUS-02"}
        };

        try (PreparedStatement psUser = conn.prepareStatement(
                "INSERT INTO users (user_id, username, password_hash, salt, name, contact, role, status, created_at) VALUES (?, ?, ?, ?, ?, ?, 'DRIVER', 'APPROVED', datetime('now', 'localtime'))");
             PreparedStatement psDrv = conn.prepareStatement("INSERT INTO drivers (driver_id, license_number, assigned_bus_id) VALUES (?, ?, ?)")) {
            for (String[] d : drivers) {
                String salt = generateSalt();
                String hash = hashPassword(d[2], salt);

                psUser.setString(1, d[0]);
                psUser.setString(2, d[1]);
                psUser.setString(3, hash);
                psUser.setString(4, salt);
                psUser.setString(5, d[3]);
                psUser.setString(6, d[4]);
                psUser.executeUpdate();

                psDrv.setString(1, d[0]);
                psDrv.setString(2, d[5]);
                psDrv.setString(3, d[6]);
                psDrv.executeUpdate();
            }
        }

        // 6. Registered Students (25CSR001, 25CSR002, 25CSR003, 25CSR004)
        String[][] students = {
            {"STU-001", "25CSR001", "Student@123", "Priyadharshini K", "9842133331", "CSE", "S08", "BUS-01"},
            {"STU-002", "25CSR002", "Student@123", "Vigneshwaran R", "9842133332", "IT", "S01", "BUS-01"},
            {"STU-003", "25CSR003", "Student@123", "Ananya M", "9842133333", "ECE", "S11", "BUS-01"},
            {"STU-004", "25CSR004", "Student@123", "Karthikeyan S", "9842133334", "MECH", "S02", "BUS-02"}
        };

        try (PreparedStatement psUser = conn.prepareStatement(
                "INSERT INTO users (user_id, username, password_hash, salt, name, contact, role, status, created_at) VALUES (?, ?, ?, ?, ?, ?, 'STUDENT', 'APPROVED', datetime('now', 'localtime'))");
             PreparedStatement psStu = conn.prepareStatement(
                "INSERT INTO students (student_id, roll_no, department, regular_stop_id, assigned_bus_id, is_present, estimated_pickup_time) VALUES (?, ?, ?, ?, ?, 1, '07:34 AM')")) {
            for (String[] s : students) {
                String salt = generateSalt();
                String hash = hashPassword(s[2], salt);

                psUser.setString(1, s[0]);
                psUser.setString(2, s[1]); // roll_no is also username for student login
                psUser.setString(3, hash);
                psUser.setString(4, salt);
                psUser.setString(5, s[3]);
                psUser.setString(6, s[4]);
                psUser.executeUpdate();

                psStu.setString(1, s[0]);
                psStu.setString(2, s[1]);
                psStu.setString(3, s[5]);
                psStu.setString(4, s[6]);
                psStu.setString(5, s[7]);
                psStu.executeUpdate();
            }
        }

        // 7. Initial Live GPS Locations for Buses in Erode
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO bus_locations (bus_id, latitude, longitude, accuracy, speed, last_updated, source, status) VALUES (?, ?, ?, ?, ?, datetime('now', 'localtime'), ?, ?)")) {
            // BUS-01 near Thindal
            ps.setString(1, "BUS-01");
            ps.setDouble(2, 11.3195);
            ps.setDouble(3, 77.6790);
            ps.setDouble(4, 4.2);
            ps.setDouble(5, 32.5);
            ps.setString(6, "HARDWARE_GPS");
            ps.setString(7, "LIVE");
            ps.executeUpdate();

            // BUS-02 near Perundurai
            ps.setString(1, "BUS-02");
            ps.setDouble(2, 11.2750);
            ps.setDouble(3, 77.5830);
            ps.setDouble(4, 5.0);
            ps.setDouble(5, 28.0);
            ps.setString(6, "HARDWARE_GPS");
            ps.setString(7, "LIVE");
            ps.executeUpdate();
        }

        System.out.println("SMARTRIDE seed initialization complete.");
    }
}
