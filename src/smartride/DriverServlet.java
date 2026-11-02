package smartride;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import smartride.Models.Driver;

@WebServlet({"/driver/board", "/driver/noshow", "/driver/breakdown", "/api/gps"})
public class DriverServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        String servletPath = request.getServletPath();
        TransportDAO dao = new TransportDAO();

        try {
            if ("/driver/board".equals(servletPath)) {
                handleBoarding(request, response, dao, "BOARDED");
            } else if ("/driver/noshow".equals(servletPath)) {
                handleBoarding(request, response, dao, "NO_SHOW");
            } else if ("/driver/breakdown".equals(servletPath)) {
                handleBreakdown(request, response, dao);
            } else if ("/api/gps".equals(servletPath)) {
                handleGpsUpdate(request, response, dao);
            } else {
                response.sendRedirect(request.getContextPath() + "/index.jsp");
            }
        } catch (Exception e) {
            String encodedErr = URLEncoder.encode(e.getMessage() != null ? e.getMessage() : "Operation failed", StandardCharsets.UTF_8.name());
            String referer = request.getHeader("Referer");
            if (referer != null && referer.contains("admin")) {
                response.sendRedirect(request.getContextPath() + "/admin_dashboard.jsp?error=" + encodedErr);
            } else {
                response.sendRedirect(request.getContextPath() + "/driver_dashboard.jsp?error=" + encodedErr);
            }
        }
    }

    private void handleBoarding(HttpServletRequest request, HttpServletResponse response, TransportDAO dao, String status)
            throws Exception {
        HttpSession session = request.getSession(false);
        if (session == null || !"DRIVER".equals(session.getAttribute("userRole"))) {
            response.sendRedirect(request.getContextPath() + "/index.jsp?error=Driver+login+required.");
            return;
        }

        String userId = (String) session.getAttribute("userId");
        Driver driver = dao.getDriverByUserId(userId);
        if (driver == null || driver.getAssignedBusId() == null) {
            throw new IllegalStateException("Driver account has no active bus assignment.");
        }

        String rollNo = request.getParameter("rollNo");
        String tripDate = request.getParameter("tripDate");
        if (tripDate == null || tripDate.trim().isEmpty()) {
            tripDate = LocalDate.now().toString();
        }

        if (rollNo == null || rollNo.trim().isEmpty()) {
            throw new IllegalArgumentException("Student roll number is required to record boarding.");
        }

        dao.recordBoarding(driver.getAssignedBusId(), rollNo.trim().toUpperCase(), tripDate, status);

        String msg = "BOARDED".equals(status)
                ? "Student " + rollNo.trim().toUpperCase() + " successfully verified and boarded."
                : "Student " + rollNo.trim().toUpperCase() + " recorded as NO_SHOW.";

        response.sendRedirect(request.getContextPath() + "/driver_dashboard.jsp?msg=" +
                URLEncoder.encode(msg, StandardCharsets.UTF_8.name()));
    }

    private void handleBreakdown(HttpServletRequest request, HttpServletResponse response, TransportDAO dao)
            throws Exception {
        HttpSession session = request.getSession(false);
        if (session == null || !"DRIVER".equals(session.getAttribute("userRole"))) {
            response.sendRedirect(request.getContextPath() + "/index.jsp?error=Driver+login+required.");
            return;
        }

        String userId = (String) session.getAttribute("userId");
        Driver driver = dao.getDriverByUserId(userId);
        if (driver == null || driver.getAssignedBusId() == null) {
            throw new IllegalStateException("Driver account has no active bus assignment.");
        }

        String reason = request.getParameter("reason");
        dao.reportBreakdown(driver.getAssignedBusId(), reason);

        response.sendRedirect(request.getContextPath() + "/driver_dashboard.jsp?msg=" +
                URLEncoder.encode("Breakdown reported. Bus " + driver.getAssignedBusId() + " marked OUT OF SERVICE.", StandardCharsets.UTF_8.name()));
    }

    private void handleGpsUpdate(HttpServletRequest request, HttpServletResponse response, TransportDAO dao)
            throws Exception {
        String busId = request.getParameter("bus_id");
        String latStr = request.getParameter("latitude");
        String lonStr = request.getParameter("longitude");
        String accStr = request.getParameter("accuracy");
        String spdStr = request.getParameter("speed");
        String source = request.getParameter("source");

        if (busId == null || latStr == null || lonStr == null) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().println("Missing required parameters: bus_id, latitude, longitude.");
            return;
        }

        double lat = Double.parseDouble(latStr.trim());
        double lon = Double.parseDouble(lonStr.trim());
        double accuracy = (accStr != null && !accStr.isEmpty()) ? Double.parseDouble(accStr.trim()) : 5.0;
        double speed = (spdStr != null && !spdStr.isEmpty()) ? Double.parseDouble(spdStr.trim()) : 0.0;
        if (source == null || source.trim().isEmpty()) {
            source = "HARDWARE_GPS";
        }

        dao.updateBusLocation(busId.trim().toUpperCase(), lat, lon, accuracy, speed, source.trim());

        String redirect = request.getParameter("redirect");
        if (redirect != null && !redirect.isEmpty()) {
            String encoded = URLEncoder.encode("GPS location updated for " + busId.toUpperCase() + " (" + lat + ", " + lon + ").", StandardCharsets.UTF_8.name());
            response.sendRedirect(request.getContextPath() + "/" + redirect + "?msg=" + encoded);
        } else {
            response.setContentType("text/plain");
            response.getWriter().println("GPS_UPDATE_SUCCESS");
        }
    }
}
