<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page import="java.time.LocalDate, java.util.List, java.util.Map, smartride.TransportDAO, smartride.Models.*" %>
<%
    // Role-based session access check
    HttpSession userSession = request.getSession(false);
    if (userSession == null || !"STUDENT".equals(userSession.getAttribute("userRole"))) {
        response.sendRedirect(request.getContextPath() + "/index.jsp?error=Please+sign+in+as+student.");
        return;
    }

    String userId = (String) userSession.getAttribute("userId");
    TransportDAO dao = new TransportDAO();
    Student student = dao.getStudentByUserId(userId);
    if (student == null) {
        response.sendRedirect(request.getContextPath() + "/logout");
        return;
    }

    String today = LocalDate.now().toString();
    String tomorrow = LocalDate.now().plusDays(1).toString();

    List<PickupStop> allStops = dao.getAllStops();
    Map<String, CollegeBus> allBuses = dao.getAllBusesMap();

    // Recalculate routes to ensure arrival times are up-to-date
    Map<String, RouteOptimizationResult> routeResults = dao.recalculateAllRoutes(today);
    // Refresh student model from DB after recalculation
    student = dao.getStudentByUserId(userId);

    String effectiveBusId = student.getEffectiveBusId(today);
    CollegeBus bus = allBuses.get(effectiveBusId);
    RouteOptimizationResult optResult = (bus != null) ? routeResults.get(bus.getBusId()) : null;

    String regularStopName = dao.getStopName(student.getRegularStopId());
    String effectiveStopId = student.getEffectiveStopId(today);
    String effectiveStopName = dao.getStopName(effectiveStopId);
    boolean hasTempChange = student.getTempStopId() != null && !student.getTempStopId().trim().isEmpty() && today.equals(student.getTempStopDate());

    GPSLocation gps = (bus != null) ? dao.getBusLocation(bus.getBusId()) : null;
    List<TempChangeRequest> tempRequests = dao.getStudentTempRequests(student.getUserId());
%>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <meta http-equiv="refresh" content="30">
    <title>Student Dashboard — SMARTRIDE</title>
    <link rel="stylesheet" href="css/style.css">
