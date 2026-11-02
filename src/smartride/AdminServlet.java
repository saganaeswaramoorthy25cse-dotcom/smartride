package smartride;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

@WebServlet({"/admin/add-stop", "/admin/add-edge", "/admin/add-driver", "/admin/add-bus", "/admin/temp-request", "/admin/resolve-breakdown"})
public class AdminServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        HttpSession session = request.getSession(false);
        if (session == null || !"ADMIN".equals(session.getAttribute("userRole"))) {
            response.sendRedirect(request.getContextPath() + "/index.jsp?error=Admin+access+required.");
            return;
        }

        String servletPath = request.getServletPath();
        TransportDAO dao = new TransportDAO();

        try {
            if ("/admin/add-stop".equals(servletPath)) {
                String stopId = request.getParameter("stopId");
                String stopName = request.getParameter("stopName");
                double lat = Double.parseDouble(request.getParameter("latitude"));
                double lon = Double.parseDouble(request.getParameter("longitude"));
                String roadType = request.getParameter("roadType");

                dao.addStop(stopId, stopName, lat, lon, roadType);
                redirectSuccess(request, response, "Verified stop " + stopName + " (" + stopId + ") added to road graph.");

            } else if ("/admin/add-edge".equals(servletPath)) {
                String fromStop = request.getParameter("fromStop");
                String toStop = request.getParameter("toStop");
                int travelTime = Integer.parseInt(request.getParameter("travelTime"));
                double distanceKm = Double.parseDouble(request.getParameter("distanceKm"));
                String roadType = request.getParameter("roadType");
                boolean bidirectional = "on".equalsIgnoreCase(request.getParameter("bidirectional"));

                dao.addEdge(fromStop, toStop, travelTime, distanceKm, roadType, bidirectional);
                redirectSuccess(request, response, "Road connection " + fromStop + " <-> " + toStop + " added. Dijkstra routes recalculated.");

            } else if ("/admin/add-driver".equals(servletPath)) {
                String driverId = request.getParameter("driverId");
                String username = request.getParameter("username");
                String password = request.getParameter("password");
                String name = request.getParameter("name");
                String contact = request.getParameter("contact");
                String license = request.getParameter("license");
                String busId = request.getParameter("busId");

                dao.addDriver(driverId, username, password, name, contact, license, busId);
                redirectSuccess(request, response, "Driver account created for " + name + " (" + username + ").");

            } else if ("/admin/add-bus".equals(servletPath)) {
                String busId = request.getParameter("busId");
                String busNumber = request.getParameter("busNumber");
                int capacity = Integer.parseInt(request.getParameter("capacity"));
                String busType = request.getParameter("busType");

                dao.addBus(busId, busNumber, capacity, busType);
                redirectSuccess(request, response, "Bus " + busId + " (" + busNumber + ") registered into fleet.");

            } else if ("/admin/temp-request".equals(servletPath)) {
                int requestId = Integer.parseInt(request.getParameter("requestId"));
                String action = request.getParameter("action"); // APPROVE or REJECT
                String status = "APPROVE".equalsIgnoreCase(action) ? "APPROVED" : "REJECTED";

                dao.updateTempRequestStatus(requestId, status);
                redirectSuccess(request, response, "Temporary change request #" + requestId + " marked as " + status + ".");

            } else if ("/admin/resolve-breakdown".equals(servletPath)) {
                int breakdownId = Integer.parseInt(request.getParameter("breakdownId"));
                String busId = request.getParameter("busId");
                String replacementBusId = request.getParameter("replacementBusId");

                dao.resolveBreakdown(breakdownId, busId, replacementBusId);
                redirectSuccess(request, response, "Breakdown #" + breakdownId + " resolved. Fleet status updated.");
            } else {
                response.sendRedirect(request.getContextPath() + "/admin_dashboard.jsp");
            }
        } catch (Exception e) {
            String encodedErr = URLEncoder.encode(e.getMessage() != null ? e.getMessage() : "Operation failed", StandardCharsets.UTF_8.name());
            response.sendRedirect(request.getContextPath() + "/admin_dashboard.jsp?error=" + encodedErr);
        }
    }

    private void redirectSuccess(HttpServletRequest request, HttpServletResponse response, String message)
            throws IOException {
        String encoded = URLEncoder.encode(message, StandardCharsets.UTF_8.name());
        response.sendRedirect(request.getContextPath() + "/admin_dashboard.jsp?msg=" + encoded);
    }
}
