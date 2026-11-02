<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page import="java.time.LocalDate, java.util.*, smartride.TransportDAO, smartride.Models.*" %>
<%
    // Role-based session access check
    HttpSession userSession = request.getSession(false);
    if (userSession == null || !"DRIVER".equals(userSession.getAttribute("userRole"))) {
        response.sendRedirect(request.getContextPath() + "/index.jsp?error=Please+sign+in+as+driver.");
        return;
    }

    String userId = (String) userSession.getAttribute("userId");
    TransportDAO dao = new TransportDAO();
    Driver driver = dao.getDriverByUserId(userId);
    if (driver == null || driver.getAssignedBusId() == null) {
        response.sendRedirect(request.getContextPath() + "/index.jsp?error=Driver+account+has+no+assigned+bus.");
        return;
    }

    String today = LocalDate.now().toString();
    String busId = driver.getAssignedBusId();
    CollegeBus bus = dao.getBus(busId);
    GPSLocation gps = dao.getBusLocation(busId);

    // Recalculate routes to get latest planned sequence
    Map<String, RouteOptimizationResult> routeResults = dao.recalculateAllRoutes(today);
    RouteOptimizationResult optResult = routeResults.get(busId);

    // Boarding roster
    List<BoardingRecord> boardings = dao.getDailyBoardingRecords(busId, today);
    Map<String, String> studentBoardingStatus = new HashMap<>();
    for (BoardingRecord br : boardings) {
        studentBoardingStatus.put(br.getRollNo(), br.getStatus());
    }

    // Filter students assigned to this bus today
    List<Student> allStudents = dao.getAllStudents();
    List<Student> busStudents = new ArrayList<>();
    int presentCount = 0;
    int absentCount = 0;
    int boardedCount = 0;
    int noShowCount = 0;

    for (Student s : allStudents) {
        String effBus = s.getEffectiveBusId(today);
        if (busId.equals(effBus)) {
            busStudents.add(s);
            if (s.isAbsentOn(today)) {
                absentCount++;
            } else {
                presentCount++;
                String bStatus = studentBoardingStatus.get(s.getRollNo());
                if ("BOARDED".equals(bStatus)) boardedCount++;
                else if ("NO_SHOW".equals(bStatus)) noShowCount++;
            }
        }
    }
%>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <meta http-equiv="refresh" content="30">
    <title>Driver Dashboard — SMARTRIDE</title>
    <link rel="stylesheet" href="css/style.css">
