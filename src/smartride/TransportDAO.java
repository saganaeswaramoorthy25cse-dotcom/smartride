package smartride;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import smartride.Models.*;
import smartride.Models.Driver;
import smartride.DijkstraRouter.Graph;

public class TransportDAO {

    public TransportDAO() {
        DBUtil.initDatabase();
        autoRestoreExpiredChanges();
    }

    /**
     * Automatically restore temporary stop changes and absences that have passed their date.
     */
    public void autoRestoreExpiredChanges() {
        String today = LocalDate.now().toString();
        try (Connection conn = DBUtil.getConnection()) {
            // Restore expired 1-day temporary stops
            try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE students SET temp_stop_id = NULL, temp_bus_id = NULL, temp_stop_date = NULL, temp_request_status = 'NONE' " +
                    "WHERE temp_stop_date IS NOT NULL AND temp_stop_date < ?")) {
                ps.setString(1, today);
                ps.executeUpdate();
            }

            // Restore expired daily absences
            try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE students SET is_present = 1, absence_date = NULL " +
                    "WHERE absence_date IS NOT NULL AND absence_date < ?")) {
                ps.setString(1, today);
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            System.err.println("Error in autoRestoreExpiredChanges: " + e.getMessage());
        }
    }

    /* ========================================================================
     * AUTHENTICATION & USERS
     * ======================================================================== */

    public User authenticate(String username, String rawPassword, UserRole expectedRole) {
        if (username == null || rawPassword == null) return null;
        String sql = "SELECT * FROM users WHERE (username = ? OR user_id = ?) AND role = ? AND status = 'APPROVED'";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username.trim());
            ps.setString(2, username.trim());
            ps.setString(3, expectedRole.name());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String salt = rs.getString("salt");
                    String storedHash = rs.getString("password_hash");
                    String computedHash = DBUtil.hashPassword(rawPassword, salt);
                    if (storedHash.equals(computedHash)) {
                        return new User(
                                rs.getString("user_id"),
                                rs.getString("username"),
                                rs.getString("name"),
                                rs.getString("contact"),
                                UserRole.valueOf(rs.getString("role")),
                                rs.getString("status"),
                                rs.getString("created_at")
                        );
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Auth error: " + e.getMessage());
        }
        return null;
    }

    public Student getStudentByUserId(String userId) {
        String sql = "SELECT u.user_id, u.username, u.name, u.contact, u.status, u.created_at, " +
                "s.roll_no, s.department, s.regular_stop_id, s.temp_stop_id, s.temp_bus_id, s.temp_stop_date, " +
                "s.temp_request_status, s.assigned_bus_id, s.is_present, s.absence_date, s.estimated_pickup_time " +
                "FROM users u JOIN students s ON u.user_id = s.student_id WHERE u.user_id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Student st = new Student();
                    st.setUserId(rs.getString("user_id"));
                    st.setUsername(rs.getString("username"));
                    st.setName(rs.getString("name"));
                    st.setContact(rs.getString("contact"));
                    st.setStatus(rs.getString("status"));
                    st.setCreatedAt(rs.getString("created_at"));
                    st.setRollNo(rs.getString("roll_no"));
                    st.setDepartment(rs.getString("department"));
                    st.setRegularStopId(rs.getString("regular_stop_id"));
                    st.setTempStopId(rs.getString("temp_stop_id"));
                    st.setTempBusId(rs.getString("temp_bus_id"));
                    st.setTempStopDate(rs.getString("temp_stop_date"));
                    st.setTempRequestStatus(rs.getString("temp_request_status"));
                    st.setAssignedBusId(rs.getString("assigned_bus_id"));
                    st.setPresent(rs.getInt("is_present") == 1);
                    st.setAbsenceDate(rs.getString("absence_date"));
                    st.setEstimatedPickupTime(rs.getString("estimated_pickup_time"));
                    return st;
                }
            }
        } catch (SQLException e) {
            System.err.println("Error getStudentByUserId: " + e.getMessage());
        }
        return null;
    }

    public Student getStudentByRollNo(String rollNo) {
        String sql = "SELECT u.user_id, u.username, u.name, u.contact, u.status, u.created_at, " +
                "s.roll_no, s.department, s.regular_stop_id, s.temp_stop_id, s.temp_bus_id, s.temp_stop_date, " +
                "s.temp_request_status, s.assigned_bus_id, s.is_present, s.absence_date, s.estimated_pickup_time " +
                "FROM users u JOIN students s ON u.user_id = s.student_id WHERE s.roll_no = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, rollNo.trim().toUpperCase());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Student st = new Student();
                    st.setUserId(rs.getString("user_id"));
                    st.setUsername(rs.getString("username"));
                    st.setName(rs.getString("name"));
                    st.setContact(rs.getString("contact"));
                    st.setStatus(rs.getString("status"));
                    st.setCreatedAt(rs.getString("created_at"));
                    st.setRollNo(rs.getString("roll_no"));
                    st.setDepartment(rs.getString("department"));
                    st.setRegularStopId(rs.getString("regular_stop_id"));
                    st.setTempStopId(rs.getString("temp_stop_id"));
                    st.setTempBusId(rs.getString("temp_bus_id"));
                    st.setTempStopDate(rs.getString("temp_stop_date"));
                    st.setTempRequestStatus(rs.getString("temp_request_status"));
                    st.setAssignedBusId(rs.getString("assigned_bus_id"));
                    st.setPresent(rs.getInt("is_present") == 1);
                    st.setAbsenceDate(rs.getString("absence_date"));
                    st.setEstimatedPickupTime(rs.getString("estimated_pickup_time"));
                    return st;
                }
            }
        } catch (SQLException e) {
            System.err.println("Error getStudentByRollNo: " + e.getMessage());
        }
        return null;
    }

    public Driver getDriverByUserId(String userId) {
        String sql = "SELECT u.user_id, u.username, u.name, u.contact, u.status, u.created_at, " +
                "d.license_number, d.assigned_bus_id " +
                "FROM users u JOIN drivers d ON u.user_id = d.driver_id WHERE u.user_id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Driver drv = new Driver();
                    drv.setUserId(rs.getString("user_id"));
                    drv.setUsername(rs.getString("username"));
                    drv.setName(rs.getString("name"));
                    drv.setContact(rs.getString("contact"));
                    drv.setStatus(rs.getString("status"));
                    drv.setCreatedAt(rs.getString("created_at"));
                    drv.setLicenseNumber(rs.getString("license_number"));
                    drv.setAssignedBusId(rs.getString("assigned_bus_id"));
                    return drv;
                }
            }
        } catch (SQLException e) {
            System.err.println("Error getDriverByUserId: " + e.getMessage());
        }
        return null;
    }

    public List<Student> getAllStudents() {
        List<Student> list = new ArrayList<>();
        String sql = "SELECT u.user_id, u.username, u.name, u.contact, u.status, u.created_at, " +
                "s.roll_no, s.department, s.regular_stop_id, s.temp_stop_id, s.temp_bus_id, s.temp_stop_date, " +
                "s.temp_request_status, s.assigned_bus_id, s.is_present, s.absence_date, s.estimated_pickup_time " +
                "FROM users u JOIN students s ON u.user_id = s.student_id ORDER BY s.roll_no ASC";
        try (Connection conn = DBUtil.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                Student st = new Student();
                st.setUserId(rs.getString("user_id"));
                st.setUsername(rs.getString("username"));
                st.setName(rs.getString("name"));
                st.setContact(rs.getString("contact"));
                st.setStatus(rs.getString("status"));
                st.setCreatedAt(rs.getString("created_at"));
                st.setRollNo(rs.getString("roll_no"));
                st.setDepartment(rs.getString("department"));
                st.setRegularStopId(rs.getString("regular_stop_id"));
                st.setTempStopId(rs.getString("temp_stop_id"));
                st.setTempBusId(rs.getString("temp_bus_id"));
                st.setTempStopDate(rs.getString("temp_stop_date"));
                st.setTempRequestStatus(rs.getString("temp_request_status"));
                st.setAssignedBusId(rs.getString("assigned_bus_id"));
                st.setPresent(rs.getInt("is_present") == 1);
                st.setAbsenceDate(rs.getString("absence_date"));
                st.setEstimatedPickupTime(rs.getString("estimated_pickup_time"));
                list.add(st);
            }
        } catch (SQLException e) {
            System.err.println("Error getAllStudents: " + e.getMessage());
        }
        return list;
    }

    public List<Driver> getAllDrivers() {
        List<Driver> list = new ArrayList<>();
        String sql = "SELECT u.user_id, u.username, u.name, u.contact, u.status, u.created_at, " +
                "d.license_number, d.assigned_bus_id " +
                "FROM users u JOIN drivers d ON u.user_id = d.driver_id ORDER BY u.name ASC";
        try (Connection conn = DBUtil.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                Driver drv = new Driver();
                drv.setUserId(rs.getString("user_id"));
                drv.setUsername(rs.getString("username"));
                drv.setName(rs.getString("name"));
                drv.setContact(rs.getString("contact"));
                drv.setStatus(rs.getString("status"));
                drv.setCreatedAt(rs.getString("created_at"));
                drv.setLicenseNumber(rs.getString("license_number"));
                drv.setAssignedBusId(rs.getString("assigned_bus_id"));
                list.add(drv);
            }
        } catch (SQLException e) {
            System.err.println("Error getAllDrivers: " + e.getMessage());
        }
        return list;
    }

    /* ========================================================================
     * FLEET & ROAD NETWORK
     * ======================================================================== */

    public List<CollegeBus> getAllBuses() {
        List<CollegeBus> list = new ArrayList<>();
        String sql = "SELECT * FROM buses ORDER BY bus_id ASC";
        try (Connection conn = DBUtil.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(new CollegeBus(
                        rs.getString("bus_id"),
                        rs.getString("bus_number"),
                        rs.getInt("capacity"),
                        rs.getString("bus_type"),
                        BusStatus.valueOf(rs.getString("status"))
                ));
            }
        } catch (SQLException e) {
            System.err.println("Error getAllBuses: " + e.getMessage());
        }
        return list;
    }

    public Map<String, CollegeBus> getAllBusesMap() {
        Map<String, CollegeBus> map = new HashMap<>();
        for (CollegeBus b : getAllBuses()) {
            map.put(b.getBusId(), b);
        }
        return map;
    }

    public CollegeBus getBus(String busId) {
        if (busId == null) return null;
        String sql = "SELECT * FROM buses WHERE bus_id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, busId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new CollegeBus(
                            rs.getString("bus_id"),
                            rs.getString("bus_number"),
                            rs.getInt("capacity"),
                            rs.getString("bus_type"),
                            BusStatus.valueOf(rs.getString("status"))
                    );
                }
            }
        } catch (SQLException e) {
            System.err.println("Error getBus: " + e.getMessage());
        }
        return null;
    }

    public List<PickupStop> getAllStops() {
        List<PickupStop> list = new ArrayList<>();
        String sql = "SELECT * FROM stops ORDER BY stop_name ASC";
        try (Connection conn = DBUtil.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(new PickupStop(
                        rs.getString("stop_id"),
                        rs.getString("stop_name"),
                        rs.getDouble("latitude"),
                        rs.getDouble("longitude"),
                        rs.getString("road_type")
                ));
            }
        } catch (SQLException e) {
            System.err.println("Error getAllStops: " + e.getMessage());
        }
        return list;
    }

    public Map<String, PickupStop> getAllStopsMap() {
        Map<String, PickupStop> map = new HashMap<>();
        for (PickupStop ps : getAllStops()) {
            map.put(ps.getStopId(), ps);
        }
        return map;
    }

    public PickupStop getStop(String stopId) {
        if (stopId == null) return null;
        String sql = "SELECT * FROM stops WHERE stop_id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, stopId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new PickupStop(
                            rs.getString("stop_id"),
                            rs.getString("stop_name"),
                            rs.getDouble("latitude"),
                            rs.getDouble("longitude"),
                            rs.getString("road_type")
                    );
                }
            }
        } catch (SQLException e) {
            System.err.println("Error getStop: " + e.getMessage());
        }
        return null;
    }

    public String getStopName(String stopId) {
        PickupStop ps = getStop(stopId);
        return ps != null ? ps.getStopName() : stopId;
    }

    public List<RoadEdge> getAllEdges() {
        List<RoadEdge> list = new ArrayList<>();
        String sql = "SELECT * FROM road_edges ORDER BY id ASC";
        try (Connection conn = DBUtil.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(new RoadEdge(
                        rs.getInt("id"),
                        rs.getString("from_stop"),
                        rs.getString("to_stop"),
                        rs.getInt("travel_time"),
                        rs.getDouble("distance_km"),
                        rs.getString("road_type")
                ));
            }
        } catch (SQLException e) {
            System.err.println("Error getAllEdges: " + e.getMessage());
        }
        return list;
    }

    public Graph loadGraph() {
        Graph graph = new Graph();
        for (PickupStop ps : getAllStops()) {
            graph.addVertex(ps.getStopId());
        }
        for (RoadEdge re : getAllEdges()) {
            graph.addEdge(re.getFromStop(), re.getToStop(), re.getTravelTime(), re.getDistanceKm(), re.getRoadType(), false);
        }
        return graph;
    }

    /* ========================================================================
     * ROUTE RECALCULATION & ARRIVAL TIMES PERSISTENCE
     * ======================================================================== */

    public synchronized Map<String, RouteOptimizationResult> recalculateAllRoutes(String forDate) {
        if (forDate == null || forDate.trim().isEmpty()) {
            forDate = LocalDate.now().toString();
        }

        Graph graph = loadGraph();
        Map<String, PickupStop> stops = getAllStopsMap();
        List<Student> allStudents = getAllStudents();
        Map<String, CollegeBus> buses = getAllBusesMap();

        Map<String, RouteOptimizationResult> results = new HashMap<>();

        for (CollegeBus bus : buses.values()) {
            if (bus.getStatus() == BusStatus.OUT_OF_SERVICE) {
                continue;
            }

            RouteOptimizationResult optResult = DijkstraRouter.planRoute(
                    graph, bus, "C00", stops, allStudents, forDate);
            results.put(bus.getBusId(), optResult);

            // Update student estimated pickup times in database
            Map<String, String> stopTimes = optResult.getStopEstimatedPickupTimes();
            for (Student s : allStudents) {
                String effBus = s.getEffectiveBusId(forDate);
                if (effBus != null && effBus.equals(bus.getBusId())) {
                    if (s.isAbsentOn(forDate)) {
                        updateStudentPickupTimeInDb(s.getUserId(), "Absent");
                    } else {
                        String effStop = s.getEffectiveStopId(forDate);
                        String time = stopTimes.get(effStop);
                        if (time == null) {
                            time = "07:30 AM";
                        }
                        s.setEstimatedPickupTime(time);
                        updateStudentPickupTimeInDb(s.getUserId(), time);
                    }
                }
            }
        }

        return results;
    }

    private void updateStudentPickupTimeInDb(String userId, String time) {
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement("UPDATE students SET estimated_pickup_time = ? WHERE student_id = ?")) {
            ps.setString(1, time);
            ps.setString(2, userId);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error updateStudentPickupTimeInDb: " + e.getMessage());
        }
    }

    /* ========================================================================
     * STUDENT ATTENDANCE & TEMPORARY CHANGE REQUESTS
     * ======================================================================== */

    public synchronized void setStudentAbsence(String rollNo, String date, boolean isAbsent) throws SQLException {
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "UPDATE students SET is_present = ?, absence_date = ? WHERE roll_no = ?")) {
            ps.setInt(1, isAbsent ? 0 : 1);
            ps.setString(2, isAbsent ? date : null);
            ps.setString(3, rollNo.trim().toUpperCase());
            ps.executeUpdate();
        }
        recalculateAllRoutes(date);
    }

    public synchronized void requestTemporaryChange(String studentId, String rollNo, String studentName,
                                                    String date, String newStopId, String newBusId, String reason) throws SQLException {
        try (Connection conn = DBUtil.getConnection()) {
            // Record in temporary_change_requests
            String stopName = getStopName(newStopId);
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO temporary_change_requests (student_id, roll_no, student_name, change_date, requested_stop_id, requested_stop_name, requested_bus_id, reason, status, created_at) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, 'APPROVED', datetime('now', 'localtime'))")) {
                ps.setString(1, studentId);
                ps.setString(2, rollNo);
                ps.setString(3, studentName);
                ps.setString(4, date);
                ps.setString(5, newStopId);
                ps.setString(6, stopName);
                ps.setString(7, newBusId != null && !newBusId.isEmpty() ? newBusId : null);
                ps.setString(8, reason != null ? reason : "Personal convenience");
                ps.executeUpdate();
            }

            // Immediately apply approved temporary change to student record
            try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE students SET temp_stop_id = ?, temp_bus_id = ?, temp_stop_date = ?, temp_request_status = 'APPROVED' WHERE student_id = ?")) {
                ps.setString(1, newStopId);
                ps.setString(2, newBusId != null && !newBusId.isEmpty() ? newBusId : null);
                ps.setString(3, date);
                ps.setString(4, studentId);
                ps.executeUpdate();
            }
        }
        recalculateAllRoutes(date);
    }

    public List<TempChangeRequest> getStudentTempRequests(String studentId) {
        List<TempChangeRequest> list = new ArrayList<>();
        String sql = "SELECT * FROM temporary_change_requests WHERE student_id = ? ORDER BY id DESC";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, studentId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new TempChangeRequest(
                            rs.getInt("id"),
                            rs.getString("student_id"),
                            rs.getString("roll_no"),
                            rs.getString("student_name"),
                            rs.getString("change_date"),
                            rs.getString("requested_stop_id"),
                            rs.getString("requested_stop_name"),
                            rs.getString("requested_bus_id"),
                            rs.getString("reason"),
                            rs.getString("status"),
                            rs.getString("created_at")
                    ));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error getStudentTempRequests: " + e.getMessage());
        }
        return list;
    }

    public List<TempChangeRequest> getAllTempRequests() {
        List<TempChangeRequest> list = new ArrayList<>();
        String sql = "SELECT * FROM temporary_change_requests ORDER BY id DESC";
        try (Connection conn = DBUtil.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(new TempChangeRequest(
                        rs.getInt("id"),
                        rs.getString("student_id"),
                        rs.getString("roll_no"),
                        rs.getString("student_name"),
                        rs.getString("change_date"),
                        rs.getString("requested_stop_id"),
                        rs.getString("requested_stop_name"),
                        rs.getString("requested_bus_id"),
                        rs.getString("reason"),
                        rs.getString("status"),
                        rs.getString("created_at")
                ));
            }
        } catch (SQLException e) {
            System.err.println("Error getAllTempRequests: " + e.getMessage());
        }
        return list;
    }

    public synchronized void updateTempRequestStatus(int requestId, String status) throws SQLException {
        try (Connection conn = DBUtil.getConnection()) {
            String studentId = null;
            String changeDate = null;
            String newStop = null;
            String newBus = null;

            try (PreparedStatement ps = conn.prepareStatement("SELECT * FROM temporary_change_requests WHERE id = ?")) {
                ps.setInt(1, requestId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        studentId = rs.getString("student_id");
                        changeDate = rs.getString("change_date");
                        newStop = rs.getString("requested_stop_id");
                        newBus = rs.getString("requested_bus_id");
                    }
                }
            }

            try (PreparedStatement ps = conn.prepareStatement("UPDATE temporary_change_requests SET status = ? WHERE id = ?")) {
                ps.setString(1, status);
                ps.setInt(2, requestId);
                ps.executeUpdate();
            }

            if (studentId != null) {
                if ("APPROVED".equalsIgnoreCase(status)) {
                    try (PreparedStatement ps = conn.prepareStatement(
                            "UPDATE students SET temp_stop_id = ?, temp_bus_id = ?, temp_stop_date = ?, temp_request_status = 'APPROVED' WHERE student_id = ?")) {
                        ps.setString(1, newStop);
                        ps.setString(2, newBus);
                        ps.setString(3, changeDate);
                        ps.setString(4, studentId);
                        ps.executeUpdate();
                    }
                } else {
                    try (PreparedStatement ps = conn.prepareStatement(
                            "UPDATE students SET temp_stop_id = NULL, temp_bus_id = NULL, temp_stop_date = NULL, temp_request_status = 'REJECTED' WHERE student_id = ?")) {
                        ps.setString(1, studentId);
                        ps.executeUpdate();
                    }
                }
            }

            if (changeDate != null) {
                recalculateAllRoutes(changeDate);
            }
        }
    }

    public synchronized void registerStudent(String rollNo, String name, String password,
                                            String department, String stopId, String contact) throws SQLException {
        String cleanRoll = rollNo.trim().toUpperCase();
        if (getStudentByRollNo(cleanRoll) != null) {
            throw new IllegalArgumentException("Student roll number '" + cleanRoll + "' already registered.");
        }

        String userId = "STU-" + System.currentTimeMillis() % 100000;
        String salt = DBUtil.generateSalt();
        String hash = DBUtil.hashPassword(password, salt);

        // Find appropriate bus covering this stop or nearby
        String assignedBus = "BUS-01";
        if ("S02".equals(stopId) || "S07".equals(stopId) || "S12".equals(stopId)) {
            assignedBus = "BUS-02";
        }

        try (Connection conn = DBUtil.getConnection()) {
            conn.setAutoCommit(false);
            try {
                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO users (user_id, username, password_hash, salt, name, contact, role, status, created_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?, 'STUDENT', 'APPROVED', datetime('now', 'localtime'))")) {
                    ps.setString(1, userId);
                    ps.setString(2, cleanRoll);
                    ps.setString(3, hash);
                    ps.setString(4, salt);
                    ps.setString(5, name.trim());
                    ps.setString(6, contact != null ? contact.trim() : "");
                    ps.executeUpdate();
                }

                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO students (student_id, roll_no, department, regular_stop_id, assigned_bus_id, is_present, estimated_pickup_time) " +
                        "VALUES (?, ?, ?, ?, ?, 1, '07:30 AM')")) {
                    ps.setString(1, userId);
                    ps.setString(2, cleanRoll);
                    ps.setString(3, department.trim().toUpperCase());
                    ps.setString(4, stopId.trim());
                    ps.setString(5, assignedBus);
                    ps.executeUpdate();
                }

                conn.commit();
            } catch (SQLException ex) {
                conn.rollback();
                throw ex;
            } finally {
                conn.setAutoCommit(true);
            }
        }

        recalculateAllRoutes(LocalDate.now().toString());
    }

    /* ========================================================================
     * DRIVER BOARDING & ATTENDANCE RECORDING
     * ======================================================================== */

    public synchronized void recordBoarding(String busId, String rollNo, String tripDate, String status) throws SQLException {
        Student st = getStudentByRollNo(rollNo);
        if (st == null) {
            throw new IllegalArgumentException("No registered student found with roll number '" + rollNo + "'.");
        }

        String effBus = st.getEffectiveBusId(tripDate);
        if (effBus == null || !effBus.equalsIgnoreCase(busId)) {
            throw new IllegalArgumentException("Student " + rollNo + " is assigned to bus " + (effBus != null ? effBus : "Unassigned") + ", not " + busId + ".");
        }

        String now = new java.text.SimpleDateFormat("hh:mm a").format(new java.util.Date());
        String effStop = st.getEffectiveStopId(tripDate);

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "INSERT INTO boarding_records (trip_date, bus_id, student_id, roll_no, student_name, stop_id, boarding_time, status) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?)")) {
            ps.setString(1, tripDate);
            ps.setString(2, busId);
            ps.setString(3, st.getUserId());
            ps.setString(4, st.getRollNo());
            ps.setString(5, st.getName());
            ps.setString(6, effStop);
            ps.setString(7, now);
            ps.setString(8, status != null ? status : "BOARDED");
            ps.executeUpdate();
        } catch (SQLException e) {
            if (e.getMessage() != null && e.getMessage().contains("UNIQUE")) {
                throw new SQLException("Duplicate entry: Student " + rollNo + " boarding status already recorded for trip on " + tripDate + ".");
            }
            throw e;
        }
    }

    public List<BoardingRecord> getDailyBoardingRecords(String busId, String tripDate) {
        List<BoardingRecord> list = new ArrayList<>();
        String sql = (busId != null && !busId.isEmpty())
                ? "SELECT * FROM boarding_records WHERE bus_id = ? AND trip_date = ? ORDER BY id DESC"
                : "SELECT * FROM boarding_records WHERE trip_date = ? ORDER BY id DESC";

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            if (busId != null && !busId.isEmpty()) {
                ps.setString(1, busId);
                ps.setString(2, tripDate);
            } else {
                ps.setString(1, tripDate);
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new BoardingRecord(
                            rs.getInt("id"),
                            rs.getString("trip_date"),
                            rs.getString("bus_id"),
                            rs.getString("student_id"),
                            rs.getString("roll_no"),
                            rs.getString("student_name"),
                            rs.getString("stop_id"),
                            rs.getString("boarding_time"),
                            rs.getString("status")
                    ));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error getDailyBoardingRecords: " + e.getMessage());
        }
        return list;
    }

    public synchronized void reportBreakdown(String busId, String reason) throws SQLException {
        String now = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date());
        try (Connection conn = DBUtil.getConnection()) {
            conn.setAutoCommit(false);
            try {
                try (PreparedStatement ps = conn.prepareStatement("UPDATE buses SET status = 'OUT_OF_SERVICE' WHERE bus_id = ?")) {
                    ps.setString(1, busId);
                    ps.executeUpdate();
                }

                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO breakdowns (bus_id, reason, reported_at, status) VALUES (?, ?, ?, 'ACTIVE')")) {
                    ps.setString(1, busId);
                    ps.setString(2, reason != null && !reason.trim().isEmpty() ? reason : "Engine mechanical breakdown");
                    ps.setString(3, now);
                    ps.executeUpdate();
                }

                conn.commit();
            } catch (SQLException ex) {
                conn.rollback();
                throw ex;
            } finally {
                conn.setAutoCommit(true);
            }
        }
        recalculateAllRoutes(LocalDate.now().toString());
    }

    public synchronized void resolveBreakdown(int breakdownId, String busId, String replacementBusId) throws SQLException {
        try (Connection conn = DBUtil.getConnection()) {
            conn.setAutoCommit(false);
            try {
                try (PreparedStatement ps = conn.prepareStatement(
                        "UPDATE breakdowns SET status = 'RESOLVED', replacement_bus_id = ? WHERE id = ?")) {
                    ps.setString(1, replacementBusId);
                    ps.setInt(2, breakdownId);
                    ps.executeUpdate();
                }

                if (replacementBusId == null || replacementBusId.isEmpty()) {
                    try (PreparedStatement ps = conn.prepareStatement("UPDATE buses SET status = 'AVAILABLE' WHERE bus_id = ?")) {
                        ps.setString(1, busId);
                        ps.executeUpdate();
                    }
                } else {
                    // Reassign students to replacement bus
                    try (PreparedStatement ps = conn.prepareStatement("UPDATE students SET assigned_bus_id = ? WHERE assigned_bus_id = ?")) {
                        ps.setString(1, replacementBusId);
                        ps.setString(2, busId);
                        ps.executeUpdate();
                    }
                }

                conn.commit();
            } catch (SQLException ex) {
                conn.rollback();
                throw ex;
            } finally {
                conn.setAutoCommit(true);
            }
        }
        recalculateAllRoutes(LocalDate.now().toString());
    }

    public List<BreakdownRecord> getAllBreakdowns() {
        List<BreakdownRecord> list = new ArrayList<>();
        String sql = "SELECT * FROM breakdowns ORDER BY id DESC";
        try (Connection conn = DBUtil.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(new BreakdownRecord(
                        rs.getInt("id"),
                        rs.getString("bus_id"),
                        rs.getString("reason"),
                        rs.getString("reported_at"),
                        rs.getString("status"),
                        rs.getString("replacement_bus_id")
                ));
            }
        } catch (SQLException e) {
            System.err.println("Error getAllBreakdowns: " + e.getMessage());
        }
        return list;
    }

    /* ========================================================================
     * REAL-TIME GPS TRACKING & MONITORING
     * ======================================================================== */

    public synchronized void updateBusLocation(String busId, double lat, double lon, double accuracy, double speed, String source) throws SQLException {
        // Validate coordinates in Tamil Nadu range
        if (lat < 8.0 || lat > 15.0 || lon < 76.0 || lon > 81.0) {
            throw new IllegalArgumentException("Coordinates (" + lat + ", " + lon + ") outside valid Tamil Nadu geographic boundaries.");
        }

        if (getBus(busId) == null) {
            throw new IllegalArgumentException("Bus '" + busId + "' does not exist.");
        }

        String now = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").format(LocalDateTime.now());
        String status = "LIVE";

        String sql = "INSERT INTO bus_locations (bus_id, latitude, longitude, accuracy, speed, last_updated, source, status) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?) " +
                "ON CONFLICT(bus_id) DO UPDATE SET " +
                "latitude = excluded.latitude, " +
                "longitude = excluded.longitude, " +
                "accuracy = excluded.accuracy, " +
                "speed = excluded.speed, " +
                "last_updated = excluded.last_updated, " +
                "source = excluded.source, " +
                "status = excluded.status";

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, busId);
            ps.setDouble(2, lat);
            ps.setDouble(3, lon);
            ps.setDouble(4, accuracy);
            ps.setDouble(5, speed);
            ps.setString(6, now);
            ps.setString(7, source != null ? source : "HARDWARE_GPS");
            ps.setString(8, status);
            ps.executeUpdate();
        }
    }

    public GPSLocation getBusLocation(String busId) {
        if (busId == null) return null;
        String sql = "SELECT * FROM bus_locations WHERE bus_id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, busId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String lastUpdated = rs.getString("last_updated");
                    String status = evaluateGpsFreshness(lastUpdated);

                    return new GPSLocation(
                            rs.getString("bus_id"),
                            rs.getDouble("latitude"),
                            rs.getDouble("longitude"),
                            rs.getDouble("accuracy"),
                            rs.getDouble("speed"),
                            lastUpdated,
                            rs.getString("source"),
                            status
                    );
                }
            }
        } catch (SQLException e) {
            System.err.println("Error getBusLocation: " + e.getMessage());
        }
        return null;
    }

    public Map<String, GPSLocation> getAllBusLocations() {
        Map<String, GPSLocation> map = new HashMap<>();
        String sql = "SELECT * FROM bus_locations";
        try (Connection conn = DBUtil.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                String busId = rs.getString("bus_id");
                String lastUpdated = rs.getString("last_updated");
                String status = evaluateGpsFreshness(lastUpdated);

                map.put(busId, new GPSLocation(
                        busId,
                        rs.getDouble("latitude"),
                        rs.getDouble("longitude"),
                        rs.getDouble("accuracy"),
                        rs.getDouble("speed"),
                        lastUpdated,
                        rs.getString("source"),
                        status
                ));
            }
        } catch (SQLException e) {
            System.err.println("Error getAllBusLocations: " + e.getMessage());
        }
        return map;
    }

    public static String evaluateGpsFreshness(String lastUpdatedStr) {
        if (lastUpdatedStr == null || lastUpdatedStr.trim().isEmpty()) {
            return "GPS UNAVAILABLE";
        }
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            LocalDateTime updateTime = LocalDateTime.parse(lastUpdatedStr, formatter);
            long minutesAgo = Duration.between(updateTime, LocalDateTime.now()).toMinutes();
            if (minutesAgo < 2) {
                return "LIVE";
            } else if (minutesAgo < 10) {
                return "STALE";
            } else {
                return "GPS UNAVAILABLE";
            }
        } catch (Exception e) {
            return "GPS UNAVAILABLE";
        }
    }

    /* ========================================================================
     * ADMIN GRAPH AND FLEET EDITING
     * ======================================================================== */

    public synchronized void addStop(String stopId, String stopName, double lat, double lon, String roadType) throws SQLException {
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "INSERT INTO stops (stop_id, stop_name, latitude, longitude, road_type) VALUES (?, ?, ?, ?, ?) " +
                     "ON CONFLICT(stop_id) DO UPDATE SET stop_name = excluded.stop_name, latitude = excluded.latitude, " +
                     "longitude = excluded.longitude, road_type = excluded.road_type")) {
            ps.setString(1, stopId.trim().toUpperCase());
            ps.setString(2, stopName.trim());
            ps.setDouble(3, lat);
            ps.setDouble(4, lon);
            ps.setString(5, roadType.trim().toUpperCase());
            ps.executeUpdate();
        }
        recalculateAllRoutes(LocalDate.now().toString());
    }

    public synchronized void addEdge(String fromStop, String toStop, int travelTime, double distanceKm, String roadType, boolean bidirectional) throws SQLException {
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "INSERT INTO road_edges (from_stop, to_stop, travel_time, distance_km, road_type) VALUES (?, ?, ?, ?, ?)")) {
            ps.setString(1, fromStop.trim());
            ps.setString(2, toStop.trim());
            ps.setInt(3, travelTime);
            ps.setDouble(4, distanceKm);
            ps.setString(5, roadType.trim().toUpperCase());
            ps.executeUpdate();

            if (bidirectional) {
                ps.setString(1, toStop.trim());
                ps.setString(2, fromStop.trim());
                ps.setInt(3, travelTime);
                ps.setDouble(4, distanceKm);
                ps.setString(5, roadType.trim().toUpperCase());
                ps.executeUpdate();
            }
        }
        recalculateAllRoutes(LocalDate.now().toString());
    }

    public synchronized void addDriver(String driverId, String username, String password,
                                       String name, String contact, String license, String busId) throws SQLException {
        String salt = DBUtil.generateSalt();
        String hash = DBUtil.hashPassword(password, salt);

        try (Connection conn = DBUtil.getConnection()) {
            conn.setAutoCommit(false);
            try {
                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO users (user_id, username, password_hash, salt, name, contact, role, status, created_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?, 'DRIVER', 'APPROVED', datetime('now', 'localtime'))")) {
                    ps.setString(1, driverId.trim().toUpperCase());
                    ps.setString(2, username.trim());
                    ps.setString(3, hash);
                    ps.setString(4, salt);
                    ps.setString(5, name.trim());
                    ps.setString(6, contact != null ? contact.trim() : "");
                    ps.executeUpdate();
                }

                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO drivers (driver_id, license_number, assigned_bus_id) VALUES (?, ?, ?)")) {
                    ps.setString(1, driverId.trim().toUpperCase());
                    ps.setString(2, license.trim());
                    ps.setString(3, busId != null && !busId.isEmpty() ? busId.trim().toUpperCase() : null);
                    ps.executeUpdate();
                }

                conn.commit();
            } catch (SQLException ex) {
                conn.rollback();
                throw ex;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    public synchronized void addBus(String busId, String busNumber, int capacity, String busType) throws SQLException {
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "INSERT INTO buses (bus_id, bus_number, capacity, bus_type, status) VALUES (?, ?, ?, ?, 'AVAILABLE')")) {
            ps.setString(1, busId.trim().toUpperCase());
            ps.setString(2, busNumber.trim().toUpperCase());
            ps.setInt(3, capacity);
            ps.setString(4, busType.trim());
            ps.executeUpdate();
        }
    }
}
