package com.servlets;

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 *
 * @author Lorenzo
 */
@WebServlet(name = "Logout", urlPatterns = {"/logout"})
public class Logout extends HttpServlet {
    /**
     * Handles the HTTP <code>GET</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String message = null;
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
            message = "Successifully logged out.";
        }
        if (message != null) {
            request.setAttribute("message", message);
        }
        request.getRequestDispatcher("login.jsp").forward(request, response);
    }
}
