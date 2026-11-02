<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ page import="java.util.List, smartride.TransportDAO, smartride.Models.PickupStop" %>
<%
    TransportDAO dao = new TransportDAO();
    List<PickupStop> stops = dao.getAllStops();
%>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Student Registration — SMARTRIDE</title>
    <link rel="stylesheet" href="css/style.css">
</head>
<body>

    <header class="navbar">
        <a href="index.jsp" class="brand">
            <span>&#128652; SMARTRIDE</span>
            <span class="brand-subtitle">| Student Registration</span>
        </a>
        <div class="nav-links">
            <a href="index.jsp" class="btn btn-outline btn-sm" style="color: #ffffff; border-color: rgba(255,255,255,0.4);">&larr; Back to Login</a>
        </div>
    </header>

    <main class="container-narrow">

        <%-- Alerts --%>
        <%
            String error = request.getParameter("error");
            if (error != null && !error.trim().isEmpty()) {
        %>
            <div class="alert alert-error">
                <strong>Registration Error:</strong> <%= error %>
            </div>
        <%
            }
        %>

        <div class="card" style="margin-top: 10px;">
            <div class="card-header">
                <h1 class="card-title">&#127891; New Student Fleet Enrollment</h1>
            </div>

            <p style="font-size: 0.92rem; color: var(--text-muted); margin-bottom: 20px;">
                Register your college roll number to be automatically assigned to campus bus routes and Dijkstra dynamic pickup scheduling.
            </p>

            <form method="POST" action="register-student">
                <div class="form-row">
                    <div class="form-group">
                        <label for="rollNo">College Roll Number *</label>
                        <input type="text" id="rollNo" name="rollNo" class="form-control" placeholder="e.g. 25CSR005" required>
                        <small style="color: var(--text-muted); font-size: 0.78rem;">Your unique roll number will be used for sign-in.</small>
                    </div>

                    <div class="form-group">
                        <label for="name">Full Name *</label>
                        <input type="text" id="name" name="name" class="form-control" placeholder="e.g. Priyadharshini K" required>
                    </div>
                </div>

                <div class="form-row">
                    <div class="form-group">
                        <label for="password">Create Password *</label>
                        <input type="password" id="password" name="password" class="form-control" placeholder="Minimum 6 characters" required>
                    </div>

                    <div class="form-group">
                        <label for="department">Department *</label>
                        <select id="department" name="department" class="form-control" required>
                            <option value="">-- Select Department --</option>
                            <option value="CSE">Computer Science &amp; Engineering (CSE)</option>
                            <option value="IT">Information Technology (IT)</option>
                            <option value="AIDS">Artificial Intelligence &amp; Data Science (AI&amp;DS)</option>
                            <option value="ECE">Electronics &amp; Communication (ECE)</option>
                            <option value="EEE">Electrical &amp; Electronics (EEE)</option>
                            <option value="MECH">Mechanical Engineering (MECH)</option>
                        </select>
                    </div>
                </div>

                <div class="form-row">
                    <div class="form-group">
                        <label for="stopId">Primary Boarding Pickup Stop *</label>
                        <select id="stopId" name="stopId" class="form-control" required>
                            <option value="">-- Select Verified Erode Location --</option>
                            <% for (PickupStop ps : stops) { %>
                                <% if (!"C00".equals(ps.getStopId())) { %>
                                    <option value="<%= ps.getStopId() %>">
                                        <%= ps.getStopName() %> (<%= ps.getRoadType() %>)
                                    </option>
                                <% } %>
                            <% } %>
                        </select>
                        <small style="color: var(--text-muted); font-size: 0.78rem;">Select your regular boarding point in and around Erode.</small>
                    </div>

                    <div class="form-group">
                        <label for="contact">Contact Mobile Number</label>
                        <input type="tel" id="contact" name="contact" class="form-control" placeholder="e.g. 9842100000">
                    </div>
                </div>

                <div style="margin-top: 10px; display: flex; justify-content: flex-end; gap: 12px;">
                    <a href="index.jsp" class="btn btn-outline">Cancel</a>
                    <button type="submit" class="btn btn-primary">&#10003; Complete Registration</button>
                </div>
            </form>
        </div>

    </main>

    <footer class="footer">
        <strong>SMARTRIDE — Intelligent Campus Transport Management System</strong> | Zero JavaScript College Mini-Project
    </footer>

</body>
</html>
