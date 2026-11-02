package smartride;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class Models {

    public enum UserRole {
        STUDENT, DRIVER, ADMIN
    }

    public enum BusStatus {
        AVAILABLE, OUT_OF_SERVICE
    }

    public static class User implements Serializable {
        private static final long serialVersionUID = 1L;
        private String userId;
        private String username;
        private String name;
        private String contact;
        private UserRole role;
        private String status;
        private String createdAt;

        public User() {}

        public User(String userId, String username, String name, String contact, UserRole role, String status, String createdAt) {
            this.userId = userId;
            this.username = username;
            this.name = name;
            this.contact = contact;
            this.role = role;
            this.status = status;
            this.createdAt = createdAt;
        }

        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getContact() { return contact; }
        public void setContact(String contact) { this.contact = contact; }
        public UserRole getRole() { return role; }
        public void setRole(UserRole role) { this.role = role; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public String getCreatedAt() { return createdAt; }
        public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    }

    public static class Student extends User {
        private static final long serialVersionUID = 1L;
        private String rollNo;
        private String department;
        private String regularStopId;
        private String tempStopId;
        private String tempBusId;
        private String tempStopDate;
        private String tempRequestStatus; // NONE, PENDING, APPROVED, REJECTED
        private String assignedBusId;
        private boolean present;
        private String absenceDate;
        private String estimatedPickupTime;

        public Student() {
            super();
            setRole(UserRole.STUDENT);
        }

        public String getRollNo() { return rollNo; }
        public void setRollNo(String rollNo) { this.rollNo = rollNo; }
        public String getDepartment() { return department; }
        public void setDepartment(String department) { this.department = department; }
        public String getRegularStopId() { return regularStopId; }
        public void setRegularStopId(String regularStopId) { this.regularStopId = regularStopId; }
        public String getTempStopId() { return tempStopId; }
        public void setTempStopId(String tempStopId) { this.tempStopId = tempStopId; }
        public String getTempBusId() { return tempBusId; }
        public void setTempBusId(String tempBusId) { this.tempBusId = tempBusId; }
        public String getTempStopDate() { return tempStopDate; }
        public void setTempStopDate(String tempStopDate) { this.tempStopDate = tempStopDate; }
        public String getTempRequestStatus() { return tempRequestStatus != null ? tempRequestStatus : "NONE"; }
        public void setTempRequestStatus(String tempRequestStatus) { this.tempRequestStatus = tempRequestStatus; }
        public String getAssignedBusId() { return assignedBusId; }
        public void setAssignedBusId(String assignedBusId) { this.assignedBusId = assignedBusId; }
        public boolean isPresent() { return present; }
        public void setPresent(boolean present) { this.present = present; }
        public String getAbsenceDate() { return absenceDate; }
        public void setAbsenceDate(String absenceDate) { this.absenceDate = absenceDate; }
        public String getEstimatedPickupTime() { return estimatedPickupTime != null ? estimatedPickupTime : "07:30 AM"; }
        public void setEstimatedPickupTime(String estimatedPickupTime) { this.estimatedPickupTime = estimatedPickupTime; }

        public String getEffectiveStopId(String forDate) {
            if (tempStopId != null && !tempStopId.trim().isEmpty() && tempStopDate != null && tempStopDate.equals(forDate)) {
                if ("APPROVED".equalsIgnoreCase(tempRequestStatus) || "NONE".equalsIgnoreCase(tempRequestStatus)) {
                    return tempStopId;
                }
            }
            return regularStopId;
        }

        public String getEffectiveBusId(String forDate) {
            if (tempBusId != null && !tempBusId.trim().isEmpty() && tempStopDate != null && tempStopDate.equals(forDate)) {
                if ("APPROVED".equalsIgnoreCase(tempRequestStatus) || "NONE".equalsIgnoreCase(tempRequestStatus)) {
                    return tempBusId;
                }
            }
            return assignedBusId;
        }

        public boolean isAbsentOn(String forDate) {
            return !present && absenceDate != null && absenceDate.equals(forDate);
        }
    }

    public static class Driver extends User {
        private static final long serialVersionUID = 1L;
        private String licenseNumber;
        private String assignedBusId;

        public Driver() {
            super();
            setRole(UserRole.DRIVER);
        }

        public String getLicenseNumber() { return licenseNumber; }
        public void setLicenseNumber(String licenseNumber) { this.licenseNumber = licenseNumber; }
        public String getAssignedBusId() { return assignedBusId; }
        public void setAssignedBusId(String assignedBusId) { this.assignedBusId = assignedBusId; }
    }

    public static class CollegeBus implements Serializable {
        private static final long serialVersionUID = 1L;
        private String busId;
        private String busNumber;
        private int capacity;
        private String busType;
        private BusStatus status;

        public CollegeBus() {}

        public CollegeBus(String busId, String busNumber, int capacity, String busType, BusStatus status) {
            this.busId = busId;
            this.busNumber = busNumber;
            this.capacity = capacity;
            this.busType = busType;
            this.status = status;
        }

        public String getBusId() { return busId; }
        public void setBusId(String busId) { this.busId = busId; }
        public String getBusNumber() { return busNumber; }
        public void setBusNumber(String busNumber) { this.busNumber = busNumber; }
        public int getCapacity() { return capacity; }
        public void setCapacity(int capacity) { this.capacity = capacity; }
        public String getBusType() { return busType; }
        public void setBusType(String busType) { this.busType = busType; }
        public BusStatus getStatus() { return status; }
        public void setStatus(BusStatus status) { this.status = status; }

        public boolean canUseRoad(String roadType) {
            if (roadType == null) return true;
            String rt = roadType.toUpperCase();
            if ("Standard 50-Seat".equalsIgnoreCase(busType) || "Standard".equalsIgnoreCase(busType)) {
                return !rt.equals("NARROW");
            }
            return true;
        }
    }

    public static class PickupStop implements Serializable {
        private static final long serialVersionUID = 1L;
        private String stopId;
        private String stopName;
        private double latitude;
        private double longitude;
        private String roadType; // HIGHWAY, ARTERIAL, SUBURBAN, NARROW

        public PickupStop() {}

        public PickupStop(String stopId, String stopName, double latitude, double longitude, String roadType) {
            this.stopId = stopId;
            this.stopName = stopName;
            this.latitude = latitude;
            this.longitude = longitude;
            this.roadType = roadType != null ? roadType.toUpperCase() : "ARTERIAL";
        }

        public String getStopId() { return stopId; }
        public void setStopId(String stopId) { this.stopId = stopId; }
        public String getStopName() { return stopName; }
        public void setStopName(String stopName) { this.stopName = stopName; }
        public double getLatitude() { return latitude; }
        public void setLatitude(double latitude) { this.latitude = latitude; }
        public double getLongitude() { return longitude; }
        public void setLongitude(double longitude) { this.longitude = longitude; }
        public String getRoadType() { return roadType; }
        public void setRoadType(String roadType) { this.roadType = roadType; }
    }

    public static class RoadEdge implements Serializable {
        private static final long serialVersionUID = 1L;
        private int id;
        private String fromStop;
        private String toStop;
        private int travelTime;
        private double distanceKm;
        private String roadType;

        public RoadEdge() {}

        public RoadEdge(int id, String fromStop, String toStop, int travelTime, double distanceKm, String roadType) {
            this.id = id;
            this.fromStop = fromStop;
            this.toStop = toStop;
            this.travelTime = travelTime;
            this.distanceKm = distanceKm;
            this.roadType = roadType != null ? roadType.toUpperCase() : "ARTERIAL";
        }

        public int getId() { return id; }
        public void setId(int id) { this.id = id; }
        public String getFromStop() { return fromStop; }
        public void setFromStop(String fromStop) { this.fromStop = fromStop; }
        public String getToStop() { return toStop; }
        public void setToStop(String toStop) { this.toStop = toStop; }
        public int getTravelTime() { return travelTime; }
        public void setTravelTime(int travelTime) { this.travelTime = travelTime; }
        public double getDistanceKm() { return distanceKm; }
        public void setDistanceKm(double distanceKm) { this.distanceKm = distanceKm; }
        public String getRoadType() { return roadType; }
        public void setRoadType(String roadType) { this.roadType = roadType; }
    }

    public static class GPSLocation implements Serializable {
        private static final long serialVersionUID = 1L;
        private String busId;
        private double latitude;
        private double longitude;
        private double accuracy;
        private double speed;
        private String lastUpdated;
        private String source; // HARDWARE_GPS, DRIVER_DEVICE, TEST_SIMULATION
        private String status; // LIVE, STALE, GPS_UNAVAILABLE

        public GPSLocation() {}

        public GPSLocation(String busId, double latitude, double longitude, double accuracy, double speed,
                           String lastUpdated, String source, String status) {
            this.busId = busId;
            this.latitude = latitude;
            this.longitude = longitude;
            this.accuracy = accuracy;
            this.speed = speed;
            this.lastUpdated = lastUpdated;
            this.source = source;
            this.status = status;
        }

        public String getBusId() { return busId; }
        public void setBusId(String busId) { this.busId = busId; }
        public double getLatitude() { return latitude; }
        public void setLatitude(double latitude) { this.latitude = latitude; }
        public double getLongitude() { return longitude; }
        public void setLongitude(double longitude) { this.longitude = longitude; }
        public double getAccuracy() { return accuracy; }
        public void setAccuracy(double accuracy) { this.accuracy = accuracy; }
        public double getSpeed() { return speed; }
        public void setSpeed(double speed) { this.speed = speed; }
        public String getLastUpdated() { return lastUpdated; }
        public void setLastUpdated(String lastUpdated) { this.lastUpdated = lastUpdated; }
        public String getSource() { return source; }
        public void setSource(String source) { this.source = source; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }

        public boolean isTestSimulation() {
            return "TEST_SIMULATION".equalsIgnoreCase(source);
        }
    }

    public static class BoardingRecord implements Serializable {
        private static final long serialVersionUID = 1L;
        private int id;
        private String tripDate;
        private String busId;
        private String studentId;
        private String rollNo;
        private String studentName;
        private String stopId;
        private String boardingTime;
        private String status; // BOARDED, NO_SHOW

        public BoardingRecord() {}

        public BoardingRecord(int id, String tripDate, String busId, String studentId,
                              String rollNo, String studentName, String stopId, String boardingTime, String status) {
            this.id = id;
            this.tripDate = tripDate;
            this.busId = busId;
            this.studentId = studentId;
            this.rollNo = rollNo;
            this.studentName = studentName;
            this.stopId = stopId;
            this.boardingTime = boardingTime;
            this.status = status;
        }

        public int getId() { return id; }
        public String getTripDate() { return tripDate; }
        public String getBusId() { return busId; }
        public String getStudentId() { return studentId; }
        public String getRollNo() { return rollNo; }
        public String getStudentName() { return studentName; }
        public String getStopId() { return stopId; }
        public String getBoardingTime() { return boardingTime; }
        public String getStatus() { return status; }
    }

    public static class BreakdownRecord implements Serializable {
        private static final long serialVersionUID = 1L;
        private int id;
        private String busId;
        private String reason;
        private String reportedAt;
        private String status; // ACTIVE, RESOLVED
        private String replacementBusId;

        public BreakdownRecord() {}

        public BreakdownRecord(int id, String busId, String reason, String reportedAt, String status, String replacementBusId) {
            this.id = id;
            this.busId = busId;
            this.reason = reason;
            this.reportedAt = reportedAt;
            this.status = status;
            this.replacementBusId = replacementBusId;
        }

        public int getId() { return id; }
        public String getBusId() { return busId; }
        public String getReason() { return reason; }
        public String getReportedAt() { return reportedAt; }
        public String getStatus() { return status; }
        public String getReplacementBusId() { return replacementBusId; }
    }

    public static class TempChangeRequest implements Serializable {
        private static final long serialVersionUID = 1L;
        private int id;
        private String studentId;
        private String rollNo;
        private String studentName;
        private String changeDate;
        private String requestedStopId;
        private String requestedStopName;
        private String requestedBusId;
        private String reason;
        private String status; // PENDING, APPROVED, REJECTED
        private String createdAt;

        public TempChangeRequest() {}

        public TempChangeRequest(int id, String studentId, String rollNo, String studentName,
                                 String changeDate, String requestedStopId, String requestedStopName,
                                 String requestedBusId, String reason, String status, String createdAt) {
            this.id = id;
            this.studentId = studentId;
            this.rollNo = rollNo;
            this.studentName = studentName;
            this.changeDate = changeDate;
            this.requestedStopId = requestedStopId;
            this.requestedStopName = requestedStopName;
            this.requestedBusId = requestedBusId;
            this.reason = reason;
            this.status = status;
            this.createdAt = createdAt;
        }

        public int getId() { return id; }
        public String getStudentId() { return studentId; }
        public String getRollNo() { return rollNo; }
        public String getStudentName() { return studentName; }
        public String getChangeDate() { return changeDate; }
        public String getRequestedStopId() { return requestedStopId; }
        public String getRequestedStopName() { return requestedStopName; }
        public String getRequestedBusId() { return requestedBusId; }
        public String getReason() { return reason; }
        public String getStatus() { return status; }
        public String getCreatedAt() { return createdAt; }
    }

    public static class RouteStopInfo implements Serializable {
        private static final long serialVersionUID = 1L;
        private final int sequence;
        private final String stopId;
        private final String stopName;
        private final double latitude;
        private final double longitude;
        private final String roadType;
        private final int legTravelMinutes;
        private final double legDistanceKm;
        private final int cumulativeMinutes;
        private final String arrivalTime;
        private final boolean isDepot;
        private final boolean isReturnDepot;

        public RouteStopInfo(int sequence, String stopId, String stopName, double latitude, double longitude,
                             String roadType, int legTravelMinutes, double legDistanceKm, int cumulativeMinutes,
                             String arrivalTime, boolean isDepot, boolean isReturnDepot) {
            this.sequence = sequence;
            this.stopId = stopId;
            this.stopName = stopName != null ? stopName : stopId;
            this.latitude = latitude;
            this.longitude = longitude;
            this.roadType = roadType != null ? roadType.toUpperCase() : "ARTERIAL";
            this.legTravelMinutes = legTravelMinutes;
            this.legDistanceKm = legDistanceKm;
            this.cumulativeMinutes = cumulativeMinutes;
            this.arrivalTime = arrivalTime;
            this.isDepot = isDepot;
            this.isReturnDepot = isReturnDepot;
        }

        public int getSequence() { return sequence; }
        public String getStopId() { return stopId; }
        public String getStopName() { return stopName; }
        public double getLatitude() { return latitude; }
        public double getLongitude() { return longitude; }
        public String getRoadType() { return roadType; }
        public int getLegTravelMinutes() { return legTravelMinutes; }
        public double getLegDistanceKm() { return legDistanceKm; }
        public int getCumulativeMinutes() { return cumulativeMinutes; }
        public String getArrivalTime() { return arrivalTime; }
        public boolean isDepot() { return isDepot; }
        public boolean isReturnDepot() { return isReturnDepot; }
    }

    public static class RouteOptimizationResult implements Serializable {
        private static final long serialVersionUID = 1L;
        private final List<RouteStopInfo> stopSequence;
        private final ArrayList<String> route;
        private final int totalTravelTimeMinutes;
        private final double totalDistanceKm;
        private final List<String> visitedStops;
        private final List<String> skippedEmptyStops;
        private final Map<String, String> stopEstimatedPickupTimes;

        public RouteOptimizationResult(List<RouteStopInfo> stopSequence, ArrayList<String> route,
                                       int totalTravelTimeMinutes, double totalDistanceKm,
                                       List<String> visitedStops, List<String> skippedEmptyStops,
                                       Map<String, String> stopEstimatedPickupTimes) {
            this.stopSequence = stopSequence != null ? stopSequence : Collections.emptyList();
            this.route = route != null ? route : new ArrayList<>();
            this.totalTravelTimeMinutes = totalTravelTimeMinutes;
            this.totalDistanceKm = totalDistanceKm;
            this.visitedStops = visitedStops != null ? visitedStops : Collections.emptyList();
            this.skippedEmptyStops = skippedEmptyStops != null ? skippedEmptyStops : Collections.emptyList();
            this.stopEstimatedPickupTimes = stopEstimatedPickupTimes != null ? stopEstimatedPickupTimes : Collections.emptyMap();
        }

        public List<RouteStopInfo> getStopSequence() { return stopSequence; }
        public ArrayList<String> getRoute() { return route; }
        public int getTotalTravelTimeMinutes() { return totalTravelTimeMinutes; }
        public double getTotalDistanceKm() { return totalDistanceKm; }
        public List<String> getVisitedStops() { return visitedStops; }
        public List<String> getSkippedEmptyStops() { return skippedEmptyStops; }
        public Map<String, String> getStopEstimatedPickupTimes() { return stopEstimatedPickupTimes; }

        public String getReturnDepotTime() {
            if (!stopSequence.isEmpty()) {
                RouteStopInfo last = stopSequence.get(stopSequence.size() - 1);
                if (last.isReturnDepot()) {
                    return last.getArrivalTime();
                }
            }
            return "08:30 AM";
        }
    }
}
