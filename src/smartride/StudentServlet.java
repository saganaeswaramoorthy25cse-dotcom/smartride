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
import smartride.Models.Student;

@WebServlet({"/register-student", "/student/absence", "/student/temp-change"})
public class StudentServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        String servletPath = request.getServletPath();
        TransportDAO dao = new TransportDAO();

        try {
            if ("/register-student".equals(servletPath)) {
                handleRegistration(request, response, dao);
            } else if ("/student/absence".equals(servletPath)) {
                handleAbsence(request, response, dao);
            } else if ("/student/temp-change".equals(servletPath)) {
                handleTempChange(request, response, dao);
            } else {
                response.sendRedirect(request.getContextPath() + "/index.jsp");
            }
        } catch (Exception e) {
            String encodedErr = URLEncoder.encode(e.getMessage() != null ? e.getMessage() : "An error occurred", StandardCharsets.UTF_8.name());
            if ("/register-student".equals(servletPath)) {
                response.sendRedirect(request.getContextPath() + "/register_student.jsp?error=" + encodedErr);
            } else {
                response.sendRedirect(request.getContextPath() + "/student_dashboard.jsp?error=" + encodedErr);
            }
        }
    }

    private void handleRegistration(HttpServletRequest request, HttpServletResponse response, TransportDAO dao)
            throws Exception {
        String rollNo = request.getParameter("rollNo");
        String name = request.getParameter("name");
        String password = request.getParameter("password");
        String department = request.getParameter("department");
        String stopId = request.getParameter("stopId");
        String contact = request.getParameter("contact");

        if (rollNo == null || rollNo.trim().isEmpty() ||
            name == null || name.trim().isEmpty() ||
            password == null || password.trim().isEmpty() ||
            stopId == null || stopId.trim().isEmpty()) {
            throw new IllegalArgumentException("Roll Number, Name, Password, and Pickup Stop are required.");
        }

        dao.registerStudent(rollNo, name, password, department, stopId, contact);
        response.sendRedirect(request.getContextPath() + "/index.jsp?msg=" +
                URLEncoder.encode("Registration successful for " + rollNo.toUpperCase() + ". Please sign in below.", StandardCharsets.UTF_8.name()));
    }

    private void handleAbsence(HttpServletRequest request, HttpServletResponse response, TransportDAO dao)
            throws Exception {
        HttpSession session = request.getSession(false);
        if (session == null || !"STUDENT".equals(session.getAttribute("userRole"))) {
            response.sendRedirect(request.getContextPath() + "/index.jsp?error=Student+login+required.");
            return;
        }

        String userId = (String) session.getAttribute("userId");
        Student student = dao.getStudentByUserId(userId);
        if (student == null) {
            response.sendRedirect(request.getContextPath() + "/logout");
            return;
        }

        String action = request.getParameter("action");
        String date = request.getParameter("date");
        if (date == null || date.trim().isEmpty()) {
            date = LocalDate.now().plusDays(1).toString();
        }

        boolean isAbsent = "mark_absent".equalsIgnoreCase(action);
        dao.setStudentAbsence(student.getRollNo(), date, isAbsent);

        String msg = isAbsent
                ? "Absence recorded for " + date + ". Unneeded pickup stops will be dynamically skipped."
                : "Attendance restored to SCHEDULED PRESENT for " + date + ".";

        response.sendRedirect(request.getContextPath() + "/student_dashboard.jsp?msg=" +
                URLEncoder.encode(msg, StandardCharsets.UTF_8.name()));
    }

    private void handleTempChange(HttpServletRequest request, HttpServletResponse response, TransportDAO dao)
            throws Exception {
        HttpSession session = request.getSession(false);
        if (session == null || !"STUDENT".equals(session.getAttribute("userRole"))) {
            response.sendRedirect(request.getContextPath() + "/index.jsp?error=Student+login+required.");
            return;
        }

        String userId = (String) session.getAttribute("userId");
        Student student = dao.getStudentByUserId(userId);
        if (student == null) {
            response.sendRedirect(request.getContextPath() + "/logout");
            return;
        }

        String date = request.getParameter("date");
        String newStopId = request.getParameter("newStopId");
        String newBusId = request.getParameter("newBusId");
        String reason = request.getParameter("reason");

        if (date == null || date.trim().isEmpty() || newStopId == null || newStopId.trim().isEmpty()) {
            throw new IllegalArgumentException("Travel date and new pickup stop are required.");
        }

        if (dao.getStop(newStopId) == null) {
            throw new IllegalArgumentException("Selected pickup stop does not exist in the road network.");
        }

        dao.requestTemporaryChange(
                student.getUserId(),
                student.getRollNo(),
                student.getName(),
                date,
                newStopId,
                newBusId,
                reason
        );

        String stopName = dao.getStopName(newStopId);
        response.sendRedirect(request.getContextPath() + "/student_dashboard.jsp?msg=" +
                URLEncoder.encode("One-day temporary stop change to " + stopName + " activated for " + date + ". Route recalculated.", StandardCharsets.UTF_8.name()));
    }
}
