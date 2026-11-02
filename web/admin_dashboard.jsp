<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page import="java.time.LocalDate, java.util.*, smartride.TransportDAO, smartride.Models.*" %>
<%
    // Role-based session access check
    HttpSession userSession = request.getSession(false);
    if (userSession == null || !"ADMIN".equals(userSession.getAttribute("userRole"))) {
        response.sendRedirect(request.getContextPath() + "/index.jsp?error=Admin+access+required.");
        return;
    }

    TransportDAO dao = new TransportDAO();
    String today = LocalDate.now().toString();

    List<Student> students = dao.getAllStudents();
    List<Driver> drivers = dao.getAllDrivers();
    List<CollegeBus> buses = dao.getAllBuses();
    List<PickupStop> stops = dao.getAllStops();
    List<RoadEdge> edges = dao.getAllEdges();
    List<BoardingRecord> boardings = dao.getDailyBoardingRecords(null, today);
    List<BreakdownRecord> breakdowns = dao.getAllBreakdowns();
    List<TempChangeRequest> tempRequests = dao.getAllTempRequests();
    Map<String, GPSLocation> allGps = dao.getAllBusLocations();

    // Recalculate dynamic Dijkstra routes for all buses
    Map<String, RouteOptimizationResult> routeMap = dao.recalculateAllRoutes(today);

    // Selected bus for map inspection
    String selectedBusId = request.getParameter("inspectBus");
    if (selectedBusId == null || selectedBusId.trim().isEmpty()) {
        selectedBusId = !buses.isEmpty() ? buses.get(0).getBusId() : "BUS-01";
    }

    // Compute fleet metrics
    int totalStudents = students.size();
    int scheduledPresent = 0;
    int declaredAbsent = 0;
    for (Student s : students) {
        if (s.isAbsentOn(today)) declaredAbsent++;
        else scheduledPresent++;
    }

    int totalBoardedToday = 0;
    int totalNoShowToday = 0;
    for (BoardingRecord br : boardings) {
        if ("BOARDED".equalsIgnoreCase(br.getStatus())) totalBoardedToday++;
        else if ("NO_SHOW".equalsIgnoreCase(br.getStatus())) totalNoShowToday++;
    }

    int activeBreakdowns = 0;
    for (BreakdownRecord br : breakdowns) {
        if ("ACTIVE".equalsIgnoreCase(br.getStatus())) activeBreakdowns++;
    }
%>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <meta http-equiv="refresh" content="45">
    <title>Admin Command Console — SMARTRIDE</title>
    <link rel="stylesheet" href="css/style.css">
