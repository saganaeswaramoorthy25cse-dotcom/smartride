package smartride;

import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDate;
import java.util.*;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import smartride.Models.*;
import smartride.DijkstraRouter.Graph;

@WebServlet("/map")
public class MapServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    // Geographic boundary box covering Erode district and surrounding stops
    private static final double MIN_LAT = 11.22;
    private static final double MAX_LAT = 11.48;
    private static final double MIN_LON = 77.40;
    private static final double MAX_LON = 77.82;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        String busId = request.getParameter("busId");
        if (busId == null || busId.trim().isEmpty()) {
            busId = "BUS-01";
        } else {
            busId = busId.trim().toUpperCase();
        }

        String date = request.getParameter("date");
        if (date == null || date.trim().isEmpty()) {
            date = LocalDate.now().toString();
        }

        TransportDAO dao = new TransportDAO();
        CollegeBus bus = dao.getBus(busId);
        List<PickupStop> allStops = dao.getAllStops();
        Map<String, PickupStop> stopsMap = dao.getAllStopsMap();
        List<RoadEdge> allEdges = dao.getAllEdges();
        GPSLocation gps = dao.getBusLocation(busId);

        // Recalculate routes to get active sequence and times
        Map<String, RouteOptimizationResult> routeResults = dao.recalculateAllRoutes(date);
        RouteOptimizationResult optResult = routeResults.get(busId);

        int width = 920;
        int height = 580;
        int padX = 75;
        int padY = 70;

        response.setContentType("image/svg+xml;charset=UTF-8");
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        response.setHeader("Pragma", "no-cache");
        response.setDateHeader("Expires", 0);

        PrintWriter out = response.getWriter();

        out.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        out.println("<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 " + width + " " + height + "\" width=\"100%\" height=\"auto\" style=\"background-color: #f1f5f9; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;\">");

        // SVG Definitions (Gradients, filters, marker arrows)
        out.println("<defs>");
        out.println("  <filter id=\"cardShadow\" x=\"-5%\" y=\"-5%\" width=\"110%\" height=\"115%\">");
        out.println("    <feDropShadow dx=\"0\" dy=\"2\" stdDeviation=\"3\" flood-opacity=\"0.1\" />");
        out.println("  </filter>");
        out.println("  <marker id=\"arrow\" viewBox=\"0 0 10 10\" refX=\"6\" refY=\"5\" markerWidth=\"6\" markerHeight=\"6\" orient=\"auto-start-reverse\">");
        out.println("    <path d=\"M 0 1 L 10 5 L 0 9 z\" fill=\"#2563eb\" />");
        out.println("  </marker>");
        out.println("  <linearGradient id=\"waterGrad\" x1=\"0%\" y1=\"0%\" x2=\"100%\" y2=\"100%\">");
        out.println("    <stop offset=\"0%\" stop-color=\"#bae6fd\" />");
        out.println("    <stop offset=\"100%\" stop-color=\"#7dd3fc\" />");
        out.println("  </linearGradient>");
        out.println("</defs>");

        // 1. Background map frame
        out.println("<rect width=\"" + width + "\" height=\"" + height + "\" fill=\"#f8fafc\" rx=\"10\" stroke=\"#cbd5e1\" stroke-width=\"1.5\" />");

        // 2. Geographic Coordinate Grid (Parallels & Meridians)
        double[] gridLats = {11.25, 11.30, 11.35, 11.40, 11.45};
        double[] gridLons = {77.45, 77.55, 77.65, 77.75};

        for (double glat : gridLats) {
            double gy = projectY(glat, height, padY);
            out.println("<line x1=\"" + (padX - 20) + "\" y1=\"" + gy + "\" x2=\"" + (width - padX + 20) + "\" y2=\"" + gy + "\" stroke=\"#e2e8f0\" stroke-dasharray=\"3,3\" stroke-width=\"1\" />");
            out.println("<text x=\"" + (padX - 25) + "\" y=\"" + (gy + 4) + "\" font-size=\"9\" fill=\"#94a3b8\" text-anchor=\"end\">" + String.format("%.2f°N", glat) + "</text>");
        }

        for (double glon : gridLons) {
            double gx = projectX(glon, width, padX);
            out.println("<line x1=\"" + gx + "\" y1=\"" + (padY - 20) + "\" x2=\"" + gx + "\" y2=\"" + (height - padY + 10) + "\" stroke=\"#e2e8f0\" stroke-dasharray=\"3,3\" stroke-width=\"1\" />");
            out.println("<text x=\"" + gx + "\" y=\"" + (height - padY + 24) + "\" font-size=\"9\" fill=\"#94a3b8\" text-anchor=\"middle\">" + String.format("%.2f°E", glon) + "</text>");
        }

        // 3. Natural Geographic Feature: Cauvery & Bhavani River Corridor in Erode
        double rx1 = projectX(77.64, width, padX);
        double ry1 = projectY(11.47, height, padY);
        double rx2 = projectX(77.68, width, padX);
        double ry2 = projectY(11.44, height, padY);
        double rx3 = projectX(77.72, width, padX);
        double ry3 = projectY(11.38, height, padY);
        double rx4 = projectX(77.77, width, padX);
        double ry4 = projectY(11.32, height, padY);

        out.println("<path d=\"M " + rx1 + " " + ry1 + " Q " + rx2 + " " + ry2 + ", " + rx3 + " " + ry3 + " T " + rx4 + " " + ry4 + "\" fill=\"none\" stroke=\"url(#waterGrad)\" stroke-width=\"12\" stroke-linecap=\"round\" opacity=\"0.75\" />");
        out.println("<text x=\"" + (rx3 + 8) + "\" y=\"" + (ry3 - 6) + "\" font-size=\"10\" font-style=\"italic\" fill=\"#0284c7\" transform=\"rotate(35 " + rx3 + " " + ry3 + ")\">Cauvery River</text>");

        // 4. Background Road Network (Verified Edges)
        for (RoadEdge edge : allEdges) {
            PickupStop from = stopsMap.get(edge.getFromStop());
            PickupStop to = stopsMap.get(edge.getToStop());
            if (from != null && to != null) {
                double x1 = projectX(from.getLongitude(), width, padX);
                double y1 = projectY(from.getLatitude(), height, padY);
                double x2 = projectX(to.getLongitude(), width, padX);
                double y2 = projectY(to.getLatitude(), height, padY);

                String strokeColor = "#cbd5e1";
                double strokeWidth = 2.0;
                String dash = "";

                if ("HIGHWAY".equalsIgnoreCase(edge.getRoadType())) {
                    strokeColor = "#fed7aa"; // light orange
                    strokeWidth = 3.5;
                } else if ("SUBURBAN".equalsIgnoreCase(edge.getRoadType())) {
                    strokeColor = "#e2e8f0";
                    strokeWidth = 1.5;
                    dash = "stroke-dasharray=\"4,3\"";
                }

                out.println("<line x1=\"" + x1 + "\" y1=\"" + y1 + "\" x2=\"" + x2 + "\" y2=\"" + y2 + "\" stroke=\"" + strokeColor + "\" stroke-width=\"" + strokeWidth + "\" " + dash + " stroke-linecap=\"round\" />");
            }
        }

        // 5. Active Planned Route Polyline
        Set<String> activeRouteStopIds = new HashSet<>();
        if (optResult != null && !optResult.getStopSequence().isEmpty()) {
            List<RouteStopInfo> seq = optResult.getStopSequence();
            for (RouteStopInfo rsi : seq) {
                activeRouteStopIds.add(rsi.getStopId());
            }

            // Draw route lines connecting consecutive stops in calculated sequence
            for (int i = 0; i < seq.size() - 1; i++) {
                RouteStopInfo curr = seq.get(i);
                RouteStopInfo next = seq.get(i + 1);

                double x1 = projectX(curr.getLongitude(), width, padX);
                double y1 = projectY(curr.getLatitude(), height, padY);
                double x2 = projectX(next.getLongitude(), width, padX);
                double y2 = projectY(next.getLatitude(), height, padY);

                // Thick glowing base
                out.println("<line x1=\"" + x1 + "\" y1=\"" + y1 + "\" x2=\"" + x2 + "\" y2=\"" + y2 + "\" stroke=\"#93c5fd\" stroke-width=\"6\" stroke-linecap=\"round\" opacity=\"0.6\" />");
                // Solid route line with arrow
                out.println("<line x1=\"" + x1 + "\" y1=\"" + y1 + "\" x2=\"" + x2 + "\" y2=\"" + y2 + "\" stroke=\"#2563eb\" stroke-width=\"3.5\" stroke-linecap=\"round\" marker-end=\"url(#arrow)\" />");
            }
        }

        // 6. Stop Markers
        for (PickupStop stop : allStops) {
            double sx = projectX(stop.getLongitude(), width, padX);
            double sy = projectY(stop.getLatitude(), height, padY);

            boolean isDepot = "C00".equals(stop.getStopId());
            boolean onActiveRoute = activeRouteStopIds.contains(stop.getStopId());

            if (isDepot) {
                // Campus Depot Icon
                out.println("<circle cx=\"" + sx + "\" cy=\"" + sy + "\" r=\"14\" fill=\"#d97706\" stroke=\"#ffffff\" stroke-width=\"3\" filter=\"url(#cardShadow)\" />");
                out.println("<text x=\"" + sx + "\" y=\"" + (sy + 5) + "\" font-size=\"12\" fill=\"#ffffff\" font-weight=\"bold\" text-anchor=\"middle\">&#127979;</text>");
                out.println("<rect x=\"" + (sx - 75) + "\" y=\"" + (sy + 18) + "\" width=\"150\" height=\"18\" rx=\"4\" fill=\"#78350f\" opacity=\"0.9\" />");
                out.println("<text x=\"" + sx + "\" y=\"" + (sy + 31) + "\" font-size=\"10\" font-weight=\"bold\" fill=\"#ffffff\" text-anchor=\"middle\">Depot (" + stop.getStopName() + ")</text>");
            } else if (onActiveRoute) {
                // Active Scheduled Stop on Route
                int stopSeqNum = 0;
                String arrivalTime = "";
                if (optResult != null) {
                    for (RouteStopInfo rsi : optResult.getStopSequence()) {
                        if (rsi.getStopId().equals(stop.getStopId())) {
                            stopSeqNum = rsi.getSequence();
                            arrivalTime = rsi.getArrivalTime();
                            break;
                        }
                    }
                }

                out.println("<circle cx=\"" + sx + "\" cy=\"" + sy + "\" r=\"12\" fill=\"#2563eb\" stroke=\"#ffffff\" stroke-width=\"2.5\" filter=\"url(#cardShadow)\" />");
                out.println("<text x=\"" + sx + "\" y=\"" + (sy + 4) + "\" font-size=\"10\" font-weight=\"bold\" fill=\"#ffffff\" text-anchor=\"middle\">" + (stopSeqNum > 0 ? String.valueOf(stopSeqNum) : "•") + "</text>");

                // Place Name and Arrival Time Tag
                double textY = (sy < height / 2) ? (sy - 16) : (sy + 22);
                out.println("<rect x=\"" + (sx - 65) + "\" y=\"" + (textY - 11) + "\" width=\"130\" height=\"22\" rx=\"4\" fill=\"#1e293b\" opacity=\"0.9\" />");
                out.println("<text x=\"" + sx + "\" y=\"" + (textY + 1) + "\" font-size=\"9\" font-weight=\"bold\" fill=\"#f8fafc\" text-anchor=\"middle\">" + escapeXml(stop.getStopName()) + "</text>");
                out.println("<text x=\"" + sx + "\" y=\"" + (textY + 9) + "\" font-size=\"7.5\" fill=\"#38bdf8\" text-anchor=\"middle\">" + arrivalTime + "</text>");
            } else {
                // Bypassed/Skipped Stop (No students waiting today)
                out.println("<circle cx=\"" + sx + "\" cy=\"" + sy + "\" r=\"6\" fill=\"#cbd5e1\" stroke=\"#64748b\" stroke-width=\"1\" />");
                out.println("<text x=\"" + sx + "\" y=\"" + (sy - 9) + "\" font-size=\"8.5\" fill=\"#64748b\" text-anchor=\"middle\">" + escapeXml(stop.getStopName()) + " (Skipped)</text>");
            }
        }

        // 7. Real-Time Bus GPS Location Marker
        if (gps != null) {
            double bx = projectX(gps.getLongitude(), width, padX);
            double by = projectY(gps.getLatitude(), height, padY);

            String statusColor = "#10b981"; // LIVE
            if ("STALE".equalsIgnoreCase(gps.getStatus())) {
                statusColor = "#f59e0b"; // STALE
            } else if ("GPS UNAVAILABLE".equalsIgnoreCase(gps.getStatus())) {
                statusColor = "#ef4444"; // UNAVAILABLE
            }

            // Pulsing GPS halo ring
            out.println("<circle cx=\"" + bx + "\" cy=\"" + by + "\" r=\"20\" fill=\"" + statusColor + "\" opacity=\"0.2\" />");
            out.println("<circle cx=\"" + bx + "\" cy=\"" + by + "\" r=\"14\" fill=\"" + statusColor + "\" stroke=\"#ffffff\" stroke-width=\"2.5\" filter=\"url(#cardShadow)\" />");
            // Bus Vehicle Icon
            out.println("<text x=\"" + bx + "\" y=\"" + (by + 4) + "\" font-size=\"12\" fill=\"#ffffff\" text-anchor=\"middle\">&#128652;</text>");

            // Bus Floating Info Badge
            double tagY = (by > 100) ? (by - 26) : (by + 32);
            String label = busId + " (" + gps.getStatus() + ") | " + gps.getSpeed() + " km/h";
            if (gps.isTestSimulation()) {
                label += " [TEST SIMULATION]";
            }

            out.println("<g filter=\"url(#cardShadow)\">");
            out.println("  <rect x=\"" + (bx - 90) + "\" y=\"" + (tagY - 14) + "\" width=\"180\" height=\"20\" rx=\"5\" fill=\"#0f172a\" stroke=\"" + statusColor + "\" stroke-width=\"1.5\" />");
            out.println("  <circle cx=\"" + (bx - 78) + "\" cy=\"" + (tagY - 4) + "\" r=\"4\" fill=\"" + statusColor + "\" />");
            out.println("  <text x=\"" + (bx - 70) + "\" y=\"" + tagY + "\" font-size=\"8.5\" font-weight=\"bold\" fill=\"#ffffff\">" + escapeXml(label) + "</text>");
            out.println("</g>");
        }

        // 8. Top Status Header Overlay
        out.println("<rect x=\"12\" y=\"12\" width=\"" + (width - 24) + "\" height=\"44\" rx=\"6\" fill=\"#0f172a\" opacity=\"0.95\" />");
        out.println("<text x=\"26\" y=\"38\" font-size=\"13\" font-weight=\"bold\" fill=\"#ffffff\">&#128652; " + busId + " Geographical Route &amp; GPS Tracker</text>");

        String durationInfo = (optResult != null) ? (optResult.getTotalTravelTimeMinutes() + " mins | " + String.format("%.1f", optResult.getTotalDistanceKm()) + " km | Ret: " + optResult.getReturnDepotTime()) : "Calculating...";
        out.println("<text x=\"" + (width / 2) + "\" y=\"38\" font-size=\"11\" fill=\"#94a3b8\" text-anchor=\"middle\">Trip Duration: <tspan font-weight=\"bold\" fill=\"#38bdf8\">" + durationInfo + "</tspan></text>");

        String gpsStatusText = (gps != null) ? gps.getStatus() : "GPS UNAVAILABLE";
        String gpsColor = (gps != null && "LIVE".equals(gps.getStatus())) ? "#10b981" : ((gps != null && "STALE".equals(gps.getStatus())) ? "#f59e0b" : "#ef4444");
        out.println("<rect x=\"" + (width - 170) + "\" y=\"22\" width=\"155\" height=\"24\" rx=\"4\" fill=\"" + gpsColor + "\" />");
        out.println("<text x=\"" + (width - 92) + "\" y=\"38\" font-size=\"10\" font-weight=\"bold\" fill=\"#ffffff\" text-anchor=\"middle\">GPS: " + gpsStatusText + "</text>");

        // 9. Bottom Map Legend
        out.println("<g transform=\"translate(" + (width - 450) + ", " + (height - 35) + ")\">");
        out.println("  <rect width=\"435\" height=\"26\" rx=\"4\" fill=\"#ffffff\" opacity=\"0.92\" stroke=\"#cbd5e1\" />");
        // Route line legend
        out.println("  <line x1=\"15\" y1=\"13\" x2=\"35\" y2=\"13\" stroke=\"#2563eb\" stroke-width=\"3\" />");
        out.println("  <text x=\"40\" y=\"16\" font-size=\"9\" fill=\"#334155\">Route</text>");
        // Stop disc legend
        out.println("  <circle cx=\"85\" cy=\"13\" r=\"5\" fill=\"#2563eb\" />");
        out.println("  <text x=\"95\" y=\"16\" font-size=\"9\" fill=\"#334155\">Pickup Stop</text>");
        // Depot icon legend
        out.println("  <circle cx=\"165\" cy=\"13\" r=\"5\" fill=\"#d97706\" />");
        out.println("  <text x=\"175\" y=\"16\" font-size=\"9\" fill=\"#334155\">Depot</text>");
        // Bus marker legend
        out.println("  <circle cx=\"220\" cy=\"13\" r=\"6\" fill=\"#10b981\" />");
        out.println("  <text x=\"230\" y=\"16\" font-size=\"9\" fill=\"#334155\">Live Bus GPS</text>");
        // Scale Bar
        out.println("  <line x1=\"320\" y1=\"13\" x2=\"370\" y2=\"13\" stroke=\"#0f172a\" stroke-width=\"2\" />");
        out.println("  <text x=\"376\" y=\"16\" font-size=\"9\" font-weight=\"bold\" fill=\"#0f172a\">5 KM</text>");
        out.println("</g>");

        out.println("</svg>");
    }

    private static double projectX(double lon, int width, int padX) {
        return padX + (lon - MIN_LON) / (MAX_LON - MIN_LON) * (width - 2 * padX);
    }

    private static double projectY(double lat, int height, int padY) {
        return height - padY - (lat - MIN_LAT) / (MAX_LAT - MIN_LAT) * (height - 2 * padY);
    }

    private static String escapeXml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }
}
