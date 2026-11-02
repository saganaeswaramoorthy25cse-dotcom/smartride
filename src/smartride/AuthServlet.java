package smartride;

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import smartride.Models.User;
import smartride.Models.UserRole;

@WebServlet({"/login", "/logout"})
public class AuthServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String servletPath = request.getServletPath();
        if ("/logout".equals(servletPath)) {
            HttpSession session = request.getSession(false);
            if (session != null) {
                session.invalidate();
            }
            response.sendRedirect(request.getContextPath() + "/index.jsp?msg=You+have+been+logged+out+successfully.");
            return;
        }
        response.sendRedirect(request.getContextPath() + "/index.jsp");
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        String username = request.getParameter("username");
        String password = request.getParameter("password");
        String roleStr = request.getParameter("role");

        if (username == null || username.trim().isEmpty() ||
            password == null || password.trim().isEmpty() ||
            roleStr == null || roleStr.trim().isEmpty()) {
            response.sendRedirect(request.getContextPath() + "/index.jsp?error=All+login+fields+are+required.");
            return;
        }

        UserRole expectedRole;
        try {
            expectedRole = UserRole.valueOf(roleStr.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            response.sendRedirect(request.getContextPath() + "/index.jsp?error=Invalid+role+specified.");
            return;
        }

        TransportDAO dao = new TransportDAO();
        User user = dao.authenticate(username, password, expectedRole);

        if (user == null) {
            response.sendRedirect(request.getContextPath() + "/index.jsp?error=Invalid+" + expectedRole.name() + "+credentials.+Please+verify+and+try+again.");
            return;
        }

        // Establish authenticated session
        HttpSession session = request.getSession(true);
        session.setAttribute("userId", user.getUserId());
        session.setAttribute("userRole", user.getRole().name());
        session.setAttribute("userName", user.getName());
        session.setMaxInactiveInterval(3600); // 1 hour session

        // Redirect to appropriate dashboard
        switch (user.getRole()) {
            case STUDENT:
                response.sendRedirect(request.getContextPath() + "/student_dashboard.jsp");
                break;
            case DRIVER:
                response.sendRedirect(request.getContextPath() + "/driver_dashboard.jsp");
                break;
            case ADMIN:
                response.sendRedirect(request.getContextPath() + "/admin_dashboard.jsp");
                break;
            default:
                session.invalidate();
                response.sendRedirect(request.getContextPath() + "/index.jsp?error=Unknown+role.");
                break;
        }
    }
}