</head>
<body>

    <header class="navbar">
        <a href="admin_dashboard.jsp" class="brand">
            <span>&#128652; SMARTRIDE</span>
            <span class="brand-subtitle">| Administrator Command Console</span>
        </a>
        <div class="nav-links">
            <span class="nav-user-info">&#9874; Campus Transport Administrator</span>
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
                <strong>System Notice:</strong> <%= error %>
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

        <%-- Overview KPI Stats Banner --%>
        <div class="stats-bar">
            <div class="stat-box primary">
                <div class="stat-label">Enrolled Students</div>
                <div class="stat-value"><%= totalStudents %></div>
                <div style="font-size: 0.85rem; color: var(--text-muted); margin-top: 4px;">Registered campus riders</div>
            </div>

            <div class="stat-box success">
                <div class="stat-label">Scheduled Present</div>
                <div class="stat-value"><%= scheduledPresent %></div>
                <div style="font-size: 0.85rem; color: var(--text-muted); margin-top: 4px;">Default present rider quota</div>
            </div>

            <div class="stat-box danger">
                <div class="stat-label">Declared Absent</div>
                <div class="stat-value"><%= declaredAbsent %></div>
                <div style="font-size: 0.85rem; color: var(--text-muted); margin-top: 4px;">Empty stops skipped</div>
            </div>

            <div class="stat-box primary">
                <div class="stat-label">Boarded / No-Shows</div>
                <div class="stat-value"><%= totalBoardedToday %> / <%= totalNoShowToday %></div>
                <div style="font-size: 0.85rem; color: var(--text-muted); margin-top: 4px;">Recorded today (<%= today %>)</div>
            </div>

            <div class="stat-box <%= activeBreakdowns > 0 ? "danger" : "success" %>">
                <div class="stat-label">Fleet Operational</div>
                <div class="stat-value"><%= buses.size() - activeBreakdowns %> / <%= buses.size() %></div>
                <div style="font-size: 0.85rem; color: var(--text-muted); margin-top: 4px;"><%= activeBreakdowns %> breakdown(s) reported</div>
            </div>
        </div>

        <%-- SECTION 1: Real Geographical Route Map & GPS Monitor (Zero JavaScript) --%>
        <div class="card" style="padding: 0; overflow: hidden;">
            <div class="map-header">
                <div style="display: flex; align-items: center; gap: 12px; flex-wrap: wrap;">
                    <strong>&#127757; Geographical Map &amp; Real-Time Bus Fleet Monitor</strong>
                    <form method="GET" action="admin_dashboard.jsp" style="display: inline-flex; align-items: center; gap: 6px;">
                        <label for="inspectBusSelect" style="color: #cbd5e1; font-size: 0.82rem; margin-bottom: 0;">Inspect Vehicle:</label>
                        <select id="inspectBusSelect" name="inspectBus" class="form-control" style="padding: 3px 8px; font-size: 0.82rem; width: auto; background: #1e293b; color: #ffffff; border-color: #475569;" onchange="this.form.submit()">
                            <% for (CollegeBus b : buses) { %>
                                <option value="<%= b.getBusId() %>" <%= b.getBusId().equals(selectedBusId) ? "selected" : "" %>>
                                    <%= b.getBusId() %> - <%= b.getBusNumber() %> (<%= b.getStatus() %>)
                                </option>
                            <% } %>
                        </select>
                        <noscript><button type="submit" class="btn btn-sm btn-outline" style="color: #ffffff; padding: 2px 6px;">View</button></noscript>
                    </form>
                </div>
                <div style="display: flex; align-items: center; gap: 8px;">
                    <%
                        GPSLocation activeGps = allGps.get(selectedBusId);
                        if (activeGps != null) {
                    %>
                        <span class="badge <%= "LIVE".equals(activeGps.getStatus()) ? "badge-success" : ("STALE".equals(activeGps.getStatus()) ? "badge-warning" : "badge-danger") %>">
                            GPS: <%= activeGps.getStatus() %>
                        </span>
                        <span style="font-size: 0.8rem; opacity: 0.85;">Ping: <%= activeGps.getLastUpdated() %></span>
                    <% } else { %>
                        <span class="badge badge-danger">GPS UNAVAILABLE</span>
                    <% } %>
                    <a href="admin_dashboard.jsp?inspectBus=<%= selectedBusId %>" class="btn btn-outline btn-sm" style="color: #ffffff; border-color: rgba(255,255,255,0.4); padding: 3px 8px;">&#8635; Refresh</a>
                </div>
            </div>

            <div style="position: relative; background-color: #f8fafc;">
                <img src="map?busId=<%= selectedBusId %>" alt="Geographical Map" class="map-img">
            </div>
        </div>

        <%-- SECTION 2: Dynamic Dijkstra Routes & Ordered Stop Sequence --%>
        <div class="card">
            <div class="card-header">
                <h2 class="card-title">&#128652; Dijkstra-Optimized Campus Bus Routes (<%= today %>)</h2>
            </div>
            <p style="font-size: 0.88rem; color: var(--text-muted); margin-bottom: 16px;">
                Routes calculated using Dijkstra's algorithm (PriorityQueue min-heap) and greedy nearest-neighbor stop sequence. Stops with 0 scheduled students are automatically skipped.
            </p>

            <% for (CollegeBus b : buses) { %>
                <% RouteOptimizationResult opt = routeMap.get(b.getBusId()); %>
                <div style="border: 1px solid var(--border-color); border-radius: var(--radius-sm); padding: 16px; margin-bottom: 18px; background-color: #ffffff;">
                    <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 10px; flex-wrap: wrap; gap: 8px;">
                        <div>
                            <strong style="font-size: 1.1rem; color: var(--primary);"><%= b.getBusId() %></strong> &bull;
                            <span style="font-weight: 600;"><%= b.getBusNumber() %></span> (<%= b.getBusType() %>, <%= b.getCapacity() %> Seats)
                        </div>
                        <div>
                            <% if (b.getStatus() == BusStatus.AVAILABLE) { %>
                                <span class="badge badge-success">&#10003; OPERATIONAL</span>
                            <% } else { %>
                                <span class="badge badge-danger">&#9888; OUT OF SERVICE (BREAKDOWN)</span>
                            <% } %>
                        </div>
                    </div>

                    <% if (opt != null && !opt.getStopSequence().isEmpty()) { %>
                        <div class="route-steps-box">
                            <%
                                List<RouteStopInfo> rStops = opt.getStopSequence();
                                for (int i = 0; i < rStops.size(); i++) {
                                    RouteStopInfo rsi = rStops.get(i);
                            %>
                                <div class="route-stop-node <%= rsi.isDepot() ? "depot" : "" %>">
                                    <div><%= rsi.isDepot() ? (rsi.isReturnDepot() ? "&#127979; Depot (Return)" : "&#127979; Depot (Start)") : rsi.getStopName() %></div>
                                    <div style="font-size: 0.72rem; font-weight: 500;">
                                        <%= rsi.getArrivalTime() %>
                                        <% if (rsi.getLegTravelMinutes() > 0) { %> (+<%= rsi.getLegTravelMinutes() %>m) <% } %>
                                    </div>
                                </div>
                                <% if (i < rStops.size() - 1) { %>
                                    <span class="route-arrow">&rarr;</span>
                                <% } %>
                            <% } %>
                        </div>

                        <div style="font-size: 0.84rem; color: var(--text-muted); display: flex; justify-content: space-between; flex-wrap: wrap; gap: 8px; margin-bottom: 10px;">
                            <span>Trip Duration: <strong><%= opt.getTotalTravelTimeMinutes() %> minutes</strong> (Depot Departure: <strong>07:30 AM</strong> &rarr; Return: <strong><%= opt.getReturnDepotTime() %></strong>) | Distance: <strong><%= String.format("%.1f km", opt.getTotalDistanceKm()) %></strong></span>
                            <% if (!opt.getSkippedEmptyStops().isEmpty()) { %>
                                <span style="color: var(--warning);">&#9888; Bypassed Empty Stops: <code><%= String.join(", ", opt.getSkippedEmptyStops()) %></code></span>
                            <% } %>
                        </div>

                        <div class="table-responsive">
                            <table class="table" style="font-size: 0.85rem;">
                                <thead>
                                    <tr>
                                        <th>Seq</th>
                                        <th>Stop Name</th>
                                        <th>Road Type</th>
                                        <th>Leg Duration</th>
                                        <th>Leg Distance</th>
                                        <th>Expected Arrival Time</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    <% for (RouteStopInfo rsi : opt.getStopSequence()) { %>
                                        <tr>
                                            <td>#<%= rsi.getSequence() %></td>
                                            <td><strong><%= rsi.getStopName() %></strong></td>
                                            <td><span class="badge badge-neutral"><%= rsi.getRoadType() %></span></td>
                                            <td><%= rsi.getLegTravelMinutes() == 0 ? "Depot Departure" : "+" + rsi.getLegTravelMinutes() + " mins" %></td>
                                            <td><%= rsi.getLegDistanceKm() == 0.0 ? "0.0 km" : String.format("%.1f km", rsi.getLegDistanceKm()) %></td>
                                            <td><strong style="color: var(--primary);"><%= rsi.getArrivalTime() %></strong></td>
                                        </tr>
                                    <% } %>
                                </tbody>
                            </table>
                        </div>
                    <% } else { %>
                        <p style="color: var(--danger); font-size: 0.9rem;">Bus is currently OUT OF SERVICE or has no scheduled stops.</p>
                    <% } %>
                </div>
            <% } %>
        </div>

        <%-- SECTION 3: Student Fleet Roster & Attendance --%>
        <div class="card">
            <div class="card-header">
                <h2 class="card-title">&#127891; Enrolled Students &amp; Daily Attendance Roster</h2>
            </div>
            <div class="table-responsive">
                <table class="table">
                    <thead>
                        <tr>
                            <th>Roll Number</th>
                            <th>Student Name</th>
                            <th>Dept</th>
                            <th>Regular Stop</th>
                            <th>Effective Stop (Today)</th>
                            <th>Assigned Bus</th>
                            <th>Estimated Pickup</th>
                            <th>Attendance</th>
                            <th>1-Day Status</th>
                        </tr>
                    </thead>
                    <tbody>
                        <% for (Student s : students) { %>
                            <%
                                boolean isAbsent = s.isAbsentOn(today);
                                String regStopName = dao.getStopName(s.getRegularStopId());
                                String effStopId = s.getEffectiveStopId(today);
                                String effStopName = dao.getStopName(effStopId);
                                boolean hasTemp = !effStopId.equals(s.getRegularStopId());
                            %>
                            <tr>
                                <td><strong><%= s.getRollNo() %></strong></td>
                                <td><%= s.getName() %></td>
                                <td><span class="badge badge-neutral"><%= s.getDepartment() %></span></td>
                                <td><%= regStopName %></td>
                                <td>
                                    <%= effStopName %>
                                    <% if (hasTemp) { %> <span class="badge badge-primary">1-Day Change</span> <% } %>
                                </td>
                                <td><strong><%= s.getEffectiveBusId(today) %></strong></td>
                                <td><%= isAbsent ? "Absent" : s.getEstimatedPickupTime() %></td>
                                <td>
                                    <% if (isAbsent) { %>
                                        <span class="badge badge-danger">&#10007; ABSENT DECLARED</span>
                                    <% } else { %>
                                        <span class="badge badge-success">&#10003; SCHEDULED PRESENT</span>
                                    <% } %>
                                </td>
                                <td>
                                    <span class="badge <%= "APPROVED".equals(s.getTempRequestStatus()) ? "badge-success" : ("PENDING".equals(s.getTempRequestStatus()) ? "badge-warning" : "badge-neutral") %>">
                                        <%= s.getTempRequestStatus() %>
                                    </span>
                                </td>
                            </tr>
                        <% } %>
                    </tbody>
                </table>
            </div>
        </div>

        <%-- SECTION 4: 1-Day Temporary Change Requests Management --%>
        <div class="card">
            <div class="card-header">
                <h2 class="card-title">&#128260; 1-Day Temporary Stop Change Requests</h2>
            </div>
            <% if (!tempRequests.isEmpty()) { %>
                <div class="table-responsive">
                    <table class="table">
                        <thead>
                            <tr>
                                <th>ID</th>
                                <th>Roll Number</th>
                                <th>Student Name</th>
                                <th>Target Date</th>
                                <th>Requested Stop</th>
                                <th>Requested Bus</th>
                                <th>Reason</th>
                                <th>Status</th>
                                <th>Admin Actions</th>
                            </tr>
                        </thead>
                        <tbody>
                            <% for (TempChangeRequest tr : tempRequests) { %>
                                <tr>
                                    <td>#<%= tr.getId() %></td>
                                    <td><strong><%= tr.getRollNo() %></strong></td>
                                    <td><%= tr.getStudentName() %></td>
                                    <td><%= tr.getChangeDate() %></td>
                                    <td><%= tr.getRequestedStopName() %></td>
                                    <td><%= tr.getRequestedBusId() != null ? tr.getRequestedBusId() : "Auto" %></td>
                                    <td><%= tr.getReason() %></td>
                                    <td>
                                        <span class="badge <%= "APPROVED".equals(tr.getStatus()) ? "badge-success" : ("PENDING".equals(tr.getStatus()) ? "badge-warning" : "badge-danger") %>">
                                            <%= tr.getStatus() %>
                                        </span>
                                    </td>
                                    <td>
                                        <% if ("PENDING".equals(tr.getStatus())) { %>
                                            <div style="display: flex; gap: 6px;">
                                                <form method="POST" action="admin/temp-request" style="display:inline;">
                                                    <input type="hidden" name="requestId" value="<%= tr.getId() %>">
                                                    <input type="hidden" name="action" value="APPROVE">
                                                    <button type="submit" class="btn btn-success btn-sm">Approve</button>
                                                </form>
                                                <form method="POST" action="admin/temp-request" style="display:inline;">
                                                    <input type="hidden" name="requestId" value="<%= tr.getId() %>">
                                                    <input type="hidden" name="action" value="REJECT">
                                                    <button type="submit" class="btn btn-danger btn-sm">Reject</button>
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
            <% } else { %>
                <p style="color: var(--text-muted); font-size: 0.9rem;">No temporary stop change requests filed yet.</p>
            <% } %>
        </div>

        <%-- SECTION 5: Road Graph & Network Configuration --%>
        <div style="display: grid; grid-template-columns: repeat(auto-fit, minmax(360px, 1fr)); gap: 20px;">

            <%-- Add Verified Stop Form --%>
            <div class="card">
                <div class="card-header">
                    <h2 class="card-title">&#128205; Add Verified Pickup Stop</h2>
                </div>
                <form method="POST" action="admin/add-stop">
                    <div class="form-row">
                        <div class="form-group">
                            <label for="stopId">Stop Code *</label>
                            <input type="text" id="stopId" name="stopId" class="form-control" placeholder="e.g. S13" required>
                        </div>
                        <div class="form-group">
                            <label for="stopName">Real Place Name in Erode *</label>
                            <input type="text" id="stopName" name="stopName" class="form-control" placeholder="e.g. Kasipalayam" required>
                        </div>
                    </div>
                    <div class="form-row">
                        <div class="form-group">
                            <label for="latitude">Latitude (Real GPS) *</label>
                            <input type="text" id="latitude" name="latitude" class="form-control" placeholder="e.g. 11.3320" required>
                        </div>
                        <div class="form-group">
                            <label for="longitude">Longitude (Real GPS) *</label>
                            <input type="text" id="longitude" name="longitude" class="form-control" placeholder="e.g. 77.7280" required>
                        </div>
                    </div>
                    <div class="form-group">
                        <label for="roadType">Road Clearance Category *</label>
                        <select id="roadType" name="roadType" class="form-control" required>
                            <option value="ARTERIAL">ARTERIAL (All Buses)</option>
                            <option value="HIGHWAY">HIGHWAY (All Buses)</option>
                            <option value="SUBURBAN">SUBURBAN (All Buses)</option>
                            <option value="NARROW">NARROW (Mini-Bus Only)</option>
                        </select>
                    </div>
                    <button type="submit" class="btn btn-primary btn-block">&#10003; Add Verified Stop to Graph</button>
                </form>
            </div>

            <%-- Add Road Edge Form --%>
            <div class="card">
                <div class="card-header">
                    <h2 class="card-title">&#128739; Add Verified Road Connection</h2>
                </div>
                <form method="POST" action="admin/add-edge">
                    <div class="form-row">
                        <div class="form-group">
                            <label for="fromStop">From Stop *</label>
                            <select id="fromStop" name="fromStop" class="form-control" required>
                                <% for (PickupStop ps : stops) { %>
                                    <option value="<%= ps.getStopId() %>"><%= ps.getStopName() %> (<%= ps.getStopId() %>)</option>
                                <% } %>
                            </select>
                        </div>
                        <div class="form-group">
                            <label for="toStop">To Stop *</label>
                            <select id="toStop" name="toStop" class="form-control" required>
                                <% for (PickupStop ps : stops) { %>
                                    <option value="<%= ps.getStopId() %>"><%= ps.getStopName() %> (<%= ps.getStopId() %>)</option>
                                <% } %>
                            </select>
                        </div>
                    </div>
                    <div class="form-row">
                        <div class="form-group">
                            <label for="travelTime">Travel Time (Mins) *</label>
                            <input type="number" id="travelTime" name="travelTime" class="form-control" min="1" placeholder="e.g. 10" required>
                        </div>
                        <div class="form-group">
                            <label for="distanceKm">Distance (KM) *</label>
                            <input type="number" step="0.1" id="distanceKm" name="distanceKm" class="form-control" min="0.1" placeholder="e.g. 6.5" required>
                        </div>
                    </div>
                    <div class="form-row" style="align-items: center;">
                        <div class="form-group" style="flex: 1;">
                            <label for="edgeRoadType">Road Type</label>
                            <select id="edgeRoadType" name="roadType" class="form-control">
                                <option value="ARTERIAL">ARTERIAL</option>
                                <option value="HIGHWAY">HIGHWAY</option>
                                <option value="SUBURBAN">SUBURBAN</option>
                                <option value="NARROW">NARROW</option>
                            </select>
                        </div>
                        <div class="form-group" style="margin-top: 18px;">
                            <label style="cursor: pointer; display: flex; align-items: center; gap: 6px;">
                                <input type="checkbox" name="bidirectional" checked> Bidirectional Road
                            </label>
                        </div>
                    </div>
                    <button type="submit" class="btn btn-primary btn-block">&#10003; Add Edge &amp; Recalculate Dijkstra</button>
                </form>
            </div>

        </div>

        <%-- SECTION 6: Breakdown Coordination & Fleet Management --%>
        <div style="display: grid; grid-template-columns: repeat(auto-fit, minmax(360px, 1fr)); gap: 20px; margin-top: 20px;">

            <%-- Driver Account Provisioning --%>
            <div class="card">
                <div class="card-header">
                    <h2 class="card-title">&#128104;&#8205;&#128652; Provision Driver Account</h2>
                </div>
                <form method="POST" action="admin/add-driver">
                    <div class="form-row">
                        <div class="form-group">
                            <label for="driverId">Driver ID *</label>
                            <input type="text" id="driverId" name="driverId" class="form-control" placeholder="e.g. DRV-003" required>
                        </div>
                        <div class="form-group">
                            <label for="driverUsername">Username *</label>
                            <input type="text" id="driverUsername" name="username" class="form-control" placeholder="e.g. driver3" required>
                        </div>
                    </div>
                    <div class="form-row">
                        <div class="form-group">
                            <label for="driverName">Full Name *</label>
                            <input type="text" id="driverName" name="name" class="form-control" placeholder="e.g. Balakrishnan K" required>
                        </div>
                        <div class="form-group">
                            <label for="driverPassword">Password *</label>
                            <input type="password" id="driverPassword" name="password" class="form-control" placeholder="••••••••" required>
                        </div>
                    </div>
                    <div class="form-row">
                        <div class="form-group">
                            <label for="driverLicense">License Number *</label>
                            <input type="text" id="driverLicense" name="license" class="form-control" placeholder="TN33-2020-00123" required>
                        </div>
                        <div class="form-group">
                            <label for="driverBus">Assigned Bus</label>
                            <select id="driverBus" name="busId" class="form-control">
                                <option value="">-- None --</option>
                                <% for (CollegeBus b : buses) { %>
                                    <option value="<%= b.getBusId() %>"><%= b.getBusId() %> - <%= b.getBusNumber() %></option>
                                <% } %>
                            </select>
                        </div>
                    </div>
                    <button type="submit" class="btn btn-primary btn-block">&#10003; Create Driver Account</button>
                </form>
            </div>

            <%-- Breakdown Management & Vehicle Restoration --%>
            <div class="card">
                <div class="card-header">
                    <h2 class="card-title">&#9888; Fleet Breakdown Coordination</h2>
                </div>
                <% if (!breakdowns.isEmpty()) { %>
                    <div class="table-responsive">
                        <table class="table" style="font-size: 0.85rem;">
                            <thead>
                                <tr>
                                    <th>Bus</th>
                                    <th>Incident Reason</th>
                                    <th>Reported Time</th>
                                    <th>Status</th>
                                    <th>Action</th>
                                </tr>
                            </thead>
                            <tbody>
                                <% for (BreakdownRecord br : breakdowns) { %>
                                    <tr>
                                        <td><strong><%= br.getBusId() %></strong></td>
                                        <td><%= br.getReason() %></td>
                                        <td><%= br.getReportedAt() %></td>
                                        <td>
                                            <span class="badge <%= "ACTIVE".equals(br.getStatus()) ? "badge-danger" : "badge-success" %>">
                                                <%= br.getStatus() %>
                                            </span>
                                        </td>
                                        <td>
                                            <% if ("ACTIVE".equals(br.getStatus())) { %>
                                                <form method="POST" action="admin/resolve-breakdown" style="display: flex; gap: 4px;">
                                                    <input type="hidden" name="breakdownId" value="<%= br.getId() %>">
                                                    <input type="hidden" name="busId" value="<%= br.getBusId() %>">
                                                    <button type="submit" class="btn btn-success btn-sm">Mark Repaired</button>
                                                </form>
                                            <% } else { %>
                                                <span style="font-size: 0.8rem; color: var(--text-muted);">Resolved</span>
                                            <% } %>
                                        </td>
                                    </tr>
                                <% } %>
                            </tbody>
                        </table>
                    </div>
                <% } else { %>
                    <p style="color: var(--success); font-size: 0.9rem;">&#10003; All campus fleet buses are currently in service.</p>
                <% } %>
            </div>

        </div>

    </main>

    <footer class="footer">
        <strong>SMARTRIDE — Intelligent Campus Transport Management System</strong> | College CSE Mini-Project &bull; Admin Console
    </footer>

</body>
</html>