</head>
<body>

    <header class="navbar">
        <a href="student_dashboard.jsp" class="brand">
            <span>&#128652; SMARTRIDE</span>
            <span class="brand-subtitle">| Student Portal</span>
        </a>
        <div class="nav-links">
            <span class="nav-user-info">&#127891; <%= student.getName() %> (<%= student.getRollNo() %>)</span>
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
                <strong>Notice:</strong> <%= error %>
            </div>
        <%
            }
            if (msg != null && !msg.trim().isEmpty()) {
        %>
            <div class="alert alert-success">
                <strong>Success:</strong> <%= msg %>
            </div>
        <%
            }
        %>

        <%-- Top Stats Banner --%>
        <div class="stats-bar">
            <div class="stat-box primary">
                <div class="stat-label">Assigned Vehicle</div>
                <div class="stat-value"><%= bus != null ? bus.getBusId() : "Unassigned" %></div>
                <div style="font-size: 0.85rem; color: var(--text-muted); margin-top: 4px;">
                    <%= bus != null ? bus.getBusNumber() + " (" + bus.getBusType() + ")" : "Pending Route Allocation" %>
                </div>
            </div>

            <div class="stat-box success">
                <div class="stat-label">Expected Pickup Time</div>
                <div class="stat-value"><%= student.getEstimatedPickupTime() %></div>
                <div style="font-size: 0.85rem; color: var(--text-muted); margin-top: 4px;">
                    Calculated dynamically via Dijkstra
                </div>
            </div>

            <div class="stat-box <%= student.isPresent() ? "success" : "danger" %>">
                <div class="stat-label">Daily Travel Status</div>
                <div class="stat-value" style="font-size: 1.3rem; margin-top: 6px;">
                    <% if (student.isPresent()) { %>
                        <span class="badge badge-success">&#10003; SCHEDULED TO TRAVEL</span>
                    <% } else { %>
                        <span class="badge badge-danger">&#10007; MARKED ABSENT</span>
                    <% } %>
                </div>
                <div style="font-size: 0.85rem; color: var(--text-muted); margin-top: 4px;">
                    <%= student.isPresent() ? "Seat scheduled for pickup" : "Absent date: " + student.getAbsenceDate() %>
                </div>
            </div>

            <div class="stat-box warning">
                <div class="stat-label">Pickup Location</div>
                <div class="stat-value" style="font-size: 1.2rem; margin-top: 6px;">
                    <%= effectiveStopName %>
                </div>
                <div style="font-size: 0.85rem; color: var(--text-muted); margin-top: 4px;">
                    <% if (hasTempChange) { %>
                        <span class="badge badge-primary">1-Day Override (<%= student.getTempStopDate() %>)</span>
                    <% } else { %>
                        Regular Stop: <%= regularStopName %>
                    <% } %>
                </div>
            </div>
        </div>

        <%-- SECTION 1: Real-Time Geographical Map (Zero JavaScript) --%>
        <div class="card" style="padding: 0; overflow: hidden;">
            <div class="map-header">
                <div>
                    <strong>&#127757; Live Geographical Bus Route &amp; GPS Tracker</strong>
                    <span style="opacity: 0.8; font-size: 0.82rem; margin-left: 8px;">Vehicle: <%= bus != null ? bus.getBusId() : "N/A" %></span>
                </div>
                <div style="display: flex; align-items: center; gap: 8px;">
                    <% if (gps != null) { %>
                        <span class="badge <%= "LIVE".equals(gps.getStatus()) ? "badge-success" : ("STALE".equals(gps.getStatus()) ? "badge-warning" : "badge-danger") %>">
                            GPS: <%= gps.getStatus() %>
                        </span>
                        <span style="font-size: 0.8rem; opacity: 0.85;">Updated: <%= gps.getLastUpdated() %></span>
                    <% } else { %>
                        <span class="badge badge-danger">GPS UNAVAILABLE</span>
                    <% } %>
                    <a href="student_dashboard.jsp" class="btn btn-outline btn-sm" style="color: #ffffff; border-color: rgba(255,255,255,0.4); padding: 3px 8px;">&#8635; Refresh</a>
                </div>
            </div>

            <div style="position: relative; background-color: #f8fafc;">
                <% if (bus != null) { %>
                    <img src="map?busId=<%= bus.getBusId() %>" alt="Geographical Map" class="map-img">
                <% } else { %>
                    <div style="padding: 40px; text-align: center; color: var(--text-muted);">
                        No bus currently assigned to this student account.
                    </div>
                <% } %>
            </div>
        </div>

        <div style="display: grid; grid-template-columns: repeat(auto-fit, minmax(360px, 1fr)); gap: 20px;">

            <%-- SECTION 2: Daily Attendance Declaration (Default Present) --%>
            <div class="card">
                <div class="card-header">
                    <h2 class="card-title">&#128197; Attendance System (Default Present)</h2>
                </div>
                <p style="font-size: 0.88rem; color: var(--text-muted); margin-bottom: 16px;">
                    By campus policy, you are scheduled for pickup by default. If you will be absent, declare it in advance so unneeded stops can be bypassed.
                </p>

                <div style="background-color: var(--bg-page); border: 1px solid var(--border-color); border-radius: var(--radius-sm); padding: 14px; margin-bottom: 16px;">
                    <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px;">
                        <span>Current Travel Declaration:</span>
                        <% if (student.isPresent()) { %>
                            <span class="badge badge-success">&#10003; PRESENT &amp; SCHEDULED</span>
                        <% } else { %>
                            <span class="badge badge-danger">&#10007; ABSENT DECLARED (<%= student.getAbsenceDate() %>)</span>
                        <% } %>
                    </div>
                    <div style="font-size: 0.82rem; color: var(--text-muted);">
                        <%= student.isPresent() ? "Bus will stop at " + effectiveStopName + " for your pickup." : "Pickup stop " + effectiveStopName + " will be skipped if no other student boards there." %>
                    </div>
                </div>

                <form method="POST" action="student/absence">
                    <div class="form-group">
                        <label for="absenceDate">Select Target Date</label>
                        <input type="date" id="absenceDate" name="date" class="form-control" value="<%= tomorrow %>" min="<%= today %>" required>
                    </div>

                    <% if (student.isPresent()) { %>
                        <input type="hidden" name="action" value="mark_absent">
                        <button type="submit" class="btn btn-danger btn-block">&#9888; Declare Absence for Selected Date</button>
                    <% } else { %>
                        <input type="hidden" name="action" value="mark_present">
                        <button type="submit" class="btn btn-success btn-block">&#10003; Cancel Absence &amp; Restore Scheduled Present</button>
                    <% } %>
                </form>
            </div>

            <%-- SECTION 3: One-Day Temporary Pickup Stop & Bus Change --%>
            <div class="card">
                <div class="card-header">
                    <h2 class="card-title">&#128260; One-Day Flexible Pickup Change</h2>
                </div>
                <p style="font-size: 0.88rem; color: var(--text-muted); margin-bottom: 16px;">
                    Temporarily board at a different stop or transfer to another campus bus for one specific day. Your regular stop is automatically restored afterward.
                </p>

                <form method="POST" action="student/temp-change">
                    <div class="form-group">
                        <label for="tempDate">Target Date *</label>
                        <input type="date" id="tempDate" name="date" class="form-control" value="<%= today %>" min="<%= today %>" required>
                    </div>

                    <div class="form-row">
                        <div class="form-group">
                            <label for="newStopId">New Temporary Stop *</label>
                            <select id="newStopId" name="newStopId" class="form-control" required>
                                <option value="">-- Choose Erode Stop --</option>
                                <% for (PickupStop ps : allStops) { %>
                                    <% if (!"C00".equals(ps.getStopId())) { %>
                                        <option value="<%= ps.getStopId() %>">
                                            <%= ps.getStopName() %> (<%= ps.getRoadType() %>)
                                        </option>
                                    <% } %>
                                <% } %>
                            </select>
                        </div>

                        <div class="form-group">
                            <label for="newBusId">Target Bus (Optional)</label>
                            <select id="newBusId" name="newBusId" class="form-control">
                                <option value="">-- Current Bus (<%= student.getAssignedBusId() %>) --</option>
                                <% for (CollegeBus b : allBuses.values()) { %>
                                    <% if (!b.getBusId().equals(student.getAssignedBusId())) { %>
                                        <option value="<%= b.getBusId() %>"><%= b.getBusId() %> - <%= b.getBusNumber() %> (<%= b.getBusType() %>)</option>
                                    <% } %>
                                <% } %>
                            </select>
                        </div>
                    </div>

                    <div class="form-group">
                        <label for="reason">Reason for 1-Day Change</label>
                        <input type="text" id="reason" name="reason" class="form-control" placeholder="e.g. Staying with relatives in Bhavani">
                    </div>

                    <button type="submit" class="btn btn-primary btn-block">&#10003; Activate 1-Day Temporary Change</button>
                </form>
            </div>

        </div>

        <%-- SECTION 4: Active Route Stop Sequence & Arrival Times Table --%>
        <div class="card" style="margin-top: 20px;">
            <div class="card-header">
                <h2 class="card-title">&#128652; Calculated Route Sequence &amp; Arrival Times for <%= bus != null ? bus.getBusId() : "Vehicle" %></h2>
            </div>

            <% if (optResult != null && !optResult.getStopSequence().isEmpty()) { %>
                <div class="route-steps-box">
                    <%
                        List<RouteStopInfo> rStops = optResult.getStopSequence();
                        for (int i = 0; i < rStops.size(); i++) {
                            RouteStopInfo rsi = rStops.get(i);
                            boolean isMyStop = rsi.getStopId().equals(effectiveStopId);
                    %>
                        <div class="route-stop-node <%= rsi.isDepot() ? "depot" : (isMyStop ? "primary" : "") %>" style="<%= isMyStop ? "border: 2px solid var(--accent); background: #eff6ff;" : "" %>">
                            <div><%= rsi.isDepot() ? (rsi.isReturnDepot() ? "&#127979; Depot (Return)" : "&#127979; Depot (Start)") : rsi.getStopName() %></div>
                            <div style="font-size: 0.72rem; font-weight: 500; color: <%= isMyStop ? "var(--accent)" : "var(--text-muted)" %>;">
                                <%= rsi.getArrivalTime() %>
                                <% if (rsi.getLegTravelMinutes() > 0) { %> (+<%= rsi.getLegTravelMinutes() %>m) <% } %>
                            </div>
                        </div>
                        <% if (i < rStops.size() - 1) { %>
                            <span class="route-arrow">&rarr;</span>
                        <% } %>
                    <% } %>
                </div>

                <div class="table-responsive">
                    <table class="table">
                        <thead>
                            <tr>
                                <th>Seq</th>
                                <th>Stop Name</th>
                                <th>Road Type</th>
                                <th>Leg Duration</th>
                                <th>Distance</th>
                                <th>Expected Arrival</th>
                                <th>Status</th>
                            </tr>
                        </thead>
                        <tbody>
                            <% for (RouteStopInfo rsi : optResult.getStopSequence()) { %>
                                <% boolean isMyStop = rsi.getStopId().equals(effectiveStopId); %>
                                <tr style="<%= isMyStop ? "background-color: #eff6ff; font-weight: 600;" : "" %>">
                                    <td>#<%= rsi.getSequence() %></td>
                                    <td><%= rsi.getStopName() %></td>
                                    <td><span class="badge badge-neutral"><%= rsi.getRoadType() %></span></td>
                                    <td><%= rsi.getLegTravelMinutes() == 0 ? "Depot Start" : "+" + rsi.getLegTravelMinutes() + " mins" %></td>
                                    <td><%= rsi.getLegDistanceKm() == 0.0 ? "0.0 km" : String.format("%.1f km", rsi.getLegDistanceKm()) %></td>
                                    <td><strong style="color: var(--primary);"><%= rsi.getArrivalTime() %></strong></td>
                                    <td>
                                        <% if (isMyStop) { %>
                                            <span class="badge badge-primary">&#10003; YOUR PICKUP STOP</span>
                                        <% } else if (rsi.isDepot()) { %>
                                            <span class="badge badge-warning">CAMPUS DEPOT</span>
                                        <% } else { %>
                                            <span class="badge badge-neutral">Scheduled</span>
                                        <% } %>
                                    </td>
                                </tr>
                            <% } %>
                        </tbody>
                    </table>
                </div>
            <% } else { %>
                <p style="color: var(--text-muted); font-size: 0.9rem;">Route calculation pending or bus has no scheduled pickups today.</p>
            <% } %>
        </div>

        <%-- SECTION 5: Temporary Stop Requests History --%>
        <% if (!tempRequests.isEmpty()) { %>
            <div class="card" style="margin-top: 20px;">
                <div class="card-header">
                    <h2 class="card-title">&#128221; Your 1-Day Change History</h2>
                </div>
                <div class="table-responsive">
                    <table class="table">
                        <thead>
                            <tr>
                                <th>Request ID</th>
                                <th>Target Date</th>
                                <th>Requested Stop</th>
                                <th>Target Bus</th>
                                <th>Reason</th>
                                <th>Status</th>
                            </tr>
                        </thead>
                        <tbody>
                            <% for (TempChangeRequest req : tempRequests) { %>
                                <tr>
                                    <td>#<%= req.getId() %></td>
                                    <td><strong><%= req.getChangeDate() %></strong></td>
                                    <td><%= req.getRequestedStopName() %></td>
                                    <td><%= req.getRequestedBusId() != null ? req.getRequestedBusId() : "Same Bus" %></td>
                                    <td><%= req.getReason() %></td>
                                    <td>
                                        <span class="badge <%= "APPROVED".equals(req.getStatus()) ? "badge-success" : ("PENDING".equals(req.getStatus()) ? "badge-warning" : "badge-danger") %>">
                                            <%= req.getStatus() %>
                                        </span>
                                    </td>
                                </tr>
                            <% } %>
                        </tbody>
                    </table>
                </div>
            </div>
        <% } %>

    </main>

    <footer class="footer">
        <strong>SMARTRIDE — Intelligent Campus Transport Management System</strong> | College CSE Mini-Project &bull; Student Dashboard
    </footer>

</body>
</html>
