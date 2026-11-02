<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>SMARTRIDE — Intelligent Campus Transport Management System</title>
    <link rel="stylesheet" href="css/style.css">
</head>
<body>

    <header class="navbar">
        <a href="index.jsp" class="brand">
            <span>&#128652; SMARTRIDE</span>
            <span class="brand-subtitle">| Intelligent Campus Transport Management System</span>
        </a>
        <div class="nav-links">
            <a href="register_student.jsp" class="btn btn-outline btn-sm" style="color: #ffffff; border-color: rgba(255,255,255,0.4);">&#127891; New Student Registration</a>
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
                <strong>Authentication Notice:</strong> <%= error %>
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

        <div class="auth-hero">
            <h1 class="auth-hero-title">SMARTRIDE — Intelligent Campus Transport Management System</h1>
            <p class="auth-hero-sub">
                A CSE Mini-Project integrating Dijkstra-based dynamic route optimization, absence-driven stop skipping, 1-day flexible stop changes, and real-time GPS bus tracking.
            </p>
        </div>

        <%-- Three Distinct Role Sign-In Portals --%>
        <div class="auth-grid">

            <%-- 1. Student Portal --%>
            <div class="auth-card">
                <div class="auth-role-header">
                    <div class="auth-role-icon" style="background: #eff6ff; color: #2563eb;">&#127891;</div>
                    <div>
                        <div class="auth-role-title">Student Portal</div>
                        <div class="auth-role-desc">Roll Number Authentication</div>
                    </div>
                </div>

                <form method="POST" action="login">
                    <input type="hidden" name="role" value="STUDENT">
                    <div class="form-group">
                        <label for="studentRoll">College Roll Number *</label>
                        <input type="text" id="studentRoll" name="username" class="form-control" placeholder="e.g. 25CSR001" required>
                    </div>

                    <div class="form-group">
                        <label for="studentPass">Password *</label>
                        <input type="password" id="studentPass" name="password" class="form-control" placeholder="••••••••" required>
                    </div>

                    <button type="submit" class="btn btn-primary btn-block">&#128272; Sign In as Student</button>
                </form>

                <div style="margin-top: 16px; text-align: center; font-size: 0.88rem; border-top: 1px dashed var(--border-color); padding-top: 12px;">
                    New student rider? <a href="register_student.jsp" style="color: var(--accent); font-weight: 600; text-decoration: none;">Register Roll Number &rarr;</a>
                </div>
            </div>

            <%-- 2. Driver Portal --%>
            <div class="auth-card">
                <div class="auth-role-header">
                    <div class="auth-role-icon" style="background: #f0fdf4; color: #059669;">&#128652;</div>
                    <div>
                        <div class="auth-role-title">Driver Portal</div>
                        <div class="auth-role-desc">Vehicle &amp; Boarding Console</div>
                    </div>
                </div>

                <form method="POST" action="login">
                    <input type="hidden" name="role" value="DRIVER">
                    <div class="form-group">
                        <label for="driverUser">Driver Username / ID *</label>
                        <input type="text" id="driverUser" name="username" class="form-control" placeholder="e.g. driver1" required>
                    </div>

                    <div class="form-group">
                        <label for="driverPass">Password *</label>
                        <input type="password" id="driverPass" name="password" class="form-control" placeholder="••••••••" required>
                    </div>

                    <button type="submit" class="btn btn-success btn-block">&#128272; Sign In as Driver</button>
                </form>

                <div style="margin-top: 16px; text-align: center; font-size: 0.82rem; color: var(--text-muted); border-top: 1px dashed var(--border-color); padding-top: 12px;">
                    Driver accounts are provisioned and authorized by Campus Admin.
                </div>
            </div>

            <%-- 3. Admin Portal --%>
            <div class="auth-card">
                <div class="auth-role-header">
                    <div class="auth-role-icon" style="background: #fffbeb; color: #d97706;">&#9874;</div>
                    <div>
                        <div class="auth-role-title">Administrator Portal</div>
                        <div class="auth-role-desc">Fleet &amp; Graph Network Console</div>
                    </div>
                </div>

                <form method="POST" action="login">
                    <input type="hidden" name="role" value="ADMIN">
                    <div class="form-group">
                        <label for="adminUser">Administrator ID *</label>
                        <input type="text" id="adminUser" name="username" class="form-control" placeholder="e.g. admin" required>
                    </div>

                    <div class="form-group">
                        <label for="adminPass">Password *</label>
                        <input type="password" id="adminPass" name="password" class="form-control" placeholder="••••••••" required>
                    </div>

                    <button type="submit" class="btn btn-block" style="background: #d97706; color: #ffffff;">&#128272; Sign In as Administrator</button>
                </form>

                <div style="margin-top: 16px; text-align: center; font-size: 0.82rem; color: var(--text-muted); border-top: 1px dashed var(--border-color); padding-top: 12px;">
                    Protected administrative command console with salted SHA-256 hash security.
                </div>
            </div>

        </div>

        <%-- Initial Testing Credentials Reference Banner --%>
        <div class="card" style="background-color: #f1f5f9; border-left: 4px solid var(--accent);">
            <div style="font-weight: 700; color: var(--text-main); margin-bottom: 8px;">
                &#128203; College Project Review — Verified Demo Credentials:
            </div>
            <div style="display: grid; grid-template-columns: repeat(auto-fit, minmax(240px, 1fr)); gap: 12px; font-size: 0.86rem;">
                <div>
                    <strong>Student (Roll No):</strong> <code>25CSR001</code> (or <code>25CSR002</code>)<br>
                    <strong>Password:</strong> <code>Student@123</code>
                </div>
                <div>
                    <strong>Driver:</strong> <code>driver1</code> (or <code>driver2</code>)<br>
                    <strong>Password:</strong> <code>Driver@123</code>
                </div>
                <div>
                    <strong>Administrator:</strong> <code>admin</code><br>
                    <strong>Password:</strong> <code>Admin@123</code>
                </div>
            </div>
        </div>

    </main>

    <footer class="footer">
        <strong>SMARTRIDE — Intelligent Campus Transport Management System</strong> | College CSE Mini-Project &bull; Java Servlets &bull; SQLite &bull; Dijkstra Algorithm &bull; Zero JavaScript
    </footer>

</body>
</html>
