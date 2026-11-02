# 🚌 SMARTRIDE — Intelligent Campus Transport Management System

A **Java Servlet + JSP + SQLite** web application for managing campus bus transport, built as a CSE Mini-Project.

## ✨ Features

- **Dijkstra-based dynamic route optimization** — automatically computes shortest bus routes
- **Absence-driven stop skipping** — students can mark absence; stops are skipped dynamically
- **1-day flexible stop changes** — students request temporary stop changes
- **Real-time GPS bus tracking** — driver updates location, students see live bus position
- **Three-role authentication system** — Student, Driver, and Administrator portals

## 🏗️ Tech Stack

| Layer | Technology |
|-------|-----------|
| Backend | Java Servlets (Jakarta EE 3.1) |
| Frontend | JSP pages + vanilla CSS |
| Database | SQLite (via `sqlite-jdbc`) |
| Routing | Dijkstra's Algorithm (custom implementation) |
| Auth | SHA-256 salted password hashing |

## 📁 Project Structure

```
smart new/
├── src/
│   └── smartride/
│       ├── AuthServlet.java        # Login / Logout
│       ├── StudentServlet.java     # Student portal actions
│       ├── DriverServlet.java      # Driver console (boarding, GPS, breakdown)
│       ├── AdminServlet.java       # Admin fleet management
│       ├── MapServlet.java         # Route map API
│       ├── DijkstraRouter.java     # Shortest-path engine
│       ├── TransportDAO.java       # Database access layer
│       ├── DBUtil.java             # SQLite connection utility
│       └── Models.java             # Domain model classes
├── web/
│   ├── index.jsp                   # Login / Home page
│   ├── register_student.jsp        # Student self-registration
│   ├── student_dashboard.jsp       # Student portal dashboard
│   ├── driver_dashboard.jsp        # Driver console
│   ├── admin_dashboard.jsp         # Admin management panel
│   ├── css/
│   │   └── style.css
│   └── WEB-INF/
│       ├── web.xml                 # Servlet mappings
│       └── lib/
│           └── sqlite-jdbc.jar
├── build.bat                       # Windows build script
└── smartride.db                    # SQLite database (auto-created on first run)
```

## 🚀 Running Locally

### Prerequisites
- JDK 11+
- Apache Tomcat 9+ (or compatible servlet container)

### Steps
1. Clone this repository
2. Compile the Java sources:
   ```bash
   build.bat
   ```
3. Deploy `web/` folder as a WAR to Tomcat, or use an IDE (IntelliJ IDEA / Eclipse)
4. Access: `http://localhost:8080/smartride/`

## 🔑 Demo Credentials

| Role | Username | Password |
|------|----------|----------|
| Student | `25CSR001` or `25CSR002` | `Student@123` |
| Driver | `driver1` or `driver2` | `Driver@123` |
| Administrator | `admin` | `Admin@123` |

## 🌐 Deployment

> **Note:** This is a Java Servlet/JSP application. It requires a Java servlet container (e.g., Tomcat, Jetty) to run.
> It is **not** compatible with serverless platforms like Vercel.
>
> **Recommended hosting:** [Railway](https://railway.app) or [Render](https://render.com) with a Tomcat Docker image.

---

*SMARTRIDE — CSE Mini-Project | Java Servlets • SQLite • Dijkstra Algorithm*

