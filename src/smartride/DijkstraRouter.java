package smartride;

import java.io.Serializable;
import java.util.*;
import smartride.Models.CollegeBus;
import smartride.Models.PickupStop;
import smartride.Models.RouteOptimizationResult;
import smartride.Models.RouteStopInfo;
import smartride.Models.Student;

public class DijkstraRouter {

    public static final String DEFAULT_START_TIME = "07:30 AM";
    public static final int BASE_TIME_MINUTES = 7 * 60 + 30; // 450 minutes from midnight (07:30 AM)

    public static class GraphEdge implements Serializable {
        private static final long serialVersionUID = 1L;
        private final String destinationStopId;
        private final int travelTimeMinutes;
        private final double distanceKm;
        private final String roadType;

        public GraphEdge(String destinationStopId, int travelTimeMinutes, double distanceKm, String roadType) {
            this.destinationStopId = destinationStopId;
            this.travelTimeMinutes = Math.max(1, travelTimeMinutes);
            this.distanceKm = Math.max(0.1, distanceKm);
            this.roadType = roadType != null ? roadType.toUpperCase() : "ARTERIAL";
        }

        public String getDestinationStopId() { return destinationStopId; }
        public int getTravelTimeMinutes() { return travelTimeMinutes; }
        public double getDistanceKm() { return distanceKm; }
        public String getRoadType() { return roadType; }
    }

    public static class Graph implements Serializable {
        private static final long serialVersionUID = 1L;
        private final Map<String, List<GraphEdge>> adjacencyList = new HashMap<>();
        private final Set<String> vertices = new HashSet<>();

        public void addVertex(String stopId) {
            if (stopId != null && !stopId.trim().isEmpty()) {
                String id = stopId.trim();
                vertices.add(id);
                adjacencyList.putIfAbsent(id, new ArrayList<>());
            }
        }

        public void addEdge(String fromStopId, String toStopId, int travelTimeMinutes, double distanceKm, String roadType, boolean bidirectional) {
            addVertex(fromStopId);
            addVertex(toStopId);

            adjacencyList.get(fromStopId).add(new GraphEdge(toStopId, travelTimeMinutes, distanceKm, roadType));
            if (bidirectional) {
                adjacencyList.get(toStopId).add(new GraphEdge(fromStopId, travelTimeMinutes, distanceKm, roadType));
            }
        }

        public List<GraphEdge> getAdjacentEdges(String stopId) {
            return adjacencyList.getOrDefault(stopId, Collections.emptyList());
        }

        public Set<String> getAllVertices() {
            return Collections.unmodifiableSet(vertices);
        }

        public boolean containsVertex(String stopId) {
            return vertices.contains(stopId);
        }
    }

    public static class DijkstraResult {
        private final List<String> path;
        private final int totalTravelTimeMinutes;
        private final double totalDistanceKm;
        private final boolean reachable;

        public DijkstraResult(List<String> path, int totalTravelTimeMinutes, double totalDistanceKm, boolean reachable) {
            this.path = path != null ? path : Collections.emptyList();
            this.totalTravelTimeMinutes = totalTravelTimeMinutes;
            this.totalDistanceKm = totalDistanceKm;
            this.reachable = reachable;
        }

        public List<String> getPath() { return path; }
        public int getTotalTravelTimeMinutes() { return totalTravelTimeMinutes; }
        public double getTotalDistanceKm() { return totalDistanceKm; }
        public boolean isReachable() { return reachable; }
    }

    private static class NodeEntry implements Comparable<NodeEntry> {
        final String stopId;
        final int distance;

        NodeEntry(String stopId, int distance) {
            this.stopId = stopId;
            this.distance = distance;
        }

        @Override
        public int compareTo(NodeEntry other) {
            if (this.distance != other.distance) {
                return Integer.compare(this.distance, other.distance);
            }
            return this.stopId.compareTo(other.stopId);
        }
    }