</head>
<body>

    <header class="navbar">
        <a href="driver_dashboard.jsp" class="brand">
            <span>&#128652; SMARTRIDE</span>
            <span class="brand-subtitle">| Driver Console</span>
        </a>
        <div class="nav-links">
            <span class="nav-user-info">&#128652; Driver <%= driver.getName() %> (<%= busId %>)</span>
            <a href="logout" class="btn btn-outline btn-sm" style="color: #ffffff; border-color: rgba(255,255,255,0.4);">&#128682; Logout</a>
        </div>
    </header>

    <main class="container">

        <%-- Alerts --%>
        <%
            String error = request.getParameter("error");
            String msg = request.getParameter("msg");
            if (error != null && !error.trim().isEmpty()) {
        %>
            <div class="alert alert-error">
                <strong>Driver Notice:</strong> <%= error %>
            </div>
        <%
            }
            if (msg != null && !msg.trim().isEmpty()) {
        %>
            <div class="alert alert-success">
                <strong>Status:</strong> <%= msg %>
            </div>
        <%
            }
        %>

        <%-- Top Driver KPI Bar --%>
        <div class="stats-bar">
            <div class="stat-box primary">
                <div class="stat-label">Vehicle Assigned</div>
                <div class="stat-value"><%= busId %></div>
                <div style="font-size: 0.85rem; color: var(--text-muted); margin-top: 4px;">
                    <%= bus != null ? bus.getBusNumber() + " (" + bus.getBusType() + ")" : "N/A" %>
                </div>
            </div>

            <div class="stat-box success">
                <div class="stat-label">Scheduled Riders</div>
                <div class="stat-value"><%= presentCount %></div>
                <div style="font-size: 0.85rem; color: var(--text-muted); margin-top: 4px;">
                    Active riders scheduled today
                </div>
            </div>

            <div class="stat-box primary">
                <div class="stat-label">Physically Boarded</div>
                <div class="stat-value"><%= boardedCount %> / <%= presentCount %></div>
                <div style="font-size: 0.85rem; color: var(--text-muted); margin-top: 4px;">
                    Verified via Roll Number check-in
                </div>
            </div>

            <div class="stat-box danger">
                <div class="stat-label">Absences / No-Shows</div>
                <div class="stat-value"><%= absentCount %> Abs / <%= noShowCount %> NS</div>
                <div style="font-size: 0.85rem; color: var(--text-muted); margin-top: 4px;">
                    Absence declarations &amp; no-shows
                </div>
            </div>
        </div>

        <%-- SECTION 1: Real-Time Geographical Map --%>
        <div class="card" style="padding: 0; overflow: hidden;">
            <div class="map-header">
                <div>
                    <strong>&#127757; Geographical Map &amp; Real-Time Bus Tracker</strong>
                    <span style="opacity: 0.8; font-size: 0.82rem; margin-left: 8px;">Vehicle: <%= busId %></span>
                </div>
                <div style="display: flex; align-items: center; gap: 8px;">
                    <% if (gps != null) { %>
                        <span class="badge <%= "LIVE".equals(gps.getStatus()) ? "badge-success" : ("STALE".equals(gps.getStatus()) ? "badge-warning" : "badge-danger") %>">
                            GPS: <%= gps.getStatus() %>
                        </span>
                        <span style="font-size: 0.8rem; opacity: 0.85;">Last Ping: <%= gps.getLastUpdated() %></span>
                    <% } else { %>
                        <span class="badge badge-danger">GPS UNAVAILABLE</span>
                    <% } %>
                    <a href="driver_dashboard.jsp" class="btn btn-outline btn-sm" style="color: #ffffff; border-color: rgba(255,255,255,0.4); padding: 3px 8px;">&#8635; Refresh Map</a>
                </div>
            </div>

            <div style="position: relative; background-color: #f8fafc;">
                <img src="map?busId=<%= busId %>" alt="Geographical Route Map" class="map-img">
            </div>
        </div>

        <div style="display: grid; grid-template-columns: repeat(auto-fit, minmax(360px, 1fr)); gap: 20px;">

            <%-- SECTION 2: Student Boarding Check-in Form --%>
            <div class="card">
                <div class="card-header">
                    <h2 class="card-title">&#10003; Student Boarding Verification</h2>
                </div>
                <p style="font-size: 0.88rem; color: var(--text-muted); margin-bottom: 16px;">
                    Enter the student's unique college roll number when they board. Duplicate boarding records for the same trip are strictly prevented.
                </p>

                <form method="POST" action="driver/board">
                    <input type="hidden" name="tripDate" value="<%= today %>">
                    <div class="form-group">
                        <label for="boardRoll">Student College Roll Number *</label>
                        <input type="text" id="boardRoll" name="rollNo" class="form-control" placeholder="e.g. 25CSR001" required autofocus>
                    </div>

                    <button type="submit" class="btn btn-success btn-block">&#10003; Verify &amp; Record Boarding</button>
                </form>

                <div style="margin-top: 16px; border-top: 1px dashed var(--border-color); padding-top: 14px;">
                    <label style="font-size: 0.84rem; color: var(--text-muted); margin-bottom: 6px;">Mark Scheduled Student as NO-SHOW:</label>
                    <form method="POST" action="driver/noshow" style="display: flex; gap: 8px;">
                        <input type="hidden" name="tripDate" value="<%= today %>">
                        <input type="text" name="rollNo" class="form-control" placeholder="Roll No (e.g. 25CSR002)" required style="flex: 1;">
                        <button type="submit" class="btn btn-danger btn-sm">&#10007; Mark No-Show</button>
                    </form>
                </div>
            </div>

            <%-- SECTION 3: Vehicle Breakdown Reporting --%>
            <div class="card">
                <div class="card-header">
                    <h2 class="card-title">&#9888; Report Breakdown or Delay</h2>
                </div>
                <p style="font-size: 0.88rem; color: var(--text-muted); margin-bottom: 16px;">
                    Report mechanical failures, tyre punctures, or unexpected delays. Marking breakdown alerts the admin to coordinate a replacement bus.
                </p>

                <form method="POST" action="driver/breakdown">
                    <div class="form-group">
                        <label for="breakdownReason">Nature of Breakdown / Incident *</label>
                        <textarea id="breakdownReason" name="reason" class="form-control" rows="3" placeholder="e.g. Engine overheating near Perundurai NH junction" required></textarea>
                    </div>

                    <button type="submit" class="btn btn-danger btn-block">&#9888; Report Breakdown (Mark Out of Service)</button>
                </form>

                <%-- TEST MODE GPS COORDINATES INPUT --%>
                <div style="margin-top: 20px; border-top: 1px solid var(--border-color); padding-top: 14px; background: #f8fafc; padding: 12px; border-radius: var(--radius-sm);">
                    <div style="font-weight: 700; font-size: 0.82rem; color: #b45309; margin-bottom: 6px;">
                        &#128225; [TEST MODE GPS INPUT — SIMULATED COORDINATES]
                    </div>
                    <p style="font-size: 0.78rem; color: var(--text-muted); margin-bottom: 8px;">
                        If physical GPS tracker hardware is unavailable, test live coordinate updates here. Updates will be visibly labelled as Simulated Test Data.
                    </p>
                    <form method="POST" action="api/gps">
                        <input type="hidden" name="bus_id" value="<%= busId %>">
                        <input type="hidden" name="source" value="TEST_SIMULATION">
                        <input type="hidden" name="redirect" value="driver_dashboard.jsp">
                        <div class="form-row" style="margin-bottom: 8px;">
                            <input type="text" name="latitude" class="form-control" placeholder="Lat (e.g. 11.3410)" value="11.3410" required>
                            <input type="text" name="longitude" class="form-control" placeholder="Lon (e.g. 77.7172)" value="77.7172" required>
                            <input type="text" name="speed" class="form-control" placeholder="Speed (e.g. 35)" value="35">
                        </div>
                        <button type="submit" class="btn btn-outline btn-sm btn-block" style="border-color: #f59e0b; color: #b45309;">
                            &#128225; Submit Simulated GPS Location
                        </button>
                    </form>
                </div>
            </div>

        </div>

        <%-- SECTION 4: Today's Student Pickup Manifest --%>
        <div class="card" style="margin-top: 20px;">
            <div class="card-header">
                <h2 class="card-title">&#128203; Student Boarding Manifest for <%= busId %> (<%= today %>)</h2>
            </div>
            <div class="table-responsive">
                <table class="table">
                    <thead>
                        <tr>
                            <th>Roll Number</th>
                            <th>Student Name</th>
                            <th>Dept</th>
                            <th>Effective Pickup Stop</th>
                            <th>Estimated Pickup</th>
                            <th>Travel Declaration</th>
                            <th>Physical Boarding Status</th>
                            <th>Action</th>
                        </tr>
                    </thead>
                    <tbody>
                        <% for (Student s : busStudents) { %>
                            <%
                                boolean isAbsent = s.isAbsentOn(today);
                                String effStop = s.getEffectiveStopId(today);
                                String stopName = dao.getStopName(effStop);
                                String bStatus = studentBoardingStatus.get(s.getRollNo());
                            %>
                            <tr>
                                <td><strong><%= s.getRollNo() %></strong></td>
                                <td><%= s.getName() %></td>
                                <td><span class="badge badge-neutral"><%= s.getDepartment() %></span></td>
                                <td><%= stopName %></td>
                                <td><strong><%= isAbsent ? "N/A" : s.getEstimatedPickupTime() %></strong></td>
                                <td>
                                    <% if (isAbsent) { %>
                                        <span class="badge badge-danger">&#10007; ABSENT DECLARED</span>
                                    <% } else { %>
                                        <span class="badge badge-success">&#10003; SCHEDULED PRESENT</span>
                                    <% } %>
                                </td>
                                <td>
                                    <% if ("BOARDED".equals(bStatus)) { %>
                                        <span class="badge badge-success">&#10003; BOARDED</span>
                                    <% } else if ("NO_SHOW".equals(bStatus)) { %>
                                        <span class="badge badge-danger">&#10007; NO SHOW</span>
                                    <% } else { %>
                                        <span class="badge badge-warning">AWAITING PICKUP</span>
                                    <% } %>
                                </td>
                                <td>
                                    <% if (!isAbsent && !"BOARDED".equals(bStatus)) { %>
                                        <div style="display: flex; gap: 4px;">
                                            <form method="POST" action="driver/board" style="display:inline;">
                                                <input type="hidden" name="tripDate" value="<%= today %>">
                                                <input type="hidden" name="rollNo" value="<%= s.getRollNo() %>">
                                                <button type="submit" class="btn btn-success btn-sm" style="padding: 3px 8px; font-size: 0.76rem;">Board</button>
                                            </form>
                                            <form method="POST" action="driver/noshow" style="display:inline;">
                                                <input type="hidden" name="tripDate" value="<%= today %>">
                                                <input type="hidden" name="rollNo" value="<%= s.getRollNo() %>">
                                                <button type="submit" class="btn btn-danger btn-sm" style="padding: 3px 8px; font-size: 0.76rem;">No-Show</button>
                                            </form>
                                        </div>
                                    <% } else { %>
                                        <span style="font-size: 0.8rem; color: var(--text-muted);">&mdash;</span>
                                    <% } %>
                                </td>
                            </tr>
                        <% } %>
                    </tbody>
                </table>
            </div>
        </div>

    </main>

    <footer class="footer">
        <strong>SMARTRIDE — Intelligent Campus Transport Management System</strong> | College CSE Mini-Project &bull; Driver Portal
    </footer>

</body>
</html>