    /**
     * Dijkstra's Algorithm using Java's PriorityQueue min-heap.
     * Computes the shortest travel duration path from startStopId to targetStopId.
     */
    public static DijkstraResult findShortestPath(Graph graph, String startStopId, String targetStopId, CollegeBus bus) {
        if (!graph.containsVertex(startStopId) || !graph.containsVertex(targetStopId)) {
            return new DijkstraResult(Collections.emptyList(), Integer.MAX_VALUE, 0.0, false);
        }

        if (startStopId.equals(targetStopId)) {
            return new DijkstraResult(Collections.singletonList(startStopId), 0, 0.0, true);
        }

        Map<String, Integer> distances = new HashMap<>();
        Map<String, Double> distancesKm = new HashMap<>();
        Map<String, String> predecessors = new HashMap<>();
        PriorityQueue<NodeEntry> minHeap = new PriorityQueue<>();
        HashSet<String> settled = new HashSet<>();

        for (String v : graph.getAllVertices()) {
            distances.put(v, Integer.MAX_VALUE);
            distancesKm.put(v, Double.MAX_VALUE);
        }

        distances.put(startStopId, 0);
        distancesKm.put(startStopId, 0.0);
        minHeap.add(new NodeEntry(startStopId, 0));

        while (!minHeap.isEmpty()) {
            NodeEntry current = minHeap.poll();
            String u = current.stopId;

            if (settled.contains(u)) continue;
            settled.add(u);

            if (u.equals(targetStopId)) {
                break;
            }

            for (GraphEdge edge : graph.getAdjacentEdges(u)) {
                String v = edge.getDestinationStopId();
                if (settled.contains(v)) continue;

                // Enforce road clearance restrictions (e.g. Standard buses cannot enter NARROW lanes)
                if (bus != null && !bus.canUseRoad(edge.getRoadType())) {
                    continue;
                }

                int edgeWeight = edge.getTravelTimeMinutes();
                int newDist = distances.get(u) + edgeWeight;

                if (newDist < distances.get(v)) {
                    distances.put(v, newDist);
                    distancesKm.put(v, distancesKm.get(u) + edge.getDistanceKm());
                    predecessors.put(v, u);
                    minHeap.add(new NodeEntry(v, newDist));
                }
            }
        }

        if (distances.get(targetStopId) == Integer.MAX_VALUE) {
            return new DijkstraResult(Collections.emptyList(), Integer.MAX_VALUE, 0.0, false);
        }

        // Reconstruct shortest path using ArrayDeque
        ArrayDeque<String> pathDeque = new ArrayDeque<>();
        String step = targetStopId;
        while (step != null) {
            pathDeque.addFirst(step);
            step = predecessors.get(step);
        }

        return new DijkstraResult(new ArrayList<>(pathDeque), distances.get(targetStopId), distancesKm.get(targetStopId), true);
    }

    public static String formatTimeFromMinutes(int minutesAfterMidnight) {
        int hours = (minutesAfterMidnight / 60) % 24;
        int mins = minutesAfterMidnight % 60;
        String ampm = hours >= 12 ? "PM" : "AM";
        int displayHour = hours % 12;
        if (displayHour == 0) displayHour = 12;
        return String.format("%02d:%02d %s", displayHour, mins, ampm);
    }

    /**
     * Stop-Ordering Strategy & Dynamic Routing Engine.
     * Computes the ordered sequence of pickups visiting only stops with scheduled students,
     * calculates chronologically strictly increasing arrival times starting at depot departure,
     * and includes the depot return leg.
     */
    public static RouteOptimizationResult planRoute(Graph graph, CollegeBus bus, String depotId,
                                                    Map<String, PickupStop> stops, List<Student> students, String currentDate) {
        List<RouteStopInfo> stopSequence = new ArrayList<>();
        ArrayList<String> route = new ArrayList<>();
        List<String> visitedStops = new ArrayList<>();
        List<String> skippedEmptyStops = new ArrayList<>();
        Map<String, String> stopEstimatedPickupTimes = new HashMap<>();

        // 1. Identify all candidate pickup stops that have at least one scheduled student waiting
        HashSet<String> neededStops = new HashSet<>();
        for (Student s : students) {
            String effBus = s.getEffectiveBusId(currentDate);
            if (effBus != null && effBus.equals(bus.getBusId())) {
                // Check if absent
                if (s.isAbsentOn(currentDate)) {
                    continue;
                }
                String effStop = s.getEffectiveStopId(currentDate);
                if (effStop != null && !effStop.trim().isEmpty() && !effStop.equals(depotId)) {
                    neededStops.add(effStop.trim());
                }
            }
        }

        // 2. Determine bypassed stops (stops with 0 scheduled students)
        for (String stopId : stops.keySet()) {
            if (!stopId.equals(depotId) && !neededStops.contains(stopId)) {
                skippedEmptyStops.add(stopId);
            }
        }

        PickupStop depotStop = stops.get(depotId);
        String depotName = depotStop != null ? depotStop.getStopName() : "Campus Depot";
        double depotLat = depotStop != null ? depotStop.getLatitude() : 11.3180;
        double depotLon = depotStop != null ? depotStop.getLongitude() : 77.6740;
        String depotRoadType = depotStop != null ? depotStop.getRoadType() : "ARTERIAL";

        int seq = 1;
        int cumulativeMinutes = 0;
        double cumulativeDistanceKm = 0.0;

        // Sequence 1: Depot departure at 07:30 AM
        RouteStopInfo startDepot = new RouteStopInfo(
                seq++, depotId, depotName, depotLat, depotLon, depotRoadType,
                0, 0.0, 0, DEFAULT_START_TIME, true, false
        );
        stopSequence.add(startDepot);
        route.add(depotId);
        stopEstimatedPickupTimes.put(depotId, DEFAULT_START_TIME);

        String currentStop = depotId;
        Set<String> candidatePool = new HashSet<>(neededStops);

        // 3. Greedy Nearest-Neighbor Stop-Ordering using Dijkstra's shortest paths
        while (!candidatePool.isEmpty()) {
            String bestNextStop = null;
            int minTime = Integer.MAX_VALUE;
            DijkstraResult bestDijkstra = null;

            for (String candidate : candidatePool) {
                DijkstraResult dr = findShortestPath(graph, currentStop, candidate, bus);
                if (dr.isReachable()) {
                    if (dr.getTotalTravelTimeMinutes() < minTime) {
                        minTime = dr.getTotalTravelTimeMinutes();
                        bestNextStop = candidate;
                        bestDijkstra = dr;
                    } else if (dr.getTotalTravelTimeMinutes() == minTime && bestNextStop != null) {
                        // Deterministic secondary tie-breaker
                        if (candidate.compareTo(bestNextStop) < 0) {
                            bestNextStop = candidate;
                            bestDijkstra = dr;
                        }
                    }
                }
            }

            if (bestNextStop == null || bestDijkstra == null) {
                break; // remaining candidate stops are unreachable from currentStop
            }

            int legTravelTime = bestDijkstra.getTotalTravelTimeMinutes();
            double legDist = bestDijkstra.getTotalDistanceKm();
            cumulativeMinutes += legTravelTime;
            cumulativeDistanceKm += legDist;
            String arrivalTime = formatTimeFromMinutes(BASE_TIME_MINUTES + cumulativeMinutes);

            PickupStop ps = stops.get(bestNextStop);
            String stopName = ps != null ? ps.getStopName() : bestNextStop;
            double stopLat = ps != null ? ps.getLatitude() : depotLat;
            double stopLon = ps != null ? ps.getLongitude() : depotLon;
            String roadType = ps != null ? ps.getRoadType() : "ARTERIAL";

            RouteStopInfo rsi = new RouteStopInfo(
                    seq++, bestNextStop, stopName, stopLat, stopLon, roadType,
                    legTravelTime, legDist, cumulativeMinutes, arrivalTime, false, false
            );
            stopSequence.add(rsi);
            route.add(bestNextStop);
            visitedStops.add(bestNextStop);
            stopEstimatedPickupTimes.put(bestNextStop, arrivalTime);

            currentStop = bestNextStop;
            candidatePool.remove(bestNextStop);
        }

        // 4. Return journey back to Campus Depot
        if (!visitedStops.isEmpty() && !currentStop.equals(depotId)) {
            DijkstraResult returnPath = findShortestPath(graph, currentStop, depotId, bus);
            if (returnPath.isReachable()) {
                int legTravelTime = returnPath.getTotalTravelTimeMinutes();
                double legDist = returnPath.getTotalDistanceKm();
                cumulativeMinutes += legTravelTime;
                cumulativeDistanceKm += legDist;
                String returnTime = formatTimeFromMinutes(BASE_TIME_MINUTES + cumulativeMinutes);

                RouteStopInfo returnDepot = new RouteStopInfo(
                        seq++, depotId, depotName + " (Return)", depotLat, depotLon, depotRoadType,
                        legTravelTime, legDist, cumulativeMinutes, returnTime, true, true
                );
                stopSequence.add(returnDepot);
                route.add(depotId);
            }
        }

        return new RouteOptimizationResult(
                stopSequence, route, cumulativeMinutes, cumulativeDistanceKm,
                visitedStops, skippedEmptyStops, stopEstimatedPickupTimes
        );
    }
}
